package com.aranthalion.controlfinanzas.presentation.screens

import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.TransaccionesImportExportService
import kotlinx.coroutines.launch
import java.io.File
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import com.aranthalion.controlfinanzas.presentation.components.PeriodoSelectorGlobal
import com.aranthalion.controlfinanzas.presentation.global.PeriodoGlobalViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransaccionesImportExportScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var periodo by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val viewModel: TransaccionesImportExportViewModel = hiltViewModel()
    val periodoGlobalViewModel: PeriodoGlobalViewModel = hiltViewModel()
    val periodoSeleccionado by periodoGlobalViewModel.periodoSeleccionado.collectAsState()

    // Exportar: seleccionar carpeta destino
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null && periodoSeleccionado.isNotBlank()) {
            scope.launch {
                isLoading = true
                try {
                    val file = context.contentResolver.openFileDescriptor(uri, "w")?.fileDescriptor?.let { java.io.File("/proc/self/fd/${it}") }
                    if (file != null) {
                        viewModel.exportarPorPeriodo(context, periodoSeleccionado, file)
                        mensaje = "Exportación exitosa a ${uri.path}"
                    } else {
                        mensaje = "No se pudo acceder al archivo de destino."
                    }
                } catch (e: Exception) {
                    mensaje = "Error al exportar: ${e.message}"
                }
                isLoading = false
            }
        }
    }

    // Importar: seleccionar archivo fuente
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null && periodoSeleccionado.isNotBlank()) {
            scope.launch {
                isLoading = true
                try {
                    val file = context.contentResolver.openFileDescriptor(uri, "r")?.fileDescriptor?.let { java.io.File("/proc/self/fd/${it}") }
                    if (file != null) {
                        viewModel.importarPorPeriodo(context, periodoSeleccionado, file)
                        mensaje = "Importación exitosa desde ${uri.path}"
                    } else {
                        mensaje = "No se pudo acceder al archivo fuente."
                    }
                } catch (e: Exception) {
                    mensaje = "Error al importar: ${e.message}"
                }
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importar/Exportar Transacciones") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PeriodoSelectorGlobal(modifier = Modifier.fillMaxWidth())
            Button(
                onClick = { exportLauncher.launch("transacciones-$periodoSeleccionado.json") },
                enabled = periodoSeleccionado.isNotBlank() && !isLoading
            ) {
                Text("Exportar transacciones del período")
            }
            Button(
                onClick = { importLauncher.launch("application/json") },
                enabled = periodoSeleccionado.isNotBlank() && !isLoading
            ) {
                Text("Importar transacciones al período")
            }
            if (isLoading) {
                CircularProgressIndicator()
            }
            if (mensaje.isNotBlank()) {
                Text(mensaje, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
} 