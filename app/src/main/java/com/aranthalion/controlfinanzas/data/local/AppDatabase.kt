package com.aranthalion.controlfinanzas.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aranthalion.controlfinanzas.data.local.converter.DateConverter
import com.aranthalion.controlfinanzas.data.local.dao.AuditoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.ClasificacionAutomaticaDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoManualDao
import com.aranthalion.controlfinanzas.data.local.dao.PresupuestoCategoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.SueldoDao
import com.aranthalion.controlfinanzas.data.local.dao.UsuarioDao
import com.aranthalion.controlfinanzas.data.local.dao.CuentaPorCobrarDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoEliminadoDao
import com.aranthalion.controlfinanzas.data.local.entity.AuditoriaEntity
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.data.local.entity.ClasificacionAutomaticaEntity
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEliminadoEntity
import com.aranthalion.controlfinanzas.data.local.entity.PresupuestoCategoriaEntity
import com.aranthalion.controlfinanzas.data.local.entity.SueldoEntity
import com.aranthalion.controlfinanzas.data.local.entity.UsuarioEntity
import com.aranthalion.controlfinanzas.data.local.entity.CuentaPorCobrarEntity
import com.aranthalion.controlfinanzas.data.movimiento.MovimientoManualEntity

@Database(
    entities = [
        MovimientoEntity::class,
        Categoria::class,
        MovimientoManualEntity::class,
        ClasificacionAutomaticaEntity::class,
        PresupuestoCategoriaEntity::class,
        SueldoEntity::class,
        UsuarioEntity::class,
        CuentaPorCobrarEntity::class,
        AuditoriaEntity::class,
        MovimientoEliminadoEntity::class
    ],
    version = 21,
    exportSchema = false
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movimientoDao(): MovimientoDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun movimientoManualDao(): MovimientoManualDao
    abstract fun clasificacionAutomaticaDao(): ClasificacionAutomaticaDao
    abstract fun presupuestoCategoriaDao(): PresupuestoCategoriaDao
    abstract fun sueldoDao(): SueldoDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun cuentaPorCobrarDao(): CuentaPorCobrarDao
    abstract fun auditoriaDao(): AuditoriaDao
    abstract fun movimientoEliminadoDao(): MovimientoEliminadoDao

    companion object {
        private const val TAG = "AppDatabase"
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migración de la versión 10 a la 11: agregar columna descripcionLimpia a movimientos
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(TAG, "Ejecutando migración 10->11: agregando descripcionLimpia")
                database.execSQL("ALTER TABLE movimientos ADD COLUMN descripcionLimpia TEXT NOT NULL DEFAULT ''")
                Log.d(TAG, "Migración 10->11 completada")
            }
        }
        
        // Migración de la versión 11 a la 12: agregar columna descripcionLimpia a movimientos_manuales
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(TAG, "Ejecutando migración 11->12: agregando descripcionLimpia a movimientos_manuales")
                database.execSQL("ALTER TABLE movimientos_manuales ADD COLUMN descripcionLimpia TEXT NOT NULL DEFAULT ''")
                Log.d(TAG, "Migración 11->12 completada")
            }
        }
        
        // Migración de la versión 12 a la 13: agregar campos de auditoría a movimientos
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(TAG, "Ejecutando migración 12->13: agregando campos de auditoría")
                database.execSQL("ALTER TABLE movimientos ADD COLUMN fechaCreacion INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                database.execSQL("ALTER TABLE movimientos ADD COLUMN fechaActualizacion INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                database.execSQL("ALTER TABLE movimientos ADD COLUMN metodoActualizacion TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE movimientos ADD COLUMN daoResponsable TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE movimientos ADD COLUMN usuarioResponsable TEXT NOT NULL DEFAULT 'SYSTEM'")
                Log.d(TAG, "Migración 12->13 completada")
            }
        }
        
        // Migración de la versión 13 a la 14: agregar tabla de auditoría
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(TAG, "Ejecutando migración 13->14: creando tabla auditoría")
                database.execSQL("""
                    CREATE TABLE auditoria (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        tabla TEXT NOT NULL,
                        operacion TEXT NOT NULL,
                        entidadId INTEGER,
                        detalles TEXT NOT NULL,
                        daoResponsable TEXT NOT NULL,
                        usuarioResponsable TEXT NOT NULL DEFAULT 'SYSTEM'
                    )
                """)
                Log.d(TAG, "Migración 13->14 completada")
            }
        }
        
        // Migración de la versión 14 a la 15: agregar campos normalizados para optimización (HITO 1.1)
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(TAG, "Ejecutando migración 14->15: agregando campos normalizados")
                
                // Agregar campos normalizados con valores por defecto correctos
                database.execSQL("ALTER TABLE movimientos ADD COLUMN descripcionNormalizada TEXT NOT NULL DEFAULT 'undefined'")
                database.execSQL("ALTER TABLE movimientos ADD COLUMN montoCategoria TEXT NOT NULL DEFAULT 'undefined'")
                database.execSQL("ALTER TABLE movimientos ADD COLUMN fechaMes TEXT NOT NULL DEFAULT 'undefined'")
                database.execSQL("ALTER TABLE movimientos ADD COLUMN fechaDiaSemana TEXT NOT NULL DEFAULT 'undefined'")
                database.execSQL("ALTER TABLE movimientos ADD COLUMN fechaDia INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE movimientos ADD COLUMN fechaAnio INTEGER NOT NULL DEFAULT 0")
                
                Log.d(TAG, "Campos normalizados agregados")
                
                // Crear índices optimizados para consultas frecuentes
                database.execSQL("CREATE INDEX idx_movimientos_descripcion_normalizada ON movimientos(descripcionNormalizada)")
                database.execSQL("CREATE INDEX idx_movimientos_monto_categoria ON movimientos(montoCategoria)")
                database.execSQL("CREATE INDEX idx_movimientos_fecha_mes ON movimientos(fechaMes)")
                database.execSQL("CREATE INDEX idx_movimientos_categoria_id ON movimientos(categoriaId)")
                database.execSQL("CREATE INDEX idx_movimientos_tipo_fecha ON movimientos(tipo, fecha)")
                database.execSQL("CREATE INDEX idx_movimientos_periodo_facturacion ON movimientos(periodoFacturacion)")
                
                // Índice compuesto para clasificación automática
                database.execSQL("CREATE INDEX idx_movimientos_clasificacion ON movimientos(categoriaId, descripcionNormalizada, montoCategoria)")
                
                Log.d(TAG, "Índices optimizados creados")
                Log.d(TAG, "Migración 14->15 completada")
            }
        }
        
        // Migración de la versión 15 a la 16: agregar campos de auditoría a movimientos_manuales
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(TAG, "Ejecutando migración 15->16: agregando campos de auditoría a movimientos_manuales")
                database.execSQL("ALTER TABLE movimientos_manuales ADD COLUMN fechaCreacion INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                database.execSQL("ALTER TABLE movimientos_manuales ADD COLUMN fechaActualizacion INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                database.execSQL("ALTER TABLE movimientos_manuales ADD COLUMN metodoActualizacion TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE movimientos_manuales ADD COLUMN daoResponsable TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE movimientos_manuales ADD COLUMN usuarioResponsable TEXT NOT NULL DEFAULT 'SYSTEM'")
                Log.d(TAG, "Migración 15->16 completada")
            }
        }
        
        // Migración de la versión 16 a la 17: agregar índice único a clasificacion_automatica
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(TAG, "Ejecutando migración 16->17: agregando índice único a clasificacion_automatica")
                
                // Primero, eliminar duplicados existentes en clasificacion_automatica
                Log.d(TAG, "Limpiando duplicados existentes en clasificacion_automatica...")
                database.execSQL("""
                    DELETE FROM clasificacion_automatica 
                    WHERE id NOT IN (
                        SELECT MIN(id) 
                        FROM clasificacion_automatica 
                        GROUP BY patron, categoriaId
                    )
                """)
                
                // Crear índice único en (patron, categoriaId)
                database.execSQL("CREATE UNIQUE INDEX idx_clasificacion_automatica_patron_categoria ON clasificacion_automatica(patron, categoriaId)")
                
                Log.d(TAG, "Índice único creado en clasificacion_automatica")
                Log.d(TAG, "Migración 16->17 completada")
            }
        }
        
        // Migración de la versión 17 a la 18: limpiar duplicados y agregar índice único a presupuesto_categoria
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(TAG, "Ejecutando migración 17->18: limpiando duplicados y agregando índice único a presupuesto_categoria")
                
                // Primero, eliminar duplicados existentes en presupuesto_categoria
                Log.d(TAG, "Limpiando duplicados existentes en presupuesto_categoria...")
                database.execSQL("""
                    DELETE FROM presupuesto_categoria 
                    WHERE id NOT IN (
                        SELECT MAX(id) 
                        FROM presupuesto_categoria 
                        GROUP BY categoriaId, periodo
                    )
                """)
                
                // Crear índice único en (categoriaId, periodo)
                database.execSQL("CREATE UNIQUE INDEX index_presupuesto_categoria_categoriaId_periodo ON presupuesto_categoria(categoriaId, periodo)")
                
                Log.d(TAG, "Índice único creado en presupuesto_categoria")
                Log.d(TAG, "Migración 17->18 completada")
            }
        }

        // Migración de la versión 18 a la 19: agregar scope y userId_internal a movimientos, e idServidor a usuarios
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(TAG, "Ejecutando migración 18->19: agregando scope y userId_internal a movimientos, e idServidor a usuarios")
                database.execSQL("ALTER TABLE movimientos ADD COLUMN scope TEXT NOT NULL DEFAULT 'HOUSEHOLD'")
                database.execSQL("ALTER TABLE movimientos ADD COLUMN userId_internal TEXT")
                database.execSQL("ALTER TABLE usuarios ADD COLUMN idServidor TEXT")
                Log.d(TAG, "Migración 18->19 completada")
            }
        }

        // Migración de la versión 19 a la 20: agregar scope a presupuesto_categoria y actualizar índice
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(TAG, "Ejecutando migración 19->20: agregando scope a presupuesto_categoria")
                database.execSQL("ALTER TABLE presupuesto_categoria ADD COLUMN scope TEXT NOT NULL DEFAULT 'HOUSEHOLD'")
                database.execSQL("DROP INDEX IF EXISTS index_presupuesto_categoria_categoriaId_periodo")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_presupuesto_categoria_categoriaId_periodo_scope ON presupuesto_categoria(categoriaId, periodo, scope)")
                Log.d(TAG, "Migración 19->20 completada")
            }
        }

        // Migración de la versión 20 a la 21: agregar tabla movimientos_eliminados para sincronización
        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(database: SupportSQLiteDatabase) {
                Log.d(TAG, "Ejecutando migración 20->21: creando tabla movimientos_eliminados")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS movimientos_eliminados (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        idUnico TEXT NOT NULL,
                        deletedAt INTEGER NOT NULL,
                        syncPending INTEGER NOT NULL DEFAULT 1
                    )
                """)
                Log.d(TAG, "Migración 20->21 completada")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.d(TAG, "Inicializando base de datos...")
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "control_finanzas_db"
                )
                .addMigrations(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21)
                .fallbackToDestructiveMigration() // Permite recrear la BD si la migración falla
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        Log.d(TAG, "Base de datos creada exitosamente")
                    }
                    
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        Log.d(TAG, "Base de datos abierta exitosamente")
                    }
                })
                .build()
                INSTANCE = instance
                Log.d(TAG, "Instancia de base de datos creada")
                instance
            }
        }
    }
} 