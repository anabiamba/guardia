package br.com.guardia.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import com.google.firebase.messaging.FirebaseMessagingException;

/** Códigos operacionais sem mensagens ou payloads do provedor. */
public class FalhaPush extends RuntimeException {
    public enum Tipo { TRANSITORIA, TOKEN_INVALIDO, DEFINITIVA }
    private final Tipo tipo;
    private final Duration esperaMinima;

    public FalhaPush(Tipo tipo, Duration esperaMinima) {
        super("Falha no despacho push: " + tipo.name());
        this.tipo = tipo;
        this.esperaMinima = esperaMinima;
    }

    public Tipo getTipo() { return tipo; }
    public Duration getEsperaMinima() { return esperaMinima; }

    public static FalhaPush de(FirebaseMessagingException erro, Instant agora) {
        var codigo = erro.getMessagingErrorCode();
        if (codigo == com.google.firebase.messaging.MessagingErrorCode.UNREGISTERED) {
            return new FalhaPush(Tipo.TOKEN_INVALIDO, Duration.ZERO);
        }
        boolean temporaria = codigo == com.google.firebase.messaging.MessagingErrorCode.UNAVAILABLE
                || codigo == com.google.firebase.messaging.MessagingErrorCode.INTERNAL
                || codigo == com.google.firebase.messaging.MessagingErrorCode.QUOTA_EXCEEDED;
        if (codigo == null) {
            temporaria = erro.getErrorCode() == com.google.firebase.ErrorCode.UNAVAILABLE
                    || erro.getErrorCode() == com.google.firebase.ErrorCode.INTERNAL
                    || erro.getErrorCode() == com.google.firebase.ErrorCode.RESOURCE_EXHAUSTED
                    || erro.getErrorCode() == com.google.firebase.ErrorCode.UNKNOWN;
        }
        if (!temporaria) return new FalhaPush(Tipo.DEFINITIVA, Duration.ZERO);
        Duration espera = Duration.ofSeconds(60);
        if (erro.getHttpResponse() != null) {
            Object valor = erro.getHttpResponse().getHeaders().entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase("Retry-After"))
                    .map(java.util.Map.Entry::getValue).findFirst().orElse(null);
            if (valor instanceof java.util.List<?> valores && !valores.isEmpty()) valor = valores.get(0);
            if (valor != null) {
                try {
                    Duration solicitada;
                    String texto = valor.toString().trim();
                    if (texto.matches("[0-9]+")) solicitada = Duration.ofSeconds(Long.parseLong(texto));
                    else solicitada = Duration.between(agora, ZonedDateTime.parse(texto, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
                    if (solicitada.compareTo(espera) > 0) espera = solicitada;
                } catch (IllegalArgumentException | java.time.DateTimeException e) {
                    // Mantém espera conservadora se o cabeçalho for inválido.
                }
            }
        }
        return new FalhaPush(Tipo.TRANSITORIA, espera);
    }
}
