package com.example.castano.screens

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
// IMPORTS NUEVOS (añádelos si no están)
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.app.DatePickerDialog
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.input.ImeAction
import com.example.castano.utils.ImportCatalog
import com.example.castano.utils.calcularTurnoAutomatico
import com.example.castano.utils.codigoSemanaDiaTurno
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CastanoApp() }
    }
}

@Composable
fun CastanoApp() {
    val nav = rememberNavController()
    MaterialTheme {
        NavHost(navController = nav, startDestination = "home") {

            composable("home") {
                HomeEntryScreen(
                    onTomarInventario = { nav.navigate("form") }
                )
            }

            composable("form") {
                IngresoDatosApp(
                    onRevisar = { nav.navigate("revisado") },
                    onVolver = { nav.popBackStack() }
                )
            }

            composable("revisado") {
                RevisadoScreen(onVolver = { nav.popBackStack() })
            }
        }
    }
}


@Composable
fun IngresoDatosApp(onRevisar: () -> Unit, onVolver: () -> Unit) {
    val fondoMarron = Color(0xFFAF703D)
    val celesteCampo = Color(0xFFD6F3FF)
    val celesteBoton = Color(0xFF8CE8F0)
    val borde = Color(0xFFB0B0B0)
    val ctx = LocalContext.current

    var ubicacion by rememberSaveable { mutableStateOf("") }
    var codigo by rememberSaveable { mutableStateOf("") }
    var pataIzq by rememberSaveable { mutableStateOf("") }
    var pataDer by rememberSaveable { mutableStateOf("0") }
    var bandejasIzq by rememberSaveable { mutableStateOf("") }
    var bandejasDer by rememberSaveable { mutableStateOf("0") }
    var unidadIzq by rememberSaveable { mutableStateOf("") }
    var unidadDer by rememberSaveable { mutableStateOf("0") }
    var cajasIzq by rememberSaveable { mutableStateOf("") }
    var cajasDer by rememberSaveable { mutableStateOf("0") }
    var total by rememberSaveable { mutableStateOf("0") }
    var descripcion by rememberSaveable { mutableStateOf("") }
    val scrollState = rememberScrollState()
    var lockPataDer by rememberSaveable { mutableStateOf(false) }
    var lockBandejasDer by rememberSaveable { mutableStateOf(false) }
    var lockCajasDer by rememberSaveable { mutableStateOf(false) }
    var lockDescripcion by rememberSaveable { mutableStateOf(false) }
    var confirmarVolver by remember { mutableStateOf(false) }

    // ▼▼▼ nuevo estado para mostrar el diálogo
    var showCDia by remember { mutableStateOf(false) }
    val totalCalc by remember(
        pataIzq, pataDer, bandejasIzq, bandejasDer, unidadIzq, cajasIzq, cajasDer
    ) {
        derivedStateOf {
            calcTotal(
                pataIzq, pataDer, bandejasIzq, bandejasDer,
                unidadIzq, cajasIzq, cajasDer
            )
        }
    }
    var wwdt by rememberSaveable { mutableStateOf<String?>(null) }
    var fechaFactMs by rememberSaveable { mutableStateOf<Long?>(null) }
    var turnoSel by rememberSaveable { mutableStateOf<Int?>(null) }
    var fechaCaptMs by rememberSaveable { mutableStateOf<Long?>(null) }

    var ean by rememberSaveable { mutableStateOf("") }

    fun lookupProducto(code: String = codigo) {
        val row = ImportCatalog.get(code)
        if (row == null) {
            Toast.makeText(ctx, "Producto no encontrado", Toast.LENGTH_SHORT).show()
            return
        }
        // Completa campos derechas + descripción + ean
        pataDer     = row.patas ?: "0"
        bandejasDer = row.bandejas ?: "0"
        cajasDer    = row.cajas ?: "0"
        descripcion = row.descripcion ?: ""
        ean         = row.codigo.orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(fondoMarron)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Ingreso de Datos",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        FilaUna("Ubicacion :", ubicacion, { ubicacion = it }, borde)

        FilaCodigoConBuscar(
            value = codigo,
            onValueChange = { nuevo ->
                codigo = nuevo
                // Autolookup si parece EAN (>= 8 dígitos). Ajusta a 13 si prefieres.
                val digits = nuevo.filter { it.isDigit() }
                if (digits.length >= 8) lookupProducto(nuevo)
            },
            bordeColor = borde,
            onBuscar = { lookupProducto(codigo) }   // Enter del teclado o botón Buscar
        )


        FilaDoble("Pata :",     pataIzq, { pataIzq = it }, pataDer, { /* no-op */ }, borde, celesteCampo, derEditable = false)


// Bandejas (izquierda) — si escriben un número > 0, apaga cajas
        FilaDoble(
            "Bandejas :", bandejasIzq, { v ->
                bandejasIzq = v
                if (toIntSafe(v) > 0) cajasIzq = "0"
            },
            bandejasDer, { /* importado, no editable */ },
            borde, celesteCampo,
            derEditable = false
        )

// Cajas (izquierda) — si escriben un número > 0, apaga bandejas
        FilaDoble(
            "Cajas :", cajasIzq, { v ->
                cajasIzq = v
                if (toIntSafe(v) > 0) bandejasIzq = "0"
            },
            cajasDer, { /* importado, no editable */ },
            borde, celesteCampo,
            derEditable = false
        )


        FilaDoble("Unidad :", unidadIzq, { unidadIzq = it }, unidadDer, { /* no-op */ },
            borde, celesteCampo, tecladoIzq = KeyboardType.Number, tecladoDer = KeyboardType.Number,
            derEditable = false)


        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Etiqueta("Total :")
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = totalCalc.toString(),
                onValueChange = { /* nada, es solo lectura */ },
                readOnly = true,
                enabled = false,
                singleLine = true,
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledContainerColor = Color.White,
                    disabledBorderColor = borde,
                    disabledTextColor = Color.Black
                )
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { showCDia = true },
                colors = ButtonDefaults.buttonColors(containerColor = celesteBoton, contentColor = Color.Black),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.height(48.dp)
            ) { Text("C. Día") }
        }

        OutlinedTextField(
            value = descripcion,
            onValueChange = {},                 // no aceptar cambios del usuario
            readOnly = true,
            enabled = false,                    // no enfoca ni abre teclado
            label = { Text("Descripción") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp),
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledContainerColor = celesteCampo,
                disabledBorderColor = borde,
                disabledTextColor = Color.Black,
                disabledLeadingIconColor = Color.Black,
                disabledTrailingIconColor = Color.Black,
                disabledLabelColor = Color.Black
            )
        )



        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            BotonCeleste("Volver", modifier = Modifier.weight(1f)) {
                confirmarVolver = true
            }

            BotonCeleste("Grabar", modifier = Modifier.weight(1f)) {
                val faltantes = mutableListOf<String>()

                if (ubicacion.isBlank() || ubicacion.trim() == "0") faltantes += "Ubicación"
                if (wwdt.isNullOrBlank()) faltantes += "C. Día"
                if (totalCalc <= 0) faltantes += "Total > 0"

                if (faltantes.isNotEmpty()) {
                    Toast.makeText(
                        ctx,
                        "No se puede guardar. Falta: ${faltantes.joinToString(", ")}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val reg = Registro(
                        ubicacion = ubicacion,
                        codigo = codigo,
                        pataIzq = pataIzq, pataDer = pataDer,
                        bandejasIzq = bandejasIzq, bandejasDer = bandejasDer,
                        unidadIzq = unidadIzq, unidadDer = unidadDer,
                        cajasIzq = cajasIzq, cajasDer = cajasDer,
                        total = totalCalc.toString(),              // <- usa el calculado
                        descripcion = descripcion,
                        wwdt = wwdt,
                        fechaFacturacion = fechaFactMs,
                        turno = turnoSel,
                        fechaCaptura = fechaCaptMs,
                        ean = ean
                    )
                    RegistroStore.add(ctx, reg)
                    Toast.makeText(ctx, "Registro guardado", Toast.LENGTH_SHORT).show()
                }
            }

            BotonCeleste("Revisar", modifier = Modifier.weight(1f)) { onRevisar() }
        }
    }
    if (showCDia) {
        FacturacionDialog(
            onCancel = { showCDia = false },
            onSelect = { fechaFact, turnoElegido, fechaCapt ->
                showCDia = false
                val code = codigoSemanaDiaTurno(fechaFact, turnoElegido)
                // guardamos en estado (para persistir hasta “Grabar”)
                wwdt = code
                fechaFactMs = fechaFact
                turnoSel = turnoElegido
                fechaCaptMs = fechaCapt
            }
        )
    }
    if (confirmarVolver) {
        AlertDialog(
            onDismissRequest = { confirmarVolver = false },
            title = { Text("¿Volver sin guardar?") },
            text = { Text("Perderás los cambios no guardados. ¿Deseas salir?") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarVolver = false
                    onVolver()        // navega atrás
                }) { Text("Sí, volver") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarVolver = false }) { Text("Cancelar") }
            }
        )
    }

}


@Composable
fun FacturacionDialog(
    onCancel: () -> Unit,
    onSelect: (fechaFacturacionMillis: Long, turno: Int, fechaCapturaMillis: Long) -> Unit
) {
    val fondoMarron = Color(0xFFAF703D)
    val celesteCampo = Color(0xFFD6F3FF)
    val celesteBoton = Color(0xFF8CE8F0)
    val borde = Color(0xFFB0B0B0)

    var turno by remember { mutableStateOf(calcularTurnoAutomatico()) }
    var fechaFact by remember { mutableStateOf(System.currentTimeMillis()) }

    // Fecha de captura fija: hoy (no editable)
    val fechaCapt = remember { System.currentTimeMillis() }

    // Número WWDT con fecha de facturación + turno
    val codigo by remember(fechaFact, turno) { mutableStateOf(codigoSemanaDiaTurno(fechaFact, turno)) }

    AlertDialog(
        onDismissRequest = onCancel,
        confirmButton = {
            Button(
                onClick = { onSelect(fechaFact, turno, fechaCapt) },
                colors = ButtonDefaults.buttonColors(containerColor = celesteBoton, contentColor = Color.Black)
            ) { Text("Seleccionar") }
        },
        dismissButton = { OutlinedButton(onClick = onCancel) { Text("Cancelar") } },
        title = { Text("C. Día", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                Text("Fecha de Facturación", color = Color.White, fontWeight = FontWeight.SemiBold)
                DateField(
                    valueMillis = fechaFact,
                    onChange = { fechaFact = it },     // ← editable: abre calendario
                    celeste = celesteCampo,
                    borde = borde,
                    enabled = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                        TurnoRadioItem("Turno 1", 1, turno == 1) { v -> turno = v }
                        TurnoRadioItem("Turno 2", 2, turno == 2) { v -> turno = v }
                        TurnoRadioItem("Turno 3", 3, turno == 3) { v -> turno = v }
                    }
                    Text(
                        codigo,
                        color = Color.White,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Fecha de Captura", color = Color.White, fontWeight = FontWeight.SemiBold)
                DateField(
                    valueMillis = fechaCapt,
                    onChange = {},                      // ← NO cambia
                    celeste = celesteCampo,
                    borde = borde,
                    enabled = false                     // ← bloqueado
                )
            }
        },
        containerColor = fondoMarron,
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
private fun TurnoRadioItem(
    texto: String,
    value: Int,
    selected: Boolean,
    onSelect: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = { onSelect(value) })
        Spacer(Modifier.width(8.dp))
        Text(texto, color = Color.White)
    }
}

@Composable
private fun DateField(
    valueMillis: Long,
    onChange: (Long) -> Unit,
    celeste: Color,
    borde: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val df = remember { SimpleDateFormat("EEEE  ,  MMMM  dd,  yyyy", Locale.getDefault()) }
    val texto = remember(valueMillis) { df.format(Date(valueMillis)) }

    val base = modifier
        .fillMaxWidth()
        .height(48.dp)

    // El click va en el contenedor, no en el TextField
    val clickable = if (enabled) {
        Modifier.clickable {
            val cal = Calendar.getInstance().apply { timeInMillis = valueMillis }
            DatePickerDialog(
                ctx,
                { _, y, m, d ->
                    val c = Calendar.getInstance()
                    c.set(y, m, d, 0, 0, 0)
                    c.set(Calendar.MILLISECOND, 0)
                    onChange(c.timeInMillis)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    } else Modifier

    Box(modifier = base.then(clickable)) {
        OutlinedTextField(
            value = texto,
            onValueChange = {},
            readOnly = true,
            enabled = false, // ¡clave! así no intercepta toques
            singleLine = true,
            modifier = Modifier.matchParentSize(),
            colors = OutlinedTextFieldDefaults.colors(
                // forzamos look “habilitado” aun estando disabled
                disabledContainerColor = Color.White,
                disabledBorderColor = borde,
                disabledTextColor = Color.Black,
                disabledLeadingIconColor = Color.Black,
                disabledTrailingIconColor = Color.Black,
                disabledPrefixColor = Color.Black,
                disabledSuffixColor = Color.Black,
            ),
            shape = RoundedCornerShape(4.dp)
        )
    }
}


@Composable private fun Etiqueta(texto: String) {
    Text(texto, color = Color.White, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.widthIn(min = 88.dp))
}

@Composable
fun FilaCodigoConBuscar(
    value: String,
    onValueChange: (String) -> Unit,
    bordeColor: Color,
    onBuscar: () -> Unit
) {
    val focus = LocalFocusManager.current

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Etiqueta("Código :")
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onBuscar()                           // ¡Llamar, no reasignar!
                    focus.moveFocus(FocusDirection.Down)
                }
            ),
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = bordeColor,
                unfocusedBorderColor = bordeColor,
                cursorColor = Color.Black
            )
        )
//        Spacer(Modifier.width(8.dp))
//        Button(
//            onClick = onBuscar,
//            colors = ButtonDefaults.buttonColors(
//                containerColor = Color(0xFF8CE8F0),
//                contentColor = Color.Black
//            ),
//            shape = RoundedCornerShape(4.dp),
//            modifier = Modifier.height(48.dp)
//        ) { Text("Buscar") }
    }
}

@Composable
private fun FilaUna(
    etiqueta: String,
    value: String,
    onValueChange: (String) -> Unit,
    bordeColor: Color
) {
    val focus = LocalFocusManager.current

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Etiqueta(etiqueta)
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focus.moveFocus(FocusDirection.Down) }
            ),
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = bordeColor,
                unfocusedBorderColor = bordeColor,
                cursorColor = Color.Black
            )
        )
    }
}


@Composable
private fun FilaDoble(
    etiqueta: String,
    izq: String,
    onIzq: (String) -> Unit,
    der: String,
    onDer: (String) -> Unit,
    bordeColor: Color,
    celeste: Color,
    tecladoIzq: KeyboardType = KeyboardType.Number,
    tecladoDer: KeyboardType = KeyboardType.Number,
    derEditable: Boolean = true
) {
    val focus = LocalFocusManager.current
    val nextFromLeft = if (derEditable) FocusDirection.Right else FocusDirection.Down

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Etiqueta(etiqueta)
        Spacer(Modifier.width(8.dp))

        OutlinedTextField(
            value = izq,
            onValueChange = onIzq,
            singleLine = true,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                keyboardType = tecladoIzq,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focus.moveFocus(nextFromLeft) }
            ),
            shape = RoundedCornerShape(4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = bordeColor,
                unfocusedBorderColor = bordeColor,
                cursorColor = Color.Black
            )
        )

        Spacer(Modifier.width(8.dp))

        OutlinedTextField(
            value = der,
            onValueChange = onDer,
            singleLine = true,
            readOnly = !derEditable,
            enabled = derEditable,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(
                keyboardType = tecladoDer,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                // del derecho pasamos a la siguiente fila
                onNext = { focus.moveFocus(FocusDirection.Down) }
            ),
            shape = RoundedCornerShape(4.dp),
            colors = if (derEditable)
                OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = celeste,
                    unfocusedContainerColor = celeste,
                    focusedBorderColor = bordeColor,
                    unfocusedBorderColor = bordeColor,
                    cursorColor = Color.Black
                )
            else
                OutlinedTextFieldDefaults.colors(
                    disabledContainerColor = celeste,
                    disabledBorderColor = bordeColor,
                    disabledTextColor = Color.Black
                )
        )
    }
}




@Composable
private fun BotonCeleste(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8CE8F0), contentColor = Color.Black),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier.height(48.dp)
    ) { Text(text) }
}

private fun toIntSafe(s: String): Int = s.trim().toIntOrNull() ?: 0

private fun calcTotal(
    pataIzq: String,
    pataDer: String,
    bandejasIzq: String,
    bandejasDer: String,
    unidadesIzq: String,
    cajasIzq: String,
    cajasDer: String
): Int {
    val pI = toIntSafe(pataIzq)
    val pD = toIntSafe(pataDer)              // bandejas por pata
    val bI = toIntSafe(bandejasIzq)          // bandejas extra (solo si cajas = 0)
    val bD = toIntSafe(bandejasDer)          // unidades por bandeja
    val uI = toIntSafe(unidadesIzq)          // unidades sueltas digitadas
    val cI = toIntSafe(cajasIzq)             // cajas extra
    val cD = toIntSafe(cajasDer)             // unidades por caja

    val basePorPatas = pI * pD * bD
    val extra = if (cI > 0) cI * cD else bI * bD
    return basePorPatas + extra + uI
}
