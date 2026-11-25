package br.com.guardia.service;

import java.time.Clock;
import java.time.Duration;
import br.com.guardia.model.Usuario;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ValidadeLocalizacao {
    private final Clock clock;
    private final Duration validade;
    @Autowired
    public ValidadeLocalizacao(@Value("${guardia.localizacao.validade:PT15M}") Duration validade) {
        this(Clock.systemUTC(), validade);
    }
    public ValidadeLocalizacao(Clock clock, Duration validade) {
        if (validade.isNegative() || validade.isZero()) throw new IllegalArgumentException("Validade de localização inválida.");
        this.clock = clock; this.validade = validade;
    }
    public boolean vigente(Usuario usuario) {
        var atualizada = usuario.getLocalizacaoAtualizadaEm();
        var agora = clock.instant();
        return atualizada != null && !atualizada.isAfter(agora)
                && atualizada.isAfter(agora.minus(validade))
                && coordenadasValidas(usuario.getLatitude(), usuario.getLongitude());
    }
    public static boolean coordenadasValidas(Double latitude, Double longitude) {
        return latitude != null && longitude != null && Double.isFinite(latitude) && Double.isFinite(longitude)
                && Math.abs(latitude) <= 90 && Math.abs(longitude) <= 180;
    }
}
