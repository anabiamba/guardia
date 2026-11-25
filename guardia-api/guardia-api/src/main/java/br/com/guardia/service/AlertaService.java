package br.com.guardia.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import br.com.guardia.dto.AlertaStatusResponse;
import br.com.guardia.model.Alerta;
import br.com.guardia.model.Usuario;
import br.com.guardia.repository.AlertaRepository;
import br.com.guardia.repository.UsuarioRepository;
import br.com.guardia.util.LocationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AlertaService {

    private static final double RAIO_ALERTA_METROS = 1000.0;
    private final AlertaRepository alertaRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;
    private final ValidadeLocalizacao localizacao;

    public AlertaService(AlertaRepository alertaRepository, UsuarioRepository usuarioRepository) {
        this(alertaRepository, usuarioRepository, Clock.systemDefaultZone());
    }

    @Autowired
    public AlertaService(AlertaRepository alertaRepository, UsuarioRepository usuarioRepository,
                         ValidadeLocalizacao localizacao) {
        this(alertaRepository, usuarioRepository, Clock.systemDefaultZone(), localizacao);
    }

    public AlertaService(AlertaRepository alertaRepository, UsuarioRepository usuarioRepository, Clock clock) {
        this(alertaRepository, usuarioRepository, clock,
                new ValidadeLocalizacao(clock, java.time.Duration.ofMinutes(15)));
    }

    public AlertaService(AlertaRepository alertaRepository, UsuarioRepository usuarioRepository,
                         Clock clock, ValidadeLocalizacao localizacao) {
        this.localizacao = localizacao;
        this.alertaRepository = alertaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
    }

    @Transactional
    public Alerta criarAlerta(Long usuarioId, Double latitude, Double longitude) {
        if (!ValidadeLocalizacao.coordenadasValidas(latitude, longitude)) {
            throw new IllegalArgumentException("Coordenadas inválidas.");
        }
        Usuario autora = usuarioRepository.bloquearParaCriarAlerta(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuária não encontrada."));
        LocalDateTime agora = LocalDateTime.now(clock);
        if (alertaRepository.findFirstByUsuarioIdAndAtivoTrueAndDataHoraCriacaoAfter(
                usuarioId, agora.minusHours(1)).isPresent()) {
            throw new IllegalArgumentException("A usuária já possui um alerta ativo.");
        }

        autora.setLatitude(latitude);
        autora.setLongitude(longitude);
        autora.setLocalizacaoAtualizadaEm(clock.instant());
        Alerta alerta = new Alerta();
        alerta.setUsuarioId(usuarioId);
        alerta.setLatitude(latitude);
        alerta.setLongitude(longitude);
        alerta.setAtivo(true);
        alerta.setDataHoraCriacao(agora);
        return alertaRepository.save(alerta);
    }

    @Transactional(readOnly = true)
    public List<br.com.guardia.dto.HistoricoAlertaResponse> historico(Long usuarioId) {
        LocalDateTime agora = LocalDateTime.now(clock);
        return alertaRepository.findTop50ByUsuarioIdOrderByDataHoraCriacaoDescIdDesc(usuarioId)
                .stream().map(a -> {
                    boolean expirado = !a.getDataHoraCriacao().isAfter(agora.minusHours(1));
                    String situacao = Boolean.TRUE.equals(a.getAtivo())
                            ? (expirado ? "EXPIRADO" : "ATIVO") : "ENCERRADO";
                    return new br.com.guardia.dto.HistoricoAlertaResponse(a.getId(),
                            a.getDataHoraCriacao(), a.getDataHoraEncerramento(),
                            situacao, a.getUsuarioAjudandoId() != null);
                }).toList();
    }

    public List<Alerta> listarAlertasAtivos() {
        return alertaRepository.findByAtivoTrueAndDataHoraCriacaoAfter(limite());
    }

    @Transactional
    public void encerrarAlerta(Long alertaId, Long usuarioId) {
        encerrar(bloquearAlerta(alertaId), usuarioId, false);
    }

    @Transactional
    public void encerrarComoAutora(Long alertaId, Long autoraId) {
        encerrar(bloquearAlerta(alertaId), autoraId, true);
    }

    private void encerrar(Alerta alerta, Long autoraId, boolean autenticado) {
        if (!alerta.getUsuarioId().equals(autoraId)) {
            if (autenticado) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            throw new IllegalArgumentException("Somente a usuária que criou o alerta pode encerrá-lo.");
        }
        if (Boolean.TRUE.equals(alerta.getAtivo())) {
            alerta.setAtivo(false);
            alerta.setDataHoraEncerramento(LocalDateTime.now(clock));
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void encerrarAlertasExpirados() {
        LocalDateTime agora = LocalDateTime.now(clock);
        alertaRepository.encerrarExpirados(agora.minusHours(1), agora);
    }

    @Transactional
    public Alerta registrarAjuda(Long alertaId, Long usuarioId) {
        return registrarAjuda(bloquearAlerta(alertaId), usuarioId, false);
    }

    @Transactional
    public Alerta registrarAjudaComoProxima(Long alertaId, Long usuarioId) {
        return registrarAjuda(bloquearAlerta(alertaId), usuarioId, true);
    }

    private Alerta registrarAjuda(Alerta alerta, Long usuarioId, boolean ocultarIndisponivel) {
        var usuaria = usuarioRepository.findById(usuarioId);
        if (!vigente(alerta) || usuaria.isEmpty() || !estaProxima(alerta, usuaria.get())) {
            if (ocultarIndisponivel) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            throw new IllegalArgumentException("Alerta indisponível para esta usuária.");
        }
        if (alerta.getUsuarioAjudandoId() != null) {
            throw new IllegalArgumentException("Este alerta já possui uma pessoa ajudando.");
        }
        alerta.setUsuarioAjudandoId(usuarioId);
        return alerta;
    }

    @Transactional(readOnly = true)
    public Optional<AlertaStatusResponse> buscarStatusAlerta(Long usuarioId) {
        return alertaRepository.findFirstByUsuarioIdAndAtivoTrueAndDataHoraCriacaoAfter(
                usuarioId, limite()).flatMap(alerta -> {
            LocalDateTime agora = LocalDateTime.now(clock);
            LocalDateTime expira = alerta.getDataHoraCriacao().plusHours(1);
            if (!Boolean.TRUE.equals(alerta.getAtivo()) || !agora.isBefore(expira)) return Optional.empty();
            Long ajudante = alerta.getUsuarioAjudandoId();
            String nome = ajudante == null ? null
                    : usuarioRepository.findById(ajudante).map(Usuario::getNome).orElse(null);
            return Optional.of(new AlertaStatusResponse(alerta.getId(), alerta.getUsuarioId(),
                    alerta.getAtivo(), ajudante, nome, alerta.getLatitude(), alerta.getLongitude(),
                    alerta.getDataHoraCriacao(), expira,
                    java.time.Duration.between(agora, expira).getSeconds()));
        });
    }

    public List<Usuario> buscarUsuariosProximas(Long alertaId) {
        Alerta alerta = alertaRepository.findById(alertaId).filter(this::vigente)
                .orElseThrow(() -> new IllegalArgumentException("Alerta não encontrado."));
        return buscarUsuariosProximas(alerta);
    }

    public int contarProximasComoAutora(Long alertaId, Long autoraId) {
        Alerta alerta = alertaRepository.findById(alertaId)
                .filter(a -> a.getUsuarioId().equals(autoraId) && vigente(a))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return buscarUsuariosProximas(alerta).size();
    }

    private List<Usuario> buscarUsuariosProximas(Alerta alerta) {
        return usuarioRepository.findAll().stream().filter(u -> estaProxima(alerta, u)).toList();
    }

    public List<Alerta> buscarAlertasProximos(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuária não encontrada."));
        return listarAlertasAtivos().stream().filter(a ->
                usuarioId.equals(a.getUsuarioAjudandoId()) || estaProxima(a, usuario)).toList();
    }

    @Transactional(readOnly = true)
    public List<br.com.guardia.dto.AlertaProximoResponse> buscarAlertasProximosComNome(Long usuarioId) {
        // Buscar nomes somente depois do filtro existente de acesso/proximidade/participação.
        List<Alerta> permitidos = buscarAlertasProximos(usuarioId);
        java.util.Map<Long, String> nomes = new java.util.HashMap<>();
        return permitidos.stream().map(a -> {
            if (!nomes.containsKey(a.getUsuarioId())) {
                String nome = usuarioRepository.findById(a.getUsuarioId()).map(Usuario::getNome).orElse(null);
                String primeiro = nome == null ? null : nome.replaceAll("(?U)^\\s+|\\s+$", "");
                if (primeiro != null) primeiro = primeiro.isBlank() ? null : primeiro.split("(?U)\\s+", 2)[0];
                nomes.put(a.getUsuarioId(), primeiro);
            }
            return new br.com.guardia.dto.AlertaProximoResponse(a.getId(), a.getUsuarioId(),
                    a.getLatitude(), a.getLongitude(), a.getAtivo(), a.getDataHoraCriacao(),
                    a.getDataHoraEncerramento(), a.getUsuarioAjudandoId(), nomes.get(a.getUsuarioId()));
        }).toList();
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public LocalDateTime exigirParticipanteDoChat(Long alertaId, Long usuariaId) {
        Alerta alerta = bloquearAlerta(alertaId);
        Long ajudante = alerta.getUsuarioAjudandoId();
        if (!vigente(alerta) || ajudante == null
                || !(alerta.getUsuarioId().equals(usuariaId) || ajudante.equals(usuariaId))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return LocalDateTime.now(clock);
    }

    private Alerta bloquearAlerta(Long alertaId) {
        return alertaRepository.bloquear(alertaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private LocalDateTime limite() {
        return LocalDateTime.now(clock).minusHours(1);
    }

    private boolean vigente(Alerta alerta) {
        return Boolean.TRUE.equals(alerta.getAtivo()) && alerta.getDataHoraCriacao() != null
                && alerta.getDataHoraCriacao().isAfter(limite());
    }

    private boolean estaProxima(Alerta alerta, Usuario usuaria) {
        return !alerta.getUsuarioId().equals(usuaria.getId())
                && localizacao.vigente(usuaria)
                && LocationUtils.calcularDistanciaEmMetros(alerta.getLatitude(), alerta.getLongitude(),
                        usuaria.getLatitude(), usuaria.getLongitude()) <= RAIO_ALERTA_METROS;
    }
}
