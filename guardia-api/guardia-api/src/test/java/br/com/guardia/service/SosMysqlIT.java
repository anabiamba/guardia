package br.com.guardia.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import br.com.guardia.config.DataSourceTesteConfig;
import br.com.guardia.model.Alerta;
import br.com.guardia.model.Usuario;
import br.com.guardia.repository.AlertaRepository;
import br.com.guardia.repository.UsuarioRepository;
import br.com.guardia.repository.MensagemAlertaRepository;
import br.com.guardia.repository.SessaoRepository;
import br.com.guardia.repository.VinculoFcmRepository;
import br.com.guardia.repository.EventoPushRepository;
import br.com.guardia.repository.EntregaPushRepository;
import br.com.guardia.model.EntregaPush;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

/** Integração opt-in: nenhuma API, agendamento ou configuração Firebase é carregada. */
@EnabledIfEnvironmentVariable(named = "GUARDIA_RUN_MYSQL_TESTS", matches = "true")
class SosMysqlIT {
    private AnnotationConfigApplicationContext contexto;
    private AlertaService servico;
    private UsuarioRepository usuarios;
    private AlertaRepository alertas;
    private final List<Long> autorasCriadas = new ArrayList<>();
    private final List<SessaoService.Credencial> credenciaisCriadas = new java.util.concurrent.CopyOnWriteArrayList<>();

    @Test
    void filaGravadaComAlertaTambemSofreRollback() {
        Long autora = criarConta();
        var id = new java.util.concurrent.atomic.AtomicReference<Long>();
        var transacao = new TransactionTemplate(contexto.getBean(JpaTransactionManager.class));
        assertThrows(IllegalStateException.class, () -> transacao.executeWithoutResult(status -> {
            id.set(contexto.getBean(DespachoSosService.class).criarAlerta(autora, 0.0, 0.0).getId());
            throw new IllegalStateException("Falha fictícia após gravação da outbox");
        }));
        assertTrue(alertas.findById(id.get()).isEmpty());
        assertTrue(contexto.getBean(EventoPushRepository.class).findAll().stream()
                .noneMatch(e -> id.get().equals(e.getAlertaId())));
    }

    @Test
    void doisProcessadoresNaoReservamMesmaEntrega() throws Exception {
        criarEntregaFicticia();
        var fila = contexto.getBean(FilaPushTransacoes.class);
        var resultados = simultaneos(List.of(
                () -> !fila.reservar().isEmpty(),
                () -> !fila.reservar().isEmpty()));
        assertEquals(1, resultados.stream().filter(Boolean::booleanValue).count());
    }

    @Test
    void filaRecuperaReservaPersistidaAposReabrirContextoSemDuplicarConclusao() {
        Long alerta = criarEntregaFicticia();
        var fila = contexto.getBean(FilaPushTransacoes.class);
        var antiga = fila.reservar().get(0);
        var antesDaQueda = fila.preparar(antiga);
        assertNotNull(antesDaQueda);
        // Simula envio concluído seguido de queda antes de persistir a confirmação.
        assertEquals(NotificacaoService.ResultadoPush.SIMULADO,
                contexto.getBean(NotificacaoService.class).despacharAvisoSos(
                        antesDaQueda.token(), antesDaQueda.alertaId(), antesDaQueda.usuariaId(), antesDaQueda.validade()));
        Instant recuperacao = contexto.getBean(RelogioTeste.class).instant().plusSeconds(121);
        contexto.close();
        abrirSomenteBancoDeTesteComValidacao();
        contexto.getBean(RelogioTeste.class).agora = recuperacao;
        fila = contexto.getBean(FilaPushTransacoes.class);
        var nova = fila.reservar().get(0);
        assertNotEquals(antiga.identificador(), nova.identificador());
        fila.concluir(antiga, NotificacaoService.ResultadoPush.ACEITO_FCM);
        var entregas = contexto.getBean(EntregaPushRepository.class);
        assertEquals(EntregaPush.Estado.PROCESSANDO, entregas.findById(nova.id()).orElseThrow().getEstado());
        var preparado = fila.preparar(nova);
        assertNotNull(preparado);
        assertEquals(alerta, preparado.alertaId());
        var resultado = contexto.getBean(NotificacaoService.class).despacharAvisoSos(
                preparado.token(), preparado.alertaId(), preparado.usuariaId());
        fila.concluir(nova, resultado);
        assertEquals(EntregaPush.Estado.SIMULADO, entregas.findById(nova.id()).orElseThrow().getEstado());
    }

    private Long criarEntregaFicticia() {
        assertTrue(contexto.getBean(EntregaPushRepository.class).findAll().stream().noneMatch(e ->
                        e.getEstado() == EntregaPush.Estado.PENDENTE || e.getEstado() == EntregaPush.Estado.PROCESSANDO),
                "Há trabalhos pendentes de outra rodada. Verifique a fila antes deste ensaio.");
        Long autora = criarConta();
        Long destinataria = criarConta();
        var credencial = criarSessao(destinataria, provaFicticia());
        var sessoes = contexto.getBean(SessaoService.class);
        sessoes.atualizarTokenFcm(sessoes.autenticar(credencial.token()).orElseThrow(), "fcm-ficticio-" + UUID.randomUUID());
        return contexto.getBean(DespachoSosService.class).criarAlerta(autora, 0.0, 0.0).getId();
    }

    @Test
    void sessaoExpiraEm24HorasEDeixaDeSerDestinataria() {
        Long usuaria = criarConta();
        var sessoes = contexto.getBean(SessaoService.class);
        var credencial = criarSessao(usuaria, provaFicticia());
        var identidade = sessoes.autenticar(credencial.token()).orElseThrow();
        sessoes.atualizarTokenFcm(identidade, "fcm-ficticio-" + UUID.randomUUID());
        var relogio = contexto.getBean(RelogioTeste.class);
        relogio.agora = credencial.expiraEm().minusNanos(1000);
        assertTrue(sessoes.autenticar(credencial.token()).isPresent());
        assertEquals(1, sessoes.buscarTokensFcmAtivos(usuaria).size());
        relogio.agora = credencial.expiraEm();
        assertTrue(sessoes.autenticar(credencial.token()).isEmpty());
        assertTrue(sessoes.buscarTokensFcmAtivos(usuaria).isEmpty());
        var erro = assertThrows(ResponseStatusException.class,
                () -> sessoes.atualizarTokenFcm(identidade, "fcm-ficticio-vencido"));
        assertEquals(401, erro.getStatusCode().value());
    }

    @Test
    void rotacaoFcmSubstituiTokenSemManterAnterior() {
        Long usuaria = criarConta();
        var sessoes = contexto.getBean(SessaoService.class);
        var credencial = criarSessao(usuaria, provaFicticia());
        var identidade = sessoes.autenticar(credencial.token()).orElseThrow();
        String antigo = "fcm-ficticio-" + UUID.randomUUID();
        String novo = "fcm-ficticio-" + UUID.randomUUID();
        sessoes.atualizarTokenFcm(identidade, antigo);
        sessoes.atualizarTokenFcm(identidade, novo);
        var tokens = sessoes.buscarTokensFcmAtivos(usuaria);
        assertEquals(1, tokens.size());
        assertTrue(tokens.contains(novo));
        assertFalse(tokens.contains(antigo));
    }

    @Test
    void mesmoTokenConcorrenteEmInstalacoesDistintasTemSomenteUmProprietario() throws Exception {
        Long a = criarConta();
        Long b = criarConta();
        var sessoes = contexto.getBean(SessaoService.class);
        var ca = criarSessao(a, provaFicticia());
        var cb = criarSessao(b, provaFicticia());
        var ia = sessoes.autenticar(ca.token()).orElseThrow();
        var ib = sessoes.autenticar(cb.token()).orElseThrow();
        String token = "fcm-ficticio-" + UUID.randomUUID();
        // A violação de unicidade esperada pode incluir o digest no log do driver Hibernate.
        var logSql = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("org.hibernate.orm.jdbc.error");
        var nivelAnterior = logSql.getLevel();
        List<Boolean> resultados;
        try {
            logSql.setLevel(ch.qos.logback.classic.Level.OFF);
            resultados = simultaneos(List.of(
                    () -> { sessoes.atualizarTokenFcm(ia, token); return true; },
                    () -> { sessoes.atualizarTokenFcm(ib, token); return true; }));
        } finally {
            logSql.setLevel(nivelAnterior);
        }
        assertEquals(1, resultados.stream().filter(Boolean::booleanValue).count());
        assertEquals(1, sessoes.buscarTokensFcmAtivos(a).size() + sessoes.buscarTokensFcmAtivos(b).size());
    }

    @Test
    void registroAtrasadoNaoRecuperaInstalacaoAposTrocaConcorrente() throws Exception {
        Long a = criarConta();
        Long b = criarConta();
        String prova = provaFicticia();
        var sessoes = contexto.getBean(SessaoService.class);
        var ca = criarSessao(a, prova);
        var ia = sessoes.autenticar(ca.token()).orElseThrow();
        var cb = new java.util.concurrent.atomic.AtomicReference<SessaoService.Credencial>();
        String tokenAntigo = "fcm-ficticio-" + UUID.randomUUID();
        var resultados = simultaneos(List.of(
                () -> { cb.set(criarSessao(b, prova)); return true; },
                () -> { sessoes.atualizarTokenFcm(ia, tokenAntigo); return true; }));
        assertTrue(resultados.get(0));
        var ib = sessoes.autenticar(cb.get().token()).orElseThrow();
        String atual = "fcm-ficticio-" + UUID.randomUUID();
        sessoes.atualizarTokenFcm(ib, atual);
        var erro = assertThrows(ResponseStatusException.class,
                () -> sessoes.atualizarTokenFcm(ia, tokenAntigo));
        assertEquals(409, erro.getStatusCode().value());
        sessoes.revogarComCredencial(ca.tokenRevogacao());
        sessoes.revogarComCredencial(ca.tokenRevogacao());
        assertTrue(sessoes.autenticar(ca.token()).isEmpty());
        assertTrue(sessoes.autenticar(cb.get().token()).isPresent());
        assertTrue(sessoes.buscarTokensFcmAtivos(a).isEmpty());
        assertTrue(sessoes.buscarTokensFcmAtivos(b).contains(atual));
    }

    private SessaoService.Credencial criarSessao(Long usuaria, String prova) {
        var credencial = contexto.getBean(SessaoService.class).criar(usuaria, UUID.randomUUID(), prova);
        credenciaisCriadas.add(credencial);
        return credencial;
    }

    private String provaFicticia() {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                new java.security.SecureRandom().generateSeed(32));
    }

    @Test
    void expiracaoAutomaticaConcorrenteComAceiteEEncerramentoMantemSosFechado() throws Exception {
        Long autora = criarConta();
        Long ajudante = criarConta();
        Long id = servico.criarAlerta(autora, 0.0, 0.0).getId();
        exigirSemOutrosAlertasAtivos();
        var relogio = contexto.getBean(RelogioTeste.class);
        relogio.agora = relogio.instant().plusSeconds(3600);
        var resultados = simultaneos(List.of(
                () -> { servico.registrarAjudaComoProxima(id, ajudante); return true; },
                () -> { servico.encerrarComoAutora(id, autora); return true; },
                () -> { servico.encerrarAlertasExpirados(); return true; }));
        assertEquals(List.of(false, true, true), resultados);
        Alerta persistido = alertas.findById(id).orElseThrow();
        assertFalse(persistido.getAtivo());
        assertNull(persistido.getUsuarioAjudandoId());
        assertNotNull(persistido.getDataHoraEncerramento());
        assertTrue(servico.buscarStatusAlerta(autora).isEmpty());
        var encerramento = persistido.getDataHoraEncerramento();
        servico.encerrarAlertasExpirados();
        assertEquals(encerramento, alertas.findById(id).orElseThrow().getDataHoraEncerramento());
    }

    @Test
    void criacaoConcorrenteComVarreduraPreservaNovoSos() throws Exception {
        Long autora = criarConta();
        Long antigo = servico.criarAlerta(autora, 0.0, 0.0).getId();
        exigirSemOutrosAlertasAtivos();
        var relogio = contexto.getBean(RelogioTeste.class);
        relogio.agora = relogio.instant().plusSeconds(3600);
        var novo = new java.util.concurrent.atomic.AtomicReference<Long>();
        var resultados = simultaneos(List.of(
                () -> { novo.set(servico.criarAlerta(autora, 0.0, 0.0).getId()); return true; },
                () -> { servico.encerrarAlertasExpirados(); return true; }));
        assertEquals(List.of(true, true), resultados);
        assertNotNull(novo.get());
        assertNotEquals(antigo, novo.get());
        assertFalse(alertas.findById(antigo).orElseThrow().getAtivo());
        assertTrue(alertas.findById(novo.get()).orElseThrow().getAtivo());
        assertEquals(novo.get(), servico.buscarStatusAlerta(autora).orElseThrow().getId());
    }

    private void exigirSemOutrosAlertasAtivos() {
        assertTrue(alertas.findAll().stream().noneMatch(a -> Boolean.TRUE.equals(a.getAtivo())
                && !autorasCriadas.contains(a.getUsuarioId())),
                "Há outros SOS ativos no banco de teste. Encerre-os antes da rotina global.");
    }

    @Test
    void destinatariaImediatamenteDentroDeUmKmVisualizaEAceita() {
        Long autora = criarConta();
        Long proxima = criarConta();
        posicionarAoNorte(proxima, 999.9);
        Long id = servico.criarAlerta(autora, 0.0, 0.0).getId();
        assertTrue(servico.buscarAlertasProximos(proxima).stream().anyMatch(a -> id.equals(a.getId())));
        assertTrue(servico.buscarUsuariosProximas(id).stream().anyMatch(u -> proxima.equals(u.getId())));
        assertFalse(servico.buscarAlertasProximos(autora).stream().anyMatch(a -> id.equals(a.getId())));
        assertThrows(ResponseStatusException.class, () -> servico.registrarAjudaComoProxima(id, autora));
        servico.registrarAjudaComoProxima(id, proxima);
        assertEquals(proxima, alertas.findById(id).orElseThrow().getUsuarioAjudandoId());
    }

    @Test
    void destinatariaImediatamenteForaDeUmKmNaoVisualizaNemAceita() {
        Long autora = criarConta();
        Long distante = criarConta();
        posicionarAoNorte(distante, 1000.1);
        Long id = servico.criarAlerta(autora, 0.0, 0.0).getId();
        assertFalse(servico.buscarAlertasProximos(distante).stream().anyMatch(a -> id.equals(a.getId())));
        assertFalse(servico.buscarUsuariosProximas(id).stream().anyMatch(u -> distante.equals(u.getId())));
        var erro = assertThrows(ResponseStatusException.class,
                () -> servico.registrarAjudaComoProxima(id, distante));
        assertEquals(404, erro.getStatusCode().value());
        assertNull(alertas.findById(id).orElseThrow().getUsuarioAjudandoId());
        assertTrue(alertas.findById(id).orElseThrow().getAtivo());
    }

    private void posicionarAoNorte(Long usuarioId, double metros) {
        // Distância meridional em esfera de raio 6.371.000 m, nas coordenadas fictícias do teste.
        var usuaria = usuarios.findById(usuarioId).orElseThrow();
        usuaria.setLatitude(Math.toDegrees(metros / 6_371_000.0));
        usuaria.setLongitude(0.0);
        usuarios.saveAndFlush(usuaria);
    }

    @Test
    void expiraExatamenteEmUmaHoraSemDependerDaVarredura() {
        Long autora = criarConta();
        Long ajudante = criarConta();
        Long id = servico.criarAlerta(autora, 0.0, 0.0).getId();
        servico.registrarAjudaComoProxima(id, ajudante);
        var chat = contexto.getBean(ChatAlertaService.class);
        var relogio = contexto.getBean(RelogioTeste.class);
        Instant inicio = relogio.instant();
        relogio.agora = inicio.plusSeconds(3600).minusNanos(1000);
        assertTrue(servico.buscarStatusAlerta(autora).isPresent());
        chat.enviar(id, autora, "Mensagem fictícia antes do prazo");
        relogio.agora = inicio.plusSeconds(3600);
        assertTrue(servico.buscarStatusAlerta(autora).isEmpty());
        assertFalse(servico.buscarAlertasProximos(ajudante).stream().anyMatch(a -> id.equals(a.getId())));
        assertThrows(ResponseStatusException.class, () -> servico.registrarAjudaComoProxima(id, ajudante));
        assertThrows(ResponseStatusException.class, () -> chat.listar(id, autora, 0));
        assertThrows(ResponseStatusException.class, () -> chat.enviar(id, ajudante, "Mensagem fictícia vencida"));
        assertTrue(alertas.findById(id).orElseThrow().getAtivo(), "Expiração deve valer antes da varredura.");
        Long novo = servico.criarAlerta(autora, 0.0, 0.0).getId();
        assertNotEquals(id, novo);
        assertEquals(novo, servico.buscarStatusAlerta(autora).orElseThrow().getId());
    }

    @Test
    void recuperaPersistenciaAoReabrirContextoEPublicarEventoDeInicializacao() {
        Long autora = criarConta();
        Long id = servico.criarAlerta(autora, 0.0, 0.0).getId();
        Instant vencimento = contexto.getBean(RelogioTeste.class).instant().plusSeconds(3600);
        // A rotina de produção é global. Impede que este ensaio afete fixtures de outras rodadas.
        exigirSemOutrosAlertasAtivos();
        contexto.close();
        abrirSomenteBancoDeTesteComValidacao();
        contexto.getBean(RelogioTeste.class).agora = vencimento;
        assertTrue(alertas.findById(id).orElseThrow().getAtivo());
        assertTrue(servico.buscarStatusAlerta(autora).isEmpty());
        contexto.publishEvent(new ApplicationReadyEvent(new SpringApplication(), new String[0], contexto, Duration.ZERO));
        Alerta recuperado = alertas.findById(id).orElseThrow();
        assertFalse(recuperado.getAtivo());
        assertNotNull(recuperado.getDataHoraEncerramento());
        var encerramento = recuperado.getDataHoraEncerramento();
        contexto.publishEvent(new ApplicationReadyEvent(new SpringApplication(), new String[0], contexto, Duration.ZERO));
        assertEquals(encerramento, alertas.findById(id).orElseThrow().getDataHoraEncerramento());
    }

    @BeforeEach
    void abrirSomenteBancoDeTesteComValidacao() {
        contexto = new AnnotationConfigApplicationContext();
        contexto.getEnvironment().setActiveProfiles("teste");
        contexto.getEnvironment().getPropertySources().addFirst(new MapPropertySource("integracao-segura", Map.of(
                "spring.datasource.url", "jdbc:mysql://localhost:3306/guardia_teste",
                "spring.datasource.username", "guardia_teste_app")));
        contexto.register(Config.class);
        contexto.refresh();
        servico = contexto.getBean(AlertaService.class);
        usuarios = contexto.getBean(UsuarioRepository.class);
        alertas = contexto.getBean(AlertaRepository.class);
        var transacao = new TransactionTemplate(contexto.getBean(JpaTransactionManager.class));
        transacao.executeWithoutResult(status -> {
            var em = org.springframework.orm.jpa.EntityManagerFactoryUtils.getTransactionalEntityManager(
                    contexto.getBean(EntityManagerFactory.class));
            assertEquals("guardia_teste", em.createNativeQuery("SELECT DATABASE()", String.class).getSingleResult());
            String usuario = (String) em.createNativeQuery("SELECT CURRENT_USER()", String.class).getSingleResult();
            assertTrue(usuario.startsWith("guardia_teste_app@"), "Usuário MySQL inesperado.");
        });
    }

    @AfterEach
    void encerrarSomenteAlertasDasContasCriadasNesteTeste() {
        if (contexto == null) return;
        try {
            if (contexto.isActive() && alertas != null) {
                var meusAlertas = alertas.findAll().stream().filter(a -> autorasCriadas.contains(a.getUsuarioId()))
                        .map(Alerta::getId).collect(java.util.stream.Collectors.toSet());
                var repositorioEntregas = contexto.getBean(EntregaPushRepository.class);
                var minhasEntregas = repositorioEntregas.findAll().stream()
                        .filter(e -> meusAlertas.contains(e.getEvento().getAlertaId()))
                        .filter(e -> e.getEstado() == EntregaPush.Estado.PENDENTE || e.getEstado() == EntregaPush.Estado.PROCESSANDO)
                        .toList();
                minhasEntregas.forEach(e -> e.concluir(EntregaPush.Estado.CANCELADO));
                repositorioEntregas.saveAllAndFlush(minhasEntregas);
                var sessoes = contexto.getBean(SessaoService.class);
                for (var credencial : credenciaisCriadas) {
                    sessoes.revogarComCredencial(credencial.tokenRevogacao());
                }
                for (Long autora : autorasCriadas) {
                    for (Alerta alerta : alertas.findAll().stream()
                            .filter(a -> autora.equals(a.getUsuarioId()) && Boolean.TRUE.equals(a.getAtivo())).toList()) {
                        servico.encerrarComoAutora(alerta.getId(), autora);
                    }
                }
            }
        } finally {
            contexto.close();
        }
    }

    @Test
    void duasCriacoesSimultaneasPersistemSomenteUmSosAtivo() throws Exception {
        Long autora = criarConta();
        var resultados = simultaneos(List.of(
                () -> { servico.criarAlerta(autora, 0.0, 0.0); return true; },
                () -> { servico.criarAlerta(autora, 0.0, 0.0); return true; }));
        assertEquals(1, resultados.stream().filter(Boolean::booleanValue).count());
        assertEquals(1, alertas.findAll().stream().filter(a -> autora.equals(a.getUsuarioId())
                && Boolean.TRUE.equals(a.getAtivo())).count());
    }

    @Test
    void doisAceitesSimultaneosPersistemSomenteUmaAjudante() throws Exception {
        Long autora = criarConta();
        Long b = criarConta();
        Long c = criarConta();
        Long id = servico.criarAlerta(autora, 0.0, 0.0).getId();
        var resultados = simultaneos(List.of(
                () -> { servico.registrarAjudaComoProxima(id, b); return true; },
                () -> { servico.registrarAjudaComoProxima(id, c); return true; }));
        assertEquals(1, resultados.stream().filter(Boolean::booleanValue).count());
        assertTrue(Set.of(b, c).contains(alertas.findById(id).orElseThrow().getUsuarioAjudandoId()));
    }

    @Test
    void aceiteConcorrenteComEncerramentoNaoReabreSos() throws Exception {
        Long autora = criarConta();
        Long ajudante = criarConta();
        Long id = servico.criarAlerta(autora, 0.0, 0.0).getId();
        var resultados = simultaneos(List.of(
                () -> { servico.registrarAjudaComoProxima(id, ajudante); return true; },
                () -> { servico.encerrarComoAutora(id, autora); return true; }));
        assertTrue(resultados.get(1), "Encerramento pela autora deve ter sucesso.");
        Alerta persistido = alertas.findById(id).orElseThrow();
        assertFalse(persistido.getAtivo());
        assertNotNull(persistido.getDataHoraEncerramento());
        assertThrows(ResponseStatusException.class, () -> servico.registrarAjudaComoProxima(id, ajudante));
    }

    private Long criarConta() {
        var conta = new Usuario("Teste concorrência", "concorrencia-" + UUID.randomUUID() + "@example.test",
                new BCryptPasswordEncoder().encode(UUID.randomUUID().toString()), 0.0, 0.0);
        conta.setLocalizacaoAtualizadaEm(contexto.getBean(RelogioTeste.class).instant());
        Long id = usuarios.saveAndFlush(conta).getId();
        autorasCriadas.add(id);
        return id;
    }

    private List<Boolean> simultaneos(List<Callable<Boolean>> operacoes) throws Exception {
        var executor = Executors.newFixedThreadPool(operacoes.size());
        var prontas = new CountDownLatch(operacoes.size());
        Set<Long> conexoes = ConcurrentHashMap.newKeySet();
        try {
            var tarefas = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (var operacao : operacoes) {
                tarefas.add(executor.submit(() -> {
                    var transacao = new TransactionTemplate(contexto.getBean(JpaTransactionManager.class));
                    try {
                        return transacao.execute(status -> {
                            var em = org.springframework.orm.jpa.EntityManagerFactoryUtils.getTransactionalEntityManager(
                                    contexto.getBean(EntityManagerFactory.class));
                            conexoes.add(((Number) em.createNativeQuery("SELECT CONNECTION_ID()", Long.class)
                                    .getSingleResult()).longValue());
                            prontas.countDown();
                            try {
                                if (!prontas.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Sem concorrência real.");
                                return operacao.call();
                            } catch (RuntimeException e) {
                                throw e;
                            } catch (Exception e) {
                                throw new IllegalStateException("Falha ao coordenar transações.", e);
                            }
                        });
                    } catch (IllegalArgumentException | ResponseStatusException esperado) {
                        return false;
                    }
                }));
            }
            var resultados = new ArrayList<Boolean>();
            for (var tarefa : tarefas) resultados.add(tarefa.get(30, TimeUnit.SECONDS));
            assertEquals(operacoes.size(), conexoes.size(), "Exige conexões MySQL distintas.");
            return resultados;
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "Transações não terminaram.");
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackages = "br.com.guardia.repository")
    @Import(DataSourceTesteConfig.class)
    static class Config {
        @Bean
        NotificacaoService notificacaoSimulada() { return new NotificacaoSimuladaService(); }

        @Bean
        DespachoSosService despachoSosService(AlertaService alertas, EventoPushRepository eventos,
                EntregaPushRepository entregas, VinculoFcmRepository vinculos, NotificacaoService notificacoes, RelogioTeste clock) {
            return new DespachoSosService(alertas, eventos, entregas, vinculos, notificacoes, clock);
        }

        @Bean
        FilaPushTransacoes filaPushTransacoes(EntregaPushRepository entregas, VinculoFcmRepository vinculos,
                AlertaService alertas, NotificacaoService notificacoes, RelogioTeste clock) {
            return new FilaPushTransacoes(entregas, vinculos, alertas, notificacoes, clock, 5);
        }

        @Bean
        SessaoService sessaoService(SessaoRepository sessoes, VinculoFcmRepository vinculos, RelogioTeste clock) {
            return new SessaoService(sessoes, Duration.ofHours(24), clock, vinculos);
        }
        @Bean
        RelogioTeste relogioTeste() {
            return new RelogioTeste();
        }

        @Bean
        ChatAlertaService chatAlertaService(AlertaService alertas, MensagemAlertaRepository mensagens) {
            return new ChatAlertaService(alertas, mensagens);
        }

        @Bean
        LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource fonte) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(fonte);
            factory.setPackagesToScan("br.com.guardia.model");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "validate",
                    "hibernate.show_sql", "false",
                    "hibernate.physical_naming_strategy",
                    "org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl"));
            return factory;
        }

        @Bean
        JpaTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }

        @Bean
        AlertaService alertaService(AlertaRepository alertas, UsuarioRepository usuarios, RelogioTeste clock) {
            return new AlertaService(alertas, usuarios, clock);
        }
    }

    static class RelogioTeste extends Clock {
        volatile Instant agora = Instant.now().truncatedTo(ChronoUnit.MICROS);

        @Override
        public ZoneId getZone() { return ZoneId.systemDefault(); }

        @Override
        public Clock withZone(ZoneId zona) { return Clock.fixed(agora, zona); }

        @Override
        public Instant instant() { return agora; }
    }
}
