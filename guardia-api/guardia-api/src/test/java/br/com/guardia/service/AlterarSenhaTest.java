package br.com.guardia.service;

import br.com.guardia.config.PasswordConfig;
import br.com.guardia.dto.AlterarSenhaRequest;
import br.com.guardia.model.*;
import br.com.guardia.repository.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class AlterarSenhaTest {
    final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    final SessaoRepository sessoes = mock(SessaoRepository.class);
    final VinculoFcmRepository vinculos = mock(VinculoFcmRepository.class);
    final org.springframework.security.crypto.password.PasswordEncoder encoder = new PasswordConfig().passwordEncoder();
    final Instant agora = Instant.parse("2026-11-22T15:00:00Z");
    final AlterarSenhaService service = new AlterarSenhaService(usuarios, sessoes, vinculos, encoder, Clock.fixed(agora, ZoneOffset.UTC));
    final String anterior = "anterior-ficticia-26";
    final String nova = "nova-ficticia-26";
    final SessaoService.Identidade identidade = new SessaoService.Identidade(1L, UUID.randomUUID(), "sessao-a");
    final Usuario usuario = new Usuario();
    final Sessao ativa = new Sessao("sessao-a", 1L, UUID.randomUUID(), agora.plusSeconds(60));
    void configurar() {
        usuario.setSenha(encoder.encode(anterior));
        when(usuarios.bloquearParaCriarAlerta(1L)).thenReturn(Optional.of(usuario));
        when(sessoes.bloquearDaConta(1L)).thenReturn(List.of(ativa));
    }
    @Test void senhaErradaNaoAlteraNemRevoga() {
        configurar();
        assertThrows(ResponseStatusException.class, () -> service.alterar(identidade, new AlterarSenhaRequest("incorreta", nova)));
        assertTrue(encoder.matches(anterior, usuario.getSenha()));
        assertTrue(ativa.estaAtiva(agora));
        verify(usuarios, never()).saveAndFlush(any());
        verifyNoInteractions(vinculos);
    }
    @Test void senhaIgualNaoRevoga() {
        configurar();
        assertThrows(ResponseStatusException.class, () -> service.alterar(identidade, new AlterarSenhaRequest(anterior, anterior)));
        assertTrue(ativa.estaAtiva(agora));
        verifyNoInteractions(vinculos);
    }
    @Test void revalidaSessaoRevogadaSobBloqueio() {
        configurar(); ativa.revogar(agora);
        assertThrows(ResponseStatusException.class, () -> service.alterar(identidade, new AlterarSenhaRequest(anterior, nova)));
        verify(usuarios, never()).saveAndFlush(any());
    }
    @Test void trocaHashRevogaTodasELimpaSomenteSessoesDaConta() {
        configurar();
        Sessao segunda = new Sessao("sessao-b", 1L, UUID.randomUUID(), agora.plusSeconds(600));
        Sessao terceira = new Sessao("outra-conta", 2L, UUID.randomUUID(), agora.plusSeconds(600));
        when(sessoes.bloquearDaConta(1L)).thenReturn(List.of(ativa, segunda));
        service.alterar(identidade, new AlterarSenhaRequest(anterior, nova));
        assertTrue(encoder.matches(nova, usuario.getSenha()));
        assertFalse(encoder.matches(anterior, usuario.getSenha()));
        assertFalse(ativa.estaAtiva(agora)); assertFalse(segunda.estaAtiva(agora));
        assertTrue(terceira.estaAtiva(agora));
        verify(vinculos).limparDaSessao("sessao-a"); verify(vinculos).limparDaSessao("sessao-b");
        verifyNoMoreInteractions(vinculos);
        var ordem = inOrder(usuarios, sessoes);
        ordem.verify(usuarios).bloquearParaCriarAlerta(1L);
        ordem.verify(sessoes).bloquearDaConta(1L);
    }
    @Test void dtoValidaBytesENaoIncluiSegredosNoToString() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new AlterarSenhaRequest(anterior, nova)).isEmpty());
            assertFalse(validator.validate(new AlterarSenhaRequest(anterior, "á".repeat(40))).isEmpty());
            assertFalse(validator.validate(new AlterarSenhaRequest(anterior, "curta")).isEmpty());
            assertFalse(validator.validate(new AlterarSenhaRequest("", nova)).isEmpty());
        }
        assertEquals("AlterarSenhaRequest", new AlterarSenhaRequest(anterior, nova).toString());
    }
    @Test void loginMantemOrdemDeBloqueioAntesDeEmitirSessao() {
        var usuarioService = mock(UsuarioService.class);
        var sessaoService = mock(SessaoService.class);
        when(usuarioService.autenticarParaSessao("a@example.test", anterior)).thenReturn(Optional.of(usuario));
        usuario.setId(1L);
        new LoginSessaoService(usuarioService, sessaoService).login("a@example.test", anterior, identidade.instalacaoId(), null);
        var ordem = inOrder(usuarioService, sessaoService);
        ordem.verify(usuarioService).autenticarParaSessao("a@example.test", anterior);
        ordem.verify(sessaoService).criar(1L, identidade.instalacaoId(), null);
    }

    @Test void novaSenhaFracaNaoAlteraHashSessaoOuVinculo() {
        configurar();
        assertThrows(ResponseStatusException.class, () -> service.alterar(identidade, new AlterarSenhaRequest(anterior, "12345678")));
        assertTrue(encoder.matches(anterior, usuario.getSenha())); assertTrue(ativa.estaAtiva(agora));
        verify(usuarios, never()).saveAndFlush(any()); verifyNoInteractions(vinculos);
    }
    @Test void senhaAntigaSemComposicaoContinuaValidaParaTroca() {
        configurar(); usuario.setSenha(encoder.encode("somenteletras"));
        service.alterar(identidade, new AlterarSenhaRequest("somenteletras", nova));
        assertTrue(encoder.matches(nova, usuario.getSenha())); assertFalse(ativa.estaAtiva(agora));
        verify(vinculos).limparDaSessao("sessao-a");
    }

}
