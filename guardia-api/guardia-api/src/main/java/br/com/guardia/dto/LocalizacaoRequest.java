package br.com.guardia.dto;

public class LocalizacaoRequest {

    private Double latitude;
    private Double longitude;

    public LocalizacaoRequest() {
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
}