package com.example.castano.utils

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/** Lee todo el archivo a memoria para poder abrir el ZIP dos veces */
private fun readAllBytes(context: Context, uri: Uri): ByteArray =
    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: error("No se pudo abrir el archivo")

/** Convierte "BC23" -> índice de columna 1-based (BC=55) y 0-based (54) */
private fun colIndexZeroBased(cellRef: String): Int {
    var col = 0
    for (ch in cellRef) {
        if (ch in 'A'..'Z') {
            col = col * 26 + (ch - 'A' + 1)
        } else if (ch in 'a'..'z') {
            col = col * 26 + (ch - 'a' + 1)
        } else break
    }
    return col - 1
}

/** sharedStrings.xml -> lista de strings compartidos */
private fun readSharedStrings(xlsx: ByteArray): List<String> {
    val out = mutableListOf<String>()
    ZipInputStream(ByteArrayInputStream(xlsx)).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            if (entry.name == "xl/sharedStrings.xml") {
                val parser = Xml.newPullParser()
                parser.setInput(zip, "UTF-8")
                var event = parser.eventType
                var collecting = false
                val sb = StringBuilder()
                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            if (parser.name == "si") {
                                sb.setLength(0)
                            } else if (parser.name == "t") {
                                collecting = true
                            }
                        }
                        XmlPullParser.TEXT -> if (collecting) sb.append(parser.text)
                        XmlPullParser.END_TAG -> when (parser.name) {
                            "t" -> collecting = false
                            "si" -> out += sb.toString()
                        }
                    }
                    event = parser.next()
                }
                break
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }
    return out
}

/** Primer worksheet encontrado -> tabla completa (incluye encabezado) */
fun importXlsxTable(context: Context, uri: Uri): List<List<String>> {
    val bytes = readAllBytes(context, uri)
    val shared = readSharedStrings(bytes)

    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
        var entry = zip.nextEntry
        while (entry != null) {
            val name = entry.name
            if (name.startsWith("xl/worksheets/") && name.endsWith(".xml")) {
                val parser = Xml.newPullParser()
                parser.setInput(zip, "UTF-8")

                val table = mutableListOf<MutableList<String>>()
                var event = parser.eventType

                var currentRow: MutableMap<Int, String>? = null
                var cellCol: Int = -1
                var cellType: String? = null
                var inInline = false

                fun flushRow() {
                    val map = currentRow ?: return
                    if (map.isEmpty()) return
                    val maxIdx = map.keys.maxOrNull() ?: -1
                    val row = MutableList(maxIdx + 1) { "" }
                    for ((k, v) in map) if (k in row.indices) row[k] = v
                    table += row
                }

                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> when (parser.name) {
                            "row" -> currentRow = mutableMapOf()
                            "c" -> {
                                val ref = parser.getAttributeValue(null, "r").orEmpty()
                                cellCol = if (ref.isNotEmpty()) colIndexZeroBased(ref) else -1
                                cellType = parser.getAttributeValue(null, "t") // "s", "b", "inlineStr", etc.
                                inInline = false
                            }
                            "v" -> {
                                val txt = parser.nextText().orEmpty()
                                val value = when (cellType) {
                                    "s" -> txt.toIntOrNull()?.let { shared.getOrNull(it) } ?: ""
                                    "b" -> if (txt == "1") "TRUE" else if (txt == "0") "FALSE" else txt
                                    else -> txt
                                }
                                if (cellCol >= 0) currentRow?.set(cellCol, value)
                            }
                            "is" -> inInline = true
                            "t" -> if (inInline && cellCol >= 0) {
                                val txt = parser.nextText().orEmpty()
                                currentRow?.set(cellCol, txt)
                            }
                        }
                        XmlPullParser.END_TAG -> when (parser.name) {
                            "row" -> { flushRow(); currentRow = null }
                            "is" -> inInline = false
                        }
                    }
                    event = parser.next()
                }

                return table
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }
    return emptyList()
}

/** CSV/TXT como tabla (auto-delimiter) */
fun importTextTable(context: Context, uri: Uri): List<List<String>> {
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "No se pudo abrir el archivo" }
        val lines = input.bufferedReader(Charsets.UTF_8).readLines()
        if (lines.isEmpty()) return emptyList()
        val header = lines.first().replace("\uFEFF", "")
        val delimiter = when {
            ';' in header  -> ';'
            '\t' in header -> '\t'
            '|' in header  -> '|'
            else           -> ','
        }
        return lines.map { raw ->
            val line = raw.replace("\r", "")
            splitCsvLineFlexible(line, delimiter)
        }
    }
}

/** API de alto nivel: devuelve tabla según extensión */
fun importTable(context: Context, uri: Uri): List<List<String>> =
    when (extFromUri(context, uri)) {
        "xlsx" -> importXlsxTable(context, uri)
        "xls"  -> emptyList() // convertir a XLSX/CSV si necesitas soportar XLS 97-2003
        "csv", "txt", "" -> importTextTable(context, uri)
        else -> importTextTable(context, uri)
    }
