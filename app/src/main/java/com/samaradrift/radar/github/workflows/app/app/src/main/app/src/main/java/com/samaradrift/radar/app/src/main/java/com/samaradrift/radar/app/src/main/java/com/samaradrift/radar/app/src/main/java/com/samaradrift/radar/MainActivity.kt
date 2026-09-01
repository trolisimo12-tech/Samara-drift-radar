package com.samaradrift.radar

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

val NEON = Color(0xFFFF6A00)
val BG = Color(0xFF0D0D0F)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        ColleagueStore.init(this)
        setContent { App() }
    }
}

@Composable
fun App() {
    var tab by remember { mutableIntStateOf(0) }
    MaterialTheme(colorScheme = darkColorScheme(primary = NEON, background = BG)) {
        Scaffold(bottomBar = {
            NavigationBar {
                NavigationBarItem({ tab == 0 }, { tab = 0 }, label = { Text("Радар") }, icon = {})
                NavigationBarItem({ tab == 1 }, { tab = 1 }, label = { Text("Смена") }, icon = {})
            }
        }) { pad ->
            Box(Modifier.padding(pad)) {
                if (tab == 0) RadarScreen() else CrewScreen()
            }
        }
    }
}

@Composable
fun RadarScreen() {
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<RadarState?>(null) }
    var log by remember { mutableStateOf("Ожидание запроса…") }
    var map by remember { mutableStateOf<MapView?>(null) }

    LaunchedEffect(Unit) {
        while (isActive) {
            val routes = ColleagueStore.routeIds()
            if (routes.isNotEmpty()) {
                runCatching {
                    val raw = TosApi.getTransportsOnRoute(routes)
                    val parsed = TransportParser.parse(raw, ColleagueStore.boardNumbers())
                    state = parsed
                    log = "Вагонов всего: ${parsed.mine.size + parsed.others.size}, коллег на линии: ${parsed.mine.size}"
                    map?.let { drawMarkers(it, parsed) }
                }.onFailure { log = "Ошибка: ${it.message}" }
            } else {
                log = "Добавьте коллегу на вкладке «Смена»"
            }
            delay(20_000)
        }
    }

    Column(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(13.0)
                    controller.setCenter(GeoPoint(53.2, 50.2))
                    map = this
                }
            },
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
        Text(log, color = NEON, modifier = Modifier.padding(8.dp))
    }
}

fun drawMarkers(map: MapView, state: RadarState) {
    map.overlays.clear()
    state.others.forEach { v ->
        map.overlays.add(Marker(map).apply {
            position = GeoPoint(v.lat, v.lng)
            title = v.stateNumber
        })
    }
    state.mine.forEach { (_, v) ->
        map.overlays.add(Marker(map).apply {
            position = GeoPoint(v.lat, v.lng)
            title = "⭐ ${v.stateNumber}"
        })
    }
    map.invalidate()
}

@Composable
fun CrewScreen() {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf(ColleagueStore.all()) }
    var name by remember { mutableStateOf("") }
    var board by remember { mutableStateOf("") }
    var route by remember { mutableStateOf("") }

    Column(Modifier.padding(12.dp)) {
        Text("СМЕНА", color = NEON, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(name, { name = it }, label = { Text("Имя коллеги") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(board, { board = it }, label = { Text("Бортовой номер") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(route, { route = it }, label = { Text("Маршрут № (номер, напр. 1)") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val n = route.trim()
            if (name.isNotBlank() && board.isNotBlank() && n.isNotEmpty()) {
                ColleagueStore.add(name.trim(), board.trim(), listOf(n.toInt()))
                list = ColleagueStore.all()
                name = ""; board = ""; route = ""
            }
        }) { Text("ДОБАВИТЬ") }

        LazyColumn {
            items(list) { c ->
                ListItem(headlineContent = { Text("${c.name} — борт ${c.stateNumber}, маршрут №${c.krIds.joinToString()}") })
            }
        }
    }
}
