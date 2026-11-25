package br.com.guardia.dto;

import br.com.guardia.model.Usuario;

public class UsuarioResponse {

    private Long id;
    private String nome;
    private String email;
    private Double latitude;
    private Double longitude;

    public UsuarioResponse() {
    }

    public UsuarioResponse(
            Long id,
            String nome,
            String email,
            Double latitude,
            Double longitude
    ) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static UsuarioResponse fromUsuario(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getLatitude(),
                usuario.getLongitude()
        );
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}