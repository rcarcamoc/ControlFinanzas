package com.aranthalion.controlfinanzas.presentation.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.presentation.configuracion.ConfiguracionViewModel
import com.aranthalion.controlfinanzas.presentation.configuracion.TemaApp

@Composable
fun TemaOption(
    tema: TemaApp,
    nombre: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (isSelected) 3.dp else 0.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = nombre,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ConfiguracionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun AiConfigCard(
    viewModel: ConfiguracionViewModel
) {
    val enabled by viewModel.aiEnabled.collectAsState()
    val groqKey by viewModel.groqApiKey.collectAsState()
    val geminiKey by viewModel.geminiApiKey.collectAsState()
    val provider by viewModel.aiProvider.collectAsState()

    var localEnabled by remember(enabled) { mutableStateOf(enabled) }
    var localGroqKey by remember(groqKey) { mutableStateOf(groqKey) }
    var localGeminiKey by remember(geminiKey) { mutableStateOf(geminiKey) }
    var localProvider by remember(provider) { mutableStateOf(provider) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Inteligencia Artificial",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Resúmenes inteligentes del mes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = localEnabled,
                    onCheckedChange = {
                        localEnabled = it
                        viewModel.guardarAiConfig(it, localGroqKey, localGeminiKey, localProvider)
                    }
                )
            }

            if (localEnabled) {
                HorizontalDivider()

                Text(
                    text = "Proveedor Preferido",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            localProvider = "groq"
                            viewModel.guardarAiConfig(localEnabled, localGroqKey, localGeminiKey, "groq")
                        }
                    ) {
                        RadioButton(
                            selected = localProvider == "groq",
                            onClick = {
                                localProvider = "groq"
                                viewModel.guardarAiConfig(localEnabled, localGroqKey, localGeminiKey, "groq")
                            }
                        )
                        Text("Groq (Llama 3)", style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            localProvider = "gemini"
                            viewModel.guardarAiConfig(localEnabled, localGroqKey, localGeminiKey, "gemini")
                        }
                    ) {
                        RadioButton(
                            selected = localProvider == "gemini",
                            onClick = {
                                localProvider = "gemini"
                                viewModel.guardarAiConfig(localEnabled, localGroqKey, localGeminiKey, "gemini")
                            }
                        )
                        Text("Gemini", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                OutlinedTextField(
                    value = localGroqKey,
                    onValueChange = {
                        localGroqKey = it
                        viewModel.guardarAiConfig(localEnabled, it, localGeminiKey, localProvider)
                    },
                    label = { Text("Groq API Key") },
                    placeholder = { Text("gsk_...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (localGroqKey.isNotBlank()) {
                            IconButton(onClick = {
                                localGroqKey = ""
                                viewModel.guardarAiConfig(localEnabled, "", localGeminiKey, localProvider)
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    }
                )

                OutlinedTextField(
                    value = localGeminiKey,
                    onValueChange = {
                        localGeminiKey = it
                        viewModel.guardarAiConfig(localEnabled, localGroqKey, it, localProvider)
                    },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (localGeminiKey.isNotBlank()) {
                            IconButton(onClick = {
                                localGeminiKey = ""
                                viewModel.guardarAiConfig(localEnabled, localGroqKey, "", localProvider)
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    }
                )

                Text(
                    text = "Nota: Si ambos proveedores fallan o no tienen API keys, se usarán plantillas de análisis locales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SyncConfigCard(
    viewModel: ConfiguracionViewModel
) {
    val syncEnabled by viewModel.syncEnabled.collectAsState()
    val syncServerUrl by viewModel.syncServerUrl.collectAsState()
    val syncHouseholdId by viewModel.syncHouseholdId.collectAsState()
    val syncEmail by viewModel.syncEmail.collectAsState()
    val syncPassword by viewModel.syncPassword.collectAsState()
    val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()

    var localSyncUrl by remember(syncServerUrl) { mutableStateOf(syncServerUrl) }
    var localHouseholdId by remember(syncHouseholdId) { mutableStateOf(syncHouseholdId) }
    var localEmail by remember(syncEmail) { mutableStateOf(syncEmail) }
    var localPassword by remember(syncPassword) { mutableStateOf(syncPassword) }

    val context = androidx.compose.ui.platform.LocalContext.current

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Sincronización Web",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Conecta con tu portal Zen Finanzas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            if (localEmail.isBlank() || localHouseholdId.isBlank()) {
                Text(
                    text = "Vincular tu dispositivo te permite compartir transacciones, presupuestos y deudas en tiempo real con el portal Zen Finanzas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("http://161.153.219.141/finanzas/dashboard/link-device")
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Vincular Cuenta desde la Web")
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Dispositivo Vinculado",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Usuario: $localEmail",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Hogar ID: ${localHouseholdId.take(12)}...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (localPassword.isBlank()) {
                    Text(
                        text = "Introduce la contraseña de tu cuenta Zen Finanzas para activar la sincronización automática en este dispositivo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )

                    OutlinedTextField(
                        value = localPassword,
                        onValueChange = {
                            localPassword = it
                            viewModel.guardarSyncConfig(true, localSyncUrl, localHouseholdId, localEmail, it)
                        },
                        label = { Text("Contraseña de Usuario") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                } else {
                    Button(
                        onClick = { viewModel.ejecutarSincronizacion() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sincronizando...")
                        } else {
                            Text("Sincronizar Ahora")
                        }
                    }
                }

                if (syncStatus.isNotBlank()) {
                    Text(
                        text = syncStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (syncStatus.startsWith("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (lastSyncTimestamp > 0L) {
                    val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastSyncTimestamp))
                    Text(
                        text = "Última sincronización: $dateStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = { 
                        viewModel.desvincular()
                        localPassword = ""
                        localEmail = ""
                        localHouseholdId = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Desvincular Dispositivo")
                }
            }
        }
    }
}
