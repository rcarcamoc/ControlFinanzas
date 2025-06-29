# Plan de Implementación - Aplicación de la Guía de Diseño FinaVision

## Resumen Ejecutivo
Este plan detalla la implementación de los cambios necesarios para alinear la aplicación Android con el diseño del prototipo web FinaVision, manteniendo la funcionalidad existente y mejorando la experiencia visual.

## 0. **Implementación de @Preview** ⭐ PRIORIDAD ALTA

### 0.1 Configuración de Previews
**Objetivo:** Agregar previews a todos los componentes para desarrollo visual más rápido

**Tareas:**
- [ ] ✅ Agregar @Preview a StatCard (ya implementado)
- [ ] Agregar @Preview a BarChart
- [ ] Agregar @Preview a PieChart
- [ ] Agregar @Preview a componentes de navegación
- [ ] Agregar @Preview a pantallas principales
- [ ] Crear previews para diferentes temas (Naranja, Azul, Verde)
- [ ] Crear previews para diferentes estados (loading, error, success)

**Beneficios:**
- Desarrollo visual más rápido
- Testing de diferentes configuraciones
- Documentación visual de componentes
- Debugging de layout más eficiente

**Archivos a modificar:**
- Todos los archivos de componentes y pantallas

## 1. **Sistema de Colores y Tipografía** ⭐ PRIORIDAD ALTA

### 1.1 Actualizar Paleta de Colores
**Objetivo:** Alinear con la paleta exacta del prototipo manteniendo los 3 temas base (Naranja, Azul, Verde)

**Tareas:**
- [ ] Actualizar `colors.xml` con los valores HSL exactos del prototipo
- [ ] Modificar `Color.kt` para usar la paleta naranja como base
- [ ] Mantener compatibilidad con temas Azul y Verde
- [ ] Implementar colores específicos: `--background`, `--foreground`, `--card`, `--primary`, etc.

**Archivos a modificar:**
- `app/src/main/res/values/colors.xml`
- `app/src/main/java/com/aranthalion/controlfinanzas/ui/theme/Color.kt`
- `app/src/main/java/com/aranthalion/controlfinanzas/ui/theme/Theme.kt`

### 1.2 Sistema de Tipografía
**Objetivo:** Implementar jerarquía de texto consistente con el prototipo

**Tareas:**
- [ ] Actualizar `Type.kt` con tamaños específicos (30sp, 24sp, 14sp, 12sp)
- [ ] Configurar pesos de fuente (400, 500, 700)
- [ ] Implementar PT Sans desde Google Fonts
- [ ] Crear estilos de texto para títulos, descripciones, etc.

**Archivos a modificar:**
- `app/src/main/java/com/aranthalion/controlfinanzas/ui/theme/Type.kt`

## 2. **Componentes Base** ⭐ PRIORIDAD ALTA

### 2.1 Actualizar StatCard
**Objetivo:** Alinear con el diseño del prototipo (4 columnas en desktop)

**Tareas:**
- [ ] Rediseñar layout: título arriba, icono a la derecha, valor grande, descripción abajo
- [ ] Ajustar espaciado y tamaños según especificaciones
- [ ] Implementar colores consistentes con la guía

**Archivos a modificar:**
- `app/src/main/java/com/aranthalion/controlfinanzas/presentation/components/StatCard.kt`

### 2.2 Componentes de Navegación
**Objetivo:** Implementar sidebar de navegación similar al prototipo

**Tareas:**
- [ ] Crear sidebar con navegación lateral
- [ ] Implementar header con menú de usuario
- [ ] Alinear iconos y rutas con el prototipo
- [ ] **NO implementar footer con copyright** (según observación)

**Archivos a modificar:**
- `app/src/main/java/com/aranthalion/controlfinanzas/presentation/navigation/AppNavigation.kt`
- Crear nuevo componente: `AppShell.kt`

### 2.3 Componentes de Formulario
**Objetivo:** Actualizar botones e inputs según especificaciones

**Tareas:**
- [ ] Implementar variantes de botones (Default, Outline, Ghost, Destructive)
- [ ] Actualizar inputs y selects con estilo correcto
- [ ] Crear diálogos modales con diseño especificado

## 3. **Dashboard Principal** ⭐ PRIORIDAD ALTA

### 3.1 Migrar HomeScreen a Dashboard
**Objetivo:** Transformar la pantalla principal en un dashboard completo

**Tareas:**
- [ ] Implementar grid de 4 StatCards
- [ ] Crear grid de 2 columnas para contenido principal
- [ ] Implementar tarjetas:
  - "Tendencia de Gasto Mensual" (con BarChart)
  - "Estado del Presupuesto" (con ProgressBar)
  - "Gasto por Categoría" (con PieChart + drill-down)
  - "Proyecciones y Perspectivas"
- [ ] Agregar interactividad (selección de presupuestos, drill-down)
- [ ] Implementar botón "Ver Resumen General" condicional

**Archivos a modificar:**
- `app/src/main/java/com/aranthalion/controlfinanzas/presentation/screens/HomeScreen.kt`
- `app/src/main/java/com/aranthalion/controlfinanzas/presentation/components/BarChart.kt`
- `app/src/main/java/com/aranthalion/controlfinanzas/presentation/components/PieChart.kt`

## 4. **Pantallas Secundarias** ⭐ PRIORIDAD MEDIA

### 4.1 Rediseñar TransaccionesScreen (Statements)
**Objetivo:** Mejorar diseño manteniendo funcionalidad existente

**Tareas:**
- [ ] **MANTENER toda la funcionalidad actual** (botones, filtros, etc.)
- [ ] Rediseñar visualmente para consistencia con el resto de la app
- [ ] Mejorar layout y espaciado
- [ ] Actualizar colores y tipografía

**Archivos a modificar:**
- `app/src/main/java/com/aranthalion/controlfinanzas/presentation/screens/TransaccionesScreen.kt`

### 4.2 Rediseñar CategoriasScreen y PresupuestosScreen
**Objetivo:** Implementar patrón tabla + diálogo con confirmación de cambios históricos

**Tareas:**
- [ ] Implementar patrón de tabla + diálogo de edición
- [ ] Agregar botones de acción (Add New, Edit, Delete)
- [ ] **NUEVO:** Implementar diálogo de confirmación al editar categoría:
  - Preguntar si aplicar cambios al histórico
  - Opciones: "Solo esta categoría" / "Aplicar a todo el histórico"
- [ ] Implementar formularios de edición mejorados

**Archivos a modificar:**
- `app/src/main/java/com/aranthalion/controlfinanzas/presentation/screens/CategoriasScreen.kt`
- `app/src/main/java/com/aranthalion/controlfinanzas/presentation/screens/PresupuestosScreen.kt`

### 4.3 Rediseñar ClasificacionPendienteScreen (Classify)
**Objetivo:** Mejorar diseño manteniendo funcionalidad

**Tareas:**
- [ ] **MANTENER toda la funcionalidad actual**
- [ ] Rediseñar visualmente para consistencia
- [ ] Mejorar tabla de transacciones sin categorizar
- [ ] Actualizar selects para edición en línea
- [ ] Implementar badges de estado mejorados

**Archivos a modificar:**
- `app/src/main/java/com/aranthalion/controlfinanzas/presentation/screens/ClasificacionPendienteScreen.kt`

## 5. **Gráficos y Visualizaciones** ⭐ PRIORIDAD MEDIA

### 5.1 Mejorar BarChart
**Objetivo:** Implementar estilo exacto del prototipo

**Tareas:**
- [ ] Alinear colores y estilos con la guía
- [ ] Implementar interactividad y tooltips
- [ ] Mejorar responsividad

**Archivos a modificar:**
- `app/src/main/java/com/aranthalion/controlfinanzas/presentation/components/BarChart.kt`

### 5.2 Mejorar PieChart
**Objetivo:** Implementar drill-down y vista de tabla

**Tareas:**
- [ ] Implementar drill-down al hacer click en porciones
- [ ] Agregar vista de tabla cuando se selecciona una categoría
- [ ] Implementar botón "Volver al Gráfico"
- [ ] Mejorar leyenda y tooltips

**Archivos a modificar:**
- `app/src/main/java/com/aranthalion/controlfinanzas/presentation/components/PieChart.kt`

### 5.3 Componentes de Progreso
**Objetivo:** Implementar ProgressBar para presupuestos

**Tareas:**
- [ ] Crear ProgressBar con estilo especificado
- [ ] Implementar indicadores de estado para presupuestos
- [ ] Agregar colores según porcentaje de uso

## 6. **Interacciones y Animaciones** ⭐ PRIORIDAD BAJA

### 6.1 Transiciones y Estados
**Objetivo:** Mejorar experiencia de usuario

**Tareas:**
- [ ] Implementar animaciones de entrada para diálogos
- [ ] Agregar transiciones de hover/pressed
- [ ] Crear animaciones para gráficos
- [ ] Implementar estados interactivos (hover, focus, active)

## 7. **Responsive Design** ⭐ PRIORIDAD BAJA

### 7.1 Adaptación Multi-pantalla
**Objetivo:** Optimizar para diferentes tamaños

**Tareas:**
- [ ] Implementar breakpoints para tablet y desktop
- [ ] Ajustar grids y layouts según tamaño
- [ ] Optimizar para orientación landscape

## Cronograma de Implementación

### Fase 1 (Semana 1): Fundación
- [ ] Sistema de colores y tipografía
- [ ] Componentes base (StatCard, botones, inputs)
- [ ] Layout principal (App Shell, navegación)

### Fase 2 (Semana 2): Dashboard
- [ ] Migrar HomeScreen a Dashboard completo
- [ ] Implementar gráficos mejorados
- [ ] Agregar interactividad

### Fase 3 (Semana 3): Pantallas Secundarias
- [ ] Rediseñar TransaccionesScreen
- [ ] Rediseñar CategoriasScreen y PresupuestosScreen
- [ ] Implementar diálogo de confirmación histórica

### Fase 4 (Semana 4): Pulido
- [ ] Rediseñar ClasificacionPendienteScreen
- [ ] Mejorar gráficos y visualizaciones
- [ ] Testing y refinamiento

### Fase 5 (Opcional): Mejoras
- [ ] Interacciones y animaciones
- [ ] Responsive design
- [ ] Optimización de performance

## Criterios de Éxito

### Visual
- [ ] Colores exactamente alineados con el prototipo
- [ ] Tipografía consistente en toda la app
- [ ] Layout responsive y funcional

### Funcional
- [ ] Todas las funcionalidades existentes preservadas
- [ ] Nueva funcionalidad de confirmación histórica implementada
- [ ] Dashboard interactivo y funcional

### Técnico
- [ ] Código limpio y mantenible
- [ ] Performance optimizada
- [ ] Compatibilidad con temas existentes

## Notas Importantes

1. **Preservar Funcionalidad:** Todas las funcionalidades existentes deben mantenerse intactas
2. **Temas Múltiples:** Mantener compatibilidad con los 3 temas (Naranja, Azul, Verde)
3. **Sin Footer:** No implementar footer con copyright según especificación
4. **Confirmación Histórica:** Nueva funcionalidad para preguntar sobre cambios históricos
5. **Dashboard Primario:** La pantalla principal debe ser un dashboard completo
6. **Rediseño Visual:** Solo mejorar diseño, no cambiar funcionalidad en pantallas existentes
