package com.alex.monitorsanatate.ui.history

import android.content.Context
import android.content.Intent
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alex.monitorsanatate.domain.model.ConnectionMethod
import com.alex.monitorsanatate.domain.model.Measurement
import com.alex.monitorsanatate.ui.components.AppScreenHeader
import com.alex.monitorsanatate.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
//  HISTORY SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToCharts: (String) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val latestEcgData by viewModel.latestEcgData.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
    val userGender by viewModel.userGender.collectAsStateWithLifecycle()
    val userAge by viewModel.userAge.collectAsStateWithLifecycle()
    val userWeight by viewModel.userWeight.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    // ID-ul înregistrării pentru care se confirmă ștergerea (null = dialog închis)
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }
    // Dialog confirmare ștergere toate vizibile
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    // Generare PDF pe thread de I/O pentru a nu bloca UI-ul
    val exportPdf: () -> Unit = {
        if (!isExporting) {
            isExporting = true
            scope.launch {
                try {
                    val file = withContext(Dispatchers.IO) {
                        generateMedicalPdf(measurements, context, currentFilter, userName, userEmail, userGender, userAge, userWeight)
                    }
                    Toast.makeText(context, "PDF salvat: ${file.name}", Toast.LENGTH_LONG).show()
                    // Încearcă să deschidă PDF-ul cu o aplicație instalată
                    try {
                        val uri = FileProvider.getUriForFile(
                            context, "${context.packageName}.provider", file
                        )
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(intent, "Deschide Raport PDF"))
                    } catch (_: Exception) {
                        // Nu există aplicație PDF instalată — fișierul e salvat oricum
                        Toast.makeText(
                            context,
                            "PDF salvat în: Documents/${file.name}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Eroare export: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isExporting = false
                }
            }
        }
    }

    // ── Dialog confirmare ștergere o singură înregistrare ────────────────────
    pendingDeleteId?.let { idToDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = PulseRedMain)
            },
            title = {
                Text("Șterge înregistrarea?", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    "Această acțiune este ireversibilă. Înregistrarea va fi eliminată definitiv din jurnal.",
                    color = TextSecondary, fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMeasurement(idToDelete)
                        pendingDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PulseRedMain)
                ) {
                    Text("Șterge", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text("Anulează", color = TextSecondary)
                }
            },
            containerColor = AppSurfaceHigh,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── Dialog confirmare ștergere toate vizibile ─────────────────────────────
    if (showDeleteAllDialog) {
        val labelFiltru = when (currentFilter) {
            "Puls" -> "Puls"
            "ECG"  -> "ECG"
            "AI"   -> "AI ECG"
            else   -> "toate categoriile"
        }
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = {
                Icon(Icons.Default.Delete, contentDescription = null, tint = PulseRedMain)
            },
            title = {
                Text("Vrei să ștergi măsurătorile înregistrate?", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = null,
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAllVisible()
                        showDeleteAllDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PulseRedMain)
                ) {
                    Text("Șterge tot", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Anulează", color = TextSecondary)
                }
            },
            containerColor = AppSurfaceHigh,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {

            AppScreenHeader(
                title = "Jurnal",
                subtitle = "Înregistrările tale",
                actions = {
                    if (currentFilter == "Puls" || currentFilter == "ECG" || currentFilter == "AI") {
                        IconButton(
                            onClick = { onNavigateToCharts(currentFilter) },
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                Icons.Filled.Timeline,
                                contentDescription = "Deschide grafice $currentFilter",
                                tint = when (currentFilter) {
                                    "ECG" -> Ral5018Light
                                    "AI"  -> Color(0xFF6750A4)
                                    else  -> PulseRedMain
                                },
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            )

            // ── [Coș] [PDF]  [Toate] [Puls] [ECG] [AI] — o singură linie fixă ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Buton Coș
                IconButton(
                    onClick = { if (measurements.isNotEmpty()) showDeleteAllDialog = true },
                    enabled = measurements.isNotEmpty(),
                    modifier = Modifier.size(36.dp).background(AppSurfaceHigh, CircleShape)
                ) {
                    Icon(
                        Icons.Outlined.DeleteOutline, "Șterge toate",
                        tint = if (measurements.isNotEmpty()) Color(0xFFEF5350) else TextDisabled,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // 2. Buton PDF
                IconButton(
                    onClick = exportPdf,
                    enabled = !isExporting,
                    modifier = Modifier.size(36.dp).background(AppSurfaceHigh, CircleShape)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(Modifier.size(15.dp), color = PulseRedMain, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.PictureAsPdf, "Export PDF", tint = PulseRedMain, modifier = Modifier.size(17.dp))
                    }
                }

                // 3–6. Chip-uri filtre — fiecare ocupa spatiu egal din ce ramane
                listOf("Toate", "Puls", "ECG", "AI").forEach { filter ->
                    val isSelected = currentFilter == filter
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .clickable { viewModel.setFilter(filter) },
                        color = if (isSelected) Ral5018Main else AppSurfaceHigh,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = filter,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else TextSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            // ── Lista / stare goală ───────────────────────────────────────────
            if (measurements.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📋", fontSize = 56.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Nu există înregistrări",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Efectuați o nouă monitorizare pentru a\ngenera un istoric medical.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    itemsIndexed(measurements, key = { _, m -> m.id }) { index, measurement ->
                        MeasurementCard(
                            measurement = measurement,
                            index = index + 1,
                            onClick = { onNavigateToDetail(measurement.id) },
                            onDelete = { pendingDeleteId = measurement.id }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  MEASUREMENT CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MeasurementCard(
    measurement: Measurement,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFmt = SimpleDateFormat("dd MMM yyyy  •  HH:mm", Locale.getDefault())
    val duration = (measurement.endTime - measurement.startTime) / 1000
    val bpm = measurement.averageBpm
    val isEcg = measurement.measurementType == "ECG" || measurement.measurementType == "EKG"
    val isAI  = measurement.measurementType == "AI_ECG"

    val (zoneLabel, zoneColor) = when {
        isAI       -> (measurement.aiResult ?: "Analiză AI") to Color(0xFF6750A4)
        bpm < 50   -> "Bradicardie severă" to Color(0xFFB71C1C)
        bpm < 60   -> "Bradicardie"         to WarningAmber
        bpm <= 100 -> "Normal"              to SuccessGreen
        bpm <= 120 -> "Tahicardie ușoară"   to WarningAmber
        else       -> "Tahicardie"          to Color(0xFFB71C1C)
    }

    val methodLabel = when (measurement.connectionMethod) {
        ConnectionMethod.CAMERA  -> "Cameră"
        ConnectionMethod.GALLERY -> "Galerie"
        ConnectionMethod.WIFI    -> "WiFi / Senzor"
        ConnectionMethod.BLE     -> "Bluetooth"
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurfaceHigh),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nr. crt + BPM
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        when {
                            isAI  -> Color(0xFF6750A4).copy(alpha = 0.15f)
                            isEcg -> Ral5018Container.copy(alpha = 0.4f)
                            else  -> MedicalBlueContainer.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isAI) {
                        Icon(
                            Icons.Filled.AutoGraph,
                            contentDescription = null,
                            tint = Color(0xFF6750A4),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "AI",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF6750A4)
                        )
                    } else {
                        Icon(
                            Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = if (isEcg) Ral5018Light else BpmRed,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "$bpm",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isEcg) Ral5018Light else BpmRed,
                            lineHeight = 21.sp
                        )
                        Text("bpm", fontSize = 8.sp, color = TextSecondary, letterSpacing = 0.5.sp)
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "#$index",
                        fontSize = 11.sp,
                        color = TextDisabled,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        dateFmt.format(Date(measurement.startTime)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${formatHistDuration(duration)}  ·  $methodLabel",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Tip măsurătoare
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isAI  -> Color(0xFF6750A4).copy(alpha = 0.15f)
                            isEcg -> Ral5018Container.copy(alpha = 0.5f)
                            else  -> AppSurface
                        }
                    ) {
                        Text(
                            when { isAI -> "AI ECG"; isEcg -> "ECG"; else -> "Puls" },
                            fontSize = 10.sp,
                            color = when {
                                isAI  -> Color(0xFF6750A4)
                                isEcg -> Ral5018Light
                                else  -> TextSecondary
                            },
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    // Interpretare clinică / rezultat AI
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = zoneColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            zoneLabel,
                            fontSize = 10.sp,
                            color = zoneColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Min/Max + buton ștergere
            Column(horizontalAlignment = Alignment.End) {
                // Buton coș — declanșează dialog confirmare
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Șterge înregistrarea",
                        tint = TextDisabled,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
                if (isAI) {
                    Text("—", fontSize = 11.sp, color = TextDisabled)
                    Text("—", fontSize = 9.sp, color = TextDisabled)
                    Text("—", fontSize = 11.sp, color = TextDisabled)
                } else {
                    Text("${measurement.minBpm}", fontSize = 11.sp, color = TextSecondary)
                    Text("—", fontSize = 9.sp, color = TextDisabled)
                    Text("${measurement.maxBpm}", fontSize = 11.sp, color = TextSecondary)
                }
                Text("min/max", fontSize = 8.sp, color = TextDisabled, letterSpacing = 0.3.sp)
            }
        }
    }
}

private fun formatHistDuration(sec: Long): String {
    val m = sec / 60; val s = sec % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}

// ─────────────────────────────────────────────────────────────────────────────
//  GENERARE PDF MEDICAL PROFESIONAL
// ─────────────────────────────────────────────────────────────────────────────

private fun generateMedicalPdf(
    measurements: List<Measurement>,
    context: Context,
    filterLabel: String,
    userName: String = "Utilizator",
    userEmail: String = "",
    userGender: String = "M",
    userAge: Int = 0,
    userWeight: Float = 0f
): File {

    // ── Dimensiuni pagină A4 (72 dpi) ────────────────────────────────────────
    val PW = 595f; val PH = 842f
    val ML = 30f;  val MR = 30f
    val CW = PW - ML - MR   // 535f lățime conținut

    // ── Poziții coloane (marginea stângă a fiecărei coloane) ─────────────────
    // Nr(26) | Data(70) | Ora(44) | Tip(46) | Metodă(56) | BPM(50) | Min/Max(60) | Durată(46) | Interpretare(137)
    val xNr     = ML
    val xData   = xNr     + 26f
    val xOra    = xData   + 70f
    val xTip    = xOra    + 44f
    val xMetoda = xTip    + 46f
    val xBpm    = xMetoda + 56f
    val xMM     = xBpm    + 50f
    val xDur    = xMM     + 60f
    val xInterp = xDur    + 46f
    val xEnd    = ML + CW   // 565f

    // ── Înălțimi rânduri/secțiuni ─────────────────────────────────────────────
    val H_HEADER   = 82f    // Header principal (teal)
    val H_SUBHDR   = 80f    // Sub-header cu informații raport (include date demografice pacient)
    val H_THEAD    = 26f    // Antet tabel
    val H_ROW      = 22f    // Înălțime rând de date
    val H_FOOTER   = 44f    // Footer pagină
    val H_SMHDR    = 42f    // Header mic pentru paginile 2+

    val MAX_Y = PH - H_FOOTER - 10f

    // ── Calcul număr total de pagini ──────────────────────────────────────────
    val rowsPage1 = ((MAX_Y - H_HEADER - H_SUBHDR - H_THEAD) / H_ROW).toInt()
    val rowsOther = ((MAX_Y - H_SMHDR - H_THEAD) / H_ROW).toInt()
    val totalPages = if (measurements.isEmpty()) 1
    else if (measurements.size <= rowsPage1) 1
    else 1 + ceil((measurements.size - rowsPage1).toDouble() / rowsOther).toInt()

    // ── Culori (ARGB int) ─────────────────────────────────────────────────────
    val C_TEAL        = 0xFF007B78.toInt()    // Header principal
    val C_TEAL_DARK   = 0xFF004F4D.toInt()    // Antet tabel
    val C_TEAL_BG     = 0xFFEAF5F5.toInt()    // Sub-header bg
    val C_TEAL_ACCENT = 0xFF009896.toInt()    // Accent text
    val C_WHITE       = android.graphics.Color.WHITE
    val C_ROW_ALT     = 0xFFF4FAFA.toInt()    // Rând alternativ
    val C_BORDER      = 0xFFCDDADA.toInt()    // Borduri tabel
    val C_TEXT        = 0xFF1A1A1A.toInt()    // Text principal
    val C_TEXT_GREY   = 0xFF555555.toInt()    // Text secundar
    val C_TEXT_LIGHT  = 0xFF999999.toInt()    // Text dezactivat
    val C_FOOTER_BG   = 0xFFF2F8F8.toInt()    // Footer bg
    // Zone clinice
    val C_GREEN_TXT   = 0xFF1B5E20.toInt()
    val C_GREEN_BG    = 0xFFE8F5E9.toInt()
    val C_ORANGE_TXT  = 0xFF7C3500.toInt()
    val C_ORANGE_BG   = 0xFFFFF8E1.toInt()
    val C_RED_TXT     = 0xFF7F0000.toInt()
    val C_RED_BG      = 0xFFFFEBEE.toInt()
    val C_DKRED_TXT   = 0xFF4A0000.toInt()
    val C_DKRED_BG    = 0xFFFFCDD2.toInt()
    val C_TEAL_TYP    = 0xFF004F4D.toInt()    // Text tip ECG
    val C_TEAL_TYPBG  = 0xFFB2DFDF.toInt()    // BG tip ECG

    // ── Formatare date ────────────────────────────────────────────────────────
    val fmtDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    val fmtTime = SimpleDateFormat("HH:mm", Locale.getDefault())
    val fmtGen  = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ro", "RO")).format(Date())

    // ── State pagini ──────────────────────────────────────────────────────────
    val doc = PdfDocument()
    var pageNum = 0
    var curY    = 0f
    var curPage: PdfDocument.Page? = null
    var cv: android.graphics.Canvas? = null

    // ── Obiecte Paint refolosibile ────────────────────────────────────────────
    val pFill   = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    val pStroke = Paint().apply {
        isAntiAlias = true; style = Paint.Style.STROKE
        strokeWidth = 0.5f; color = C_BORDER
    }

    fun txt(
        text: String, x: Float, y: Float,
        size: Float, color: Int,
        bold: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT
    ) {
        val p = Paint().apply {
            isAntiAlias  = true
            textSize     = size
            this.color   = color
            typeface     = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
            textAlign    = align
        }
        cv!!.drawText(text, x, y, p)
    }

    // ── Footer pe pagina curentă ──────────────────────────────────────────────
    fun drawFooter() {
        val fY = PH - H_FOOTER
        pFill.color = C_FOOTER_BG
        cv!!.drawRect(0f, fY, PW, PH, pFill)
        pStroke.color = C_TEAL_DARK.and(0x44FFFFFF.toInt())
        pStroke.strokeWidth = 0.8f
        cv!!.drawLine(ML, fY + 1f, xEnd, fY + 1f, pStroke)
        pStroke.strokeWidth = 0.5f

        txt("Raport medical generat automat  ·  Pagina $pageNum / $totalPages",
            PW / 2f, fY + 22f, 7.5f, C_TEXT_GREY, bold = false, align = Paint.Align.CENTER)
    }

    // ── Header principal (pagina 1) ───────────────────────────────────────────
    fun drawMainHeader() {
        // Fundal header teal
        pFill.color = C_TEAL
        cv!!.drawRect(0f, 0f, PW, H_HEADER, pFill)

        // Linie de accent (mai închis jos)
        pFill.color = C_TEAL_DARK
        cv!!.drawRect(0f, H_HEADER - 3f, PW, H_HEADER, pFill)

        // Titlu aplicație
        txt("VitalSigns", ML + 12f, 38f, 21f, C_WHITE, bold = true)

        // Subtitlu
        txt("Raport medical", ML + 12f, 57f, 10f, 0xFFB2DEDE.toInt())

        // ── Sub-header informații raport ──────────────────────────────────────
        pFill.color = C_TEAL_BG
        cv!!.drawRect(0f, H_HEADER, PW, H_HEADER + H_SUBHDR, pFill)

        // Bordură sus/jos sub-header
        pStroke.color = C_BORDER; pStroke.strokeWidth = 0.5f
        cv!!.drawLine(0f, H_HEADER + H_SUBHDR, PW, H_HEADER + H_SUBHDR, pStroke)

        val r1 = H_HEADER + 16f   // rândul 1 de informații
        val r2 = H_HEADER + 42f   // rândul 2 (mutat jos pentru a face loc emailului sub nume)
        val r3 = H_HEADER + 62f   // rândul 3 — date demografice pacient

        // Coloana stânga
        txt("Pacient:",         ML + 8f, r1, 8.5f, C_TEXT_GREY)
        txt(userName,           ML + 58f, r1, 8.5f, C_TEAL_ACCENT, bold = true)
        if (userEmail.isNotEmpty()) {
            txt(userEmail,      ML + 58f, r1 + 11f, 7.5f, C_TEXT_GREY)
        }

        txt("Data generării:",  ML + 8f, r2, 8.5f, C_TEXT_GREY)
        txt(fmtGen,             ML + 80f, r2, 8.5f, C_TEXT, bold = true)

        // Separator vertical (rândurile 1–2)
        pStroke.color = C_BORDER
        cv!!.drawLine(PW / 2f, H_HEADER + 8f, PW / 2f, H_HEADER + 53f, pStroke)

        // Coloana dreapta
        val rx = PW / 2f + 18f
        txt("Filtru activ:",       rx, r1, 8.5f, C_TEXT_GREY)
        txt(filterLabel,           rx + 65f, r1, 8.5f, C_TEAL_ACCENT, bold = true)

        txt("Total înregistrări:", rx, r2, 8.5f, C_TEXT_GREY)
        val pulsCount = measurements.count { it.measurementType != "ECG" && it.measurementType != "EKG" && it.measurementType != "AI_ECG" }
        val ecgCount  = measurements.count { it.measurementType == "ECG" || it.measurementType == "EKG" }
        val aiCount   = measurements.count { it.measurementType == "AI_ECG" }
        txt("${measurements.size}  (Puls: $pulsCount · ECG: $ecgCount · AI: $aiCount)", rx + 100f, r2, 8.5f, C_TEXT, bold = true)

        // Separator orizontal ușor sub rândurile 1–2
        pStroke.color = C_BORDER; pStroke.strokeWidth = 0.35f
        cv!!.drawLine(ML + 6f, H_HEADER + 44f, xEnd - 6f, H_HEADER + 44f, pStroke)
        pStroke.strokeWidth = 0.5f

        // Rândul 3 — profil demografic al pacientului (inclus în fiecare raport PDF generat)
        val genderLabel = if (userGender == "F") "Feminin" else "Masculin"
        val ageLabel    = if (userAge > 0) "$userAge ani" else "Nespecificată"
        val weightLabel = if (userWeight > 0f) "${userWeight.toInt()} kg" else "Nespecificată"

        txt("Gen:",      ML + 8f,   r3, 7.5f, C_TEXT_GREY)
        txt(genderLabel, ML + 30f,  r3, 7.5f, C_TEXT, bold = true)
        txt("·",         ML + 80f,  r3, 7.5f, C_TEXT_GREY)
        txt("Vârstă:",   ML + 88f,  r3, 7.5f, C_TEXT_GREY)
        txt(ageLabel,    ML + 116f, r3, 7.5f, C_TEXT, bold = true)
        txt("·",         ML + 160f, r3, 7.5f, C_TEXT_GREY)
        txt("Masă corp.:", ML + 168f, r3, 7.5f, C_TEXT_GREY)
        txt(weightLabel, ML + 210f, r3, 7.5f, C_TEXT, bold = true)
    }

    // ── Header mic (paginile 2+) ──────────────────────────────────────────────
    fun drawSmallHeader() {
        pFill.color = C_TEAL
        cv!!.drawRect(0f, 0f, PW, H_SMHDR, pFill)
        pFill.color = C_TEAL_DARK
        cv!!.drawRect(0f, H_SMHDR - 2f, PW, H_SMHDR, pFill)

        txt("VitalSigns  ·  Raport medical", ML + 10f, 26f, 11f, C_WHITE, bold = true)
        txt("Pagina $pageNum / $totalPages", xEnd, 26f, 9f, 0xFFB2DEDE.toInt(),
            align = Paint.Align.RIGHT)
    }

    // ── Antet tabel ───────────────────────────────────────────────────────────
    fun drawTableHeader(y: Float) {
        pFill.color = C_TEAL_DARK
        cv!!.drawRect(ML, y, xEnd, y + H_THEAD, pFill)

        val tY = y + H_THEAD - 7f   // baseline text antet

        fun th(text: String, x: Float) =
            txt(text, x + 3f, tY, 7.5f, C_WHITE, bold = true)

        th("NR.",                xNr)
        th("DATA",               xData)
        th("ORA",                xOra)
        th("TIP",                xTip)
        th("METODĂ",             xMetoda)
        th("BPM MED.",           xBpm)
        th("MIN / MAX",          xMM)
        th("DURATĂ",             xDur)
        th("INTERPRETARE CLINICĂ", xInterp)
    }

    // ── Rând de date ──────────────────────────────────────────────────────────
    fun drawRow(idx: Int, m: Measurement, y: Float) {
        val isAlt = idx % 2 == 1

        // Fundal rând
        pFill.color = if (isAlt) C_ROW_ALT else C_WHITE
        cv!!.drawRect(ML, y, xEnd, y + H_ROW, pFill)

        // Linie de separare jos
        pStroke.color = C_BORDER; pStroke.strokeWidth = 0.4f
        cv!!.drawLine(ML, y + H_ROW, xEnd, y + H_ROW, pStroke)

        // Separatoare verticale coloane
        for (xCol in listOf(xData, xOra, xTip, xMetoda, xBpm, xMM, xDur, xInterp)) {
            cv!!.drawLine(xCol, y + 3f, xCol, y + H_ROW - 3f, pStroke)
        }
        // Borduri stânga/dreapta
        pStroke.color = C_BORDER; pStroke.strokeWidth = 0.5f
        cv!!.drawLine(ML,   y, ML,   y + H_ROW, pStroke)
        cv!!.drawLine(xEnd, y, xEnd, y + H_ROW, pStroke)

        val tY = y + H_ROW - 7f   // baseline text rând

        // ── Conținut celule ───────────────────────────────────────────────────
        val dur = (m.endTime - m.startTime) / 1000
        val durStr = if (dur / 60 > 0) "${dur / 60}m ${dur % 60}s" else "${dur}s"

        val methodLabel = when (m.connectionMethod) {
            ConnectionMethod.CAMERA  -> "Cameră"
            ConnectionMethod.GALLERY -> "Galerie"
            ConnectionMethod.WIFI    -> "WiFi/Senzor"
            ConnectionMethod.BLE     -> "Bluetooth"
        }

        val isEcg = m.measurementType == "ECG" || m.measurementType == "EKG"
        val isAI  = m.measurementType == "AI_ECG"

        // Nr. crt (bold, gri)
        txt("${idx + 1}", xNr + 3f, tY, 8f, C_TEXT_GREY, bold = true)

        // Data
        txt(fmtDate.format(Date(m.startTime)), xData + 3f, tY, 8.5f, C_TEXT)

        // Ora
        txt(fmtTime.format(Date(m.startTime)), xOra + 3f, tY, 8.5f, C_TEXT)

        // Tip — pastilă colorată
        val typeBg  = when { isAI -> 0xFFEDE7F6.toInt(); isEcg -> C_TEAL_TYPBG;  else -> 0xFFE3F2FD.toInt() }
        val typeTxt = when { isAI -> 0xFF4527A0.toInt(); isEcg -> C_TEAL_TYP;    else -> 0xFF0D47A1.toInt() }
        val typeStr = when { isAI -> "AI ECG";            isEcg -> "ECG";          else -> "Puls" }
        pFill.color = typeBg
        cv!!.drawRoundRect(RectF(xTip + 3f, y + 5f, xTip + 42f, y + H_ROW - 5f), 4f, 4f, pFill)
        txt(typeStr, xTip + 6f, tY, 7.5f, typeTxt, bold = true)

        // Metodă
        txt(methodLabel, xMetoda + 3f, tY, 8f, C_TEXT_GREY)

        // BPM mediu — "—" pentru AI
        if (isAI) txt("—", xBpm + 3f, tY, 9f, C_TEXT_GREY)
        else txt("${m.averageBpm}", xBpm + 3f, tY, 9f, C_TEXT, bold = true)

        // Min / Max — "—/—" pentru AI
        if (isAI) txt("— / —", xMM + 3f, tY, 8f, C_TEXT_GREY)
        else txt("${m.minBpm} / ${m.maxBpm}", xMM + 3f, tY, 8f, C_TEXT_GREY)

        // Durată
        txt(durStr, xDur + 3f, tY, 8f, C_TEXT_GREY)

        // Interpretare clinică / rezultat AI — pastilă colorată
        val (interpStr, iTxt, iBg) = when {
            isAI                -> Triple(m.aiResult ?: "Analiză AI",  0xFF4527A0.toInt(), 0xFFEDE7F6.toInt())
            m.averageBpm < 50   -> Triple("Bradicardie severă",        C_DKRED_TXT,        C_DKRED_BG)
            m.averageBpm < 60   -> Triple("Bradicardie",               C_ORANGE_TXT,       C_ORANGE_BG)
            m.averageBpm <= 100 -> Triple("Normal",                    C_GREEN_TXT,        C_GREEN_BG)
            m.averageBpm <= 120 -> Triple("Tahicardie ușoară",         C_ORANGE_TXT,       C_ORANGE_BG)
            else                -> Triple("Tahicardie",                C_RED_TXT,          C_RED_BG)
        }
        val iX = xInterp + 3f
        val iW = xEnd - xInterp - 6f
        pFill.color = iBg
        cv!!.drawRoundRect(RectF(iX, y + 4f, iX + iW, y + H_ROW - 4f), 5f, 5f, pFill)
        txt(interpStr, iX + 5f, tY, 7.5f, iTxt, bold = true)
    }

    // ── Funcție pornire pagină nouă ───────────────────────────────────────────
    fun newPage(isFirst: Boolean) {
        // Finalizăm pagina anterioară (dacă există)
        if (curPage != null) {
            drawFooter()
            doc.finishPage(curPage!!)
        }
        pageNum++
        val pageInfo = PdfDocument.PageInfo.Builder(PW.toInt(), PH.toInt(), pageNum).create()
        curPage = doc.startPage(pageInfo)
        cv = curPage!!.canvas

        // Fundal alb
        pFill.color = C_WHITE
        cv!!.drawRect(0f, 0f, PW, PH, pFill)

        if (isFirst) {
            drawMainHeader()
            curY = H_HEADER + H_SUBHDR
        } else {
            drawSmallHeader()
            curY = H_SMHDR
        }
        drawTableHeader(curY)
        curY += H_THEAD
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RANDARE EFECTIVĂ
    // ─────────────────────────────────────────────────────────────────────────

    newPage(isFirst = true)

    if (measurements.isEmpty()) {
        txt(
            "Nu există înregistrări pentru filtrul selectat.",
            PW / 2f, curY + 50f, 12f, C_TEXT_GREY,
            align = Paint.Align.CENTER
        )
    }

    measurements.forEachIndexed { idx, m ->
        if (curY + H_ROW > MAX_Y) {
            newPage(isFirst = false)
        }
        drawRow(idx, m, curY)
        curY += H_ROW
    }

    // Finalizăm ultima pagină
    if (curPage != null) {
        drawFooter()
        doc.finishPage(curPage!!)
    }

    // ── Salvare fișier ────────────────────────────────────────────────────────
    val fileName = "Raport_Medical_${
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }.pdf"

    val dir  = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        ?: context.filesDir
    val file = File(dir, fileName)

    doc.writeTo(FileOutputStream(file))
    doc.close()

    return file
}
