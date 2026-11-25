package br.com.guardia.service;

import java.util.Optional;
import java.util.UUID;
import br.com.guardia.model.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginSessaoService {
    private final UsuarioService usuarios;
    private final SessaoService sessoes;
    public LoginSessaoService(UsuarioService usuarios, SessaoService sessoes) {
        this.usuarios = usuarios;
        this.sessoes = sessoes;
    }
    @Transactional
    public Optional<Resultado> login(String email, String senha, UUID instalacao, String prova) {
        // Mantém o bloqueio da conta até a sessão ser persistida.
        return usuarios.autenticarParaSessao(email, senha)
                .map(u -> new Resultado(u, sessoes.criar(u.getId(), instalacao, prova)));
    }
    public record Resultado(Usuario usuario, SessaoService.Credencial credencial) {
        @Override public String toString() { return "ResultadoLogin"; }
    }
}
