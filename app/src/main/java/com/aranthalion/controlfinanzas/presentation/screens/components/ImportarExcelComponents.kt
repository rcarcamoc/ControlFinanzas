package com.aranthalion.controlfinanzas.presentation.screens.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.data.util.ParDuplicadoSimilar
import com.aranthalion.controlfinanzas.data.util.ParDuplicadoMovimientos
import com.aranthalion.controlfinanzas.presentation.components.PeriodoSelectorGlobal
import androidx.compose.foundation.BorderStroke

@Composable
fun PanelSelectorArchivo(
    archivoNombre: String,
    onSeleccionarArchivo: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Archivo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            OutlinedButton(
                onClick = onSeleccionarArchivo,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (archivoNombre.isNotEmpty()) archivoNombre else "Seleccionar archivo Excel"
                )
            }
            
            if (archivoNombre.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Archivo seleccionado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelConfiguracionImportacion(
    tipoArchivo: String,
    tipos: List<String>,
    expandedTipo: Boolean,
    onExpandedTipoChange: (Boolean) -> Unit,
    onTipoArchivoChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Configuración",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            ExposedDropdownMenuBox(
                expanded = expandedTipo,
                onExpandedChange = onExpandedTipoChange
            ) {
                OutlinedTextField(
                    value = tipoArchivo,
                    onValueChange = {},
                    label = { Text("Tipo de archivo") },
                    readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTipo) }
                )
                ExposedDropdownMenu(
                    expanded = expandedTipo,
                    onDismissRequest = { onExpandedTipoChange(false) }
                ) {
                    tipos.forEach { tipo ->
                        DropdownMenuItem(
                            text = { Text(tipo) },
                            onClick = {
                                onTipoArchivoChange(tipo)
                                onExpandedTipoChange(false)
                            }
                        )
                    }
                }
            }
            
            PeriodoSelectorGlobal(
                modifier = Modifier.fillMaxWidth(),
                label = "Mes de ciclo de facturación"
            )
        }
    }
}

@Composable
fun PanelVistaPreviaImportacion(
    movimientos: List<MovimientoEntity>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Vista previa",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = "Movimientos a importar: ${movimientos.size}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            movimientos.take(5).forEachIndexed { i, t ->
                val clasificacionInfo = if (t.categoriaId != null) "✓ Clasificado" else "⚠ Pendiente"
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${i+1}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t.descripcion,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                            Text(
                                text = "${t.fecha} • ${FormatUtils.formatMoneyCLP(t.monto)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = clasificacionInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (t.categoriaId != null) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DialogoConfirmacionDuplicados(
    duplicado: ParDuplicadoSimilar,
    indice: Int,
    total: Int,
    onOmitirNueva: () -> Unit,
    onFusionar: () -> Unit,
    onSobrescribir: () -> Unit,
    onConservarAmbas: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "¿Transacción Duplicada?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Detectamos un posible duplicado (${indice + 1} de $total). Similitud: ${(duplicado.similitud * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Transacción Existente (En base de datos)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "EXISTENTE EN APP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = duplicado.existente.descripcion, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${FormatUtils.formatMoneyCLP(duplicado.existente.monto)} • ${duplicado.existente.fecha} • Tarjeta: ${duplicado.existente.tipoTarjeta ?: "No especificada"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Transacción Nueva (A Importar)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "NUEVA A IMPORTAR",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = duplicado.nueva.descripcion, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${FormatUtils.formatMoneyCLP(duplicado.nueva.monto)} • ${duplicado.nueva.fecha} • Tarjeta: ${duplicado.nueva.tipoTarjeta ?: "No especificada"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Seleccione cómo resolver este conflicto:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Opción 1: Fusionar
                    Button(
                        onClick = onFusionar,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Fusionar datos (Recomendado)")
                    }

                    // Opción 2: Omitir Nueva
                    OutlinedButton(
                        onClick = onOmitirNueva,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Omitir importación (Mantener existente)")
                    }

                    // Opción 3: Sobrescribir
                    OutlinedButton(
                        onClick = onSobrescribir,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Sobrescribir (Usar nueva)")
                    }

                    // Opción 4: Conservar Ambas
                    TextButton(
                        onClick = onConservarAmbas,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Son distintas (Conservar ambas)")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar proceso")
            }
        }
    )
}

@Composable
fun ResumenItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

data class ResumenImportacion(
    val totalProcesadas: Int,
    val nuevas: Int,
    val duplicadas: Int,
    val montoTotal: Long,
    val periodo: String,
    val clasificadasAutomaticamente: Int,
    val pendientesClasificacion: Int
)

@Composable
fun DialogoConfirmacionDuplicadosInternos(
    duplicado: ParDuplicadoMovimientos,
    indice: Int,
    total: Int,
    onConservarExistenteOmitirNueva: () -> Unit,
    onFusionar: () -> Unit,
    onSobrescribir: () -> Unit,
    onConservarAmbas: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Registros Duplicados en App",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Detectamos transacciones similares guardadas en la app (${indice + 1} de $total). Similitud: ${(duplicado.similitud * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Transacción Existente 1
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "REGISTRO EXISTENTE 1 (ID: ${duplicado.existente.id})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = duplicado.existente.descripcion, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${FormatUtils.formatMoneyCLP(duplicado.existente.monto)} • ${duplicado.existente.fecha} • Tarjeta: ${duplicado.existente.tipoTarjeta ?: "No especificada"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Transacción Existente 2 (La sospechosa)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "REGISTRO EXISTENTE 2 (ID: ${duplicado.nueva.id})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = duplicado.nueva.descripcion, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "${FormatUtils.formatMoneyCLP(duplicado.nueva.monto)} • ${duplicado.nueva.fecha} • Tarjeta: ${duplicado.nueva.tipoTarjeta ?: "No especificada"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Seleccione cómo resolver esta duplicación:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Opción 1: Fusionar
                    Button(
                        onClick = onFusionar,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Fusionar (Mantener Registro 1 actualizado)")
                    }

                    // Opción 2: Eliminar Registro 2
                    OutlinedButton(
                        onClick = onConservarExistenteOmitirNueva,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Conservar Registro 1 (Eliminar Registro 2)")
                    }

                    // Opción 3: Eliminar Registro 1
                    OutlinedButton(
                        onClick = onSobrescribir,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Conservar Registro 2 (Eliminar Registro 1)")
                    }

                    // Opción 4: Mantener Ambos
                    TextButton(
                        onClick = onConservarAmbas,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ambos son correctos (Mantener ambos)")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar revisión")
            }
        }
    )
}
