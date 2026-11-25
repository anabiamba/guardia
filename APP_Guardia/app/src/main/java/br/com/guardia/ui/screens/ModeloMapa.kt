package br.com.guardia.ui.screens

internal class PontoMapa private constructor(val latitude: Double, val longitude: Double) {
    companion object {
        fun criar(latitude: Double, longitude: Double): PontoMapa? =
            if (latitude.isFinite() && longitude.isFinite() &&
                latitude in -85.05112878..85.05112878 && longitude in -180.0..180.0)
                PontoMapa(latitude, longitude) else null
    }
}

// Encaminha eventos sem duplicar start/resume ou usar uma view já destruída.
internal class CicloMapa(
    criar: () -> Unit,
    private val iniciarView: () -> Unit,
    private val retomarView: () -> Unit,
    private val pausarView: () -> Unit,
    private val pararView: () -> Unit,
    private val destruirView: () -> Unit
) {
    private var iniciado = false
    private var retomado = false
    private var destruido = false
    init { criar() }
    fun iniciar() { if (!destruido && !iniciado) { iniciarView(); iniciado = true } }
    fun retomar() {
        if (destruido) return
        iniciar()
        if (!retomado) { retomarView(); retomado = true }
    }
    fun pausar() { if (!destruido && retomado) { pausarView(); retomado = false } }
    fun parar() {
        if (destruido) return
        pausar()
        if (iniciado) { pararView(); iniciado = false }
    }
    fun destruir() {
        if (destruido) return
        parar()
        destruirView()
        destruido = true
    }
}
