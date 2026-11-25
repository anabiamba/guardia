package br.com.guardia.dto;

public class EncerrarAlertaRequest {

    private Long usuarioId;

    public EncerrarAlertaRequest() {
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }
}