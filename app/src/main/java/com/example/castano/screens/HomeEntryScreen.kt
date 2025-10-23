package com.example.castano.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.castano.utils.ImportStore
import com.example.castano.utils.importCsvCount
import kotlinx.coroutines.launch
import com.example.castano.R
import com.example.castano.utils.importFileToCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HomeEntryScreen(
    onTomarInventario: () -> Unit
) {
    val fondoMarron = Color(0xFFAF703D)
    val celesteBoton = Color(0xFF8CE8F0)

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var totalImportados by remember { mutableStateOf(0) }

    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {}

            scope.launch {
                val n = withContext(Dispatchers.IO) {
                    importFileToCatalog(ctx, uri)   // ← AQUÍ cargamos el catálogo
                }
                // Guarda para mostrar en Home (opcional)
                ImportStore.setCount(ctx, n)
                ImportStore.setLastUri(ctx, uri.toString())
                totalImportados = n
                Toast.makeText(ctx, "Catálogo cargado: $n ítems", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Carga rápida del número guardado
    LaunchedEffect(Unit) {
        totalImportados = ImportStore.getCount(ctx)
    }

    // 🔁 Recarga el catálogo desde el último archivo usado
    LaunchedEffect(Unit) {
        val last = ImportStore.getLastUri(ctx) ?: return@LaunchedEffect
        val uri = Uri.parse(last)
        try {
            // reintenta el permiso persistente (por si hace falta tras reinicio)
            ctx.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) { /* puede no ser necesario */ }

        try {
            val n = withContext(Dispatchers.IO) {
                importFileToCatalog(ctx, uri) // reconstruye ImportCatalog
            }
            totalImportados = n
            ImportStore.setCount(ctx, n) // opcional: refresca el contador guardado
            // Toast.makeText(ctx, "Catálogo recargado: $n", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(ctx, "No se pudo recargar catálogo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    // UN solo launcher
    val openDoc = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {}

            scope.launch(Dispatchers.IO) {
                try {
                    // Carga CSV/XLSX al catálogo en memoria
                    val n = com.example.castano.utils.importFileToCatalog(ctx, uri)
                    // (opcional) guarda referencia
                    com.example.castano.utils.ImportStore.setLastUri(ctx, uri.toString())
                    com.example.castano.utils.ImportStore.setCount(ctx, n)

                    withContext(Dispatchers.Main) {
                        totalImportados = n
                        Toast.makeText(ctx, "Catálogo cargado: $n SKUs", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(ctx, "Error al importar: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(fondoMarron)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top)
    ) {

        Image( painter = painterResource(R.drawable.logo_castano),
            contentDescription = "Castaño",
            modifier = Modifier.size(120.dp),
            contentScale = ContentScale.Fit )

        Text(
            "Castaño",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

// Reemplaza TODO el OutlinedTextField por esto:
        Text(
            text = "Maestro cargado: $totalImportados ítems",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
                .align(Alignment.CenterHorizontally)


        )

        Button(
            onClick = { onTomarInventario() },
            colors = ButtonDefaults.buttonColors(
                containerColor = celesteBoton,
                contentColor = Color.Black
            ),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("Tomar inventario") }

        Button(
            onClick = {
                // usa tipos amplios por si es xlsx
                pickFile.launch(arrayOf(
                    "text/csv","text/plain",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel","application/octet-stream","*/*"
                ))
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = celesteBoton,
                contentColor = Color.Black
            ),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text("Cargar catálogo (CSV/XLSX)") }
    }
}

