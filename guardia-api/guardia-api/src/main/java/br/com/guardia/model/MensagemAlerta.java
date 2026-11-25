package br.com.guardia.model;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "mensagens_alerta", indexes =
        @Index(name = "idx_mensagens_alerta_id", columnList = "alerta_id,id"))
public class MensagemAlerta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alerta_id", nullable = false)
    private Long alertaId;

    @Column(nullable = false)
    private Long remetenteId;

    @Column(nullable = false, length = 500)
    private String texto;

    @Column(nullable = false)
    private LocalDateTime enviadaEm;

    protected MensagemAlerta() {}

    public MensagemAlerta(Long alertaId, Long remetenteId, String texto, LocalDateTime enviadaEm) {
        this.alertaId = alertaId;
        this.remetenteId = remetenteId;
        this.texto = texto;
        this.enviadaEm = enviadaEm;
    }

    public Long getId() { return id; }
    public Long getRemetenteId() { return remetenteId; }
    public String getTexto() { return texto; }
    public LocalDateTime getEnviadaEm() { return enviadaEm; }
}
