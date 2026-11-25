package br.com.guardia.controller;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.com.guardia.config.AcessoApiConfig;
import br.com.guardia.config.PasswordConfig;
import br.com.guardia.model.Alerta;
import br.com.guardia.model.Sessao;
import br.com.guardia.model.Usuario;
import br.com.guardia.repository.AlertaRepository;
import br.com.guardia.repository.SessaoRepository;
import br.com.guardia.repository.UsuarioRepository;
import br.com.guardia.service.AlertaService;
import br.com.guardia.service.FirebaseNotificationService;
import br.com.guardia.service.SessaoService;
import br.com.guardia.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessaoAutorizacaoTest {

    private static final Instant AGORA = Instant.parse("2026-11-11T12:00:00Z");
    private static final String EMAIL = "sessao@example.test";
    private static final String SENHA_FICTICIA = "entrada-ficticia-26";
    private static final UUID INSTALACAO = UUID.randomUUID();

    private final SessaoRepository sessoes = mock(SessaoRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final AlertaRepository alertas = mock(AlertaRepository.class);
    private final FirebaseNotificationService firebase = mock(FirebaseNotificationService.class);
    private final Clock clock = mock(Clock.class);
    private final Map<String, Sessao> armazenadas = new HashMap<>();
    private final br.com.guardia.repository.VinculoFcmRepository vinculos =
            mock(br.com.guardia.repository.VinculoFcmRepository.class);
    private final SessaoService servico = new SessaoService(sessoes, Duration.ofHours(24), clock, vinculos);
    private AnnotationConfigWebApplicationContext contexto;
    private MockMvc mvc;

    private void configurar(boolean legadoLocal) {
        when(clock.instant()).thenReturn(AGORA);
        when(sessoes.save(any(Sessao.class))).thenAnswer(chamada -> {
            Sessao sessao = chamada.getArgument(0);
            armazenadas.put((String) ReflectionTestUtils.getField(sessao, "tokenHash"), sessao);
            return sessao;
        });
        when(sessoes.findById(anyString())).thenAnswer(chamada ->
                Optional.ofNullable(armazenadas.get(chamada.getArgument(0))));

        when(sessoes.bloquearPorHash(anyString())).thenAnswer(chamada ->
                Optional.ofNullable(armazenadas.get(chamada.getArgument(0))));
        when(vinculos.findBySessaoHash(anyString())).thenAnswer(chamada ->
                Optional.of(mock(br.com.guardia.model.VinculoFcm.class)));
        when(sessoes.findByRevogacaoHash(anyString())).thenAnswer(chamada ->
                armazenadas.values().stream().filter(sessao -> chamada.getArgument(0).equals(
                        ReflectionTestUtils.getField(sessao, "revogacaoHash"))).findFirst());

        var encoder = new PasswordConfig().passwordEncoder();
        var usuarioService = new UsuarioService(usuarios, encoder);
        var alertaService = new AlertaService(alertas, usuarios);
        var despacho = mock(br.com.guardia.service.DespachoSosService.class);
        when(despacho.criarAlerta(any(), any(), any())).thenAnswer(chamada ->
                alertaService.criarAlerta(chamada.getArgument(0), chamada.getArgument(1), chamada.getArgument(2)));
        contexto = new AnnotationConfigWebApplicationContext();
        contexto.setServletContext(new MockServletContext());
        if (legadoLocal) {
            contexto.getEnvironment().setActiveProfiles("dev-local");
            contexto.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "teste-local", Map.of("server.address", "127.0.0.1")));
        }
        contexto.addBeanFactoryPostProcessor(factory -> {
            factory.registerSingleton("senhaController", new SenhaController(
                    new br.com.guardia.service.AlterarSenhaService(usuarios, sessoes, vinculos, encoder, clock)));
            factory.registerSingleton("usuariosController", new UsuarioController(usuarioService));
            factory.registerSingleton("alertasController", new AlertaController(alertaService));
            factory.registerSingleton("notificacaoController", new NotificacaoController(usuarios, firebase));
            factory.registerSingleton("sessaoController", new SessaoController(new br.com.guardia.service.LoginSessaoService(usuarioService, servico), servico));
            factory.registerSingleton("recursosController",
                    new RecursosAutenticadosController(usuarioService, alertaService, servico,
                            despacho));
            factory.registerSingleton("acessoApiConfig",
                    new AcessoApiConfig(servico, contexto.getEnvironment(), legadoLocal));
        });
        contexto.register(WebConfig.class);
        contexto.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();

        Usuario conta = conta(1L, 0.0);
        conta.setSenha(encoder.encode(SENHA_FICTICIA));
        when(usuarios.findByEmail(EMAIL)).thenReturn(Optional.of(conta));
        when(usuarios.bloquearPorEmail(EMAIL)).thenReturn(Optional.of(conta));
        when(usuarios.bloquearParaCriarAlerta(1L)).thenReturn(Optional.of(conta));
        when(sessoes.bloquearDaConta(1L)).thenAnswer(chamada -> armazenadas.values().stream()
                .filter(s -> s.getUsuarioId().equals(1L)).toList());
        when(usuarios.findById(1L)).thenReturn(Optional.of(conta));
        when(usuarios.findById(2L)).thenReturn(Optional.of(conta(2L, 2.0)));
    }

    @AfterEach
    void fechar() {
        if (contexto != null) {
            contexto.close();
        }
    }

    @Test
    void trocaSenhaExigeAutenticacaoValidaDadosERevogaTodas() throws Exception {
        configurar(false);
        mvc.perform(put("/api/v2/usuarios/me/senha").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        String token = login();
        String outraSessao = login();
        var corpo = new JsonMapper().writeValueAsString(Map.of(
                "senhaAtual", SENHA_FICTICIA, "novaSenha", "nova-ficticia-segura-26"));
        mvc.perform(put("/api/v2/usuarios/me/senha").header("Authorization", bearer(token))
                .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v2/usuarios/me/senha").header("Authorization", bearer(token))
                .param("usuarioId", "2").contentType("application/json").content(corpo))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v2/usuarios/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v2/usuarios/me").header("Authorization", bearer(outraSessao)))
                .andExpect(status().isUnauthorized());
        verify(usuarios, never()).bloquearParaCriarAlerta(2L);
        var antigo = new JsonMapper().writeValueAsString(Map.of("email", EMAIL, "senha", SENHA_FICTICIA));
        mvc.perform(post("/api/v2/sessoes/login").header("X-Instalacao-Id", INSTALACAO.toString())
                .contentType("application/json").content(antigo)).andExpect(status().isUnauthorized());
        var novo = new JsonMapper().writeValueAsString(Map.of("email", EMAIL, "senha", "nova-ficticia-segura-26"));
        mvc.perform(post("/api/v2/sessoes/login").header("X-Instalacao-Id", INSTALACAO.toString())
                .contentType("application/json").content(novo)).andExpect(status().isOk());
    }

    @Test
    void perfilSoRetornaDadosDaContaAutenticada() throws Exception {
        configurar(false);
        mvc.perform(get("/api/v2/usuarios/me")).andExpect(status().isUnauthorized());
        String token = login();
        Usuario titular = conta(1L, 0.0);
        titular.setNome("Conta fictícia A");
        titular.setEmail("a@example.test");
        when(usuarios.findById(1L)).thenReturn(Optional.of(titular));
        mvc.perform(get("/api/v2/usuarios/me").param("usuarioId", "2")
                .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.nome").value("Conta fictícia A"))
                .andExpect(jsonPath("$.email").value("a@example.test"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.latitude").doesNotExist());
        verify(usuarios, never()).findById(2L);
        when(clock.instant()).thenReturn(AGORA.plus(Duration.ofHours(24)));
        mvc.perform(get("/api/v2/usuarios/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void perfilAusenteRetorna404() throws Exception {
        configurar(false);
        String token = login();
        when(usuarios.findById(1L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/v2/usuarios/me").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void historicoExigeSessaoEIgnoraIdentidadeFornecidaPeloCliente() throws Exception {
        configurar(false);
        mvc.perform(get("/api/v2/alertas/me/historico"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(alertas);
        String token = login();
        when(alertas.findTop50ByUsuarioIdOrderByDataHoraCriacaoDescIdDesc(1L))
                .thenReturn(java.util.List.of());
        mvc.perform(get("/api/v2/alertas/me/historico").param("usuarioId", "2")
                .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.length()").value(0));
        verify(alertas).findTop50ByUsuarioIdOrderByDataHoraCriacaoDescIdDesc(1L);
        verify(alertas, never()).findTop50ByUsuarioIdOrderByDataHoraCriacaoDescIdDesc(2L);
        when(clock.instant()).thenReturn(AGORA.plus(Duration.ofHours(24)));
        mvc.perform(get("/api/v2/alertas/me/historico").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void loginPublicoEmiteTokenAleatorioEArmazenaSoHash() throws Exception {
        configurar(false);
        String token = login();
        String segundo = login();
        assertTrue(!token.equals(segundo) && !armazenadas.containsKey(token),
                "As emissões devem ser distintas e o armazenamento deve usar somente o digest.");
        assertEquals(2, armazenadas.size());
        assertTrue(armazenadas.keySet().stream().allMatch(chave -> chave.matches("[0-9a-f]{64}")));
        assertTrue(armazenadas.values().stream().allMatch(sessao ->
                sessao.getUsuarioId().equals(1L) && sessao.getInstalacaoId().equals(INSTALACAO)));

        mvc.perform(get("/api/v2/alertas/me/status").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        verify(alertas).findFirstByUsuarioIdAndAtivoTrueAndDataHoraCriacaoAfter(
                org.mockito.ArgumentMatchers.eq(1L), any());
    }

    @Test
    void bloqueiaAnonimoTokenInvalidoETokenExpirado() throws Exception {
        configurar(false);
        String token = login();
        mvc.perform(get("/api/v2/alertas/proximos"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
        mvc.perform(get("/api/v2/alertas/proximos").header("Authorization", bearer("x".repeat(43))))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v2/alertas/proximos").header("Authorization", bearer(token), bearer(token)))
                .andExpect(status().isUnauthorized());
        when(clock.instant()).thenReturn(AGORA.plus(Duration.ofHours(24)));
        mvc.perform(get("/api/v2/alertas/proximos").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(alertas);
    }

    @Test
    void revogaSessaoEImpedeReutilizacao() throws Exception {
        configurar(false);
        String token = login();
        mvc.perform(delete("/api/v2/sessoes/atual").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v2/alertas/proximos").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usaIdentidadeDaSessaoParaLocalizacaoECriacao() throws Exception {
        configurar(false);
        String token = login();
        when(usuarios.bloquearParaCriarAlerta(1L)).thenReturn(Optional.of(conta(1L, 0.0)));
        when(usuarios.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));
        when(alertas.save(any(Alerta.class))).thenAnswer(chamada -> chamada.getArgument(0));

        String corpo = "{\"usuarioId\":2,\"latitude\":0.1,\"longitude\":0.2}";
        mvc.perform(put("/api/v2/usuarios/me/localizacao").header("Authorization", bearer(token))
                        .contentType("application/json").content(corpo))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        mvc.perform(post("/api/v2/alertas").header("Authorization", bearer(token))
                        .contentType("application/json").content(corpo))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.usuarioId").value(1));
        verify(usuarios, never()).findById(2L);
    }

    @Test
    void rejeitaEncerramentoConsultaEAjudaEmAlertaDeTerceiraForaDoRaio() throws Exception {
        configurar(false);
        String token = login();
        Alerta alerta = new Alerta();
        alerta.setUsuarioId(2L);
        alerta.setLatitude(2.0);
        alerta.setLongitude(0.0);
        when(alertas.findById(4L)).thenReturn(Optional.of(alerta));
        when(alertas.bloquear(4L)).thenReturn(Optional.of(alerta));

        mvc.perform(put("/api/v2/alertas/4/encerrar").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v2/alertas/4/proximas").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v2/alertas/4/ajudar").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
        verify(alertas, never()).save(any());
    }

    @Test
    void gerenciaFcmSomenteNaSessaoAutenticada() throws Exception {
        configurar(false);
        String token = login();
        String outra = servico.criar(2L, UUID.randomUUID()).token();
        Sessao sessaoAlheia = armazenadas.values().stream()
                .filter(sessao -> sessao.getUsuarioId().equals(2L)).findFirst().orElseThrow();
        mvc.perform(put("/api/v2/usuarios/me/token-fcm").header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content("{\"usuarioId\":2,\"tokenFcm\":\"registro-ficticio\"}"))
                .andExpect(status().isNoContent());
        assertTrue(ReflectionTestUtils.getField(sessaoAlheia, "tokenFcm") == null);
        mvc.perform(delete("/api/v2/usuarios/me/token-fcm").header("Authorization", bearer(outra)))
                .andExpect(status().isNoContent());
        Sessao sessaoPropria = armazenadas.values().stream()
                .filter(sessao -> sessao.getUsuarioId().equals(1L)).findFirst().orElseThrow();
        assertTrue(ReflectionTestUtils.getField(sessaoPropria, "tokenFcm") == null);
        mvc.perform(delete("/api/v2/sessoes/atual").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        assertTrue(ReflectionTestUtils.getField(sessaoPropria, "tokenFcm") == null);
        verifyNoInteractions(firebase);
    }

    @Test
    void bloqueiaLegadoPorPadraoInclusivePushDeTeste() throws Exception {
        configurar(false);
        mvc.perform(get("/api/alertas/ativos")).andExpect(status().isNotFound());
        mvc.perform(put("/api/usuarios/2/localizacao").contentType("application/json").content("{}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/notificacoes/teste/2")).andExpect(status().isNotFound());
        mvc.perform(post("/api/usuarios/login").contentType("application/json").content(loginJson()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
        verifyNoInteractions(firebase);
    }

    @Test
    void legadoLocalRecusaOrigemRemotaEProxyEPushDeTeste() throws Exception {
        configurar(true);
        mvc.perform(get("/api/alertas/ativos")).andExpect(status().isOk());
        mvc.perform(get("/api/alertas/ativos").with(request -> {
            request.setRemoteAddr("192.0.2.1");
            return request;
        })).andExpect(status().isNotFound());
        mvc.perform(get("/api/alertas/ativos").header("X-Forwarded-For", "192.0.2.1"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/notificacoes/teste/1")).andExpect(status().isNotFound());
        verifyNoInteractions(firebase);
    }

    @Test
    void revogacaoAtrasadaERepetidaAfetaSoSessaoAntiga() throws Exception {
        configurar(false);
        var antiga = servico.criar(1L, INSTALACAO);
        var atual = servico.criar(2L, INSTALACAO);
        servico.atualizarTokenFcm(servico.autenticar(antiga.token()).orElseThrow(), "registro-antigo-ficticio");
        servico.atualizarTokenFcm(servico.autenticar(atual.token()).orElseThrow(), "registro-atual-ficticio");

        assertTrue(servico.autenticar(antiga.tokenRevogacao()).isEmpty());
        assertTrue(armazenadas.values().stream().allMatch(sessao -> {
            var digest = (String) ReflectionTestUtils.getField(sessao, "revogacaoHash");
            return digest.matches("[0-9a-f]{64}") && !digest.equals(antiga.tokenRevogacao());
        }));
        revogar(antiga.token());
        assertTrue(servico.autenticar(antiga.token()).isPresent());

        revogar(antiga.tokenRevogacao());
        revogar(antiga.tokenRevogacao());
        assertTrue(servico.autenticar(antiga.token()).isEmpty());
        assertTrue(servico.autenticar(atual.token()).isPresent());
        verify(vinculos).limparDaSessao(org.mockito.ArgumentMatchers.argThat(hash ->
                armazenadas.get(hash).getUsuarioId().equals(1L)));
        verifyNoInteractions(firebase);
    }

    @Test
    void revogaVinculoAposExpirarBearer() throws Exception {
        configurar(false);
        var credencial = servico.criar(1L, INSTALACAO);
        servico.atualizarTokenFcm(servico.autenticar(credencial.token()).orElseThrow(), "registro-ficticio");
        when(clock.instant()).thenReturn(AGORA.plus(Duration.ofHours(25)));
        assertTrue(servico.autenticar(credencial.token()).isEmpty());

        revogar(credencial.tokenRevogacao());
        assertTrue(armazenadas.values().stream().allMatch(sessao ->
                ReflectionTestUtils.getField(sessao, "tokenFcm") == null));
    }

    private void revogar(String credencial) throws Exception {
        mvc.perform(post("/api/v2/sessoes/revogacoes").contentType("application/json")
                        .content("{\"tokenRevogacao\":\"%s\"}".formatted(credencial)))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void trocaDeContaImpedeRegistroAtrasadoEPermiteRotacao() throws Exception {
        configurar(false);
        var proprietarios = new HashMap<String, String>();
        var vinculo = mock(br.com.guardia.model.VinculoFcm.class);
        org.mockito.Mockito.doAnswer(chamada -> {
            proprietarios.put(chamada.getArgument(0), chamada.getArgument(1));
            return null;
        }).when(vinculos).assumirInstalacao(anyString(), anyString());
        when(vinculos.findBySessaoHash(anyString())).thenAnswer(chamada ->
                proprietarios.containsValue(chamada.getArgument(0))
                        ? Optional.of(vinculo) : Optional.empty());

        var antiga = servico.criar(1L, INSTALACAO, "a".repeat(43));
        var nova = servico.criar(2L, INSTALACAO, "a".repeat(43));
        var outra = servico.criar(1L, INSTALACAO, "b".repeat(43));
        assertEquals(2, proprietarios.size());
        var anterior = servico.autenticar(antiga.token()).orElseThrow();
        var atual = servico.autenticar(nova.token()).orElseThrow();
        assertTrue(proprietarios.containsValue(servico.autenticar(outra.token()).orElseThrow().tokenHash()));
        assertEquals(409, org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> servico.atualizarTokenFcm(anterior, "registro-atrasado")).getStatusCode().value());
        servico.atualizarTokenFcm(atual, "registro-inicial");
        servico.atualizarTokenFcm(atual, "registro-renovado");
        verify(vinculo).registrar(org.mockito.ArgumentMatchers.eq("registro-renovado"), anyString());
        revogar(antiga.tokenRevogacao());
        verify(vinculos, never()).limparDaSessao(atual.tokenHash());
        when(clock.instant()).thenReturn(AGORA.plus(Duration.ofHours(24)));
        assertTrue(servico.autenticar(nova.token()).isEmpty());
        servico.buscarTokensFcmAtivos(2L);
        verify(vinculos).buscarTokensAtivos(2L, AGORA.plus(Duration.ofHours(24)));
    }

    private String login() throws Exception {
        byte[] resposta = mvc.perform(post("/api/v2/sessoes/login")
                        .header("X-Instalacao-Id", INSTALACAO.toString())
                        .contentType("application/json").content(loginJson()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.expiraEm").value("2026-11-12T12:00:00Z"))
                .andExpect(jsonPath("$.tokenRevogacao").isString())
                .andReturn().getResponse().getContentAsByteArray();
        return JsonMapper.shared().readTree(resposta).get("token").asString();
    }

    private static String loginJson() {
        return "{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(EMAIL, SENHA_FICTICIA);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static Usuario conta(Long id, double latitude) {
        Usuario conta = new Usuario("Conta fictícia", EMAIL, null, latitude, 0.0);
        conta.setLocalizacaoAtualizadaEm(Instant.now());
        conta.setId(id);
        return conta;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    static class WebConfig {
    }

    @Test
    void statusVisualSoExpoeSOSDaIdentidadeAutenticada() throws Exception {
        configurar(false);
        mvc.perform(get("/api/v2/alertas/me/status")).andExpect(status().isUnauthorized());
        String token = login();
        Alerta alerta = new Alerta(); ReflectionTestUtils.setField(alerta, "id", 7L); alerta.setUsuarioId(1L); alerta.setAtivo(true);
        alerta.setLatitude(0.0); alerta.setLongitude(0.0);
        alerta.setDataHoraCriacao(java.time.LocalDateTime.now().minusMinutes(10));
        when(alertas.findFirstByUsuarioIdAndAtivoTrueAndDataHoraCriacaoAfter(eq(1L), any()))
            .thenReturn(Optional.of(alerta));
        mvc.perform(get("/api/v2/alertas/me/status").param("usuarioId", "2")
            .header("Authorization", bearer(token)))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.usuarioId").value(1)).andExpect(jsonPath("$.latitude").value(0.0))
            .andExpect(jsonPath("$.longitude").value(0.0)).andExpect(jsonPath("$.criadaEm").isString())
            .andExpect(jsonPath("$.expiraEm").isString()).andExpect(jsonPath("$.segundosRestantes").isNumber());
        verify(alertas, never()).findFirstByUsuarioIdAndAtivoTrueAndDataHoraCriacaoAfter(eq(2L), any());
    }

    @Test
    void proximosRetornaPrimeiroNomeSemSobrenomeOuDadosDaConta() throws Exception {
        configurar(false);
        mvc.perform(get("/api/v2/alertas/proximos")).andExpect(status().isUnauthorized());
        String token = login();
        Usuario destinataria = conta(1L, 0.0);
        destinataria.setLocalizacaoAtualizadaEm(java.time.Instant.now());
        Usuario autora = conta(2L, 0.0); autora.setNome("Ana Beatriz");
        when(usuarios.findById(1L)).thenReturn(Optional.of(destinataria));
        when(usuarios.findById(2L)).thenReturn(Optional.of(autora));
        Alerta alerta = new Alerta(); alerta.setUsuarioId(2L); alerta.setLatitude(0.0); alerta.setLongitude(0.0);
        alerta.setDataHoraCriacao(java.time.LocalDateTime.now());
        when(alertas.findByAtivoTrueAndDataHoraCriacaoAfter(any())).thenReturn(java.util.List.of(alerta));
        mvc.perform(get("/api/v2/alertas/proximos").header("Authorization", bearer(token)))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$[0].primeiroNome").value("Ana"))
            .andExpect(jsonPath("$[0].latitude").value(0.0)).andExpect(jsonPath("$[0].longitude").value(0.0))
            .andExpect(jsonPath("$[0].nome").doesNotExist()).andExpect(jsonPath("$[0].email").doesNotExist())
            .andExpect(jsonPath("$[0].senha").doesNotExist());
    }

    @Test void loginV2PreservaCredencialBcryptAnteriorAPolitica() throws Exception {
        configurar(false);
        Usuario existente = conta(1L, 0.0);
        existente.setSenha(new PasswordConfig().passwordEncoder().encode("somenteletras"));
        when(usuarios.bloquearPorEmail(EMAIL)).thenReturn(Optional.of(existente));
        String corpo = new JsonMapper().writeValueAsString(Map.of("email", EMAIL, "senha", "somenteletras"));
        mvc.perform(post("/api/v2/sessoes/login").header("X-Instalacao-Id", INSTALACAO.toString())
            .contentType("application/json").content(corpo)).andExpect(status().isOk());
    }
    @Test void trocaFracaViaHttpNaoRevogaSessaoExistente() throws Exception {
        configurar(false);
        String token = login();
        String corpo = new JsonMapper().writeValueAsString(Map.of("senhaAtual", SENHA_FICTICIA, "novaSenha", "12345678"));
        mvc.perform(put("/api/v2/usuarios/me/senha").header("Authorization", bearer(token))
            .contentType("application/json").content(corpo)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v2/usuarios/me").header("Authorization", bearer(token))).andExpect(status().isOk());
        verify(usuarios, never()).bloquearParaCriarAlerta(1L);
    }

}
