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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aranthalion.controlfinanzas.data.remote.email.EmailConfig
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailSyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: EmailSyncViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val emailConfig by viewModel.emailConfig.collectAsState()
    val testingConnection by viewModel.testingConnection.collectAsState()
    val connectionResult by viewModel.connectionResult.collectAsState()

    var host by remember(emailConfig.host) { mutableStateOf(emailConfig.host) }
    var port by remember(emailConfig.port) { mutableStateOf(emailConfig.port.toString()) }
    var username by remember(emailConfig.username) { mutableStateOf(emailConfig.username) }
    var password by remember(emailConfig.password) { mutableStateOf(emailConfig.password) }
    var useSSL by remember(emailConfig.useSSL) { mutableStateOf(emailConfig.useSSL) }

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Configuración de Servidor de Correo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = { Text("Servidor IMAP") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = port,
                                onValueChange = { port = it },
                                label = { Text("Puerto") },
                                modifier = Modifier.weight(1f)
                            )
                            
                            Row(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .align(Alignment.CenterVertically),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = useSSL, onCheckedChange = { useSSL = it })
                                Text("Usar SSL/TLS")
                            }
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Nombre de Usuario / Correo") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.updateConfig(
                                        EmailConfig(
                                            host = host,
                                            port = port.toIntOrNull() ?: 993,
                                            username = username,
                                            password = password,
                                            protocol = if (useSSL) "imaps" else "imap",
                                            useSSL = useSSL
                                        )
                                    )
                                    viewModel.testConnection()
                                },
                                modifier = Modifier.weight(1f),
                                enabled = !testingConnection
                            ) {
                                if (testingConnection) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Probar Conexión")
                                }
                            }
                        }

                        connectionResult?.let { success ->
                            Text(
                                text = if (success) "Conexión exitosa" else "Error de conexión",
                                color = if (success) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            viewModel.updateConfig(
                                EmailConfig(
                                    host = host,
                                    port = port.toIntOrNull() ?: 993,
                                    username = username,
                                    password = password,
                                    protocol = if (useSSL) "imaps" else "imap",
                                    useSSL = useSSL
                                )
                            )
                            viewModel.fetchEmails()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
                        item {
                            Button(
                                onClick = { viewModel.importarMovimientos(state.movimientos) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Importar todos los nuevos (${state.movimientos.size})")
                            }
                        }

                        items(state.movimientos) { movimiento ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = movimiento.descripcion,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(movimiento.fecha),
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
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
