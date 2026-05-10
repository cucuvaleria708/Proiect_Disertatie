package com.alex.monitorsanatate.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import com.alex.monitorsanatate.domain.model.ConnectionState
import com.alex.monitorsanatate.domain.model.DeviceInfo
import com.alex.monitorsanatate.ui.components.AppScreenHeader
import com.alex.monitorsanatate.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collectLatest { onLogout() }
    }
    val userName     by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail    by viewModel.userEmail.collectAsStateWithLifecycle()

    val userGender   by viewModel.userGender.collectAsStateWithLifecycle()
    val userAge      by viewModel.userAge.collectAsStateWithLifecycle()
    val userWeight   by viewModel.userWeight.collectAsStateWithLifecycle()

    val esp32State   by viewModel.esp32ConnectionState.collectAsStateWithLifecycle()
    val esp32Devices by viewModel.esp32Devices.collectAsStateWithLifecycle()

    // Stare locală profil — editabilă independent, salvată explicit la buton
    var localGender  by remember { mutableStateOf(userGender) }
    var ageText      by remember { mutableStateOf(if (userAge > 0) userAge.toString() else "") }
    var weightText   by remember { mutableStateOf(if (userWeight > 0f) userWeight.toInt().toString() else "") }
    var profileSaved by remember { mutableStateOf(false) }

    // Sincronizare când DataStore termină încărcarea asincronă
    LaunchedEffect(userGender) { localGender = userGender }
    LaunchedEffect(userAge)    { if (userAge > 0) ageText    = userAge.toString() }
    LaunchedEffect(userWeight) { if (userWeight > 0f) weightText = userWeight.toInt().toString() }

    LaunchedEffect(profileSaved) {
        if (profileSaved) {
            delay(1800L)
            profileSaved = false
        }
    }

    // Calcul praguri în timp real din valorile locale (fără a astepta salvarea)
    val localAge    = ageText.toIntOrNull()?.coerceIn(1, 120) ?: userAge
    val localWeight = weightText.toFloatOrNull()?.coerceIn(30f, 300f) ?: userWeight
    val localLow    = bpmLowThreshold(localAge)
    val localHigh   = bpmHighThreshold(localAge)

    // BLE permissions
    val bleScanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) viewModel.startScan()
    }

    fun requestScan() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        bleScanLauncher.launch(perms)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            AppScreenHeader(
                title    = "Setări",
                subtitle = "Configurare aplicație"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // ── Cont utilizator ───────────────────────────────────────────
                SettingsSection(title = "Cont", icon = Icons.Filled.AccountCircle) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier              = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Ral5018Container.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = userName.take(1).uppercase(),
                                fontSize   = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Ral5018Light
                            )
                        }
                        Column {
                            Text(
                                text       = userName,
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = TextPrimary
                            )
                            if (userEmail.isNotEmpty()) {
                                Text(text = userEmail, fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }

                // ── Profil medical ────────────────────────────────────────────
                SettingsSection(title = "Profil medical", icon = Icons.Filled.Person) {

                    Text(
                        "Gen",
                        fontSize = 12.sp, color = TextSecondary,
                        fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = localGender == "M",
                            onClick  = { localGender = "M" },
                            label    = { Text("Masculin", fontSize = 13.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Ral5018Main,
                                selectedLabelColor     = Color.White
                            )
                        )
                        FilterChip(
                            selected = localGender == "F",
                            onClick  = { localGender = "F" },
                            label    = { Text("Feminin", fontSize = 13.sp) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Ral5018Main,
                                selectedLabelColor     = Color.White
                            )
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value         = ageText,
                            onValueChange = { ageText = it.filter { c -> c.isDigit() }.take(3) },
                            label         = { Text("Vârstă (ani)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine    = true,
                            modifier      = Modifier.weight(1f),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = Ral5018Main,
                                unfocusedBorderColor = AppSurface,
                                focusedLabelColor    = Ral5018Main,
                                unfocusedTextColor   = TextPrimary,
                                focusedTextColor     = TextPrimary,
                                cursorColor          = Ral5018Main
                            )
                        )
                        OutlinedTextField(
                            value         = weightText,
                            onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' }.take(5) },
                            label         = { Text("Greutate (kg)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine    = true,
                            modifier      = Modifier.weight(1f),
                            colors        = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = Ral5018Main,
                                unfocusedBorderColor = AppSurface,
                                focusedLabelColor    = Ral5018Main,
                                unfocusedTextColor   = TextPrimary,
                                focusedTextColor     = TextPrimary,
                                cursorColor          = Ral5018Main
                            )
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = AppSurface, thickness = 0.8.dp)
                    Spacer(Modifier.height(14.dp))

                    Text(
                        "ZONE CARDIACE",
                        fontSize      = 11.sp,
                        color         = TextSecondary,
                        fontWeight    = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )

                    Spacer(Modifier.height(12.dp))

                    val animLow  by animateIntAsState(targetValue = localLow,  label = "low_bpm")
                    val animHigh by animateIntAsState(targetValue = localHigh, label = "high_bpm")

                    PulseZoneBar(animLow, animHigh)

                    Spacer(Modifier.height(14.dp))

                    ThresholdZoneRow(
                        label = "Bradicardie",
                        range = "< $animLow BPM",
                        note  = "Ritm cardiac scăzut",
                        color = WarningAmber
                    )
                    Spacer(Modifier.height(8.dp))
                    ThresholdZoneRow(
                        label = "Normal",
                        range = "$animLow – $animHigh BPM",
                        note  = "Interval sănătos",
                        color = SuccessGreen
                    )
                    Spacer(Modifier.height(8.dp))
                    ThresholdZoneRow(
                        label = "Tahicardie",
                        range = "> $animHigh BPM",
                        note  = "Ritm cardiac accelerat",
                        color = LiveRed
                    )

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.setGender(localGender)
                            ageText.toIntOrNull()?.coerceIn(1, 120)?.let { viewModel.setAge(it) }
                            weightText.toFloatOrNull()?.coerceIn(30f, 300f)?.let { viewModel.setWeight(it) }
                            profileSaved = true
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = if (profileSaved) SuccessGreen else Ral5018Main
                        )
                    ) {
                        AnimatedVisibility(visible = profileSaved, enter = fadeIn(), exit = fadeOut()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Check, contentDescription = null,
                                    tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Salvat!", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        AnimatedVisibility(visible = !profileSaved, enter = fadeIn(), exit = fadeOut()) {
                            Text("Salvează profilul", fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }

                // ── Conexiune modul ESP32 ────────────────────────────────────
                SettingsSection(
                    title = "Conexiune modul ESP32",
                    icon  = Icons.Filled.Bluetooth
                ) {
                    BleConnectionContent(
                        connectionState = esp32State,
                        devices         = esp32Devices,
                        onScan          = { requestScan() },
                        onStop          = { viewModel.stopScan() },
                        onConnect       = { viewModel.connectDevice(it) },
                        onDisconnect    = { viewModel.disconnect() }
                    )
                }

                // ── Ghid de utilizare ─────────────────────────────────────────
                SettingsSection(title = "Ghid de utilizare", icon = Icons.Filled.Info) {
                    MenuGuideRow(
                        name = "Verificare Puls",
                        description = "Măsoară-ți frecvența cardiacă în câteva secunde — " +
                            "cu senzorul extern conectat prin Bluetooth sau Wi-Fi, " +
                            "ori direct prin camera telefonului. " +
                            "Fiecare sesiune se salvează automat în Jurnal."
                    )
                    Spacer(Modifier.height(10.dp))
                    MenuGuideRow(
                        name = "Verificare ECG",
                        description = "Urmărește activitatea electrică a inimii tale în timp real, " +
                            "direct de la senzorul ECG conectat. " +
                            "Poți salva oricând un segment al traseului pentru a-l analiza mai târziu."
                    )
                    Spacer(Modifier.height(10.dp))
                    MenuGuideRow(
                        name = "Analiză AI",
                        description = "Lasă inteligența artificială să interpreteze traseul tău ECG. " +
                            "Modelul detectează automat dacă ritmul este normal sau dacă există " +
                            "anomalii, și îți prezintă rezultatul într-un mod clar și ușor de înțeles."
                    )
                    Spacer(Modifier.height(10.dp))
                    MenuGuideRow(
                        name = "Jurnal",
                        description = "Toate măsurătorile tale, organizate cronologic. " +
                            "Poți filtra după tip, șterge înregistrări individuale și exporta " +
                            "un raport PDF complet, gata de prezentat medicului."
                    )
                    Spacer(Modifier.height(10.dp))
                    MenuGuideRow(
                        name = "Setări",
                        description = "Completează-ți profilul (vârstă, sex, greutate) pentru ca " +
                            "aplicația să îți ofere interpretări personalizate. " +
                            "Tot aici poți gestiona conexiunile cu dispozitivele hardware."
                    )
                }

                // ── Despre ────────────────────────────────────────────────────
                SettingsSection(title = "Despre", icon = Icons.Filled.Shield) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Aplicație", fontSize = 14.sp, color = TextSecondary)
                        Text("Vital Signs", fontSize = 14.sp,
                            fontWeight = FontWeight.Medium, color = TextPrimary)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Model AI", fontSize = 14.sp, color = TextSecondary)
                        Text("CNN ECG · 4 clase", fontSize = 14.sp,
                            fontWeight = FontWeight.Medium, color = TextPrimary)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Hardware", fontSize = 14.sp, color = TextSecondary)
                        Text("Placă de dezvoltare ESP32", fontSize = 14.sp,
                            fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text("Senzor puls", fontSize = 14.sp,
                            fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text("Senzor ECG AD8232", fontSize = 14.sp,
                            fontWeight = FontWeight.Medium, color = TextPrimary)
                    }
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick  = { viewModel.logout() },
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = LiveRed),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.ExitToApp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Deconectare", fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Conținut BLE refolosibil ──────────────────────────────────────────────────

@Composable
private fun BleConnectionContent(
    connectionState: ConnectionState,
    devices: List<DeviceInfo>,
    onScan: () -> Unit,
    onStop: () -> Unit,
    onConnect: (DeviceInfo) -> Unit,
    onDisconnect: () -> Unit
) {
    when (connectionState) {

        is ConnectionState.Disconnected -> {
            Text(
                "Asigurați-vă că placa de dezvoltare ESP32 este alimentată, apoi inițiați scanarea Bluetooth pentru a descoperi senzorul disponibil.",
                fontSize = 13.sp, color = TextSecondary
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick  = onScan,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Ral5018Main)
            ) {
                Icon(
                    Icons.Filled.Bluetooth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint     = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text("Scanare Bluetooth", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }

        is ConnectionState.Scanning -> {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color       = Ral5018Main
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Scanare în desfășurare...",
                        fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
                }
                TextButton(onClick = onStop) {
                    Text("Oprește", fontSize = 12.sp, color = TextSecondary)
                }
            }
            if (devices.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Nu s-au găsit dispozitive. Verificați că senzorul este pornit.",
                    fontSize = 12.sp, color = TextSecondary, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(Modifier.height(10.dp))
                devices.forEach { device ->
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.name, fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(device.address, fontSize = 11.sp, color = TextSecondary)
                        }
                        Button(
                            onClick        = { onConnect(device) },
                            shape          = RoundedCornerShape(10.dp),
                            colors         = ButtonDefaults.buttonColors(containerColor = Ral5018Main),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Conectează", fontSize = 12.sp,
                                fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        is ConnectionState.Connecting -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.padding(vertical = 8.dp)
            ) {
                CircularProgressIndicator(
                    modifier    = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color       = Ral5018Main
                )
                Spacer(Modifier.width(10.dp))
                Text("Se stabilește conexiunea...",
                    fontSize = 13.sp, color = TextSecondary)
            }
        }

        is ConnectionState.Connected -> {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(SuccessGreen, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Conectat", fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(connectionState.device.name,
                        fontSize = 12.sp, color = TextSecondary)
                }
                TextButton(onClick = onDisconnect) {
                    Text("Deconectare", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }

        is ConnectionState.Error -> {
            Text(connectionState.message, fontSize = 13.sp, color = PulseRedMain)
            Spacer(Modifier.height(10.dp))
            Button(
                onClick  = onScan,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Ral5018Main)
            ) {
                Text("Încearcă din nou", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

// ── Componente helper ─────────────────────────────────────────────────────────

@Composable
private fun PulseZoneBar(lowBpm: Int, highBpm: Int) {
    val minBpm   = 40f
    val maxBpm   = 160f
    val lowFrac  = ((lowBpm  - minBpm) / (maxBpm - minBpm)).coerceIn(0.05f, 0.90f)
    val highFrac = ((highBpm - minBpm) / (maxBpm - minBpm)).coerceIn(lowFrac + 0.05f, 0.95f)

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            Box(Modifier.weight(lowFrac).fillMaxHeight().background(WarningAmber.copy(alpha = 0.80f)))
            Box(Modifier.weight(highFrac - lowFrac).fillMaxHeight().background(SuccessGreen.copy(alpha = 0.80f)))
            Box(Modifier.weight(1f - highFrac).fillMaxHeight().background(LiveRed.copy(alpha = 0.75f)))
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                "$lowBpm",
                modifier   = Modifier.weight(lowFrac),
                fontSize   = 9.sp,
                color      = WarningAmber,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.End
            )
            Spacer(Modifier.weight(highFrac - lowFrac))
            Text(
                "$highBpm",
                modifier   = Modifier.weight(1f - highFrac),
                fontSize   = 9.sp,
                color      = LiveRed,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Start
            )
        }
    }
}

@Composable
private fun ThresholdZoneRow(label: String, range: String, note: String, color: Color) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier              = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = color.copy(alpha = 0.14f)
            ) {
                Text(
                    label,
                    fontSize   = 12.sp,
                    color      = color,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Text(note, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.width(8.dp))
        Text(range, fontSize = 13.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MenuGuideRow(name: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text       = name,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color      = TextPrimary
        )
        Text(
            text       = description,
            fontSize   = 12.sp,
            color      = TextSecondary,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = AppSurfaceHigh),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier            = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null,
                    tint     = MedicalBlueLight,
                    modifier = Modifier.size(20.dp))
                Text(title,
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary)
            }
            HorizontalDivider(color = AppSurface, thickness = 0.8.dp)
            content()
        }
    }
}

