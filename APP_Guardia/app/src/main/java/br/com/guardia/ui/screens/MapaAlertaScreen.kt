package br.com.guardia.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.RenderingEngine
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.geojson.Polygon
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

@Composable
internal fun MapaAlertaScreen(
    latitude: Double, longitude: Double, aoVoltar: () -> Unit, abrirExterno: () -> Unit,
    erroExterno: String? = null,
    estiloUri: String = "https://tiles.openfreemap.org/styles/liberty",
    titulo: String = "Localização do SOS",
    descricao: String = "O ponto mostra a posição registrada no alerta.",
    textoVoltar: String = "VOLTAR AO ALERTA",
    textoCentralizar: String = "SOS"
) {
    var tentativa by remember(latitude, longitude) { mutableIntStateOf(0) }
    BackHandler(onBack = aoVoltar)
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = aoVoltar) { Text(textoVoltar) }
        Text(titulo, style = MaterialTheme.typography.headlineSmall)
        Text(descricao)
        val ponto = remember(latitude, longitude) { PontoMapa.criar(latitude, longitude) }
        if (ponto == null) {
            Text("Esta localização não pode ser exibida no mapa integrado. Use o mapa externo.")
            Spacer(Modifier.weight(1f))
        } else {
            key(ponto, tentativa) {
                MapaNativo(ponto, Modifier.weight(1f).fillMaxWidth(), estiloUri, textoCentralizar, repetir = { tentativa++ })
            }
        }
        Text("OpenFreeMap · © OpenMapTiles · © OpenStreetMap contributors",
            style = MaterialTheme.typography.labelSmall)
        Text("Ao abrir, o provedor de mapas recebe requisições da região visualizada.",
            style = MaterialTheme.typography.bodySmall)
        erroExterno?.let { Text(it) }
        OutlinedButton(onClick = abrirExterno, modifier = Modifier.fillMaxWidth()) { Text("ABRIR MAPA EXTERNO") }
    }
}

private class RecursoMapa(context: Context) {
    val view: MapView
    val ciclo: CicloMapa
    init {
        MapLibre.getInstance(context.applicationContext, null, WellKnownTileServer.MapLibre, RenderingEngine.Type.OPENGL)
        view = MapView(context, MapLibreMapOptions.createFromAttributes(context).textureMode(true))
        ciclo = CicloMapa({ view.onCreate(null) }, view::onStart, view::onResume,
            view::onPause, view::onStop, view::onDestroy)
    }
}

@Composable
private fun MapaNativo(ponto: PontoMapa, modifier: Modifier, estiloUri: String, textoCentralizar: String, repetir: () -> Unit, mostrarRaio: Boolean = false) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var carregado by remember { mutableStateOf(false) }
    var falhou by remember { mutableStateOf(false) }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    val recurso = remember(context) {
        try { RecursoMapa(context) }
        catch (_: Exception) { null }
        catch (_: LinkageError) { null }
    }
    DisposableEffect(recurso, owner) {
        var ativo = true
        var pontoPronto = false
        val falha = MapView.OnDidFailLoadingMapListener { if (ativo) falhou = true }
        val renderizado = MapView.OnDidFinishRenderingMapListener { completo ->
            if (ativo && pontoPronto && completo) { carregado = true; falhou = false }
        }
        val memoria = object : android.content.ComponentCallbacks {
            override fun onConfigurationChanged(config: android.content.res.Configuration) = Unit
            override fun onLowMemory() { if (ativo) recurso?.view?.onLowMemory() }
        }
        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { recurso?.ciclo?.iniciar() }
            override fun onResume(owner: LifecycleOwner) { recurso?.ciclo?.retomar() }
            override fun onPause(owner: LifecycleOwner) { recurso?.ciclo?.pausar() }
            override fun onStop(owner: LifecycleOwner) { recurso?.ciclo?.parar() }
            override fun onDestroy(owner: LifecycleOwner) { ativo = false; recurso?.ciclo?.destruir() }
        }
        if (recurso != null) {
            recurso.view.addOnDidFailLoadingMapListener(falha)
            recurso.view.addOnDidFinishRenderingMapListener(renderizado)
            recurso.view.getMapAsync { nativo ->
                if (ativo) {
                    mapa = nativo
                    nativo.uiSettings.isAttributionEnabled = true
                    nativo.uiSettings.isZoomGesturesEnabled = true
                    nativo.uiSettings.isScrollGesturesEnabled = true
                    nativo.uiSettings.isTiltGesturesEnabled = false
                    nativo.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(ponto.latitude, ponto.longitude)).zoom(if (mostrarRaio) 13.3 else 15.0).build()
                    nativo.setStyle(estiloUri) { estilo ->
                        if (ativo) {
                            try {
                            if (mostrarRaio) {
                                val anel = anelRaioAlerta(ponto).map { Point.fromLngLat(it.second, it.first) }
                                estilo.addSource(GeoJsonSource("raio-sos", Feature.fromGeometry(Polygon.fromLngLats(listOf(anel)))))
                                estilo.addLayer(FillLayer("area-sos", "raio-sos").withProperties(
                                    fillColor("#F28AB6"), fillOpacity(0.25f), fillOutlineColor("#D44489")))
                            }
                            estilo.addSource(GeoJsonSource("ponto-sos", Feature.fromGeometry(
                                Point.fromLngLat(ponto.longitude, ponto.latitude))))
                            estilo.addLayer(CircleLayer("marcador-sos", "ponto-sos").withProperties(
                                circleColor("#A32663"), circleRadius(10f),
                                circleStrokeColor("#FFFFFF"), circleStrokeWidth(3f)))
                            pontoPronto = true
                            } catch (_: Exception) { falhou = true }
                        }
                    }
                }
            }
            context.applicationContext.registerComponentCallbacks(memoria)
            owner.lifecycle.addObserver(observer)
        }
        onDispose {
            ativo = false
            owner.lifecycle.removeObserver(observer)
            if (recurso != null) context.applicationContext.unregisterComponentCallbacks(memoria)
            recurso?.view?.removeOnDidFailLoadingMapListener(falha)
            recurso?.view?.removeOnDidFinishRenderingMapListener(renderizado)
            recurso?.ciclo?.destruir()
        }
    }
    LaunchedEffect(recurso, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            delay(20_000)
            if (!carregado) falhou = true
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (recurso == null || falhou) {
            Text("Não foi possível carregar o mapa. Verifique sua conexão ou use o mapa externo.")
            Button(onClick = repetir) { Text("TENTAR NOVAMENTE") }
        } else if (!carregado) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Carregando mapa...")
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (recurso != null) {
                AndroidView(factory = { recurso.view }, modifier = Modifier.fillMaxSize()
                    .semantics { contentDescription = "Mapa com o ponto indicado. Use os controles para aproximar ou afastar." })
            }
            Column(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                FilledTonalButton(enabled = mapa != null, onClick = { mapa?.moveCamera(CameraUpdateFactory.zoomIn()) },
                    modifier = Modifier.semantics { contentDescription = "Aproximar mapa" }) { Text("+") }
                FilledTonalButton(enabled = mapa != null, onClick = { mapa?.moveCamera(CameraUpdateFactory.zoomOut()) },
                    modifier = Modifier.semantics { contentDescription = "Afastar mapa" }) { Text("−") }
                FilledTonalButton(enabled = mapa != null, onClick = {
                    mapa?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(ponto.latitude, ponto.longitude), if (mostrarRaio) 13.3 else 15.0))
                }) { Text(textoCentralizar) }
            }
        }
    }
}

@Composable
internal fun MapaResumoSos(latitude: Double, longitude: Double, modifier: Modifier,
    estiloUri: String = "https://tiles.openfreemap.org/styles/liberty") {
    val ponto = remember(latitude, longitude) { PontoMapa.criar(latitude, longitude) }
    var tentativa by remember(latitude, longitude) { mutableIntStateOf(0) }
    if (ponto == null) {
        Box(modifier, contentAlignment = Alignment.Center) { Text("Mapa indisponível para este alerta.") }
    } else key(ponto, tentativa) {
        MapaNativo(ponto, modifier, estiloUri, "SOS", repetir = { tentativa++ }, mostrarRaio = true)
    }
}
