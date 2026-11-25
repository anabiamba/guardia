package br.com.guardia.ui.screens

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class AlertaAtivoModeloTest {
    @Test fun contadorMonotonicoDescontaLatenciaENaoFicaNegativo() {
        val modelo = ModeloAlertaAtivo(1L, segundosInformados = 60, consultaIniciadaEm = 1000)
        assertEquals(59L, segundosAlerta(modelo, 2000))
        assertEquals(0L, segundosAlerta(modelo, 61_000))
        assertEquals(0L, segundosAlerta(modelo, 70_000))
        assertEquals(60L, segundosAlerta(modelo, 0))
    }
    @Test fun novaConsultaSubstituiEstimativaEDadosAusentesNaoInventamPrazo() {
        assertNull(segundosAlerta(ModeloAlertaAtivo(1), 10_000))
        assertNull(segundosAlerta(ModeloAlertaAtivo(1, segundosInformados = 3601), 0))
        assertEquals(9L, segundosAlerta(ModeloAlertaAtivo(2, segundosInformados = 10, consultaIniciadaEm = 1000), 2000))
        assertEquals("12:30", horarioInicioAlerta("2026-11-23T12:30:15.123456"))
        assertEquals("Horário indisponível", horarioInicioAlerta("null"))
    }
    @Test fun anelFechaERepresentaUmQuilometroEmTodasDirecoes() {
        val centro = PontoMapa.criar(0.0, 0.0)!!
        val anel = anelRaioAlerta(centro)
        assertEquals(73, anel.size)
        assertEquals(anel.first(), anel.last())
        anel.forEach { (lat, lon) ->
            val a = sin(Math.toRadians(lat)/2).pow(2) + cos(Math.toRadians(lat))*sin(Math.toRadians(lon)/2).pow(2)
            assertEquals(1000.0, 6371000.0*2*atan2(sqrt(a),sqrt(1-a)), 0.01)
        }
    }
}
