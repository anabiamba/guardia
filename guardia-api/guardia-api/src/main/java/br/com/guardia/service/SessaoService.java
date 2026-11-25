package br.com.guardia.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import br.com.guardia.model.Sessao;
import br.com.guardia.repository.SessaoRepository;
import br.com.guardia.repository.VinculoFcmRepository;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SessaoService {

    private static final Pattern FORMATO_TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private final SecureRandom random = new SecureRandom();
    private final SessaoRepository repository;
    private final Duration duracao;
    private final Clock clock;
    private final VinculoFcmRepository vinculos;

    @Autowired
    public SessaoService(SessaoRepository repository,
                         @Value("${guardia.sessao.duracao:PT24H}") Duration duracao,
                         VinculoFcmRepository vinculos) {
        this(repository, duracao, Clock.systemUTC(), vinculos);
    }

    public SessaoService(SessaoRepository repository, Duration duracao, Clock clock,
                         VinculoFcmRepository vinculos) {
        this.vinculos = vinculos;
        if (duracao.isNegative() || duracao.isZero()) {
            throw new IllegalArgumentException("A duração da sessão deve ser positiva.");
        }
        this.repository = repository;
        this.duracao = duracao;
        this.clock = clock;
    }

    public Credencial criar(Long usuarioId, UUID instalacaoId) {
        return criar(usuarioId, instalacaoId, null);
    }

    @Transactional
    public Credencial criar(Long usuarioId, UUID instalacaoId, String provaInstalacao) {
        if (provaInstalacao != null && !FORMATO_TOKEN.matcher(provaInstalacao).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credencial de instalação inválida.");
        }
        String token = gerarToken();
        String tokenRevogacao = gerarToken();
        Instant expiraEm = clock.instant().plus(duracao);
        repository.save(new Sessao(hash(token), usuarioId, instalacaoId, expiraEm,
                hash(tokenRevogacao)));
        if (provaInstalacao != null) {
            vinculos.assumirInstalacao(hash(provaInstalacao), hash(token));
        }
        return new Credencial(token, expiraEm, tokenRevogacao);
    }

    private String gerarToken() {
        byte[] entropia = new byte[32];
        random.nextBytes(entropia);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(entropia);
    }

    @Transactional
    public void revogarComCredencial(String tokenRevogacao) {
        if (tokenRevogacao == null || !FORMATO_TOKEN.matcher(tokenRevogacao).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        Instant agora = clock.instant();
        repository.findByRevogacaoHash(hash(tokenRevogacao))
                .filter(sessao -> sessao.podeRevogarComCredencial(agora))
                .ifPresent(sessao -> {
                    sessao.revogar(agora);
                    salvarAlteracao(sessao);
                    vinculos.limparDaSessao(sessao.getTokenHash());
                });
    }

    public Optional<Identidade> autenticar(String token) {
        if (token == null || !FORMATO_TOKEN.matcher(token).matches()) {
            return Optional.empty();
        }
        String tokenHash = hash(token);
        return repository.findById(tokenHash)
                .filter(sessao -> sessao.estaAtiva(clock.instant()))
                .map(sessao -> new Identidade(sessao.getUsuarioId(), sessao.getInstalacaoId(), tokenHash));
    }

    @Transactional
    public void revogar(Identidade identidade) {
        Sessao sessao = sessaoAtiva(identidade);
        sessao.revogar(clock.instant());
        salvarAlteracao(sessao);
        vinculos.limparDaSessao(identidade.tokenHash());
    }

    @Transactional
    public void atualizarTokenFcm(Identidade identidade, String tokenFcm) {
        Sessao sessao = sessaoAtiva(identidade);
        var vinculo = vinculos.findBySessaoHash(identidade.tokenHash())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Sessão sem vínculo de instalação atual."));
        if (!sessao.estaAtiva(clock.instant())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        vinculo.registrar(tokenFcm, tokenFcm == null ? null : hash(tokenFcm));
        try {
            vinculos.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Registro FCM indisponível.");
        }
    }

    public List<String> buscarTokensFcmAtivos(Long usuarioId) {
        return vinculos.buscarTokensAtivos(usuarioId, clock.instant());
    }

    private Sessao sessaoAtiva(Identidade identidade) {
        return repository.bloquearPorHash(identidade.tokenHash())
                .filter(sessao -> sessao.estaAtiva(clock.instant()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private void salvarAlteracao(Sessao sessao) {
        try {
            repository.saveAndFlush(sessao);
        } catch (OptimisticLockingFailureException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sessão alterada. Repita a operação.");
        }
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível.", e);
        }
    }

    public record Credencial(String token, Instant expiraEm, String tokenRevogacao) {
        @Override
        public String toString() {
            return "Credencial";
        }
    }

    public record Identidade(Long usuarioId, UUID instalacaoId, String tokenHash) {
        @Override
        public String toString() {
            return "Identidade";
        }
    }
}
