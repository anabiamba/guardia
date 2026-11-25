package br.com.guardia.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "entregas_push",
        uniqueConstraints = @UniqueConstraint(name = "uk_entrega_push_destino", columnNames = {"evento_id", "instalacao_hash", "sessao_hash"}),
        indexes = @Index(name = "idx_entrega_push_pendente", columnList = "estado,proxima_tentativa,reservada_ate"))
public class EntregaPush {
    public enum Estado { PENDENTE, PROCESSANDO, ACEITO_FCM, SIMULADO, CANCELADO, FALHA_FINAL }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private EventoPush evento;
    @Column(nullable = false)
    private Long usuarioId;
    @Column(name = "instalacao_hash", nullable = false, length = 64)
    private String instalacaoHash;
    @Column(name = "sessao_hash", nullable = false, length = 64)
    private String sessaoHash;
    @Enumerated(EnumType.STRING) @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(nullable = false, length = 24)
    private Estado estado = Estado.PENDENTE;
    @Column(nullable = false)
    private int tentativas;
    @Column(nullable = false)
    private Instant proximaTentativa;
    private Instant reservadaAte;
    @Column(length = 36)
    private String reserva;

    protected EntregaPush() {}

    public EntregaPush(EventoPush evento, Long usuarioId, String instalacaoHash, String sessaoHash, Instant agora) {
        this.evento = evento;
        this.usuarioId = usuarioId;
        this.instalacaoHash = instalacaoHash;
        this.sessaoHash = sessaoHash;
        this.proximaTentativa = agora;
    }

    public boolean reservar(Instant agora, Duration prazo, int maximo) {
        boolean disponivel = (estado == Estado.PENDENTE && !proximaTentativa.isAfter(agora))
                || (estado == Estado.PROCESSANDO && reservadaAte != null && !reservadaAte.isAfter(agora));
        if (!disponivel) return false;
        if (!agora.isBefore(evento.getExpiraEm())) {
            concluir(Estado.CANCELADO);
            return false;
        }
        if (tentativas >= maximo) {
            concluir(Estado.FALHA_FINAL);
            return false;
        }
        reserva = UUID.randomUUID().toString();
        reservadaAte = agora.plus(prazo);
        estado = Estado.PROCESSANDO;
        tentativas++;
        return true;
    }

    public boolean pertenceA(String identificador, Instant agora) {
        return estado == Estado.PROCESSANDO && reserva != null && reserva.equals(identificador)
                && agora.isBefore(reservadaAte);
    }

    public void concluir(Estado resultado) {
        estado = resultado;
        reserva = null;
        reservadaAte = null;
    }

    public void repetir(Instant proxima, int maximo) {
        if (tentativas >= maximo || !proxima.isBefore(evento.getExpiraEm())) {
            concluir(Estado.FALHA_FINAL);
            return;
        }
        concluir(Estado.PENDENTE);
        proximaTentativa = proxima;
    }

    public Long getId() { return id; }
    public EventoPush getEvento() { return evento; }
    public Long getUsuarioId() { return usuarioId; }
    public String getInstalacaoHash() { return instalacaoHash; }
    public String getSessaoHash() { return sessaoHash; }
    public String getReserva() { return reserva; }
    public int getTentativas() { return tentativas; }
    public Estado getEstado() { return estado; }
    public Instant getProximaTentativa() { return proximaTentativa; }
}
