package br.com.guardia.dto;

import java.time.LocalDateTime;

/** Mantém os campos públicos anteriores do alerta e acrescenta apenas primeiroNome. */
public record AlertaProximoResponse(
        Long id, Long usuarioId, Double latitude, Double longitude, Boolean ativo,
        LocalDateTime dataHoraCriacao, LocalDateTime dataHoraEncerramento,
        Long usuarioAjudandoId, String primeiroNome) {
}
