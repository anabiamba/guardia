
package br.com.guardia.service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.guardia.dto.CadastroRequest;
import br.com.guardia.model.Usuario;
import br.com.guardia.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private static final Pattern HASH_BCRYPT =
            Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String hashParaCredencialAusente;
    private final java.time.Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this(usuarioRepository, passwordEncoder, java.time.Clock.systemUTC());
    }

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, java.time.Clock clock) {
        this.clock = clock;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.hashParaCredencialAusente = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public Usuario cadastrar(CadastroRequest request) {
        String erroSenha = br.com.guardia.util.PoliticaSenha.erro(request.senha());
        if (erroSenha != null) throw new IllegalArgumentException(erroSenha);
        String email = request.email();
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }

        Usuario usuario = new Usuario(
                request.nome(), email, passwordEncoder.encode(request.senha()), null, null);
        try {
            return usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            if (usuarioRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("E-mail já cadastrado.");
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Optional<br.com.guardia.dto.PerfilResponse> perfil(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(u -> new br.com.guardia.dto.PerfilResponse(u.getNome(), u.getEmail()));
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public Optional<Usuario> autenticarParaSessao(String email, String senha) {
        return verificarCredencial(usuarioRepository.bloquearPorEmail(email), senha);
    }

    public Optional<Usuario> autenticar(String email, String senha) {
        return verificarCredencial(usuarioRepository.findByEmail(email), senha);
    }

    private Optional<Usuario> verificarCredencial(Optional<Usuario> conta, String senha) {
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            return Optional.empty();
        }

        Optional<Usuario> usuarioComHash = conta
                .filter(usuario -> HASH_BCRYPT.matcher(usuario.getSenha()).matches());
        String hash = usuarioComHash.map(Usuario::getSenha).orElse(hashParaCredencialAusente);
        return passwordEncoder.matches(senha, hash) ? usuarioComHash : Optional.empty();
    }

    @Transactional
    public Usuario atualizarLocalizacao(
            Long id,
            Double latitude,
            Double longitude) {

        if (!ValidadeLocalizacao.coordenadasValidas(latitude, longitude)) {
            throw new IllegalArgumentException("Coordenadas inválidas.");
        }
        Usuario usuario = usuarioRepository.bloquearParaCriarAlerta(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuária não encontrada."));

        usuario.setLocalizacaoAtualizadaEm(clock.instant());
        usuario.setLatitude(latitude);
        usuario.setLongitude(longitude);

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public Usuario atualizarTokenFcm(
            Long id,
            String tokenFcm) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Usuária não encontrada."));

        // Remove o token de outras contas.
        if (tokenFcm != null && !tokenFcm.isBlank()) {

            for (Usuario outroUsuario : usuarioRepository.findAll()) {

                if (!outroUsuario.getId().equals(id)
                        && tokenFcm.equals(outroUsuario.getTokenFcm())) {

                    outroUsuario.setTokenFcm(null);
                    usuarioRepository.save(outroUsuario);
                }
            }
        }

        // Associa o token à usuária atual.
        usuario.setTokenFcm(tokenFcm);

        return usuarioRepository.save(usuario);
    }
}
