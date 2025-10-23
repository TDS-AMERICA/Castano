package com.example.castano.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.Normalizer
import java.util.Locale
import java.util.zip.ZipInputStream

// -------------------- MODELO --------------------
data class ImportRow(
    val sku: String,                 // EN280 / EN203 (o lo que venga como interno)
    val descripcion: String? = null,
    val patas: String? = null,
    val bandejas: String? = null,
    val cajas: String? = null,
    val codigo: String? = null       // EAN / código de barras (solo dígitos)
)

// -------------------- CATÁLOGO EN MEMORIA --------------------
object ImportCatalog {
    @Volatile private var bySku: Map<String, ImportRow> = emptyMap()
    @Volatile private var byEan: Map<String, ImportRow> = emptyMap()

    private fun normSku(s: String) = s.trim().uppercase(Locale.ROOT)
    private fun digitsOnly(s: String?) = s?.filter { it.isDigit() }.orEmpty()

    fun setAll(rows: List<ImportRow>) {
        bySku = rows.filter { it.sku.isNotBlank() }
            .associateBy { normSku(it.sku) }

        byEan = rows.mapNotNull { r ->
            val e = digitsOnly(r.codigo)
            if (e.isNotEmpty()) e to r else null
        }.toMap()
    }

    /** Busca por SKU (con letras) o por EAN (solo dígitos). */
    fun get(codeOrEan: String): ImportRow? {
        val raw = codeOrEan.trim()
        val digits = raw.filter { it.isDigit() }
        if (digits.length >= 8) byEan[digits]?.let { return it } // probable EAN
        return bySku[normSku(raw)]                               // si no, SKU
    }

    fun size(): Int = maxOf(bySku.size, byEan.size)
}

// DESPUÉS (públicas)
fun splitCsvLineFlexible(line: String, delimiter: Char): List<String> {
    val out = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when (c) {
            '"' -> {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') { sb.append('"'); i++ }
                else inQuotes = !inQuotes
            }
            delimiter -> if (inQuotes) sb.append(c) else { out += sb.toString(); sb.setLength(0) }
            else -> sb.append(c)
        }
        i++
    }
    out += sb.toString()
    return out
}

fun extFromUri(context: Context, uri: Uri): String {
    val cr = context.contentResolver
    val mime = cr.getType(uri).orEmpty()
    var name: String? = null
    cr.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c: android.database.Cursor ->
        if (c.moveToFirst()) name = c.getString(0)
    }
    val byName = name?.substringAfterLast('.', "")?.lowercase(java.util.Locale.ROOT).orEmpty()
    if (byName.isNotBlank()) return byName
    return when {
        mime.contains("spreadsheetml", true) -> "xlsx"
        mime.contains("ms-excel", true)      -> "xls"
        mime.startsWith("text/", true)       -> "csv"
        else                                 -> ""
    }
}


private fun norm(h: String): String =
    Normalizer.normalize(h.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace("[\\p{Mn}\\s_\\-]".toRegex(), "")

private fun keyForHeader(h: String): String? = when (norm(h)) {
    // Interno / SKU
    "sku","codigoint","codigo_int","codint","interno","codigoitem" -> "sku"
    // EAN
    "codigo","código","ean","codigoean","barcode","barra","codigobarra","gtin" -> "codigo"
    "descripcion","descripción","desc" -> "descripcion"
    "patas","pata" -> "patas"
    "bandejas","bandeja" -> "bandejas"
    "cajas","caja" -> "cajas"
    else -> null
}

// -------------------- CSV --------------------
private fun importCsvRows(context: Context, uri: Uri): List<ImportRow> {
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "No se pudo abrir el archivo" }
        BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { br ->
            val lines = br.readLines()
            if (lines.isEmpty()) return emptyList()

            val headerRaw = lines.first().replace("\uFEFF", "")
            val delimiter = when {
                ';' in headerRaw -> ';'
                '\t' in headerRaw -> '\t'
                '|' in headerRaw -> '|'
                else -> ','
            }
            val headers = splitCsvLineFlexible(headerRaw, delimiter)
            val idx = headers.mapIndexedNotNull { i, h -> keyForHeader(h)?.let { it to i } }.toMap()

            fun col(cols: List<String>, key: String) =
                idx[key]?.let { k -> cols.getOrNull(k)?.trim().orEmpty() } ?: ""

            val out = ArrayList<ImportRow>(lines.size - 1)
            for (raw in lines.drop(1)) {
                val cols = splitCsvLineFlexible(raw.replace("\r", ""), delimiter)
                val skuRaw = col(cols, "sku")
                val eanRaw = col(cols, "codigo")
                val sku = if (skuRaw.isNotBlank()) skuRaw else eanRaw
                if (sku.isBlank()) continue

                out += ImportRow(
                    sku = sku,
                    descripcion = col(cols, "descripcion").ifBlank { null },
                    patas = col(cols, "patas").ifBlank { null },
                    bandejas = col(cols, "bandejas").ifBlank { null },
                    cajas = col(cols, "cajas").ifBlank { null },
                    codigo = eanRaw.ifBlank { null }
                )
            }
            return out
        }
    }
}

// -------------------- XLSX --------------------
private fun readSharedStrings(context: Context, uri: Uri): List<String> {
    val list = mutableListOf<String>()
    context.contentResolver.openInputStream(uri).use { input ->
        if (input == null) return emptyList()
        ZipInputStream(input).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                if (e.name == "xl/sharedStrings.xml") {
                    val p = Xml.newPullParser()
                    p.setInput(zip, "UTF-8")
                    var event = p.eventType
                    var buf: StringBuilder? = null
                    while (event != XmlPullParser.END_DOCUMENT) {
                        when (event) {
                            XmlPullParser.START_TAG -> if (p.name == "t") buf = StringBuilder()
                            XmlPullParser.TEXT -> buf?.append(p.text)
                            XmlPullParser.END_TAG -> if (p.name == "t") {
                                list += buf?.toString().orEmpty(); buf = null
                            }
                        }
                        event = p.next()
                    }
                    break
                }
                zip.closeEntry()
                e = zip.nextEntry
            }
        }
    }
    return list
}

private fun colLettersToIndex(r: String): Int {
    val letters = r.takeWhile { it.isLetter() }.uppercase(Locale.ROOT)
    var idx = 0
    for (ch in letters) idx = idx * 26 + (ch.code - 'A'.code + 1)
    return idx - 1
}

private fun importXlsxRows(context: Context, uri: Uri): List<ImportRow> {
    val shared = readSharedStrings(context, uri)
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "No se pudo abrir el archivo" }
        ZipInputStream(input).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                if (e.name.startsWith("xl/worksheets/") && e.name.endsWith(".xml")) {
                    val p = Xml.newPullParser()
                    p.setInput(zip, "UTF-8")
                    val rows = mutableListOf<Map<Int, String>>() // fila: colIndex->valor

                    var event = p.eventType
                    var inC = false; var inV = false; var inIS = false
                    var curCol = -1; var curTypeShared = false
                    var curText = StringBuilder()
                    var rowMap = mutableMapOf<Int, String>()

                    fun flushCell() {
                        if (curCol >= 0) rowMap[curCol] = curText.toString()
                        inC = false; inV = false; inIS = false
                        curCol = -1; curTypeShared = false; curText = StringBuilder()
                    }

                    while (event != XmlPullParser.END_DOCUMENT) {
                        when (event) {
                            XmlPullParser.START_TAG -> when (p.name) {
                                "row" -> rowMap = mutableMapOf()
                                "c" -> {
                                    inC = true
                                    curText = StringBuilder()
                                    curTypeShared = (p.getAttributeValue(null, "t") == "s")
                                    curCol = colLettersToIndex(p.getAttributeValue(null, "r") ?: "A1")
                                }
                                "v" -> { inV = true; curText = StringBuilder() }
                                "is" -> { inIS = true; curText = StringBuilder() }
                            }
                            XmlPullParser.TEXT -> if (inV || inIS) curText.append(p.text)
                            XmlPullParser.END_TAG -> when (p.name) {
                                "v" -> {
                                    if (curTypeShared) {
                                        val idx = curText.toString().toIntOrNull()
                                        val real = if (idx != null && idx in shared.indices) shared[idx] else ""
                                        curText = StringBuilder(real)
                                    }
                                    inV = false
                                }
                                "is" -> inIS = false
                                "c" -> flushCell()
                                "row" -> rows += rowMap
                            }
                        }
                        event = p.next()
                    }

                    val header = rows.firstOrNull().orEmpty()
                    val idx = header.entries
                        .mapNotNull { (k, v) -> keyForHeader(v)?.let { it to k } }
                        .toMap()

                    fun col(map: Map<Int, String>, key: String) =
                        idx[key]?.let { c -> map[c]?.trim().orEmpty() } ?: ""

                    val out = ArrayList<ImportRow>(rows.size - 1)
                    for (row in rows.drop(1)) {
                        val skuRaw = col(row, "sku")
                        val eanRaw = col(row, "codigo")
                        val sku = if (skuRaw.isNotBlank()) skuRaw else eanRaw
                        if (sku.isBlank()) continue

                        out += ImportRow(
                            sku = sku,
                            descripcion = col(row, "descripcion").ifBlank { null },
                            patas = col(row, "patas").ifBlank { null },
                            bandejas = col(row, "bandejas").ifBlank { null },
                            cajas = col(row, "cajas").ifBlank { null },
                            codigo = eanRaw.ifBlank { null }
                        )
                    }
                    return out
                }
                zip.closeEntry()
                e = zip.nextEntry
            }
        }
    }
    return emptyList()
}

// -------------------- API: carga archivo → catálogo --------------------
suspend fun importFileToCatalog(context: Context, uri: Uri): Int {
    val ext = extFromUri(context, uri)
    val rows: List<ImportRow> = when (ext) {
        "xlsx" -> importXlsxRows(context, uri)
        "xls"  -> emptyList() // no soportado
        "csv", "txt", "" -> importCsvRows(context, uri)
        else -> importCsvRows(context, uri)
    }
    ImportCatalog.setAll(rows)
    return rows.size
}
