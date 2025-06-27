# Guía de Especificaciones de UI de FinaVision para Desarrollo Android

## 1. Introducción

Este documento proporciona una guía de diseño y especificaciones técnicas detalladas para la recreación de la interfaz de usuario (UI) del prototipo web de FinaVision en una aplicación nativa para Android. El objetivo es lograr una consistencia visual y funcional exacta, permitiendo a los desarrolladores de Android implementar cada pantalla y componente sin necesidad de consultar directamente el prototipo web.

- **Stack del Prototipo Web:** Next.js (App Router), React, Tailwind CSS, ShadCN UI, Lucide Icons.
- **Principios de Diseño:**
    - **Claridad y Simplicidad:** Interfaz limpia, intuitiva y fácil de usar.
    - **Modularidad:** Diseño basado en componentes reutilizables (Tarjetas, Botones, etc.).
    - **Jerarquía Visual:** Uso consistente de tipografía, colores y espaciado para guiar al usuario.
    - **Feedback Visual:** Proporcionar respuesta inmediata a las interacciones del usuario (estados de hover, focus, active, loading).

---

## 2. Sistema de Diseño (Design System)

### 2.1. Paleta de Colores (Tema Naranja Claro)

La aplicación utiliza un tema claro con tonos anaranjados. Es crucial que estos colores se definan como recursos reutilizables en el sistema de diseño de Android (ej. `colors.xml`).

| Variable CSS        | Uso Principal                                | Valor HSL         | Valor HEX Aprox. |
| ------------------- | -------------------------------------------- | ----------------- | ---------------- |
| `--background`      | Fondo principal de la app                    | `25 100% 97%`     | `#FFFAF5`        |
| `--foreground`      | Texto principal y de alto contraste          | `25 30% 25%`      | `#594940`        |
| `--card`            | Fondo de tarjetas y contenedores principales | `0 0% 100%`       | `#FFFFFF`        |
| `--primary`         | Color primario (botones, acentos, gráficos) | `30 90% 55%`      | `#F78D1D`        |
| `--primary-foreground` | Texto sobre elementos primarios (botones)   | `25 50% 98%`      | `#FEFBF9`        |
| `--secondary`       | Fondo de elementos secundarios (ej. track de progreso) | `35 80% 92%`      | `#FADFCF`        |
| `--accent`          | Acentos y estados hover/seleccionado         | `35 85% 70%`      | `#F5B991`        |
| `--accent-foreground` | Texto sobre elementos de acento             | `30 70% 25%`      | `#754C24`        |
| `--border`          | Bordes de inputs, tablas y contenedores      | `30 50% 85%`      | `#EED9C9`        |
| `--muted-foreground`| Texto secundario, descripciones, placeholders| `30 25% 45%`      | `#8C7C71`        |
| `--destructive`     | Errores, acciones de borrado                 | `0 70% 50%`       | `#D9534F` (aprox)|
| `--ring`            | Anillo de enfoque (Focus ring)               | `30 90% 55%`      | `#F78D1D`        |

### 2.2. Tipografía

- **Familia de Fuente:** `PT Sans` (importar desde Google Fonts).
- **Pesos:**
    - **Normal:** `400`
    - **Bold (Negrita):** `700`
- **Jerarquía de Tamaños (valores en `sp` para Android):**
    - **Título de Página (PageHeader):** 30sp, Bold, color `--primary`.
    - **Título de Tarjeta (CardTitle):** 24sp, Bold, color `--foreground`.
    - **Descripción de Tarjeta/Página:** 14sp, Normal, color `--muted-foreground`.
    - **Texto de Cuerpo/Tabla:** 14sp, Normal, color `--foreground`.
    - **Texto de Botón:** 14sp, Medium (500), el peso puede variar.
    - **Etiquetas de Formulario (Label):** 14sp, Medium (500), color `--foreground`.
    - **Texto de Input:** 14sp, Normal.
    - **Texto Muted/Secundario:** 12sp-14sp, Normal, color `--muted-foreground`.

### 2.3. Iconografía

- **Librería:** Replicar los iconos de `lucide-react`. La mayoría son estándar y se pueden encontrar en librerías de iconos de Material Design o exportarse como SVG/Vector Drawable.
- **Tamaño Base:** 24dp x 24dp.
- **En Botones/Items de menú:** 16dp x 16dp.
- **Color:** Generalmente `currentcolor` (hereda el color del texto), o un color específico como `--muted-foreground` o `--primary`.

### 2.4. Espaciado y Layout

- **Unidad Base:** 8dp.
- **Padding de Contenedores/Tarjetas:** 24dp (`p-6`).
- **Padding de Items de Lista/Tabla:** 16dp (`p-4`).
- **Gap entre elementos:** 16dp (`gap-4`) para elementos principales, 8dp (`gap-2`) para elementos secundarios.
- **Radio de Esquinas (Corner Radius):**
    - **Tarjetas, Diálogos:** 8dp (`rounded-lg`).
    - **Botones, Inputs:** 6dp (`rounded-md`).

---

## 3. Especificaciones de Componentes Comunes

### 3.1. Tarjeta (Card)

- **Fondo:** `var(--card)` (#FFFFFF).
- **Borde:** 1dp, `var(--border)`.
- **Sombra (Elevation):** Sombra sutil, equivalente a `shadow-sm` de Tailwind.
- **Padding Interno:**
    - **CardHeader, CardFooter:** 24dp.
    - **CardContent:** 24dp, con `padding-top: 0`.

### 3.2. Botón (Button)

| Variante    | Fondo                 | Texto                    | Borde           | Estado Hover/Pressed      |
|-------------|-----------------------|--------------------------|-----------------|---------------------------|
| **Default** | `var(--primary)`      | `var(--primary-foreground)` | Ninguno         | `var(--primary)` con 90% opacidad |
| **Outline** | Transparente          | `var(--foreground)`      | 1dp, `var(--input)` | Fondo `var(--accent)`     |
| **Ghost**   | Transparente          | `var(--foreground)`      | Ninguno         | Fondo `var(--accent)`     |
| **Destructive**| `var(--destructive)`| `var(--destructive-foreground)` | Ninguno | `var(--destructive)` con 90% opacidad |

- **Padding (Default size):** 16dp horizontal, 8dp vertical.
- **Altura (Default size):** 40dp.
- **Botón de Icono (Icon Button):** 40dp x 40dp, sin padding, icono centrado.

### 3.3. Input / Select

- **Altura:** 40dp.
- **Fondo:** `var(--background)`.
- **Borde:** 1dp, `var(--input)`.
- **Estado de Foco (Focus):** Borde de 2dp color `var(--ring)`.
- **Padding Interno:** 12dp horizontal.
- **Texto Placeholder:** Color `var(--muted-foreground)`.

### 3.4. Diálogo / Modal (Dialog)

- **Overlay:** Color negro con 80% de opacidad.
- **Contenido:** Estilo de `Card` (fondo blanco, borde, sombra, radio de esquinas de 8dp).
- **Padding:** 24dp.
- **Botón de Cierre (X):** Posicionado en la esquina superior derecha.

---

## 4. Especificaciones por Pantalla

### 4.1. App Shell (Layout Global)

- **Sidebar (Menú de Navegación Izquierdo):**
    - **Fondo:** `var(--sidebar-background)` (`#FADECB` aprox).
    - **Ancho:** 256dp.
    - **Header:** Logo "FinaVision". Padding de 16dp.
    - **Items de Menú:**
        - **Layout:** Icono a la izquierda, etiqueta a la derecha. Padding de 8dp vertical, 16dp horizontal.
        - **Icono:** 24dp x 24dp.
        - **Texto:** 14sp, Medium. Color `var(--sidebar-foreground)`.
        - **Estado Activo:** Fondo `var(--sidebar-accent)`, color de texto `var(--sidebar-accent-foreground)`.
    - **Íconos por item:**
        - **Dashboard:** `LayoutDashboard`
        - **Statements:** `FileText`
        - **Budget:** `PiggyBank`
        - **Categories:** `Tags`
        - **Classify:** `ListChecks`

- **Header (Barra Superior):**
    - **Altura:** 64dp.
    - **Fondo:** `var(--background)` con efecto blur/translúcido.
    - **Borde Inferior:** 1dp, `var(--border)`.
    - **Menú de Usuario (Derecha):**
        - Avatar de 36dp x 36dp con imagen de placeholder.
        - Al tocar, abre un menú desplegable con items: Profile, Settings, Log out.

- **Footer (Pie de Página):**
    - **Texto:** "© [Año] FinaVision. All rights reserved."
    - **Estilo:** 12sp, `var(--muted-foreground)`.
    - **Borde Superior:** 1dp, `var(--border)`.
    - **Padding:** 16dp.

### 4.2. Dashboard (`/`)

- **PageHeader:**
    - **Título:** "Resumen de Gastos", 30sp, Bold, `var(--primary)`.
    - **Descripción:** "Tu resumen financiero de un vistazo.", 14sp, `var(--muted-foreground)`.
    - **Botón "Ver Resumen General":** Aparece condicionalmente. Estilo `Button Outline`.

- **Grid de StatCards (4 columnas):**
    - Espaciado de 16dp.
    - Cada `StatCard` contiene:
        - Título (12sp, `var(--muted-foreground)`).
        - Icono (20dp, `var(--accent-foreground)`) alineado a la derecha.
        - Valor (24sp, Bold, `var(--primary)`).
        - Descripción (12sp, `var(--muted-foreground)`).

- **Grid de Contenido Principal (2 columnas):**
    - Espaciado de 24dp.
    - **Tarjeta "Tendencia de Gasto Mensual":**
        - Contiene un **Gráfico de Barras**. Ver `docs/GRAPH_STYLES_GUIDE.md`.
    - **Tarjeta "Estado del Presupuesto":**
        - Lista de presupuestos. Cada item es clickeable.
        - **Item Normal:** Padding de 8dp, `border-radius` de 6dp. `onHover/onPressed` fondo `var(--accent)` con 70% opacidad.
        - **Item Seleccionado:** Fondo `var(--accent)`, con un borde/anillo de 2dp color `var(--primary)`.
        - **Componente ProgressBar:** Alto de 12dp, fondo `var(--secondary)`, progreso `var(--primary)`.
    - **Tarjeta "Gasto por Categoría / Desglose":**
        - **Vista Gráfico:** Contiene un **Gráfico de Torta**. Ver `docs/GRAPH_STYLES_GUIDE.md`. Cada porción es clickeable.
        - **Vista Tabla:** Se muestra al hacer click en una porción. Es una tabla estándar con 3 columnas (Comercio, Monto, Transacciones). Un botón "Volver al Gráfico" (estilo `Outline`) debe estar visible.
    - **Tarjeta "Proyecciones y Perspectivas":**
        - Contiene 3 secciones apiladas verticalmente.
        - Cada sección tiene:
            - Fondo de color (`var(--accent)` con 20% opacidad o `var(--secondary)` con 30% opacidad).
            - Padding de 16dp, `border-radius` de 8dp.
            - Icono (28dp, `var(--primary)` o similar).
            - Título (14sp, Semibold), Valor (20sp, Bold), Descripción (12sp, `var(--muted-foreground)`).

### 4.3. Upload Statements (`/statements`)

- **PageHeader:** Título "Upload Statements".
- **Tarjeta Principal "Upload New Statement":**
    - Selectores para "File Type", "Month", "Year" en un layout de 3 columnas (en desktop).
    - Zona de "Drag & Drop":
        - Borde punteado de 2dp, `var(--border)`. `onHover` borde `var(--primary)`.
        - Icono `UploadCloud` (48dp, `var(--muted-foreground)`).
        - Texto y un botón "Browse Files" (`Outline`).
    - Botón principal "Upload and Process" (`Default`).

- **Tarjeta "Upload History":**
    - Muestra los últimos 5 archivos procesados.
    - Cada item tiene:
        - Icono de estado (24dp): `CheckCircle2` (verde) para éxito, `AlertTriangle` (rojo) para error, `FileText` (primario) para procesando.
        - Nombre de archivo, tamaño, tipo y período.
        - Barra de progreso (si está procesando).
        - Mensaje de estado.

### 4.4. Budget & Categories (`/budget`, `/categories`)

Ambas pantallas siguen un patrón similar de "Tabla de Datos + Diálogo de Edición".

- **PageHeader:** Título y botón "Add New" (`Default`).
- **Tarjeta de Contenido:**
    - Título, Descripción, Icono principal.
    - **Tabla:**
        - Cabecera: Texto 12sp, `var(--muted-foreground)`.
        - Celdas: Padding 16dp.
        - Fila: Borde inferior `var(--border)`, `onHover` fondo `var(--muted)` con 50% opacidad.
        - Columna de Acciones: Contiene `IconButtons` (Ghost) para Editar y Borrar (color `destructive`).
- **Diálogo (Add/Edit):**
    - Título y Descripción.
    - Formulario con Labels e Inputs/Selects.
    - Footer con botones "Cancel" (`Outline`) y "Save" (`Default`).

### 4.5. Classify (`/classify`)

- **PageHeader:** Título "Classify Transactions".
- **Tarjeta "Uncategorized Transactions":**
    - Fondo de fila (`TableRow`) especial: `var(--accent)` con 20% opacidad.
    - Las columnas de "Card Type" y "Category" contienen componentes `Select` para la edición.
    - La columna "Action" contiene un botón "Save" (`Default`, tamaño `sm`).
- **Tarjeta "Categorized Transactions":**
    - Tabla estándar. Los `Selects` están en modo de solo lectura (deshabilitados).
    - La columna "Status" contiene un `Badge` (etiqueta con fondo y bordes redondeados) color verde con el texto "Categorizado".

---

## 5. Interacciones y Animaciones

- **Transiciones:** Todas las transiciones de color/fondo (ej. en hover) deben tener una duración de 200-300ms con una curva de `ease-in-out`.
- **Entrada de Diálogos/Menús:** Deben aparecer con una animación de `fade-in` y `scale-up` (zoom-in al 95%). Duración: 200ms.
- **Gráficos:** Deben animarse al aparecer por primera vez. Ver `docs/GRAPH_STYLES_GUIDE.md`.

Esta guía debe proporcionar la base para una implementación fiel en Android. Para cualquier duda sobre animaciones específicas o microinteracciones, se puede consultar el prototipo web.
