package br.com.guardia.model;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "sessoes")
public class Sessao {

    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private UUID instalacaoId;

    @Column(nullable = false)
    private Instant expiraEm;

    private Instant revogadaEm;

    @Column(length = 64, unique = true)
    private String revogacaoHash;

    @Column(length = 500)
    private String tokenFcm;

    @Version
    private Long versao;

    protected Sessao() {
    }

    public Sessao(String tokenHash, Long usuarioId, UUID instalacaoId, Instant expiraEm) {
        this.tokenHash = tokenHash;
        this.usuarioId = usuarioId;
        this.instalacaoId = instalacaoId;
        this.expiraEm = expiraEm;
    }

    public Sessao(String tokenHash, Long usuarioId, UUID instalacaoId, Instant expiraEm,
                  String revogacaoHash) {
        this(tokenHash, usuarioId, instalacaoId, expiraEm);
        this.revogacaoHash = revogacaoHash;
    }

    public boolean podeRevogarComCredencial(Instant agora) {
        return revogadaEm == null && agora.isBefore(expiraEm.plus(java.time.Duration.ofDays(7)));
    }

    public boolean estaAtiva(Instant agora) {
        return revogadaEm == null && agora.isBefore(expiraEm);
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public UUID getInstalacaoId() {
        return instalacaoId;
    }

    public void revogar(Instant agora) {
        revogadaEm = agora;
        tokenFcm = null;
    }

    public void atualizarTokenFcm(String tokenFcm) {
        this.tokenFcm = tokenFcm;
    }
}
