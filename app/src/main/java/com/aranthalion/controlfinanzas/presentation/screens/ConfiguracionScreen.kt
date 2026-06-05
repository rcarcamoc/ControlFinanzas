package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.aranthalion.controlfinanzas.presentation.configuracion.ConfiguracionViewModel
import com.aranthalion.controlfinanzas.presentation.configuracion.TemaApp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(
    navController: NavHostController,
    viewModel: ConfiguracionViewModel = hiltViewModel()
) {
    val temaSeleccionado by viewModel.temaSeleccionado.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Configuración",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(WindowInsets.systemBars.asPaddingValues())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Apariencia",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Personaliza el aspecto de la aplicación",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Tema de la aplicación",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TemaOption(
                                tema = TemaApp.NARANJA,
                                nombre = "Naranja",
                                color = Color(0xFFE65100),
                                isSelected = temaSeleccionado == TemaApp.NARANJA,
                                onClick = { viewModel.cambiarTema(TemaApp.NARANJA) }
                            )
                            TemaOption(
                                tema = TemaApp.AZUL,
                                nombre = "Azul",
                                color = Color(0xFF2196F3),
                                isSelected = temaSeleccionado == TemaApp.AZUL,
                                onClick = { viewModel.cambiarTema(TemaApp.AZUL) }
                            )
                            TemaOption(
                                tema = TemaApp.VERDE,
                                nombre = "Verde",
                                color = Color(0xFF4CAF50),
                                isSelected = temaSeleccionado == TemaApp.VERDE,
                                onClick = { viewModel.cambiarTema(TemaApp.VERDE) }
                            )
                        }
                    }
                }
            }

            item {
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

            item {
                val syncEnabled by viewModel.syncEnabled.collectAsState()
                val syncServerUrl by viewModel.syncServerUrl.collectAsState()
                val syncHouseholdId by viewModel.syncHouseholdId.collectAsState()
                val syncEmail by viewModel.syncEmail.collectAsState()
                val syncPassword by viewModel.syncPassword.collectAsState()
                val lastSyncTimestamp by viewModel.lastSyncTimestamp.collectAsState()
                val isSyncing by viewModel.isSyncing.collectAsState()
                val syncStatus by viewModel.syncStatus.collectAsState()

                var localSyncEnabled by remember(syncEnabled) { mutableStateOf(syncEnabled) }
                var localSyncUrl by remember(syncServerUrl) { mutableStateOf(syncServerUrl) }
                var localHouseholdId by remember(syncHouseholdId) { mutableStateOf(syncHouseholdId) }
                var localEmail by remember(syncEmail) { mutableStateOf(syncEmail) }
                var localPassword by remember(syncPassword) { mutableStateOf(syncPassword) }

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
                            Switch(
                                checked = localSyncEnabled,
                                onCheckedChange = {
                                    localSyncEnabled = it
                                    viewModel.guardarSyncConfig(it, localSyncUrl, localHouseholdId, localEmail, localPassword)
                                }
                            )
                        }

                        if (localSyncEnabled) {
                            HorizontalDivider()

                            OutlinedTextField(
                                value = localSyncUrl,
                                onValueChange = {
                                    localSyncUrl = it
                                    viewModel.guardarSyncConfig(localSyncEnabled, it, localHouseholdId, localEmail, localPassword)
                                },
                                label = { Text("URL del Servidor de Sincronización") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = localHouseholdId,
                                onValueChange = {
                                    localHouseholdId = it
                                    viewModel.guardarSyncConfig(localSyncEnabled, localSyncUrl, it, localEmail, localPassword)
                                },
                                label = { Text("Hogar ID (Household CUID)") },
                                placeholder = { Text("c... ") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = localEmail,
                                onValueChange = {
                                    localEmail = it
                                    viewModel.guardarSyncConfig(localSyncEnabled, localSyncUrl, localHouseholdId, it, localPassword)
                                },
                                label = { Text("Email de Usuario Zen Finanzas") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = localPassword,
                                onValueChange = {
                                    localPassword = it
                                    viewModel.guardarSyncConfig(localSyncEnabled, localSyncUrl, localHouseholdId, localEmail, it)
                                },
                                label = { Text("Contraseña de Usuario") },
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Button(
                                onClick = { viewModel.ejecutarSincronizacion() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSyncing && localHouseholdId.isNotBlank() && localSyncUrl.isNotBlank() && localEmail.isNotBlank() && localPassword.isNotBlank()
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
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "General",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Configuraciones generales de la aplicación",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        ConfiguracionItem(
                            icon = Icons.Default.Notifications,
                            title = "Notificaciones",
                            subtitle = "Configurar alertas y recordatorios",
                            onClick = { /* TODO */ }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        ConfiguracionItem(
                            icon = Icons.Default.Lock,
                            title = "Privacidad y Seguridad",
                            subtitle = "Configurar opciones de seguridad",
                            onClick = { /* TODO */ }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        ConfiguracionItem(
                            icon = Icons.Default.Info,
                            title = "Ayuda y Soporte",
                            subtitle = "Obtener ayuda y contactar soporte",
                            onClick = { /* TODO */ }
                        )
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        ConfiguracionItem(
                            icon = Icons.Default.Info,
                            title = "Acerca de",
                            subtitle = "Información de la aplicación",
                            onClick = { /* TODO */ }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ConfiguracionItem(
                            icon = Icons.Default.Email,
                            title = "Consultar Correos",
                            subtitle = "Configurar e importar movimientos de tu correo",
                            onClick = { navController.navigate("email_sync") }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ConfiguracionItem(
                            icon = Icons.Default.List,
                            title = "Auditoría de Base de Datos",
                            subtitle = "Ver logs y estado de la base de datos",
                            onClick = { navController.navigate("auditoria_database") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemaOption(
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
private fun ConfiguracionItem(
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
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
} 