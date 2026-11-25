package br.com.guardia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    private Double latitude;

    private Double longitude;

    @Column(name = "localizacao_atualizada_em")
    private java.time.Instant localizacaoAtualizadaEm;

    public java.time.Instant getLocalizacaoAtualizadaEm() { return localizacaoAtualizadaEm; }
    public void setLocalizacaoAtualizadaEm(java.time.Instant instante) { localizacaoAtualizadaEm = instante; }

    @Column(name = "token_fcm", length = 500)
    private String tokenFcm;

    public Usuario() {
    }

    public Usuario(
            String nome,
            String email,
            String senha,
            Double latitude,
            Double longitude) {

        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getTokenFcm() {
        return tokenFcm;
    }

    public void setTokenFcm(String tokenFcm) {
        this.tokenFcm = tokenFcm;
    }
}