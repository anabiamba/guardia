package br.com.guardia.ui.screens

import org.junit.Assert.*
import org.junit.Test

class MapaModeloTest {
    @Test fun rejeitaPontosInvalidosSemSubstituirPorOutroLugar() {
        assertNotNull(PontoMapa.criar(0.0, 0.0))
        assertNull(PontoMapa.criar(Double.NaN, 0.0))
        assertNull(PontoMapa.criar(0.0, Double.POSITIVE_INFINITY))
        assertNull(PontoMapa.criar(0.0, 181.0))
        assertNull(PontoMapa.criar(90.0, 0.0))
        val ponto = PontoMapa.criar(-10.25, -40.5)!!
        assertEquals(-10.25, ponto.latitude, 0.0)
        assertEquals(-40.5, ponto.longitude, 0.0)
    }
    @Test fun eventosRepetidosNaoDuplicamLifecycleNemUsamViewDestruida() {
        val chamadas = mutableListOf<String>()
        val ciclo = CicloMapa({ chamadas += "criar" }, { chamadas += "iniciar" },
            { chamadas += "retomar" }, { chamadas += "pausar" }, { chamadas += "parar" }, { chamadas += "destruir" })
        ciclo.retomar(); ciclo.iniciar(); ciclo.retomar()
        ciclo.parar(); ciclo.parar(); ciclo.retomar()
        ciclo.destruir(); ciclo.destruir(); ciclo.retomar(); ciclo.iniciar()
        assertEquals(listOf("criar", "iniciar", "retomar", "pausar", "parar",
            "iniciar", "retomar", "pausar", "parar", "destruir"), chamadas)
    }
    @Test fun podeDescartarMapaAntesDaPrimeiraExibicao() {
        val chamadas = mutableListOf<String>()
        val ciclo = CicloMapa({ chamadas += "criar" }, { error("não deve iniciar") },
            { error("não deve retomar") }, { error("não deve pausar") }, { error("não deve parar") },
            { chamadas += "destruir" })
        ciclo.destruir(); ciclo.destruir()
        assertEquals(listOf("criar", "destruir"), chamadas)
    }
}
