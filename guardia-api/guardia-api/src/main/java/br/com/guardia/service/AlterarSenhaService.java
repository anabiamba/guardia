package br.com.guardia.service;

import java.time.Clock;
import br.com.guardia.dto.AlterarSenhaRequest;
import br.com.guardia.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AlterarSenhaService {
    private final UsuarioRepository usuarios;
    private final SessaoRepository sessoes;
    private final VinculoFcmRepository vinculos;
    private final PasswordEncoder encoder;
    private final Clock clock;
    @Autowired
    public AlterarSenhaService(UsuarioRepository usuarios, SessaoRepository sessoes,
            VinculoFcmRepository vinculos, PasswordEncoder encoder) {
        this(usuarios, sessoes, vinculos, encoder, Clock.systemUTC());
    }
    public AlterarSenhaService(UsuarioRepository usuarios, SessaoRepository sessoes,
            VinculoFcmRepository vinculos, PasswordEncoder encoder, Clock clock) {
        this.usuarios = usuarios; this.sessoes = sessoes;
        this.vinculos = vinculos; this.encoder = encoder; this.clock = clock;
    }
    @Transactional
    public void alterar(SessaoService.Identidade identidade, AlterarSenhaRequest request) {
        // Mesma ordem do login: conta primeiro. Trocas simultâneas são serializadas.
        var usuario = usuarios.bloquearParaCriarAlerta(identidade.usuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        var todas = sessoes.bloquearDaConta(identidade.usuarioId());
        var agora = clock.instant();
        if (todas.stream().noneMatch(s -> s.getTokenHash().equals(identidade.tokenHash()) && s.estaAtiva(agora))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String hash = usuario.getSenha();
        if (hash == null || !hash.matches("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}")
                || !encoder.matches(request.senhaAtual(), hash)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta.");
        }
        String erroSenha = br.com.guardia.util.PoliticaSenha.erro(request.novaSenha());
        if (erroSenha != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, erroSenha);
        if (encoder.matches(request.novaSenha(), hash)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha uma senha diferente da atual.");
        }
        usuario.setSenha(encoder.encode(request.novaSenha()));
        for (var sessao : todas) {
            if (sessao.estaAtiva(agora)) sessao.revogar(agora);
            else sessao.atualizarTokenFcm(null);
        }
        usuarios.saveAndFlush(usuario);
        sessoes.flush();
        for (var sessao : todas) vinculos.limparDaSessao(sessao.getTokenHash());
    }
}
