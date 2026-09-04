package com.example.miband5.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.miband5.ble.BleConnectionState
import com.example.miband5.ui.components.BellCurveChart
import com.example.miband5.ui.components.DaySelector
import com.example.miband5.ui.components.GlassCard
import com.example.miband5.ui.components.StatPill
import com.example.miband5.ui.theme.BatteryStops
import com.example.miband5.ui.theme.HeartRateStops
import com.example.miband5.ui.theme.SleepStops
import com.example.miband5.ui.theme.StepsStops
import com.example.miband5.ui.theme.StressStops
import kotlinx.coroutines.delay
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel(),
    connectionViewModel: ConnectionViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val connState by connectionViewModel.state.collectAsState()
    val context = LocalContext.current

    var keyText by remember { mutableStateOf(connectionViewModel.savedKeyHex) }
    var showConnect by remember {
        mutableStateOf(connectionViewModel.savedKeyHex.isEmpty())
    }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) connectionViewModel.startScan()
    }

    fun neededPerms(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_SCAN)
            add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun requestPermsAndScan() {
        val missing = neededPerms().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) connectionViewModel.startScan()
        else permLauncher.launch(missing.toTypedArray())
    }

    LaunchedEffect(Unit) {
        if (connectionViewModel.savedKeyHex.isNotEmpty() &&
            connectionViewModel.savedDeviceName.isNotEmpty()
        ) {
            showConnect = false
        }
        while (true) {
            viewModel.refresh()
            delay(8_000)
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        if (showConnect) {
            ConnectPane(
                modifier = Modifier.padding(padding),
                keyText = keyText,
                onKeyChange = { keyText = it },
                connState = connState,
                onConnect = {
                    if (connectionViewModel.saveKey(keyText)) {
                        showConnect = false
                        requestPermsAndScan()
                    }
                }
            )
        } else {
            when (val s = state) {
                is DashboardUiState.Loading -> {
                    Column(Modifier.padding(padding).padding(24.dp)) {
                        Text("Loading…", color = MaterialTheme.colorScheme.onBackground)
                    }
                }
                is DashboardUiState.Ready -> ReadyDashboard(
                    modifier = Modifier.padding(padding),
                    ready = s.data,
                    viewModel = viewModel,
                    connState = connState,
                    onConnectClick = { showConnect = true },
                    onSync = { requestPermsAndScan() },
                    onDisconnect = { connectionViewModel.disconnect() }
                )
            }
        }
    }
}

@Composable
private fun ConnectPane(
    modifier: Modifier,
    keyText: String,
    onKeyChange: (String) -> Unit,
    connState: BleConnectionState,
    onConnect: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Center
    ) {
        GlassCard(Modifier.fillMaxWidth()) {
            Text("Band Stats", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text(
                "Connect over Bluetooth for live steps, battery, and heart rate. Nothing is uploaded.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = keyText,
                onValueChange = onKeyChange,
                singleLine = true,
                label = { Text("Auth key — 32 hex chars") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onConnect,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text("Connect with Bluetooth")
            }
            Spacer(Modifier.height(8.dp))
            Text(statusLabel(connState), color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun ReadyDashboard(
    modifier: Modifier,
    ready: DashboardReady,
    viewModel: DashboardViewModel,
    connState: BleConnectionState,
    onConnectClick: () -> Unit,
    onSync: () -> Unit,
    onDisconnect: () -> Unit
) {
    val day = ready.days.getOrNull(viewModel.selectedIndex) ?: ready.days.last()
    val today = ready.days.last()
    val pct = if (ready.goal > 0) ((today.steps * 100f) / ready.goal).roundToInt() else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Band Stats", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text(statusLabel(connState), fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
            StatusDot(connState)
            Spacer(Modifier.size(10.dp))
            TextButton(onClick = onConnectClick) { Text("BT") }
            Button(onClick = onSync, shape = RoundedCornerShape(999.dp)) { Text("Sync") }
        }

        Spacer(Modifier.height(16.dp))

        GlassCard(Modifier.fillMaxWidth()) {
            Text("Today", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            Text("${today.steps}", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
            Text("steps  ·  $pct% of ${ready.goal}", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                if (ready.streak > 0) "${ready.streak}-day streak" else "No streak yet",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("chart" to "Chart", "vitals" to "Vitals", "body" to "Body", "journal" to "Journal").forEach { (id, label) ->
                val on = viewModel.page == id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (on) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                        .clickable { viewModel.selectPage(id) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(label, color = if (on) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f), fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        when (viewModel.page) {
            "vitals" -> VitalsPage(ready, today)
            "body" -> BodyPage(ready, viewModel)
            "journal" -> JournalPage(day, viewModel)
            else -> ChartPage(ready, viewModel, day)
        }

        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onDisconnect) { Text("Disconnect band") }
        TextButton(onClick = { viewModel.clearAll() }) { Text("Clear saved days") }
    }
}

@Composable
private fun ChartPage(ready: DashboardReady, viewModel: DashboardViewModel, day: DayData) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row {
            listOf("Steps", "Heart", "Sleep", "Km", "Cal").forEachIndexed { i, label ->
                val selected = viewModel.selectedTab == i
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                        .clickable { viewModel.selectTab(i) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(label, color = if (selected) Color.White else Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val values = when (viewModel.selectedTab) {
            1 -> ready.days.map { (it.hrAvg ?: 0).toFloat() }
            2 -> ready.days.map { it.sleep.toFloat() }
            3 -> ready.days.map { it.distanceMeters / 1000f }
            4 -> ready.days.map { it.calories.toFloat() }
            else -> ready.days.map { it.steps.toFloat() }
        }
        BellCurveChart(values = values, selectedIndex = viewModel.selectedIndex)
        Spacer(Modifier.height(16.dp))
        DaySelector(days = ready.days, selectedIndex = viewModel.selectedIndex, onSelect = viewModel::selectDay)
    }

    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatPill("Steps", day.steps.toString(), "steps", changePct(ready, viewModel.selectedIndex) { it.steps.toFloat() }, StepsStops, Modifier.weight(1f))
        StatPill("Heart Rate", (day.hrAvg ?: 0).toString(), "bpm", changePct(ready, viewModel.selectedIndex) { (it.hrAvg ?: 0).toFloat() }, HeartRateStops, Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatPill("Sleep", formatSleep(day.sleep), "hrs", changePct(ready, viewModel.selectedIndex) { it.sleep.toFloat() }, SleepStops, Modifier.weight(1f))
        StatPill("Battery", (day.battery ?: 0).toString(), "%", null, BatteryStops, Modifier.weight(1f))
    }
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatPill("Stress", (day.stress ?: 0).toString(), "avg", null, StressStops, Modifier.weight(1f))
        StatPill("Distance", String.format("%.1f", day.distanceMeters / 1000f), "km", null, StepsStops, Modifier.weight(1f))
    }
}

@Composable
private fun VitalsPage(ready: DashboardReady, today: DayData) {
    val score = todayScore(today, ready.goal)
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Today's score", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
        Text("$score", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text(
            when {
                score >= 80 -> "Strong day"
                score >= 50 -> "Decent load"
                else -> "Light so far — connect the band to fill this in"
            },
            color = Color.White.copy(alpha = 0.75f)
        )
    }
    Spacer(Modifier.height(12.dp))
    val strain = ((today.steps / 80f) + (today.hrAvg ?: 0) / 4f).roundToInt().coerceIn(0, 21)
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Strain", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
        Text("$strain", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        Text("Built from today's steps and heart rate", color = Color.White.copy(alpha = 0.75f))
    }
    Spacer(Modifier.height(12.dp))
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Insight", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text(insight(ready, today), color = Color.White, fontSize = 15.sp)
    }
}

@Composable
private fun BodyPage(ready: DashboardReady, viewModel: DashboardViewModel) {
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Log a workout", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.logWorkout("Running") }, shape = RoundedCornerShape(999.dp)) { Text("Running") }
            Button(onClick = { viewModel.logWorkout("Freestyle") }, shape = RoundedCornerShape(999.dp)) { Text("Freestyle") }
        }
    }
    Spacer(Modifier.height(12.dp))
    GlassCard(Modifier.fillMaxWidth()) {
        Text("Workouts", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        if (ready.workouts.isEmpty()) {
            Text("Nothing logged yet.", color = Color.White.copy(alpha = 0.7f))
        } else {
            ready.workouts.take(12).forEach { w ->
                Text("${w.date}  ·  ${w.label}", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun JournalPage(day: DayData, viewModel: DashboardViewModel) {
    var note by remember(day.date) { mutableStateOf(day.notes) }
    GlassCard(Modifier.fillMaxWidth()) {
        Text(
            "Note for ${day.date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))}",
            color = Color.White.copy(alpha = 0.75f),
            fontSize = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Sports day, rest day…") }
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { viewModel.saveNote(day.date, note) }, shape = RoundedCornerShape(999.dp)) {
            Text("Save note")
        }
    }
}

@Composable
private fun StatusDot(connState: BleConnectionState) {
    val color = when (connState) {
        is BleConnectionState.Connected -> Color(0xFF4CD964)
        is BleConnectionState.Scanning,
        is BleConnectionState.Connecting,
        is BleConnectionState.Authenticating,
        is BleConnectionState.DiscoveringServices,
        is BleConnectionState.Found -> Color(0xFFFFCC00)
        is BleConnectionState.Error -> Color(0xFFFF3B30)
        else -> Color.White.copy(alpha = 0.3f)
    }
    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
}

private fun statusLabel(state: BleConnectionState): String = when (state) {
    is BleConnectionState.Idle -> "Not connected"
    is BleConnectionState.Scanning -> "Scanning…"
    is BleConnectionState.Found -> "Found ${state.name}"
    is BleConnectionState.Connecting -> "Connecting…"
    is BleConnectionState.DiscoveringServices -> "Discovering…"
    is BleConnectionState.Authenticating -> "Authenticating… tap the band if it vibrates"
    is BleConnectionState.Connected -> "Connected — live values every 10s"
    is BleConnectionState.Error -> state.message
    is BleConnectionState.Disconnected -> "Disconnected — ${state.reason}"
}

private fun formatSleep(minutes: Int): String {
    if (minutes <= 0) return "0"
    return String.format("%.1f", minutes / 60f)
}

private fun changePct(ready: DashboardReady, index: Int, value: (DayData) -> Float): Float? {
    if (index <= 0) return null
    val cur = value(ready.days[index])
    val prev = value(ready.days[index - 1])
    if (prev == 0f) return null
    return (cur - prev) / prev * 100f
}

private fun todayScore(today: DayData, goal: Int): Int {
    val stepPart = if (goal > 0) (today.steps * 60f / goal).coerceAtMost(60f) else 0f
    val hrPart = ((today.hrAvg ?: 0) / 3f).coerceAtMost(25f)
    val sleepPart = (today.sleep / 20f).coerceAtMost(15f)
    return (stepPart + hrPart + sleepPart).roundToInt().coerceIn(0, 100)
}

private fun insight(ready: DashboardReady, today: DayData): String {
    val avgSteps = ready.days.map { it.steps }.average()
    return when {
        today.steps == 0 && today.hrAvg == null ->
            "Connect the band to start filling today. Live steps and heart rate stay on this phone."
        today.steps >= ready.goal ->
            "Goal hit. Today's ${today.steps} steps are above your ${ready.goal} target."
        avgSteps > 0 && today.steps < avgSteps * 0.6 ->
            "Quieter than your week so far. A short walk would close the gap."
        else ->
            "Week average is ${avgSteps.roundToInt()} steps. Keep the band nearby so Sync can keep up."
    }
}
