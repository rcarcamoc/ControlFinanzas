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
            .padding(WindowInsets.systemBars.asPaddingValues())
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
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text = "Gasto Actual",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "% Gasto",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Proyección",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Desviación",
                    style = MaterialTheme.typography.bodyMedium,
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
                    AnalisisGastoRow(item = item)
                }
            }
        }
    }
}

@Composable
private fun AnalisisGastoRow(item: AnalisisGastoCategoria) {
    val backgroundColor = when (item.estado) {
        EstadoAnalisis.EXCELENTE -> Color(0xFFE8F5E8) // Verde claro
        EstadoAnalisis.NORMAL -> Color.Transparent
        EstadoAnalisis.ADVERTENCIA -> Color(0xFFFFF3E0) // Naranja claro
        EstadoAnalisis.CRITICO -> Color(0xFFFFEBEE) // Rojo claro
    }
    
    val textColor = when (item.estado) {
        EstadoAnalisis.EXCELENTE -> Color(0xFF2E7D32) // Verde oscuro
        EstadoAnalisis.NORMAL -> MaterialTheme.colorScheme.onSurface
        EstadoAnalisis.ADVERTENCIA -> Color(0xFFE65100) // Naranja oscuro
        EstadoAnalisis.CRITICO -> Color(0xFFC62828) // Rojo oscuro
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
                style = MaterialTheme.typography.bodyMedium,
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
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = textColor
        )
        
        // Porcentaje de Gasto
        Text(
            text = "${String.format("%.1f", item.porcentajeGastado)}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = textColor
        )
        
        // Proyección
        Text(
            text = "${String.format("%.1f", item.porcentajeProyeccion)}%",
            style = MaterialTheme.typography.bodyMedium,
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
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = desviacionColor
            )
        }
    }
    
    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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