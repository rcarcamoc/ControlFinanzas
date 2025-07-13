package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aranthalion.controlfinanzas.domain.categoria.Categoria
import com.aranthalion.controlfinanzas.domain.clasificacion.ClasificacionAutomatica
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import com.aranthalion.controlfinanzas.presentation.components.AppShell
import com.aranthalion.controlfinanzas.presentation.components.ClasificacionMetricsCard
import com.aranthalion.controlfinanzas.presentation.components.ClasificacionMetrics
import com.aranthalion.controlfinanzas.presentation.components.CategoriaMetric
import com.aranthalion.controlfinanzas.presentation.components.PatronMetric
import java.text.SimpleDateFormat
import java.util.*

data class ClasificacionDebugUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val patrones: List<ClasificacionAutomatica> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val estadisticas: EstadisticasClasificacion = EstadisticasClasificacion(),
    val metrics: ClasificacionMetrics? = null,
    val filtroCategoria: Long? = null,
    val filtroConfianzaMinima: Double = 0.0,
    val ordenarPor: OrdenPatrones = OrdenPatrones.CONFIANZA
)

data class EstadisticasClasificacion(
    val totalPatrones: Int = 0,
    val patronesActivos: Int = 0,
    val promedioConfianza: Double = 0.0,
    val categoriaMasUsada: String = "",
    val patronMasEfectivo: String = "",
    val precisionPromedio: Double = 0.0
)

enum class OrdenPatrones {
    CONFIANZA,
    FRECUENCIA,
    FECHA,
    PATRON
}

@Composable
fun ClasificacionAutomaticaDebugScreen(
    onNavigateBack: () -> Unit,
    viewModel: ClasificacionAutomaticaDebugViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            }
            Text(
                text = "Debug Clasificación Automática",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(48.dp))
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header con estadísticas generales
            item {
                EstadisticasGeneralesCard(uiState.estadisticas)
            }
            
            // Métricas avanzadas
            item {
                uiState.metrics?.let { metrics ->
                    ClasificacionMetricsCard(metrics = metrics)
                }
            }
            
            // Filtros y controles
            item {
                ControlesFiltrosCard(
                    categorias = uiState.categorias,
                    filtroCategoria = uiState.filtroCategoria,
                    filtroConfianzaMinima = uiState.filtroConfianzaMinima,
                    ordenarPor = uiState.ordenarPor,
                    onFiltroCategoriaChange = viewModel::setFiltroCategoria,
                    onFiltroConfianzaChange = viewModel::setFiltroConfianzaMinima,
                    onOrdenarPorChange = viewModel::setOrdenarPor
                )
            }
            
            // Lista de patrones
            items(uiState.patrones) { patron ->
                PatronClasificacionCard(
                    patron = patron,
                    categoria = uiState.categorias.find { it.id == patron.categoriaId },
                    onEditarPatron = viewModel::editarPatron,
                    onEliminarPatron = viewModel::eliminarPatron
                )
            }
            
            // Botón para recargar datos
            item {
                Button(
                    onClick = viewModel::recargarDatos,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Recargar",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recargar Datos")
                }
            }
        }
    }
}

@Composable
private fun EstadisticasGeneralesCard(estadisticas: EstadisticasClasificacion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Estadísticas del Sistema",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EstadisticaItem(
                    icon = Icons.Default.List,
                    label = "Total Patrones",
                    value = estadisticas.totalPatrones.toString(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                EstadisticaItem(
                    icon = Icons.Default.CheckCircle,
                    label = "Activos",
                    value = estadisticas.patronesActivos.toString(),
                    color = Color.Green
                )
                
                EstadisticaItem(
                    icon = Icons.Default.Info,
                    label = "Confianza Prom.",
                    value = "${(estadisticas.promedioConfianza * 100).toInt()}%",
                    color = Color.Blue
                )
            }
            
            Divider()
            
            // Información adicional
            Text(
                text = "Categoría más usada: ${estadisticas.categoriaMasUsada}",
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = "Patrón más efectivo: ${estadisticas.patronMasEfectivo}",
                style = MaterialTheme.typography.bodySmall
            )
            
            Text(
                text = "Precisión promedio: ${(estadisticas.precisionPromedio * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ControlesFiltrosCard(
    categorias: List<Categoria>,
    filtroCategoria: Long?,
    filtroConfianzaMinima: Double,
    ordenarPor: OrdenPatrones,
    onFiltroCategoriaChange: (Long?) -> Unit,
    onFiltroConfianzaChange: (Double) -> Unit,
    onOrdenarPorChange: (OrdenPatrones) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Filtros y Ordenamiento",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            // Filtro por categoría
            Text(
                text = "Filtrar por categoría:",
                style = MaterialTheme.typography.bodySmall
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filtroCategoria == null,
                    onClick = { onFiltroCategoriaChange(null) },
                    label = { Text("Todas") }
                )
                
                // Mostrar solo las primeras 3 categorías para evitar overflow
                categorias.take(3).forEach { categoria ->
                    FilterChip(
                        selected = filtroCategoria == categoria.id,
                        onClick = { onFiltroCategoriaChange(categoria.id) },
                        label = { Text(categoria.nombre) }
                    )
                }
            }
            
            // Filtro por confianza mínima
            Text(
                text = "Confianza mínima: ${(filtroConfianzaMinima * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall
            )
            
            Slider(
                value = filtroConfianzaMinima.toFloat(),
                onValueChange = { onFiltroConfianzaChange(it.toDouble()) },
                valueRange = 0f..1f,
                steps = 19
            )
            
            // Ordenamiento
            Text(
                text = "Ordenar por:",
                style = MaterialTheme.typography.bodySmall
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = ordenarPor == OrdenPatrones.CONFIANZA,
                    onClick = { onOrdenarPorChange(OrdenPatrones.CONFIANZA) },
                    label = { Text("Confianza") }
                )
                
                FilterChip(
                    selected = ordenarPor == OrdenPatrones.FRECUENCIA,
                    onClick = { onOrdenarPorChange(OrdenPatrones.FRECUENCIA) },
                    label = { Text("Frecuencia") }
                )
                
                FilterChip(
                    selected = ordenarPor == OrdenPatrones.FECHA,
                    onClick = { onOrdenarPorChange(OrdenPatrones.FECHA) },
                    label = { Text("Fecha") }
                )
            }
        }
    }
}

@Composable
private fun PatronClasificacionCard(
    patron: ClasificacionAutomatica,
    categoria: Categoria?,
    onEditarPatron: (ClasificacionAutomatica) -> Unit,
    onEliminarPatron: (ClasificacionAutomatica) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header con patrón y acciones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = patron.patron,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Row {
                    IconButton(
                        onClick = { onEditarPatron(patron) }
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { onEliminarPatron(patron) }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            modifier = Modifier.size(18.dp),
                            tint = Color.Red
                        )
                    }
                }
            }
            
            // Información del patrón
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Categoría: ${categoria?.nombre ?: "Desconocida"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "Confianza: ${(patron.nivelConfianza * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            patron.nivelConfianza >= 0.8 -> Color.Green
                            patron.nivelConfianza >= 0.6 -> Color.Yellow
                            else -> Color.Red
                        }
                    )
                    
                    Text(
                        text = "Frecuencia: ${patron.frecuencia}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Última actualización:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Text(
                        text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            .format(Date(patron.ultimaActualizacion)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Barra de confianza visual
            LinearProgressIndicator(
                progress = patron.nivelConfianza.toFloat(),
                modifier = Modifier.fillMaxWidth(),
                color = when {
                    patron.nivelConfianza >= 0.8 -> Color.Green
                    patron.nivelConfianza >= 0.6 -> Color.Yellow
                    else -> Color.Red
                }
            )
        }
    }
}

@Composable
private fun EstadisticaItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
} 