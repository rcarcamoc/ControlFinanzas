package com.aranthalion.controlfinanzas.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.aranthalion.controlfinanzas.data.util.FormatUtils
import com.aranthalion.controlfinanzas.presentation.components.BarChart
import com.aranthalion.controlfinanzas.presentation.components.BarChartData
import com.aranthalion.controlfinanzas.presentation.components.PeriodoSelectorGlobal
import com.aranthalion.controlfinanzas.presentation.global.PeriodoGlobalViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardAnalisisScreen(
    navController: NavController,
    viewModel: DashboardAnalisisViewModel = hiltViewModel(),
    periodoGlobalViewModel: PeriodoGlobalViewModel = hiltViewModel()
) {
    val periodoSeleccionado by periodoGlobalViewModel.periodoSeleccionado.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(periodoSeleccionado) {
        viewModel.cargarAnalisis(periodoSeleccionado)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel de Análisis", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.cargarAnalisis(periodoSeleccionado, force = true) }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                is DashboardAnalisisUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Calculando análisis financiero...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is DashboardAnalisisUiState.Success -> {
                    val data = uiState as DashboardAnalisisUiState.Success
                    DashboardContent(
                        data = data,
                        viewModel = viewModel,
                        navController = navController
                    )
                }

                is DashboardAnalisisUiState.Error -> {
                    val errorState = uiState as DashboardAnalisisUiState.Error
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = errorState.mensaje,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { errorState.onRetry() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Reintentar", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardContent(
    data: DashboardAnalisisUiState.Success,
    viewModel: DashboardAnalisisViewModel,
    navController: NavController
) {
    var expandedCategoryId by remember { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Selector de período global
        item {
            Spacer(modifier = Modifier.height(8.dp))
            PeriodoSelectorGlobal()
        }

        // 1. Alertas (si existen)
        if (data.alertas.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.alertas.forEach { alerta ->
                        AlertaBanner(alerta = alerta)
                    }
                }
            }
        }

        // 2. Resumen IA del mes
        item {
            CardResumenIa(data = data)
        }

        // 3. Dónde Gasto (Distribución de categorías con drill-down)
        item {
            CardDistribucionGasto(
                data = data,
                expandedCategoryId = expandedCategoryId,
                onCategoryClick = { catId ->
                    expandedCategoryId = if (expandedCategoryId == catId) null else catId
                },
                navController = navController
            )
        }

        // 4. Estado del Presupuesto (Comparación con límites)
        item {
            CardEstadoPresupuesto(data = data, navController = navController)
        }

        // 5. Ritmo de Gasto (Pace indicator + proyección)
        item {
            CardRitmoGasto(data = data)
        }

        // 6. Gastos Hormiga
        if (data.gastosHormiga.isNotEmpty()) {
            item {
                CardGastosHormiga(data = data)
            }
        }

        // 7. Tendencia Mensual vs Semanal (con selector de granularidad)
        item {
            CardTendencias(
                data = data,
                onGranularidadChange = { viewModel.cambiarGranularidadTendencia(it) }
            )
        }
    }
}

@Composable
private fun AlertaBanner(alerta: AlertaAnalisis) {
    val containerColor = when (alerta.tipo) {
        TipoAlerta.DANGER -> MaterialTheme.colorScheme.errorContainer
        TipoAlerta.WARNING -> Color(0xFFFFF9C4) // Amarillo suave
        TipoAlerta.INFO -> MaterialTheme.colorScheme.secondaryContainer
        TipoAlerta.SUCCESS -> Color(0xFFC8E6C9) // Verde suave
    }
    val contentColor = when (alerta.tipo) {
        TipoAlerta.DANGER -> MaterialTheme.colorScheme.onErrorContainer
        TipoAlerta.WARNING -> Color(0xFF5D4037)
        TipoAlerta.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
        TipoAlerta.SUCCESS -> Color(0xFF1B5E20)
    }
    val icon = when (alerta.tipo) {
        TipoAlerta.DANGER -> Icons.Default.Error
        TipoAlerta.WARNING -> Icons.Default.Warning
        TipoAlerta.INFO -> Icons.Default.Info
        TipoAlerta.SUCCESS -> Icons.Default.CheckCircle
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = alerta.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = alerta.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun CardResumenIa(data: DashboardAnalisisUiState.Success) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        // Borde superior degradado para resaltar el look premium
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Brush.horizontalGradient(listOf(primaryColor, secondaryColor)))
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Resumen del Mes (IA)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!data.cargandoResumenIa && data.resumenIa != null) {
                    val badgeColor = when (data.resumenIa.proveedor) {
                        "groq" -> Color(0xFFE0F7FA)
                        "gemini" -> Color(0xFFE8F5E9)
                        else -> Color(0xFFF5F5F5)
                    }
                    val badgeText = when (data.resumenIa.proveedor) {
                        "groq" -> "Groq (Llama 3)"
                        "gemini" -> "Gemini"
                        else -> "Local"
                    }
                    val badgeContentColor = when (data.resumenIa.proveedor) {
                        "groq" -> Color(0xFF006064)
                        "gemini" -> Color(0xFF1B5E20)
                        else -> Color(0xFF616161)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeContentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (data.cargandoResumenIa) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Generando resumen financiero...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (data.errorResumenIa != null) {
                Text(
                    text = "No se pudo generar el resumen inteligente. Se usarán los datos locales.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (data.resumenIa != null) {
                Text(
                    text = data.resumenIa.texto,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CardDistribucionGasto(
    data: DashboardAnalisisUiState.Success,
    expandedCategoryId: Long?,
    onCategoryClick: (Long) -> Unit,
    navController: NavController
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Dónde Gasto (Top 5)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (data.distribucionCategorias.isEmpty()) {
                Text(
                    text = "No hay gastos registrados en este período.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    data.distribucionCategorias.forEach { categoria ->
                        val isExpanded = expandedCategoryId == categoria.categoriaId

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCategoryClick(categoria.categoriaId) }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = categoria.nombreCategoria,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${categoria.porcentajeDelTotal.toInt()}% del total",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = FormatUtils.formatMoneyCLP(categoria.totalGastado),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { (categoria.porcentajeDelTotal / 100.0).toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            // Progressive Disclosure: movimientos expansibles inline
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp, bottom = 4.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val catMovements = data.movimientos.filter { it.categoriaId == categoria.categoriaId }
                                    
                                    if (catMovements.isEmpty()) {
                                        Text(
                                            text = "No se encontraron movimientos registrados en esta categoría.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Transacciones del Período:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    navController.navigate("transacciones?categoriaId=${categoria.categoriaId}")
                                                }
                                            ) {
                                                Text(
                                                    text = "Ver todas",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        
                                        catMovements.take(5).forEach { mov ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = mov.descripcion,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(mov.fecha),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = FormatUtils.formatMoneyCLP(abs(mov.monto)),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }

                                        if (catMovements.size > 5) {
                                            Text(
                                                text = "Y ${catMovements.size - 5} transacciones más...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(top = 4.dp)
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
    }
}

@Composable
private fun CardEstadoPresupuesto(
    data: DashboardAnalisisUiState.Success,
    navController: NavController
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Estado del Presupuesto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (data.presupuestosConBrecha.isEmpty()) {
                Text(
                    text = "No tienes presupuestos configurados para este período.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    data.presupuestosConBrecha.forEach { presupuesto ->
                        val pct = presupuesto.porcentajeGastado
                        
                        // Determinación del color del semáforo (barra vs texto legible)
                        val barColor = when {
                            pct <= 80.0 -> Color(0xFF4CAF50) // Verde
                            pct <= 100.0 -> Color(0xFFFFC107) // Amber/Yellow
                            else -> Color(0xFFF44336) // Rojo
                        }
                        val textColor = when {
                            pct <= 80.0 -> Color(0xFF2E7D32) // Verde oscuro
                            pct <= 100.0 -> Color(0xFFB78103) // Amber oscuro (legible)
                            else -> Color(0xFFC62828) // Rojo oscuro
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    navController.navigate("transacciones?categoriaId=${presupuesto.categoriaId}")
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = presupuesto.nombreCategoria,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${FormatUtils.formatMoneyCLP(presupuesto.gastoActual)} / ${FormatUtils.formatMoneyCLP(presupuesto.presupuesto)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))

                            Box(modifier = Modifier.fillMaxWidth()) {
                                LinearProgressIndicator(
                                    progress = { (pct / 100.0).coerceAtMost(1.0).toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = barColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${pct.toInt()}% gastado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = textColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val brecha = presupuesto.brechaPresupuesto
                                Text(
                                    text = if (brecha >= 0) "Disponible: ${FormatUtils.formatMoneyCLP(brecha)}" 
                                           else "Exceso: ${FormatUtils.formatMoneyCLP(abs(brecha))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (brecha >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardRitmoGasto(data: DashboardAnalisisUiState.Success) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Ritmo y Proyección de Gasto",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (data.presupuestoTotal == 0.0) {
                Text(
                    text = "Se requiere configurar presupuestos para estimar el ritmo de gasto.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Presupuesto Mensual:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FormatUtils.formatMoneyCLP(data.presupuestoTotal),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Proyección Fin de Mes:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val proyeccionColor = if (data.proyeccionFinMes > data.presupuestoTotal) MaterialTheme.colorScheme.error 
                                              else Color(0xFF4CAF50)
                        Text(
                            text = FormatUtils.formatMoneyCLP(data.proyeccionFinMes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = proyeccionColor
                        )
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Barra 1: Progreso del tiempo (días del mes)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Días Transcurridos:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Día ${data.diaActual} de ${data.diasTotales} (${data.porcentajePeriodoTranscurrido.toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (data.porcentajePeriodoTranscurrido / 100.0).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    // Barra 2: Progreso del presupuesto consumido
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Presupuesto Gastado:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${FormatUtils.formatMoneyCLP(data.gastoActual)} (${data.porcentajePresupuestoGastado.toInt()}%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val pctColor = if (data.diferenciaRitmo > 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        LinearProgressIndicator(
                            progress = { (data.porcentajePresupuestoGastado / 100.0).coerceAtMost(1.0).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = pctColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }

                // Mensaje contextual del Ritmo
                val mensajeRitmo = when {
                    data.diferenciaRitmo > 15 -> "⚠️ Vas un ${data.diferenciaRitmo.toInt()}% más rápido de lo presupuestado. A este ritmo, agotarás tus fondos antes de fin de mes."
                    data.diferenciaRitmo > 5 -> "Vas un poco rápido en tus consumos. Se recomienda moderar tus compras no esenciales."
                    data.diferenciaRitmo < -10 -> "✅ ¡Excelente ritmo! Vas gastando un ${abs(data.diferenciaRitmo).toInt()}% más lento que el avance del mes."
                    else -> "El avance de tus gastos va perfectamente en línea con el período del mes."
                }
                val mensajeColor = if (data.diferenciaRitmo > 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

                Text(
                    text = mensajeRitmo,
                    style = MaterialTheme.typography.bodySmall,
                    color = mensajeColor,
                    fontWeight = if (data.diferenciaRitmo > 10) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun CardGastosHormiga(data: DashboardAnalisisUiState.Success) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gastos Hormiga 🐜",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total: ${FormatUtils.formatMoneyCLP(data.gastosHormigaTotal)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Compras pequeñas recurrentes que terminan impactando significativamente tu presupuesto mensual.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                data.gastosHormiga.take(4).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = item.descripcion,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${item.cantidadCompras} compras • Promedio: ${FormatUtils.formatMoneyCLP(item.promedioCompra)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = FormatUtils.formatMoneyCLP(item.totalAcumulado),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CardTendencias(
    data: DashboardAnalisisUiState.Success,
    onGranularidadChange: (GranularidadTendencia) -> Unit
) {
    val items = if (data.granularidadTendencia == GranularidadTendencia.MENSUAL) data.tendenciaMensual else data.tendenciaSemanal

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tendencia de Gastos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Selector de Granularidad
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (data.granularidadTendencia == GranularidadTendencia.MENSUAL) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onGranularidadChange(GranularidadTendencia.MENSUAL) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Mes",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (data.granularidadTendencia == GranularidadTendencia.MENSUAL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (data.granularidadTendencia == GranularidadTendencia.SEMANAL) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { onGranularidadChange(GranularidadTendencia.SEMANAL) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Semana",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (data.granularidadTendencia == GranularidadTendencia.SEMANAL) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (items.isEmpty()) {
                Text(
                    text = "No hay datos de tendencias suficientes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            } else {
                // Escalar valores a miles de CLP para prevenir overlapping en los labels del chart
                val scaledData = items.map { item ->
                    BarChartData(
                        label = item.etiqueta,
                        value = (item.gastos / 1000f).toFloat(),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text = "(Valores expresados en miles de CLP)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                BarChart(
                    data = scaledData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }
    }
}