package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aranthalion.controlfinanzas.domain.usecase.AnalisisGastoCategoria
import com.aranthalion.controlfinanzas.domain.usecase.EstadoAnalisis
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import kotlin.math.abs
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalisisGastoPorCategoriaScreen(
    navController: NavController,
    viewModel: AnalisisGastoPorCategoriaViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.cargarAnalisis()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Análisis de Gasto por Categoría",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { viewModel.cargarAnalisis() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Resumen
        uiState.resumen?.let { resumen ->
            ResumenAnalisisCard(resumen = resumen)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Botón de Re-analizar
        if (!uiState.isLoading && uiState.error == null) {
            Button(
                onClick = { viewModel.cargarAnalisis() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Re-analizar Datos",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Estado de carga
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            ErrorCard(
                error = uiState.error!!,
                onDismiss = { viewModel.limpiarError() }
            )
        } else {
            // Tabla de análisis
            AnalisisGastoTable(
                analisis = uiState.analisis,
                periodoActual = uiState.periodoActual
            )
        }
    }
}

@Composable
private fun ResumenAnalisisCard(resumen: com.aranthalion.controlfinanzas.domain.usecase.ResumenAnalisisGasto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Resumen del Análisis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResumenItem(
                    label = "Categorías",
                    value = "${resumen.categoriasAnalizadas}/${resumen.totalCategorias}",
                    icon = Icons.Default.List
                )
                ResumenItem(
                    label = "Con Desviación",
                    value = "${resumen.categoriasConDesviacion}",
                    icon = Icons.Default.Warning,
                    color = if (resumen.categoriasConDesviacion > 0) Color.Red else Color.Green
                )
                ResumenItem(
                    label = "Críticas",
                    value = "${resumen.categoriasCriticas}",
                    icon = Icons.Default.Warning,
                    color = if (resumen.categoriasCriticas > 0) Color.Red else Color.Green
                )
                ResumenItem(
                    label = "Excelentes",
                    value = "${resumen.categoriasExcelentes}",
                    icon = Icons.Default.Star,
                    color = Color.Green
                )
            }
        }
    }
}

@Composable
private fun ResumenItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AnalisisGastoTable(
    analisis: List<AnalisisGastoCategoria>,
    periodoActual: String
) {
    var categoriaSeleccionada by remember { mutableStateOf<AnalisisGastoCategoria?>(null) }
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Análisis Detallado - $periodoActual",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Header de la tabla
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Categoría",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text = "Gasto Actual",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "% Gasto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Desviación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Filas de datos
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(analisis) { item ->
                    Box(modifier = Modifier.clickable { categoriaSeleccionada = item }) {
                        AnalisisGastoRow(item = item)
                    }
                }
            }
            // Tabla de movimientos si hay categoría seleccionada
            categoriaSeleccionada?.let { catSel ->
                Spacer(modifier = Modifier.height(24.dp))
                DetalleMovimientosCategoria(categoria = catSel.categoria, periodo = periodoActual, onClose = { categoriaSeleccionada = null })
            }
        }
    }
}

@Composable
private fun AnalisisGastoRow(item: AnalisisGastoCategoria) {
    val backgroundColor = when {
        item.porcentajeGastado < 90 -> Color(0xFFE8F5E8) // Verde claro
        item.porcentajeGastado <= 100 -> Color(0xFFFFF3E0) // Amarillo claro
        else -> Color(0xFFFFEBEE) // Rojo claro
    }
    
    val textColor = when {
        item.porcentajeGastado < 90 -> Color(0xFF2E7D32) // Verde oscuro
        item.porcentajeGastado <= 100 -> Color(0xFFE65100) // Amarillo oscuro
        else -> Color(0xFFC62828) // Rojo oscuro
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Categoría
        Column(
            modifier = Modifier.weight(2f)
        ) {
            Text(
                text = item.categoria.nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
            if (item.presupuesto > 0) {
                Text(
                    text = "Presupuesto: ${FormatUtils.formatMoney(item.presupuesto)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
        
        // Gasto Actual
        Text(
            text = FormatUtils.formatMoney(item.gastoActual),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = textColor
        )
        
        // Porcentaje de Gasto
        Text(
            text = "${String.format("%.1f", item.porcentajeGastado)}%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = textColor
        )
        
        // Desviación
        val desviacionColor = when {
            item.desviacion > 0 -> Color.Red
            item.desviacion < 0 -> Color.Green
            else -> textColor
        }
        val desviacionIcon = when {
            item.desviacion > 0 -> Icons.Default.KeyboardArrowUp
            item.desviacion < 0 -> Icons.Default.KeyboardArrowDown
            else -> Icons.Default.Check
        }
        
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = desviacionIcon,
                contentDescription = null,
                tint = desviacionColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${String.format("%.1f", abs(item.desviacion))}%",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = desviacionColor
            )
        }
    }
    
    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
}

@Composable
private fun DetalleMovimientosCategoria(
    categoria: com.aranthalion.controlfinanzas.domain.categoria.Categoria,
    periodo: String,
    onClose: () -> Unit
) {
    val viewModel: AnalisisGastoPorCategoriaViewModel = hiltViewModel()
    var movimientos by remember { mutableStateOf<List<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(categoria.id, periodo) {
        isLoading = true
        movimientos = viewModel.obtenerMovimientosPorCategoriaYPeriodo(categoria.id, periodo)
        isLoading = false
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Movimientos de la categoría: ${categoria.nombre}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (isLoading) {
            CircularProgressIndicator()
        } else if (movimientos.isEmpty()) {
            Text("No hay movimientos para esta categoría en el período seleccionado.", style = MaterialTheme.typography.bodyMedium)
        } else {
            DetalleMovimientosTable(movimientos)
        }
    }
}

@Composable
private fun DetalleMovimientosTable(movimientos: List<com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Fecha", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text("Descripción", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold)
            Text("Monto", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp) // Altura máxima visible, luego scroll
        ) {
            items(movimientos) { mov ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = java.text.SimpleDateFormat("dd/MM/yyyy").format(mov.fecha),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = mov.descripcion,
                        modifier = Modifier.weight(2f)
                    )
                    Text(
                        text = com.aranthalion.controlfinanzas.data.util.FormatUtils.formatMoney(mov.monto),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar"
                )
            }
        }
    }
} 