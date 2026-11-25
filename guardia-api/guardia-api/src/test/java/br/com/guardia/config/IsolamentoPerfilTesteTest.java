package br.com.guardia.config;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import br.com.guardia.service.FirebaseNotificationService;
import br.com.guardia.service.NotificacaoService;
import br.com.guardia.service.NotificacaoSimuladaService;
import br.com.guardia.service.NotificacaoTestePushService;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class IsolamentoPerfilTesteTest {

    @Test
    void permitePausarWorkerSemIniciarFirebaseOuProcessarTrabalhos() {
        try (var contexto = new AnnotationConfigApplicationContext()) {
            contexto.getEnvironment().getPropertySources().addFirst(new MapPropertySource("fila-pausada",
                    Map.of("guardia.push.fila.habilitada", "false")));
            contexto.register(br.com.guardia.service.ProcessadorFilaPush.class);
            contexto.refresh();
            assertTrue(contexto.getBeansOfType(br.com.guardia.service.ProcessadorFilaPush.class).isEmpty());
        }
    }

    @Test
    void perfilPushMantemIsolamentoEPerfilTesteSemprePrevalece() {
        var push = candidatos("teste-push");
        assertTrue(push.contains(DataSourceTesteConfig.class.getName()));
        assertTrue(push.contains(FirebaseTestePushConfig.class.getName()));
        assertTrue(push.contains(NotificacaoTestePushService.class.getName()));
        assertFalse(push.contains(FirebaseConfig.class.getName()));
        assertFalse(push.contains(FirebaseNotificationService.class.getName()));
        var ambos = candidatos("teste", "teste-push");
        assertTrue(ambos.contains(NotificacaoSimuladaService.class.getName()));
        assertFalse(ambos.contains(FirebaseTestePushConfig.class.getName()));
        assertFalse(ambos.contains(NotificacaoTestePushService.class.getName()));
    }

    @Test
    void perfilPushRejeitaDdlInseguroSemConectar() {
        for (String ddl : new String[]{"update", "create", "create-drop", "none"}) {
            try (var contexto = contextoBanco("jdbc:mysql://localhost/guardia_teste",
                    "guardia_teste_app", "ficticio")) {
                contexto.getEnvironment().setActiveProfiles("teste-push");
                contexto.getEnvironment().getPropertySources().addFirst(new MapPropertySource("ddl",
                        Map.of("spring.jpa.hibernate.ddl-auto", ddl)));
                assertThrows(BeanCreationException.class, contexto::refresh);
            }
        }
    }

    @Test
    void perfilPushPermiteValidateSemAbrirPool() {
        try (var contexto = contextoBanco("jdbc:mysql://localhost/guardia_teste", "guardia_teste_app", "ficticio")) {
            contexto.getEnvironment().setActiveProfiles("teste-push");
            contexto.getEnvironment().getPropertySources().addFirst(new MapPropertySource("ddl",
                    Map.of("spring.jpa.hibernate.ddl-auto", "validate", "spring.sql.init.mode", "never")));
            contexto.refresh();
            assertNull(contexto.getBean(HikariDataSource.class).getHikariPoolMXBean());
        }
    }

    @Test
    void perfilTesteExcluiFirebaseRealMesmoJuntoComOutroPerfil() {
        Set<String> candidatos = candidatos("teste", "dev-local");
        assertFalse(candidatos.contains(FirebaseConfig.class.getName()));
        assertFalse(candidatos.contains(FirebaseNotificationService.class.getName()));
        assertTrue(candidatos.contains(NotificacaoSimuladaService.class.getName()));

        try (var contexto = new AnnotationConfigApplicationContext()) {
            contexto.getEnvironment().setActiveProfiles("teste", "dev-local");
            contexto.register(FirebaseConfig.class, FirebaseNotificationService.class,
                    NotificacaoSimuladaService.class);
            contexto.refresh();

            assertTrue(contexto.getBeansOfType(FirebaseConfig.class).isEmpty());
            assertEquals(1, contexto.getBeansOfType(NotificacaoService.class).size());
            assertEquals("envio-simulado", contexto.getBean(NotificacaoService.class)
                    .enviarNotificacao("registro-ficticio", "Título de teste", "Mensagem fictícia"));
        }
    }

    @Test
    void outrosPerfisMantemBeansReaisSemInicializaLos() {
        for (String[] perfis : new String[][]{ {}, { "dev-local" } }) {
            Set<String> candidatos = candidatos(perfis);
            assertTrue(candidatos.contains(FirebaseConfig.class.getName()));
            assertTrue(candidatos.contains(FirebaseNotificationService.class.getName()));
            assertFalse(candidatos.contains(NotificacaoSimuladaService.class.getName()));
            assertFalse(candidatos.contains(DataSourceTesteConfig.class.getName()));
        }
    }

    @Test
    void datasourceTesteUsaSomenteSchemaEUsuarioDedicados() {
        try (var contexto = contextoBanco("jdbc:mysql://127.0.0.1:3306/guardia_teste",
                "guardia_teste_app", "valor-ficticio")) {
            contexto.refresh();
            var fonte = contexto.getBean(HikariDataSource.class);
            assertEquals("jdbc:mysql://127.0.0.1:3306/guardia_teste", fonte.getJdbcUrl());
            assertEquals("guardia_teste_app", fonte.getUsername());
            assertNull(fonte.getHikariPoolMXBean(), "O pool deve ficar inativo neste teste.");
        }
    }

    @Test
    void rejeitaDestinoUsuarioEVariavelIncorretosAntesDeAbrirConexao() {
        for (String[] entrada : new String[][]{
                { "jdbc:mysql://localhost/guardia", "guardia_teste_app", "valor-ficticio" },
                { "jdbc:mysql://host.example.test/guardia_teste", "guardia_teste_app", "valor-ficticio" },
                { "jdbc:mysql://localhost/guardia_teste?user=root", "guardia_teste_app", "valor-ficticio" },
                { "jdbc:mysql://localhost/guardia_teste", "root", "valor-ficticio" },
                { "jdbc:mysql://localhost/guardia_teste", "guardia_teste_app", "" }
        }) {
            try (var contexto = contextoBanco(entrada[0], entrada[1], entrada[2])) {
                var erro = assertThrows(BeanCreationException.class, contexto::refresh);
                assertInstanceOf(IllegalStateException.class, erro.getMostSpecificCause());
            }
        }
    }

    private static AnnotationConfigApplicationContext contextoBanco(String url, String usuario, String senha) {
        var contexto = new AnnotationConfigApplicationContext();
        var ambiente = contexto.getEnvironment();
        ambiente.setActiveProfiles("teste");
        ambiente.getPropertySources().addFirst(new MapPropertySource("fixture", Map.of(
                "spring.datasource.url", url,
                "spring.datasource.username", usuario,
                "spring.datasource.hikari.jdbc-url", "jdbc:mysql://localhost/guardia",
                "spring.datasource.hikari.username", "root",
                "GUARDIA_TEST_DB_PASSWORD", senha)));
        contexto.register(DataSourceTesteConfig.class);
        return contexto;
    }

    private static Set<String> candidatos(String... perfis) {
        var ambiente = new StandardEnvironment();
        ambiente.setActiveProfiles(perfis);
        var scanner = new ClassPathScanningCandidateComponentProvider(true, ambiente);
        return scanner.findCandidateComponents("br.com.guardia").stream()
                .map(BeanDefinition::getBeanClassName).collect(Collectors.toSet());
    }
}
