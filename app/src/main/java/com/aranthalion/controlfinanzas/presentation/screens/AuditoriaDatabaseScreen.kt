package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.local.entity.ClasificacionAutomaticaEntity
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.style.TextAlign
import com.aranthalion.controlfinanzas.data.local.entity.AuditoriaEntity
import com.aranthalion.controlfinanzas.presentation.screens.AuditoriaDatabaseUiState
import com.aranthalion.controlfinanzas.presentation.screens.AuditoriaDatabaseData
import com.aranthalion.controlfinanzas.presentation.components.ErrorCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditoriaDatabaseScreen(
    viewModel: AuditoriaDatabaseViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var tablaSeleccionada by remember { mutableStateOf("movimientos") }
    var expandedTabla by remember { mutableStateOf(false) }
    
    val tablasDisponibles = mapOf(
        "movimientos" to "Movimientos",
        "presupuestos" to "Presupuestos", 
        "categorias" to "Categorías",
        "clasificacion" to "Clasificación",
        "todos" to "Todas las tablas"
    )
    
    LaunchedEffect(tablaSeleccionada) {
        viewModel.cargarDatosAuditoria(tablaSeleccionada)
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
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            }
            Text(
                text = "Auditoría de Base de Datos",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { /* TODO: Configuración */ }) {
                Icon(Icons.Default.Settings, contentDescription = "Configuración")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Selector de tabla
        ExposedDropdownMenuBox(
            expanded = expandedTabla,
            onExpandedChange = { expandedTabla = it }
        ) {
            OutlinedTextField(
                value = tablasDisponibles[tablaSeleccionada] ?: "Seleccionar tabla",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTabla) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                label = { Text("Tabla a auditar") }
            )
            
            DropdownMenu(
                expanded = expandedTabla,
                onDismissRequest = { expandedTabla = false }
            ) {
                tablasDisponibles.forEach { (key, value) ->
                    DropdownMenuItem(
                        text = { Text(value) },
                        onClick = {
                            tablaSeleccionada = key
                            expandedTabla = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Contenido de auditoría
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (uiState) {
                is AuditoriaDatabaseUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                
                is AuditoriaDatabaseUiState.Success -> {
                    val data = (uiState as AuditoriaDatabaseUiState.Success).data
                    
                    // Resumen general
                    item {
                        ResumenAuditoriaCard(
                            totalRegistros = data.auditoriaReciente.size,
                            tablaSeleccionada = data.tablaSeleccionada
                        )
                    }
                    
                    // Registros de auditoría
                    items(data.auditoriaReciente) { auditoria ->
                        AuditoriaCard(auditoria = auditoria)
                    }
                    
                    // Movimientos recientes (solo si se seleccionó movimientos)
                    if (data.tablaSeleccionada == "movimientos" || data.tablaSeleccionada == "todos") {
                        items(data.movimientosRecientes) { movimiento ->
                            MovimientoAuditoriaCard(movimiento = movimiento)
                        }
                    }
                }
                
                is AuditoriaDatabaseUiState.Error -> {
                    item {
                        ErrorCard(
                            title = "Error al cargar auditoría",
                            message = (uiState as AuditoriaDatabaseUiState.Error).mensaje
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumenAuditoriaCard(totalRegistros: Int, tablaSeleccionada: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Resumen de Auditoría",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AuditoriaStat(
                    label = "Registros",
                    value = "$totalRegistros",
                    icon = Icons.Default.List
                )
                AuditoriaStat(
                    label = "Tabla",
                    value = tablaSeleccionada,
                    icon = Icons.Default.List
                )
            }
            
            // Métodos de actualización más usados
            // TODO: Implementar lógica de métodos de actualización
            Text(
                text = "Métodos de Actualización Recientes:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            // TODO: Mostrar métodos de actualización
        }
    }
}

@Composable
private fun AuditoriaCard(auditoria: AuditoriaEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = auditoria.operacion,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (auditoria.operacion) {
                        "INSERT" -> MaterialTheme.colorScheme.primary
                        "UPDATE" -> MaterialTheme.colorScheme.secondary
                        "DELETE" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                        .format(Date(auditoria.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "Tabla: ${auditoria.tabla}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = auditoria.detalles,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DAO: ${auditoria.daoResponsable}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (auditoria.entidadId != null) {
                    Text(
                        text = "ID: ${auditoria.entidadId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun MovimientoAuditoriaCard(movimiento: MovimientoEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = movimiento.descripcion,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$${String.format("%.2f", movimiento.monto)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (movimiento.tipo == "INGRESO") 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tipo: ${movimiento.tipo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Categoría: ${movimiento.categoriaId ?: "Sin categorizar"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Método: ${movimiento.metodoActualizacion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "DAO: ${movimiento.daoResponsable}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = "Última actualización: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                    .format(Date(movimiento.fechaActualizacion))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AuditoriaStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
} 