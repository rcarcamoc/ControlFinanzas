package com.aranthalion.controlfinanzas.presentation.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import com.aranthalion.controlfinanzas.data.util.ExcelProcessor
import com.aranthalion.controlfinanzas.data.util.ExcelTransaction
import androidx.hilt.navigation.compose.hiltViewModel
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportarExcelScreen(viewModel: MovimientosViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var archivoUri by remember { mutableStateOf<Uri?>(null) }
    var archivoNombre by remember { mutableStateOf("") }
    var tipoArchivo by remember { mutableStateOf("") }
    var expandedTipo by remember { mutableStateOf(false) }
    var expandedMes by remember { mutableStateOf(false) }
    var mesSeleccionado by remember { mutableStateOf("") }
    val tipos = listOf("Estado de cierre", "Últimos movimientos")
    val calendar = Calendar.getInstance()
    val meses = (0..11).map { offset ->
        val cal = calendar.clone() as Calendar
        cal.add(Calendar.MONTH, -offset)
        val year = cal.get(Calendar.YEAR)
        val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        "$year-$month"
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        archivoUri = uri
        archivoNombre = uri?.let {
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (c.moveToFirst() && nameIndex >= 0) c.getString(nameIndex) else ""
            } ?: ""
        } ?: ""
    }
    var resultado by remember { mutableStateOf<List<MovimientoEntity>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var exito by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Importar archivo Excel", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Button(onClick = { launcher.launch("application/vnd.ms-excel") }) {
            Text(if (archivoNombre.isNotEmpty()) "Archivo: $archivoNombre" else "Seleccionar archivo Excel")
        }
        ExposedDropdownMenuBox(expanded = expandedTipo, onExpandedChange = { expandedTipo = !expandedTipo }) {
            OutlinedTextField(
                value = tipoArchivo,
                onValueChange = {},
                label = { Text("Tipo de archivo") },
                readOnly = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) }
            )
            ExposedDropdownMenu(expanded = expandedTipo, onDismissRequest = { expandedTipo = false }) {
                tipos.forEach { tipo ->
                    DropdownMenuItem(
                        text = { Text(tipo) },
                        onClick = {
                            tipoArchivo = tipo
                            expandedTipo = false
                        }
                    )
                }
            }
        }
        ExposedDropdownMenuBox(expanded = expandedMes, onExpandedChange = { expandedMes = !expandedMes }) {
            OutlinedTextField(
                value = mesSeleccionado,
                onValueChange = {},
                label = { Text("Mes de ciclo de facturación") },
                readOnly = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMes) }
            )
            ExposedDropdownMenu(expanded = expandedMes, onDismissRequest = { expandedMes = false }) {
                meses.forEach { mes ->
                    DropdownMenuItem(
                        text = { Text(mes) },
                        onClick = {
                            mesSeleccionado = mes
                            expandedMes = false
                        }
                    )
                }
            }
        }
        Button(
            onClick = {
                resultado = null
                error = null
                exito = false
                if (archivoUri != null) {
                    try {
                        val inputStream = context.contentResolver.openInputStream(archivoUri!!)
                        val transacciones = when (tipoArchivo) {
                            "Estado de cierre" -> ExcelProcessor.importarEstadoDeCierre(inputStream!!, mesSeleccionado)
                            "Últimos movimientos" -> ExcelProcessor.importarUltimosMovimientos(inputStream!!, mesSeleccionado)
                            else -> emptyList()
                        }
                        // Filtrar y transformar a MovimientoEntity
                        val movimientos = transacciones.filter {
                            it.descripcion.trim().uppercase() != "MONTO CANCELADO"
                        }.mapNotNull { t ->
                            if (t.fecha == null) return@mapNotNull null
                            MovimientoEntity(
                                tipo = "GASTO",
                                monto = t.monto,
                                descripcion = t.descripcion,
                                fecha = t.fecha,
                                periodoFacturacion = mesSeleccionado,
                                categoriaId = null,
                                tipoTarjeta = t.tipoTarjeta
                            )
                        }
                        resultado = movimientos
                        // Insertar en la base de datos
                        CoroutineScope(Dispatchers.IO).launch {
                            movimientos.forEach { mov ->
                                viewModel.agregarMovimiento(mov)
                            }
                            exito = true
                        }
                    } catch (e: Exception) {
                        error = "Error al procesar el archivo: ${e.message}"
                    }
                }
            },
            enabled = archivoUri != null && tipoArchivo.isNotEmpty() && mesSeleccionado.isNotEmpty()
        ) {
            Text("Procesar archivo e importar")
        }
        if (resultado != null) {
            Text("Movimientos a importar: ${resultado!!.size}", fontWeight = FontWeight.Bold)
            resultado!!.take(5).forEachIndexed { i, t ->
                Text("#${i+1}: ${t.fecha} | ${t.descripcion} | ${t.monto}")
            }
        }
        if (exito) {
            Text("¡Importación completada!", color = MaterialTheme.colorScheme.primary)
        }
        if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error)
        }
    }
} 