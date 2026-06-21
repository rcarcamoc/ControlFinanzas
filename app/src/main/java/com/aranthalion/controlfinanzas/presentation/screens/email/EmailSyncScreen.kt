package com.aranthalion.controlfinanzas.presentation.screens.email

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aranthalion.controlfinanzas.data.remote.email.EmailConfig
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailSyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: EmailSyncViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val emailConfigs by viewModel.emailConfigs.collectAsState()
    val testingConnection by viewModel.testingConnection.collectAsState()

    var showConfigDialog by remember { mutableStateOf(false) }
    var selectedConfig by remember { mutableStateOf<EmailConfig?>(null) }

    if (showConfigDialog) {
        EmailConfigDialog(
            config = selectedConfig,
            onDismiss = {
                showConfigDialog = false
                selectedConfig = null
            },
            onConfirm = { config ->
                if (selectedConfig == null) {
                    viewModel.addEmailConfig(config)
                } else {
                    viewModel.updateEmailConfig(config)
                }
                showConfigDialog = false
                selectedConfig = null
            },
            onTestConnection = { config, onResult ->
                viewModel.testConnectionForConfig(config, onResult)
            },
            testingConnection = testingConnection
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consultar Correos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cuentas Configuradas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            selectedConfig = null
                            showConfigDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Añadir Cuenta")
                    }
                }
            }

            if (emailConfigs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay cuentas de correo configuradas.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(emailConfigs, key = { it.id }) { config ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (config.enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = config.username,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${if (config.protocol.startsWith("pop")) "POP3" else "IMAP"} • ${config.host}:${config.port}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Switch(
                                checked = config.enabled,
                                onCheckedChange = { enabled ->
                                    viewModel.toggleEmailConfig(config.id, enabled)
                                }
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            IconButton(onClick = {
                                selectedConfig = config
                                showConfigDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                            }
                            
                            IconButton(onClick = {
                                viewModel.deleteEmailConfig(config.id)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transacciones Detectadas",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Button(
                        onClick = {
                            viewModel.fetchEmails()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = emailConfigs.any { it.enabled }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Buscar Correos")
                    }
                }
            }

            when (val state = uiState) {
                is EmailSyncUiState.Idle -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Toca 'Buscar Correos' para leer notificaciones", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                is EmailSyncUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is EmailSyncUiState.Error -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                is EmailSyncUiState.Success -> {
                    if (state.movimientos.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No se encontraron correos con transacciones bancarias recientes.")
                            }
                        }
                    } else {
                        val importableCount = state.movimientos.count { it.estado != EstadoImportacion.DUPLICADO_EXACTO }
                        
                        item {
                            Button(
                                onClick = { viewModel.importarMovimientos(state.movimientos) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = importableCount > 0
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Importar/Actualizar nuevos y sugeridos ($importableCount)")
                            }
                        }

                        items(state.movimientos) { item ->
                            val movimiento = item.movimiento
                            val cardBgColor = when (item.estado) {
                                EstadoImportacion.DUPLICADO_EXACTO -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                EstadoImportacion.SUGERENCIA_FUSION -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                EstadoImportacion.NUEVO -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            }
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = cardBgColor)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = movimiento.descripcion,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (movimiento.tipoTarjeta != null) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    SuggestionChip(
                                                        onClick = {},
                                                        label = { Text(movimiento.tipoTarjeta, style = MaterialTheme.typography.labelSmall) }
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(movimiento.fecha),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = if (movimiento.tipo == "INGRESO") {
                                                "+${FormatUtils.formatMoneyCLP(movimiento.monto)}"
                                            } else {
                                                "-${FormatUtils.formatMoneyCLP(movimiento.monto)}"
                                            },
                                            color = if (movimiento.tipo == "INGRESO") Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }

                                    when (item.estado) {
                                        EstadoImportacion.DUPLICADO_EXACTO -> {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "✓ Ya registrado en el sistema",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        EstadoImportacion.SUGERENCIA_FUSION -> {
                                            val match = item.matchExistente
                                            if (match != null) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "💡 Sugerencia: Fusión con movimiento existente",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Text(
                                                    text = "Existe un movimiento por ${FormatUtils.formatMoneyCLP(match.monto)} registrado como \"${match.descripcion}\" el ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(match.fecha)}.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                
                                                val extCal = java.util.Calendar.getInstance().apply { time = match.fecha }
                                                val extHoraCero = extCal.get(java.util.Calendar.HOUR_OF_DAY) == 0 && extCal.get(java.util.Calendar.MINUTE) == 0
                                                val nuevoCal = java.util.Calendar.getInstance().apply { time = movimiento.fecha }
                                                val nuevoHoraNoCero = nuevoCal.get(java.util.Calendar.HOUR_OF_DAY) != 0 || nuevoCal.get(java.util.Calendar.MINUTE) != 0
                                                
                                                val actualizaraTarjeta = match.tipoTarjeta.isNullOrBlank() && movimiento.tipoTarjeta != null
                                                val actualizaraHora = extHoraCero && nuevoHoraNoCero
                                                
                                                if (actualizaraTarjeta || actualizaraHora) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Se completarán los siguientes datos faltantes:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (actualizaraTarjeta) {
                                                        Text(
                                                            text = " • Tipo de Tarjeta: ${movimiento.tipoTarjeta}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF2E7D32)
                                                        )
                                                    }
                                                    if (actualizaraHora) {
                                                        Text(
                                                            text = " • Hora: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(movimiento.fecha)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF2E7D32)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        EstadoImportacion.NUEVO -> {
                                            // No mostrar mensaje extra para nuevos
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailConfigDialog(
    config: EmailConfig?,
    onDismiss: () -> Unit,
    onConfirm: (EmailConfig) -> Unit,
    onTestConnection: (EmailConfig, (Boolean) -> Unit) -> Unit,
    testingConnection: Boolean
) {
    var host by remember { mutableStateOf(config?.host ?: "") }
    var port by remember { mutableStateOf(config?.port?.toString() ?: "993") }
    var username by remember { mutableStateOf(config?.username ?: "") }
    var password by remember { mutableStateOf(config?.password ?: "") }
    var useSSL by remember { mutableStateOf(config?.useSSL ?: true) }
    var isPop3 by remember { mutableStateOf(config?.protocol?.startsWith("pop") ?: false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var connectionTestResult by remember { mutableStateOf<Boolean?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (config == null) "Añadir Cuenta de Correo" else "Editar Cuenta de Correo") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Protocolo", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !isPop3, onClick = { 
                            isPop3 = false 
                            port = "993"
                        })
                        Text("IMAP")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = isPop3, onClick = { 
                            isPop3 = true 
                            port = "995"
                        })
                        Text("POP3")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            isPop3 = false
                            host = "imap.gmail.com"
                            port = "993"
                            useSSL = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Gmail IMAP", style = MaterialTheme.typography.labelSmall)
                    }
                    TextButton(
                        onClick = {
                            isPop3 = true
                            host = "pop.gmail.com"
                            port = "995"
                            useSSL = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Gmail POP", style = MaterialTheme.typography.labelSmall)
                    }
                }

                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(if (isPop3) "Servidor POP" else "Servidor IMAP") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Puerto") },
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        modifier = Modifier.weight(1.2f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = useSSL, onCheckedChange = { useSSL = it })
                        Text("SSL/TLS")
                    }
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario / Correo") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val tempConfig = EmailConfig(
                                host = host,
                                port = port.toIntOrNull() ?: (if (isPop3) 995 else 993),
                                username = username,
                                password = password,
                                protocol = if (isPop3) (if (useSSL) "pop3s" else "pop3") else (if (useSSL) "imaps" else "imap"),
                                useSSL = useSSL
                            )
                            onTestConnection(tempConfig) { success ->
                                connectionTestResult = success
                            }
                        },
                        enabled = !testingConnection && host.isNotBlank() && username.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (testingConnection) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Probar Conexión")
                        }
                    }
                }

                connectionTestResult?.let { success ->
                    Text(
                        text = if (success) "Conexión exitosa" else "Error de conexión",
                        color = if (success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        EmailConfig(
                            id = config?.id ?: UUID.randomUUID().toString(),
                            host = host,
                            port = port.toIntOrNull() ?: (if (isPop3) 995 else 993),
                            username = username,
                            password = password,
                            protocol = if (isPop3) (if (useSSL) "pop3s" else "pop3") else (if (useSSL) "imaps" else "imap"),
                            useSSL = useSSL,
                            enabled = config?.enabled ?: true
                        )
                    )
                },
                enabled = host.isNotBlank() && username.isNotBlank() && password.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
