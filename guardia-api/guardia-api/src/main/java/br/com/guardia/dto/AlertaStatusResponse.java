package br.com.guardia.dto;

public class AlertaStatusResponse {

    private Long id;
    private Long usuarioId;
    private Boolean ativo;
    private Long usuarioAjudandoId;
    private String nomeUsuarioAjudando;
    private Double latitude;
    private Double longitude;
    private java.time.LocalDateTime criadaEm;
    private java.time.LocalDateTime expiraEm;
    private Long segundosRestantes;

    public AlertaStatusResponse() {
    }

    public AlertaStatusResponse(
            Long id,
            Long usuarioId,
            Boolean ativo,
            Long usuarioAjudandoId,
            String nomeUsuarioAjudando
    ) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.ativo = ativo;
        this.usuarioAjudandoId = usuarioAjudandoId;
        this.nomeUsuarioAjudando = nomeUsuarioAjudando;
    }

    public Long getId() {
        return id;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public Long getUsuarioAjudandoId() {
        return usuarioAjudandoId;
    }

    public String getNomeUsuarioAjudando() {
        return nomeUsuarioAjudando;
    }

    public AlertaStatusResponse(Long id, Long usuarioId, Boolean ativo,
            Long usuarioAjudandoId, String nomeUsuarioAjudando,
            Double latitude, Double longitude, java.time.LocalDateTime criadaEm,
            java.time.LocalDateTime expiraEm, Long segundosRestantes) {
        this(id, usuarioId, ativo, usuarioAjudandoId, nomeUsuarioAjudando);
        this.latitude = latitude;
        this.longitude = longitude;
        this.criadaEm = criadaEm;
        this.expiraEm = expiraEm;
        this.segundosRestantes = segundosRestantes;
    }

    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public java.time.LocalDateTime getCriadaEm() { return criadaEm; }
    public java.time.LocalDateTime getExpiraEm() { return expiraEm; }
    public Long getSegundosRestantes() { return segundosRestantes; }
}
