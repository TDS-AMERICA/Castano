package com.example.castano.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Xml
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import java.util.zip.ZipInputStream
import org.xmlpull.v1.XmlPullParser



/** -------- CSV/TXT flexible -------- */


private fun importTextCount(context: Context, uri: Uri): Int {
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "No se pudo abrir el archivo" }
        BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { br ->
            val lines = br.readLines()
            if (lines.isEmpty()) return 0
            val header = lines.first().replace("\uFEFF", "")
            val delimiter = when {
                ';' in header  -> ';'
                '\t' in header -> '\t'
                '|' in header  -> '|'
                else           -> ','
            }
            return lines.drop(1).count { raw ->
                val cols = splitCsvLineFlexible(raw.replace("\r", ""), delimiter)
                cols.any { it.trim().isNotEmpty() }
            }
        }
    }
}

private fun importXlsxCount(context: Context, uri: Uri): Int {
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "No se pudo abrir el archivo" }
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name.startsWith("xl/worksheets/") && name.endsWith(".xml")) {
                    val parser = Xml.newPullParser()
                    parser.setInput(zip, "UTF-8")

                    var event = parser.eventType
                    var inRow = false
                    var rowHasValue = false
                    var inInline = false
                    var rows = 0

                    while (event != XmlPullParser.END_DOCUMENT) {
                        when (event) {
                            XmlPullParser.START_TAG -> when (parser.name) {
                                "row" -> { inRow = true; rowHasValue = false }
                                "v" -> if (inRow) {
                                    val txt = parser.nextText()
                                    if (txt.isNotBlank()) rowHasValue = true
                                }
                                "is" -> if (inRow) inInline = true
                                "t" -> if (inRow && inInline) {
                                    val txt = parser.nextText()
                                    if (txt.isNotBlank()) rowHasValue = true
                                }
                            }
                            XmlPullParser.END_TAG -> when (parser.name) {
                                "row" -> if (inRow) {
                                    if (rowHasValue) rows++
                                    inRow = false
                                }
                                "is" -> inInline = false
                            }
                        }
                        event = parser.next()
                    }
                    return if (rows > 0) rows - 1 else 0 // descuenta encabezado
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            return 0
        }
    }
}


/** -------- API pública: igual nombre que tenías -------- */
suspend fun importCsvCount(context: Context, uri: Uri): Int {
    return when (extFromUri(context, uri)) {
        "xlsx" -> importXlsxCount(context, uri)
        "xls"  -> 0 // XLS (97-2003) no soportado sin subir minSdk; conviértelo a XLSX/CSV
        "csv", "txt", "" -> importTextCount(context, uri)
        else -> importTextCount(context, uri)
    }
}
