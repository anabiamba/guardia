package br.com.guardia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vinculos_fcm")
public class VinculoFcm {
    @Id
    @Column(length = 64)
    private String instalacaoHash;

    @Column(nullable = false, unique = true, length = 64)
    private String sessaoHash;

    @Column(length = 500)
    private String token;

    @Column(unique = true, length = 64)
    private String tokenHash;

    protected VinculoFcm() {
    }

    public String getSessaoHash() {
        return sessaoHash;
    }

    public String getInstalacaoHash() { return instalacaoHash; }
    public String getToken() { return token; }

    public void registrar(String token, String tokenHash) {
        this.token = token;
        this.tokenHash = tokenHash;
    }
}
