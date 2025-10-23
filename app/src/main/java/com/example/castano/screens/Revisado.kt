package com.example.castano.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.FileProvider
import com.example.castano.utils.ImportCatalog
import java.io.File
import java.util.Collections.emptyList
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevisadoScreen(onVolver: () -> Unit) {
    val fondoMarron = Color(0xFFAF703D)
    val ctx = LocalContext.current
    var registros by remember { mutableStateOf(RegistroStore.load(ctx)) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showFormat by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = fondoMarron,   // ← fondo general
        topBar = {
            SmallTopAppBar(
                title = { Text("Registros guardados") },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = fondoMarron,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(color = fondoMarron, shadowElevation = 4.dp) {   // ← mismo fondo
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onVolver,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) { Text("Volver") }

                    Button(
                        onClick = { showFormat = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8CE8F0),
                            contentColor = Color.Black
                        )
                    ) { Text("Enviar") }

                    Button(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) { Text("Eliminar") }
                }
            }
        }
    ) { padding ->
        if (registros.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay registros", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .background(fondoMarron)
                    .padding(padding)
            ) {
                items(registros, key = { it.timestamp }) { r ->
                    RegistroCard(r)
                }
                item { Spacer(Modifier.height(48.dp)) }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminar todos") },
            text = { Text("¿Seguro que quieres eliminar todos los registros?") },
            confirmButton = {
                TextButton(onClick = {
                    RegistroStore.clear(ctx)      // ← asegúrate de tener este método (abajo dejo snippet)
                    registros = emptyList()
                    confirmDelete = false
                    Toast.makeText(ctx, "Registros eliminados", Toast.LENGTH_SHORT).show()
                }) { Text("Sí, eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") }
            },
            containerColor = fondoMarron,
        )
    }
    // Diálogo para elegir CSV/TXT
    if (showFormat) {
        AlertDialog(
            onDismissRequest = { showFormat = false },
            title = { Text("Exportar como…") },
            text = { Text("Elige el formato del archivo a enviar por correo.") },
// dentro de if (showFormat) { AlertDialog( ... ) }
            confirmButton = {
                TextButton(onClick = {
                    showFormat = false
                    exportAndShare(ctx, registros, true)   // CSV -> true
                }) { Text("CSV") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showFormat = false
                    exportAndShare(ctx, registros, false)  // TXT -> false
                }) { Text("TXT") }
            },
            containerColor = fondoMarron,

        )
    }

    // Confirmación de eliminación (como ya tenías)
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminar todos") },
            text = { Text("¿Seguro que quieres eliminar todos los registros?") },
            confirmButton = {
                TextButton(onClick = {
                    RegistroStore.clear(ctx)
                    registros = emptyList()
                    confirmDelete = false
                    Toast.makeText(ctx, "Registros eliminados", Toast.LENGTH_SHORT).show()
                }) { Text("Sí, eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RegistroCard(r: Registro) {
    val dfFecha = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val dfSoloFecha = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFFF3F3F3)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

            Text("${r.codigo} • ${r.ubicacion}",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (r.wwdt != null || r.turno != null || r.fechaFacturacion != null || r.fechaCaptura != null) {
                FlowRow( // ← se parte en varias líneas si no cabe
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    r.wwdt?.let { SuggestionChip(onClick = {}, label = { Text(it) }) }
                    r.turno?.let { SuggestionChip(onClick = {}, label = { Text("T$it") }) }
                    r.fechaFacturacion?.let {
                        SuggestionChip(onClick = {}, label = { Text("Fact: ${dfSoloFecha.format(Date(it))}") })
                    }
                    r.fechaCaptura?.let {
                        SuggestionChip(onClick = {}, label = { Text("Cap: ${dfSoloFecha.format(Date(it))}") })
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Pata: ${r.pataIzq} | ${r.pataDer}")
                Text("Bandejas: ${r.bandejasIzq} | ${r.bandejasDer}")
                Text("Unidad: ${r.unidadIzq} | ${r.unidadDer}")
                Text("Cajas: ${r.cajasIzq} | ${r.cajasDer}")
                Text("Total: ${r.total}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (r.descripcion.isNotBlank()) {
                    Text(
                        r.descripcion,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, // ← corta textos largos
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Divider()
            Text(dfFecha.format(Date(r.timestamp)), style = MaterialTheme.typography.labelMedium)
        }
    }
}


private fun writeTempFile(ctx: Context, fileName: String, bytes: ByteArray): Uri {
    val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }   // <- /cache/exports/
    val f = File(dir, fileName)
    f.outputStream().use { it.write(bytes) }
    return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f)
}


private fun shareFileByEmail(ctx: Context, uri: Uri, mime: String, subject: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        ctx.startActivity(Intent.createChooser(intent, "Enviar con…"))
    } catch (e: ActivityNotFoundException) {
        android.widget.Toast.makeText(ctx, "No hay app para enviar", android.widget.Toast.LENGTH_LONG).show()
    }
}

fun exportAndShare(ctx: Context, registros: List<Registro>, asCsv: Boolean) {
    val fmt = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
    val base = "castano_registros_${fmt.format(Date())}"
    val name = if (asCsv) "$base.csv" else "$base.txt"
    val mime = if (asCsv) "text/csv" else "text/plain"
    val body = if (asCsv) buildCsvBody(registros) else buildTxtBody(registros)
    val uri  = writeTempFile(ctx, name, body.toByteArray(Charsets.UTF_8))
    shareFileByEmail(ctx, uri, mime, "Registros Castaño")
}

private fun formatoProcesoDesde(r: Registro): String {
    val millis = r.fechaCaptura ?: System.currentTimeMillis()
    return SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date(millis))
}



// Si no guardaste ean en Registro, intenta sacarlo del catálogo por codigoInt

private fun filaExport(r: Registro): List<String> {
    val factura   = r.wwdt.orEmpty()              // C.Día numérico (ej. 4342)
    val proceso   = formatoProcesoDesde(r)        // fecha captura d/M/yyyy
    val codigoEAN = eanDe(r)                      // EAN (780…)
    val codigoInt = r.codigo                      // EN203/EN280…
    val ubic      = r.ubicacion
    val desc      = descripcionParaExportar(r)
    val pata      = r.pataIzq
    val bandeja   = r.bandejasIzq
    val caja      = r.cajasIzq
    val unidad    = r.unidadIzq
    val total     = r.total

    return listOf(factura, proceso, codigoEAN, codigoInt, ubic, desc, pata, bandeja, caja, unidad, total)
}






private fun buildRowsFlat(regs: List<Registro>): String {
    val header = listOf(
        "Factura","Proceso","codigo","CodigoInt",
        "Ubicacion","Descripcion","Pata","Bandeja","Caja","Unidad","Total"
    ).joinToString(",")

    val rows = regs.joinToString("\n") { r ->
        val factura     = facturaFrom(r)                       // ← AQUÍ LA FACTURA
        val proceso     = procesoFrom(r)                       // fecha (día/mes/año)
        val codigoEAN   = r.ean.orEmpty()                      // “codigo” (780…)
        val codigoInt   = r.codigo                             // “CodigoInt” (EN280…)
        val ubic        = r.ubicacion
        val desc        = r.descripcion.replace("\n"," ").trim()
        val pata        = r.pataIzq
        val bandeja     = r.bandejasIzq
        val caja        = r.cajasIzq
        val unidad      = r.unidadIzq
        val total       = r.total

        listOf(
            factura, proceso, codigoEAN, codigoInt, ubic, desc, pata, bandeja, caja, unidad, total
        ).joinToString(",") { csvEscape(it) }
    }

    return "$header\n$rows"
}

private fun csvEscape(s: String): String =
    if (s.contains(',') || s.contains('"') || s.contains('\n')) {
        "\"" + s.replace("\"", "\"\"") + "\""
    } else s


private fun buildTxtBody(regs: List<Registro>): String {
    val header = "Factura,Proceso,codigo,CodigoInt,Ubicacion,Descripcion,Pata,Bandeja,Caja,Unidad,Total"
    val rows = regs.joinToString("\n") { r ->
        val factura   = facturaFrom(r)
        val proceso   = procesoFrom(r)
        val codigoEAN = eanDe(r)
        val codigoInt = r.codigo
        val ubic      = r.ubicacion
        val desc      = descripcionParaExportar(r).replace(",", " ")
        val pata      = r.pataIzq
        val bandeja   = r.bandejasIzq
        val caja      = r.cajasIzq
        val unidad    = r.unidadIzq
        val total     = r.total

        // TXT como tu ejemplo: sin comillas y con coma al final
        listOf(factura, proceso, codigoEAN, codigoInt, ubic, desc, pata, bandeja, caja, unidad, total)
            .joinToString(",") + ","
    }
    return "$header\n$rows"
}

private fun buildCsvBody(regs: List<Registro>): String {
    fun csvEscape(s: String): String =
        if (s.contains(',') || s.contains('"') || s.contains('\n')) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else s

    val header = "Factura,Proceso,codigo,CodigoInt,Ubicacion,Descripcion,Pata,Bandeja,Caja,Unidad,Total"
    val rows = regs.joinToString("\n") { r ->
        val factura   = facturaFrom(r)
        val proceso   = procesoFrom(r)
        val codigoEAN = eanDe(r)
        val codigoInt = r.codigo
        val ubic      = r.ubicacion
        val desc      = descripcionParaExportar(r)
        val pata      = r.pataIzq
        val bandeja   = r.bandejasIzq
        val caja      = r.cajasIzq
        val unidad    = r.unidadIzq
        val total     = r.total

        listOf(factura, proceso, codigoEAN, codigoInt, ubic, desc, pata, bandeja, caja, unidad, total)
            .joinToString(",") { csvEscape(it) }
    }
    return "$header\n$rows"
}


// ---------- Helpers de exportación ----------
private fun onlyDigits(s: String?): String = s?.filter { it.isDigit() } ?: ""

private val DF_PROC = java.text.SimpleDateFormat("d/M/yyyy", java.util.Locale.getDefault())

private fun procesoFrom(r: Registro): String =
    r.fechaCaptura?.let { DF_PROC.format(java.util.Date(it)) }.orEmpty()

// Si la 2ª línea de la descripción es el WWDT, la omitimos al exportar
private fun descripcionParaExportar(r: Registro): String {
    val w = r.wwdt?.trim().orEmpty()
    val lineas = r.descripcion.lines().map { it.trim() }.filter { it.isNotEmpty() }
    return lineas.firstOrNull { it != w } ?: r.descripcion.replace("\n", " ")
}

private fun eanDe(r: Registro): String =
    (r.ean ?: ImportCatalog.get(r.codigo)?.codigo).orEmpty()

// ← NUEVA versión con "fallback" por si el WWDT no se guardó
private fun facturaFrom(r: Registro): String {
    // 1) Preferimos el campo wwdt guardado
    val fromWwdt = onlyDigits(r.wwdt)
    if (fromWwdt.isNotEmpty()) return fromWwdt

    // 2) Fallback: intentar extraer de la descripción (segunda línea u otra)
    //    Busca la primera secuencia de 3-6 dígitos
    for (line in r.descripcion.lines()) {
        val digits = onlyDigits(line)
        if (digits.length in 3..6) return digits
    }
    return ""
}
