package br.com.guardia.dto;

import java.time.LocalDateTime;

public record HistoricoAlertaResponse(Long id, LocalDateTime criadaEm,
        LocalDateTime encerradaEm, String situacao, boolean ajudaRegistrada) {}
