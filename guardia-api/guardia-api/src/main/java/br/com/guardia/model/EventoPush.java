package br.com.guardia.model;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "eventos_push", uniqueConstraints = @UniqueConstraint(name = "uk_evento_push_alerta", columnNames = "alerta_id"))
public class EventoPush {
    public enum Modo { SIMULADO, RESTRITO, REAL }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "alerta_id", nullable = false)
    private Long alertaId;
    @Enumerated(EnumType.STRING) @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private Modo modo;
    @Column(nullable = false)
    private Instant criadoEm;
    @Column(nullable = false)
    private Instant expiraEm;

    protected EventoPush() {}

    public EventoPush(Long alertaId, Modo modo, Instant criadoEm, Instant expiraEm) {
        this.alertaId = alertaId;
        this.modo = modo;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    public Long getId() { return id; }
    public Long getAlertaId() { return alertaId; }
    public Instant getExpiraEm() { return expiraEm; }
}
