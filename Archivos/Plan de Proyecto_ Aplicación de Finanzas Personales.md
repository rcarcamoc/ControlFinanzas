# Plan de Proyecto: Aplicación de Finanzas Personales

## Resumen Ejecutivo

Este documento detalla el plan de desarrollo para la aplicación de Finanzas Personales, desglosando el proyecto en hitos funcionales. Cada hito representa una fase de desarrollo con entregables claros y criterios de aceptación específicos, lo que permitirá un enfoque iterativo y la validación continua del progreso. El objetivo es construir una aplicación robusta, intuitiva y privada para la gestión financiera personal, operando completamente offline.

**ESTADO ACTUAL: Hito 4 completado - Sistema de clasificación automática implementado con enfoque híbrido**

### Mejoras Implementadas (Basadas en recomendaciones de análisis)

#### ✅ Análisis Financiero Avanzado
- **KPIs Jerárquicos**: Implementados 3 niveles de análisis (resumen ejecutivo, tendencias, detalle por categorías)
- **Métricas Avanzadas**: Tasa de ahorro, promedio diario, categorías con mayor impacto
- **Análisis de Presupuestos**: Integración completa con sistema de presupuestos
- **Proyecciones Financieras**: Forecasting con regresión lineal e intervalos de confianza

#### ✅ Sistema de Clasificación Híbrido
- **Primera Capa**: Reglas exactas con diccionario de sinónimos y normalización
- **Segunda Capa**: Expresiones regulares para patrones comunes
- **Tercera Capa**: Machine Learning con extracción de características y active learning
- **Active Learning**: Umbrales de confianza y aprendizaje continuo

#### ✅ Componentes de Visualización
- **Gráficos Avanzados**: BarChart, PieChart con interactividad
- **Heatmap Calendar**: Visualización de días de mayor gasto (en desarrollo)
- **Indicadores de Progreso**: Barras de progreso para presupuestos

## Tabla de Contenidos

1.  [Hito 1: Base del Proyecto y Gestión de Categorías](#hito-1-base-del-proyecto-y-gestión-de-categorías) ✅ **COMPLETADO**
2.  [Hito 2: Módulo de Movimientos Manuales](#hito-2-módulo-de-movimientos-manuales) ✅ **COMPLETADO**
3.  [Hito 3: Procesamiento de Archivos Excel](#hito-3-procesamiento-de-archivos-excel) ✅ **COMPLETADO**
4.  [Hito 4: Sistema de Clasificación Automática](#hito-4-sistema-de-clasificación-automática) ✅ **COMPLETADO**
5.  [Hito 5: Dashboards Interactivos y Análisis Financiero](#hito-5-dashboards-interactivos-y-análisis-financiero) 🔄 **EN PROGRESO**
6.  [Hito 6: Sistema de Temas Personalizables](#hito-6-sistema-de-temas-personalizables)
7.  [Hito 7: Gestión de Presupuestos](#hito-7-gestión-de-presupuestos) ✅ **COMPLETADO**
8.  [Hito 8: Refinamiento y Estabilidad](#hito-8-refinamiento-y-estabilidad)
9.  [Hito 9: Proyecciones y Forecasting](#hito-9-proyecciones-y-forecasting) ✅ **COMPLETADO**
10. [Hito 10: Optimización y Características Avanzadas](#hito-10-optimización-y-características-avanzadas)

## MVP 2: Aplicación de Finanzas Personales - Versión Avanzada

### Objetivo

Desarrollar una versión avanzada de la aplicación de finanzas personales que incluya todas las funcionalidades del MVP 1 más características adicionales de análisis financiero avanzado, sistema de alertas inteligentes, exportación de reportes y optimizaciones de rendimiento para dispositivos móviles. **Se prioriza el cálculo proporcional de aportes mensuales en pareja**.

### Entregables Funcionales

#### 1. **Hito Prioritario: Aporte Proporcional en Pareja**
- **Registro de Ingresos Mensuales:**
  * Captura del sueldo de cada miembro de la pareja por período.
  * Almacenamiento persistente en base de datos local.
  * Validación de datos y cálculos automáticos.

- **Cálculo de Aportes Proporcionales:**
  * Exclusión automática de categorías "tarjeta titular" del total distribuible.
  * Cálculo proporcional basado en ingresos de cada persona.
  * Persistencia de resultados por período para análisis histórico.

- **Visualización Avanzada:**
  * Gráficos de líneas con historial de aportes por persona.
  * Gráfico de barras/área con total distribuido por mes.
  * Pantalla independiente con tabla detallada de análisis.
  * Comparativas entre períodos y personas.

#### 2. **Hito 2: Presupuesto Mensual por Categoría**
- **Gestión de Presupuestos:**
  * Campo de presupuesto en edición de categorías.
  * Almacenamiento persistente del valor ingresado.
  * Validación y límites configurables.

- **Indicadores Visuales:**
  * Barras de progreso mostrando porcentaje gastado vs presupuesto.
  * Colores reactivos según nivel de ejecución (verde, amarillo, rojo).
  * Alertas visuales al alcanzar 80%, 90% y 100%.

#### 3. **Hito 3: Simplificación del Home**
- **Eliminación de Componentes:**
  * Remoción de tarjeta "Balance e Ingresos" del home.
  * Ajuste del diseño general de la pantalla principal.
  * Optimización del espacio disponible.

#### 4. **Hito 4: Análisis Financiero Avanzado**
- **Análisis de Tendencias Temporales:**
  * Implementación de algoritmos de series de tiempo.
  * Identificación de patrones estacionales en gastos e ingresos.
  * Visualización con gráficos interactivos usando MPAndroidChart o Vico.

- **Análisis de Comportamiento:**
  * Identificación automática de categorías con mayor impacto.
  * Detección de gastos inusuales mediante análisis estadístico.
  * Predicciones de gasto futuro basadas en patrones históricos.

#### 5. **Hito 5: Sistema de Alertas y Notificaciones Inteligentes**
- **Alertas de Presupuesto:**
  * Notificaciones push al alcanzar 80%, 90% y 100% del presupuesto por categoría.
  * Alertas de gastos inusuales que superen el promedio histórico.
  * Recordatorios de pagos recurrentes próximos a vencer.

- **Insights Financieros:**
  * Generación automática de insights semanales y mensuales.
  * Comparación con períodos anteriores (mismo mes del año anterior).
  * Sugerencias de optimización de gastos basadas en análisis de datos.

#### 6. **Hito 6: Exportación y Reportes Personalizados**
- **Generación de Reportes:**
  * Exportación de datos a formatos CSV y Excel para análisis externo.
  * Filtros por período, categorías y tipos de transacciones.
  * Vista previa de datos antes de exportar.

- **Dashboard Ejecutivo:**
  * Vista resumida con KPIs principales para toma de decisiones rápidas.
  * Gráficos de evolución patrimonial a largo plazo.
  * Análisis de flujo de caja proyectado.

#### 7. **Hito 7: Sistema de Clasificación Inteligente**
- **Machine Learning Mejorado:**
  * Modelo de clasificación más sofisticado con mayor precisión.
  * Aprendizaje continuo basado en feedback del usuario.
  * Clasificación automática de transacciones recurrentes.

- **Gestión de Reglas Avanzadas:**
  * Editor visual de reglas de clasificación.
  * Importación/exportación de reglas de clasificación.
  * Validación automática de reglas para evitar conflictos.

#### 8. **Hito 8: Sistema de Metas Financieras y Simulador de Escenarios**
- **Gestión de Metas:**
  * Definición de metas financieras a corto, mediano y largo plazo.
  * Seguimiento del progreso hacia las metas con visualizaciones.
  * Alertas y recordatorios para mantener el progreso.

- **Simulador de Escenarios:**
  * Simulación de diferentes escenarios financieros.
  * Análisis de impacto de cambios en ingresos o gastos.
  * Proyecciones de ahorro basadas en diferentes estrategias.

### Criterios de Aceptación

#### Aporte Proporcional en Pareja
- Se puede registrar el sueldo de cada persona por mes.
- El cálculo se realiza correctamente y persiste en base de datos.
- Los gráficos muestran correctamente la evolución por período.
- La UI es clara y entendible.
- Funciona offline en dispositivos de gama media.

#### Presupuesto por Categoría
- El presupuesto puede ser editado por categoría.
- Las alertas visuales funcionan según umbrales definidos.
- Se incluye en los reportes.
- Se visualiza claramente el gasto frente al presupuesto.

#### Análisis Financiero
- Las predicciones tienen una precisión mínima del 70% en el primer mes de uso.
- Los gráficos son comprensibles y útiles.
- Las sugerencias son relevantes y accionables.

#### Sistema de Alertas
- Las notificaciones se envían en el momento correcto y son informativas.
- El usuario puede personalizar las alertas según sus preferencias.
- Las alertas no son intrusivas y respetan la configuración del dispositivo.

#### Exportación y Reportes
- La exportación a CSV/Excel mantiene la integridad de los datos.
- Los filtros se aplican correctamente.
- Los reportes incluyen todos los datos relevantes sin omisiones.

#### Clasificación Automática
- La precisión de clasificación automática supera el 85%.
- El sistema aprende efectivamente de las correcciones del usuario.
- Las reglas de clasificación son fáciles de crear y gestionar.

#### Metas Financieras
- Se pueden registrar metas con fecha objetivo.
- Las proyecciones son lógicas y coherentes con históricos.
- El seguimiento de metas es preciso y actualizado en tiempo real.

### Orden de Implementación Recomendado

| Orden | Hito                      | Prioridad | Complejidad | Duración Estimada |
|-------|---------------------------|-----------|-------------|-------------------|
| 1     | Aporte proporcional       | Alta      | Alta        | 4 semanas         |
| 2     | Presupuesto por categoría | Alta      | Media       | 2 semanas         |
| 3     | Eliminar tarjeta del home | Media     | Baja        | 1 semana          |
| 4     | Análisis financiero       | Alta      | Media/Alta  | 3 semanas         |
| 5     | Alertas inteligentes      | Media     | Media       | 2 semanas         |
| 6     | Exportación y reportes    | Media     | Media       | 2 semanas         |
| 7     | Clasificación automática  | Alta      | Alta        | 3 semanas         |
| 8     | Metas y simulador         | Media     | Alta        | 3 semanas         |

### Total Estimado: 20 semanas

### Sugerencias Técnicas Específicas

#### Aporte Proporcional en Pareja
- **Tabla `Sueldo`:** `id`, `nombrePersona`, `periodo`, `sueldo`
- **Cálculo:** Sumar gastos excluyendo categoría "tarjeta titular"
- **Aportes:** Calcular proporción de cada sueldo y aplicar al total distribuible
- **Base de datos:** Room con relaciones entre sueldos y movimientos
- **Vistas:** Jetpack Compose con gráficos MPAndroidChart o Vico
- **UseCase:** Lógica centralizada para cálculos proporcionales

#### Presupuesto por Categoría
- **Modificar entidad `Categoria`:** Agregar `presupuestoMensual: Double?`
- **Mostrar progreso:** Dashboard por categoría con barras de progreso
- **Lógica de comparación:** Estilos visuales reactivos (colores o íconos)
- **Integración:** Alertas push futuras con WorkManager

#### Análisis Financiero Avanzado
- **Gráficos:** MPAndroidChart para líneas y barras
- **Algoritmos:** Moving average, ARIMA si es viable
- **Módulo de insights:** Resumen textual semanal o mensual
- **Predicciones:** Modelos de series de tiempo simples

#### Sistema de Alertas
- **Programación:** WorkManager con AlarmManager
- **Alertas locales:** Activación al abrir la app o programadas
- **Sistema de umbrales:** Parametrizable desde configuración
- **Notificaciones:** Push con prioridad configurable

#### Exportación y Reportes
- **Librerías:** Apache POI o iText para generación
- **Vista previa:** Mostrar datos antes de exportar
- **Compartir:** Correo o almacenamiento local
- **Filtros:** Por fecha, categoría, tipo de transacción

#### Clasificación Automática
- **Modelo local:** TensorFlow Lite o reglas simples con aprendizaje
- **Dataset:** Basado en `descripción`, `monto`, `categoría`
- **Rendimiento:** Evaluar para no afectar experiencia de usuario
- **Precisión:** Objetivo >85% con feedback continuo

#### Metas Financieras
- **Gráficos:** Progreso circular o barras
- **Motor de simulación:** Considerar ingresos, gastos y ahorro proyectado
- **Escenarios:** Aumento de sueldo, reducción de gasto, ingresos extra
- **Proyecciones:** Basadas en datos históricos y tendencias

## Hito 1: Base del Proyecto y Gestión de Categorías

### Objetivo

Establecer la base arquitectónica del proyecto, configurar el entorno de desarrollo y desarrollar el módulo fundamental de gestión de categorías, que servirá como pilar para la clasificación de todas las transacciones financieras.
las categorias de inicio se encuentran en: /home/rick/AndroidStudioProjects/ControlFinanzas/Archivos/categorias/categorias.txt

### Entregables Funcionales

1.  **Proyecto Android Configurado:**
    *   Repositorio de código inicializado con la estructura de Clean Architecture (capas de Dominio, Datos, Presentación).
    *   Configuración de Gradle para Kotlin, Jetpack Compose, Room y Hilt.
    *   Base de datos Room inicializada con la tabla `Categorias`.
    *   Módulo de inyección de dependencias (Hilt) configurado para las capas iniciales.

2.  **Módulo de Gestión de Categorías (CRUD):**
    *   **Entidad `Categoria`:** Definida en la capa de Dominio.
    *   **DAO `CategoriaDao`:** Definido en la capa de Datos para operaciones CRUD básicas.
    *   **Repositorio `CategoriaRepository`:** Interfaz en Dominio, implementación en Datos.
    *   **Caso de Uso `GestionarCategoriasUseCase`:** En la capa de Dominio, con métodos para crear, leer, actualizar y eliminar categorías.
    *   **ViewModel `CategoriasViewModel`:** En la capa de Presentación, para gestionar el estado de la UI de categorías.
    *   **Pantalla `CategoriasScreen`:** Interfaz de usuario en Jetpack Compose para listar, añadir, editar y eliminar categorías.

### Criterios de Aceptación

-   El proyecto compila y se ejecuta sin errores en un emulador o dispositivo Android.
-   La aplicación muestra una pantalla inicial de gestión de categorías.
-   El usuario puede:
    *   Añadir nuevas categorías (ej. "Alimentos", "Transporte", "Entretenimiento").
    *   Ver una lista de todas las categorías añadidas.
    *   Editar el nombre de una categoría existente.
    *   Eliminar una categoría (sin transacciones asociadas por ahora).
    *   Las categorías persisten después de cerrar y reabrir la aplicación.
-   La estructura de capas (Dominio, Datos, Presentación) es clara y las dependencias fluyen correctamente (hacia el interior).
-   La inyección de dependencias con Hilt funciona para los componentes de categorías.
-   Se han definido pruebas unitarias básicas para el caso de uso y el ViewModel de categorías.

## Hito 2: Módulo de Movimientos Manuales (Gastos e Ingresos)

### Objetivo

Implementar la funcionalidad para que el usuario pueda registrar manualmente sus transacciones financieras, diferenciando entre gastos e ingresos, y asociándolos a categorías existentes. Este módulo será la base para todas las operaciones de registro de transacciones.

### Entregables Funcionales

1.  **Entidad `MovimientoManual`:**
    *   Definida en la capa de Dominio, incluyendo campos para `fecha`, `descripcion`, `monto`, `tipo` (enum `GASTO`/`INGRESO`), `categoriaId` (opcional) y `notas`.

2.  **DAO `MovimientoManualDao`:**
    *   Definido en la capa de Datos, con operaciones CRUD para `MovimientoManual`.
    *   Métodos para obtener movimientos por tipo (gastos/ingresos) y por rango de fechas.
    *   Métodos para sumar montos por tipo y por categoría.

3.  **Repositorio `MovimientoManualRepository`:**
    *   Interfaz en Dominio, implementación en Datos.

4.  **Caso de Uso `GestionarMovimientosManualesUseCase`:**
    *   En la capa de Dominio, con métodos para crear, leer, actualizar y eliminar `MovimientoManual`.
    *   Lógica para asociar movimientos a categorías existentes.

5.  **ViewModel `MovimientosManualesViewModel`:**
    *   En la capa de Presentación, para gestionar el estado de la UI de movimientos manuales.
    *   Manejo de filtros por tipo de movimiento (gastos, ingresos, todos).

6.  **Pantallas de UI (Jetpack Compose):**
    *   **`MovimientosManualesScreen`:** Muestra una lista de movimientos manuales, con diferenciación visual entre gastos e ingresos. Incluye filtros por tipo y navegación a la pantalla de agregar/editar.
    *   **`AgregarEditarMovimientoScreen`:** Formulario para introducir los detalles de un nuevo movimiento o editar uno existente. Incluye un selector para el tipo de movimiento (Gasto/Ingreso) y un selector de categoría.

7.  **Actualización de la Base de Datos:**
    *   Migración de Room para añadir la tabla `MovimientosManuales` (si se parte de una base de datos existente) o inclusión directa en el esquema inicial.

### Criterios de Aceptación

-   El usuario puede acceder a la pantalla de movimientos manuales desde la pantalla principal.
-   En la pantalla de movimientos manuales, el usuario puede:
    *   Añadir un nuevo movimiento, especificando si es un gasto o un ingreso, su monto, descripción, fecha periodo de facturacion al q corresponde y categoría (opcional).
    *   Ver la lista de movimientos manuales, con gastos e ingresos claramente diferenciados (ej. por color).
    *   Filtrar la lista para mostrar solo gastos, solo ingresos o todos los movimientos.
    *   Editar los detalles de un movimiento existente.
    *   Eliminar un movimiento.
-   Los movimientos persisten después de cerrar y reabrir la aplicación.
-   La suma de gastos e ingresos se calcula correctamente en el ViewModel.
-   Se han definido pruebas unitarias para el caso de uso y el ViewModel de movimientos manuales.
-   

## Hito 3: Procesamiento de Archivos Excel

### Objetivo

Desarrollar la capacidad de la aplicación para importar transacciones financieras automáticamente desde archivos Excel de estados de cuenta bancarios, incluyendo la detección de duplicados y la asignación inicial de categorías. lor archivos de ejemplo reales se encuentran en /home/rick/AndroidStudioProjects/ControlFinanzas/Archivos/excel
son 22 tipos de archivos que se procesaran
estado de cuenta de cierre de mes :
/home/rick/AndroidStudioProjects/ControlFinanzas/Archivos/excel/estado de cierre.xls
y ultimos movimientos:
/home/rick/AndroidStudioProjects/ControlFinanzas/Archivos/excel/ultimosMovimientos.xls

### Entregables Funcionales

1.  **Módulo de Procesamiento de Excel:**
    *   Integración de una librería para leer archivos `.xls` y `.xlsx` (ej. Apache POI).
    *   Lógica para extraer `fecha`, `descripcion`, `monto`, `tipo de tarjeta`, `código de referencia` y `mes de facturación` de filas de transacciones.
    *   Manejo de diferentes formatos de columnas y hojas dentro de los archivos Excel.

2.  **Detección de Duplicados:**
    *   Implementación de una `clave única` para cada transacción (ej. combinación de fecha y monto).
    *   Lógica para verificar si una transacción ya existe en la base de datos antes de insertarla.

3.  **Integración con la Base de Datos:**
    *   **Entidad `Movimiento`:** Definida en la capa de Dominio para transacciones importadas, similar a `MovimientoManual` pero con campos específicos de extractos bancarios.
    *   **DAO `MovimientoDao`:** Para operaciones CRUD de `Movimiento`.
    *   **Repositorio `MovimientoRepository`:** Interfaz en Dominio, implementación en Datos.
    *   **Caso de Uso `GestionarMovimientosUseCase`:** En la capa de Dominio, con métodos para importar transacciones desde un archivo y guardarlas en la base de datos.

4.  **Interfaz de Usuario para Importación:**
    *   **Pantalla `ImportarExcelScreen`:** Permite al usuario seleccionar un archivo Excel del almacenamiento del dispositivo.
    * el usuario debe seleccionar el periodo  de factuacion al que corresponde en una lista desplegable
    *   Indicador de progreso durante el procesamiento del archivo.
    *   Mensajes de éxito/error después de la importación.

### Criterios de Aceptación

-   El usuario puede seleccionar un archivo Excel de estado de cuenta bancario desde la aplicación.
-   La aplicación procesa el archivo y extrae correctamente las transacciones.
-   Las transacciones extraídas se guardan en la base de datos.
-   La detección de duplicados funciona correctamente: si se intenta importar el mismo archivo dos veces, las transacciones duplicadas no se añaden a la base de datos.
-   Las transacciones importadas se pueden visualizar en una lista (inicialmente, una lista simple, sin categorización automática aún).
-   Se manejan errores comunes de archivos (ej. archivo no válido, formato incorrecto) con mensajes claros al usuario.
-   Se han definido pruebas unitarias para el módulo de procesamiento de Excel y el caso de uso de importación.
-   Se debe modificar el historial el cual mostrara en un principio los movimientos sin categoria y luego los demas
-   la pantalla princiipal debe mostrar un contador de los movimientos no clasificados. 

## Hito 4: Sistema de Clasificación Automática

### Objetivo

Implementar un sistema de machine learning local que aprenda de las clasificaciones manuales del usuario para sugerir automáticamente categorías para nuevas transacciones importadas, reduciendo la necesidad de intervención manual.

###datos complementarios
dispones del archivo /home/rick/AndroidStudioProjects/ControlFinanzas/Archivos/Gastos_historicos/historial_gastos.csv
donde tiene 3 columnas Item,periodo de facturacion,monto de varios meses hacia atras, incluyeelos como data historica 

dispones del archivo /home/rick/AndroidStudioProjects/ControlFinanzas/Archivos/Movimientos_historicos/Historia.csv
donde tienes las clasificaaciones realizadas los meses anteriores.
para que veas como se comporto la asignacion de categorias en meses pasados y puedas determinar cual es el mejor  plan para ejecutar esta tarea

### Entregables Funcionales

1.  **Entidad `ClasificacionAutomatica`:**
    *   Definida en la capa de Dominio, para almacenar patrones de descripción, `categoriaId` asociada y un `nivelConfianza`.

2.  **DAO `ClasificacionAutomaticaDao` y Repositorio `ClasificacionAutomaticaRepository`:**
    *   Para gestionar la persistencia de los patrones de clasificación.

3.  **Caso de Uso `GestionarClasificacionAutomaticaUseCase`:**
    *   En la capa de Dominio, con métodos para:
        *   `aprenderPatron(descripcion: String, categoriaId: Long)`: Registra un nuevo patrón o actualiza uno existente basado en la clasificación manual del usuario.
        *   `sugerirCategoria(descripcion: String)`: Busca el patrón más relevante en la base de conocimiento y devuelve una sugerencia de `categoriaId` con su `nivelConfianza`.

4.  **Integración con la Importación de Excel:**
    *   Después de importar transacciones, el sistema intentará sugerir una categoría automáticamente utilizando el `GestionarClasificacionAutomaticaUseCase`.
    *   Las transacciones se guardarán con la categoría sugerida (si el nivel de confianza es alto) o como "no clasificadas" si no hay una sugerencia fiable.

5.  **Interfaz de Usuario para Clasificación:**
    *   **Pantalla `ClasificacionPendienteScreen`:** Muestra una lista de transacciones que requieren clasificación (ya sea porque no se pudo sugerir una categoría o porque la sugerencia necesita confirmación).
    *   Permite al usuario aceptar la sugerencia o clasificar manualmente la transacción.
    *   Al clasificar manualmente, el sistema aprende del nuevo patrón.

### Criterios de Aceptación

-   Cuando se importa un archivo Excel, las transacciones se procesan y se intenta asignar una categoría automáticamente.
-   Si una transacción se clasifica manualmente, el sistema "aprende" de esa clasificación y la registra como un patrón.
-   Para transacciones futuras con descripciones similares, el sistema sugiere la categoría aprendida.
-   El usuario puede ver una lista de transacciones pendientes de clasificación.
-   El usuario puede aceptar una sugerencia de clasificación o anularla y clasificar manualmente.
-   La precisión de las sugerencias mejora a medida que el usuario clasifica más transacciones.
-   Se han definido pruebas unitarias para el caso de uso de clasificación automática.

## Hito 5: Dashboards Interactivos y Análisis Financiero

### Objetivo

Proporcionar al usuario una visión clara y concisa de su situación financiera a través de dashboards interactivos que muestren resúmenes, tendencias y análisis por categorías, utilizando tanto los movimientos importados como los manuales.

### Entregables Funcionales

1.  **Caso de Uso `AnalisisFinancieroUseCase`:**
    *   En la capa de Dominio, con métodos para:
        *   `obtenerResumenFinanciero(fechaInicio, fechaFin)`: Calcula el total de ingresos, gastos y balance neto para un período.
        *   `obtenerMovimientosPorCategoria(fechaInicio, fechaFin)`: Agrupa y suma movimientos por categoría.
        *   `obtenerTendenciaMensual(cantidadMeses)`: Calcula ingresos, gastos y balance para los últimos N meses.
    *   Este caso de uso utilizará tanto `MovimientoRepository` como `MovimientoManualRepository` para consolidar los datos.

2.  **ViewModel `DashboardAnalisisViewModel`:**
    *   En la capa de Presentación, para gestionar el estado de la UI del dashboard.
    *   Manejo de filtros de período (ej. último mes, 3 meses, 6 meses, 12 meses).

3.  **Pantalla `DashboardAnalisisScreen` (Jetpack Compose):**
    *   **Tarjeta de Resumen Financiero:** Muestra ingresos totales, gastos totales y balance neto para el período seleccionado.
    *   **Tarjeta de Tendencia Mensual:** Muestra la evolución de ingresos, gastos y balance mes a mes (inicialmente en formato de lista, con preparación para gráficos).
    *   **Tarjeta de Análisis por Categorías:** Muestra un desglose de gastos e ingresos por las categorías más relevantes (ej. top 10).
    *   **Filtros de Período:** Permite al usuario seleccionar el rango de tiempo para el análisis.

4.  **Integración de Datos:**
    *   Asegurar que los datos de `Movimiento` (importados) y `MovimientoManual` (manuales) se consoliden correctamente para todos los cálculos y visualizaciones del dashboard.

### Criterios de Aceptación

-   La aplicación muestra una pantalla de dashboard con un resumen financiero (ingresos, gastos, balance).
-   El usuario puede cambiar el período de tiempo del análisis (ej. último mes, últimos 3 meses), y los datos del dashboard se actualizan correctamente.
-   El dashboard muestra una sección de análisis por categorías, indicando cuánto se ha gastado/ingresado en cada una.
-   El dashboard muestra una sección de tendencia mensual, con los totales de ingresos, gastos y balance para los últimos meses.
-   Todos los cálculos del dashboard incluyen tanto los movimientos importados como los manuales.
-   Los datos presentados son precisos y consistentes con las transacciones registradas.
-   Se han definido pruebas unitarias para el caso de uso de análisis financiero.

## Hito 6: Sistema de Temas Personalizables

### Objetivo

Permitir al usuario personalizar la apariencia visual de la aplicación seleccionando entre un conjunto de temas predefinidos, mejorando la experiencia de usuario y la estética de la aplicación.

### Entregables Funcionales

1.  **Entidad `TemaApp` (Enum):**
    *   Definida en la capa de Dominio, con los nombres de los temas disponibles (ej. `NARANJA`, `AZUL`, `VERDE`, `PURPURA`, `ROJO`, `GRIS`).

2.  **Paletas de Colores (`TemasColores.kt`):**
    *   Definición de `ColorScheme` para cada tema, incluyendo variantes para modo claro y oscuro.

3.  **Sistema de Preferencias:**
    *   `ConfiguracionPreferences`: Clase en la capa de Datos para guardar y recuperar la preferencia del tema seleccionado por el usuario (usando `DataStore` o `SharedPreferences`).

4.  **Caso de Uso `GestionarConfiguracionUseCase`:**
    *   En la capa de Dominio, con métodos para `guardarTemaSeleccionado` y `obtenerTemaSeleccionado` (que devuelve un `Flow`).

5.  **ViewModel `ConfiguracionViewModel`:**
    *   En la capa de Presentación, para gestionar el estado de la UI de configuración de temas.

6.  **Pantalla `ConfiguracionScreen` (Jetpack Compose):**
    *   Interfaz de usuario que permite al usuario seleccionar un tema de una lista de opciones.
    *   Muestra una vista previa del tema seleccionado.

7.  **Integración del Tema en la Aplicación:**
    *   Modificación del archivo `Theme.kt` principal para que la aplicación observe el tema seleccionado por el usuario y aplique el `ColorScheme` correspondiente dinámicamente.

### Criterios de Aceptación

-   La aplicación incluye una nueva pantalla de configuración donde el usuario puede elegir un tema.
-   Al seleccionar un tema, la apariencia de toda la aplicación (colores de la barra superior, botones, fondos, etc.) cambia instantáneamente.
-   La selección del tema persiste después de cerrar y reabrir la aplicación.
-   Los temas predefinidos (Naranja, Azul, Verde, Púrpura, Rojo, Gris) están disponibles y funcionan correctamente.
-   El modo oscuro se aplica correctamente a cada tema si el sistema lo tiene activado.
-   Se han definido pruebas unitarias para el caso de uso de configuración y el ViewModel.

## Hito 7: Gestión de Presupuestos

### Objetivo

Permitir al usuario establecer y seguir presupuestos para sus categorías de gastos, proporcionando herramientas para monitorear su progreso y recibir alertas cuando se acerquen o excedan sus límites.

### Entregables Funcionales

1.  **Entidad `Presupuesto`:**
    *   Definida en la capa de Dominio, incluyendo campos para `categoriaId`, `montoPresupuestado`, `mes` y `año`.

2.  **DAO `PresupuestoDao` y Repositorio `PresupuestoRepository`:**
    *   Para gestionar la persistencia de los presupuestos.
    *   Métodos para obtener presupuestos por categoría y período.

3.  **Caso de Uso `GestionarPresupuestosUseCase`:**
    *   En la capa de Dominio, con métodos para crear, leer, actualizar y eliminar presupuestos.
    *   Lógica para calcular el gasto actual de una categoría en un período y compararlo con el presupuesto.

4.  **ViewModel `PresupuestosViewModel`:**
    *   En la capa de Presentación, para gestionar el estado de la UI de presupuestos.
    *   Manejo de la lista de presupuestos y el progreso de cada uno.

5.  **Pantallas de UI (Jetpack Compose):**
    *   **`PresupuestosScreen`:** Muestra una lista de los presupuestos definidos por el usuario, con un indicador visual del progreso (ej. barra de progreso) y el monto restante/excedido.
    *   **`AgregarEditarPresupuestoScreen`:** Formulario para establecer un nuevo presupuesto o modificar uno existente, permitiendo seleccionar la categoría, el monto y el período.

6.  **Integración con Dashboards (Opcional en este hito):**
    *   Actualizar el `AnalisisFinancieroUseCase` para incluir métricas de presupuesto en el dashboard, mostrando el porcentaje de uso del presupuesto por categoría.

### Criterios de Aceptación

-   El usuario puede acceder a la pantalla de gestión de presupuestos.
-   En la pantalla de presupuestos, el usuario puede:
    *   Añadir un nuevo presupuesto para una categoría específica y un mes/año.
    *   Ver una lista de sus presupuestos, mostrando el monto presupuestado y el gasto actual para ese período.
    *   Editar un presupuesto existente.
    *   Eliminar un presupuesto.
-   El progreso del presupuesto se calcula correctamente (gasto actual vs. monto presupuestado).
-   Los presupuestos persisten después de cerrar y reabrir la aplicación.
-   Se han definido pruebas unitarias para el caso de uso y el ViewModel de presupuestos.

## Hito 8: Refinamiento y Estabilidad

### Objetivo

Realizar una revisión exhaustiva de la aplicación, optimizar el rendimiento, mejorar la experiencia de usuario, implementar pruebas de integración y asegurar la estabilidad general del sistema antes de un posible lanzamiento.

### Entregables Funcionales

1.  **Optimización de Rendimiento:**
    *   Revisión y optimización de consultas a la base de datos.
    *   Optimización de la UI para reducir recomposiciones innecesarias en Jetpack Compose.
    *   Manejo eficiente de operaciones en segundo plano (coroutine scopes).

2.  **Mejoras de Experiencia de Usuario (UX):**
    *   Implementación de animaciones y transiciones fluidas.
    *   Mejora de la retroalimentación visual al usuario (ej. estados de carga, mensajes de éxito/error más claros).
    *   Revisión de la usabilidad de los formularios y flujos de navegación.
    *   Consideración de casos de borde y manejo de entradas inválidas.

3.  **Pruebas de Integración y UI:**
    *   Implementación de pruebas de integración para verificar la interacción entre diferentes componentes y capas (ej. UI con ViewModel, ViewModel con Casos de Uso, Casos de Uso con Repositorios).
    *   Pruebas de UI con Espresso o Compose Testing para asegurar que las pantallas se comportan como se espera.

4.  **Manejo de Errores y Robustez:**
    *   Implementación de un sistema de manejo de errores centralizado para capturar y reportar excepciones.
    *   Mejora de la robustez de la aplicación frente a datos corruptos o inesperados.

5.  **Documentación Adicional:**
    *   Actualización de la documentación técnica con los cambios finales.
    *   Creación de un `README.md` completo para el proyecto.
    *   Generación de un archivo `APK` de lanzamiento.

### Criterios de Aceptación

-   La aplicación se ejecuta de manera fluida y sin retrasos perceptibles en dispositivos de gama media.
-   No se observan crashes inesperados durante el uso normal de la aplicación.
-   Todas las funcionalidades implementadas en hitos anteriores funcionan correctamente de forma integrada.
-   Las pruebas de integración y UI pasan con éxito.
-   La aplicación proporciona una experiencia de usuario pulida y profesional.
-   La documentación del proyecto está actualizada y es completa.
-   Se ha generado un APK de lanzamiento que puede ser instalado y probado externamente.
-   El código base es limpio, bien comentado y sigue las mejores prácticas de Kotlin y Android.

## Hito 9: Proyecciones y Forecasting

### Objetivo

Implementar herramientas para la generación de proyecciones y forecasting de ingresos, gastos y balance neto, utilizando modelos de machine learning y análisis de series de tiempo.

### Entregables Funcionales

1.  **Modelo de Machine Learning:**
    *   Implementación de un modelo de machine learning para la predicción de ingresos, gastos y balance neto.

2.  **Análisis de Series de Tiempo:**
    *   Implementación de técnicas de análisis de series de tiempo para identificar patrones y tendencias.

3.  **Interfaz de Usuario para Forecasting:**
    *   **Pantalla `ForecastingScreen`:** Permite al usuario seleccionar un período para la generación de proyecciones.
    *   Muestra las proyecciones generadas por el modelo de machine learning.

4.  **Integración con Dashboards:**
    *   Actualizar el `AnalisisFinancieroUseCase` para incluir las proyecciones generadas por el modelo.

### Criterios de Aceptación

-   La aplicación muestra una nueva pantalla de forecasting.
-   El usuario puede seleccionar un período para la generación de proyecciones.
-   Las proyecciones generadas son precisas y útiles para la toma de decisiones.
-   Se han definido pruebas unitarias para el caso de uso de forecasting.

## Hito 10: Optimización y Características Avanzadas

### Objetivo

Realizar una revisión exhaustiva de la aplicación, optimizar el rendimiento, mejorar la experiencia de usuario, implementar pruebas de integración y asegurar la estabilidad general del sistema antes de un posible lanzamiento.

### Entregables Funcionales

1.  **Optimización de Rendimiento:**
    *   Revisión y optimización de consultas a la base de datos.
    *   Optimización de la UI para reducir recomposiciones innecesarias en Jetpack Compose.
    *   Manejo eficiente de operaciones en segundo plano (coroutine scopes).

2.  **Mejoras de Experiencia de Usuario (UX):**
    *   Implementación de animaciones y transiciones fluidas.
    *   Mejora de la retroalimentación visual al usuario (ej. estados de carga, mensajes de éxito/error más claros).
    *   Revisión de la usabilidad de los formularios y flujos de navegación.
    *   Consideración de casos de borde y manejo de entradas inválidas.

3.  **Pruebas de Integración y UI:**
    *   Implementación de pruebas de integración para verificar la interacción entre diferentes componentes y capas (ej. UI con ViewModel, ViewModel con Casos de Uso, Casos de Uso con Repositorios).
    *   Pruebas de UI con Espresso o Compose Testing para asegurar que las pantallas se comportan como se espera.

4.  **Manejo de Errores y Robustez:**
    *   Implementación de un sistema de manejo de errores centralizado para capturar y reportar excepciones.
    *   Mejora de la robustez de la aplicación frente a datos corruptos o inesperados.

5.  **Documentación Adicional:**
    *   Actualización de la documentación técnica con los cambios finales.
    *   Creación de un `README.md` completo para el proyecto.
    *   Generación de un archivo `APK` de lanzamiento.

### Criterios de Aceptación

-   La aplicación se ejecuta de manera fluida y sin retrasos perceptibles en dispositivos de gama media.
-   No se observan crashes inesperados durante el uso normal de la aplicación.
-   Todas las funcionalidades implementadas en hitos anteriores funcionan correctamente de forma integrada.
-   Las pruebas de integración y UI pasan con éxito.
-   La aplicación proporciona una experiencia de usuario pulida y profesional.
-   La documentación del proyecto está actualizada y es completa.
-   Se ha generado un APK de lanzamiento que puede ser instalado y probado externamente.
-   El código base es limpio, bien comentado y sigue las mejores prácticas de Kotlin y Android.