package com.aranthalion.controlfinanzas.data.local

import android.content.Context
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
import com.aranthalion.controlfinanzas.data.local.entity.AuditoriaEntity
import com.aranthalion.controlfinanzas.data.local.entity.Categoria
import com.aranthalion.controlfinanzas.data.local.entity.ClasificacionAutomaticaEntity
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
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
        AuditoriaEntity::class
    ],
    version = 14,
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migración de la versión 10 a la 11: agregar columna descripcionLimpia a movimientos
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE movimientos ADD COLUMN descripcionLimpia TEXT NOT NULL DEFAULT ''")
            }
        }
        // Migración de la versión 11 a la 12: agregar columna descripcionLimpia a movimientos_manuales
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE movimientos_manuales ADD COLUMN descripcionLimpia TEXT NOT NULL DEFAULT ''")
            }
        }
        
        // Migración de la versión 12 a la 13: agregar campos de auditoría a movimientos
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE movimientos ADD COLUMN fechaCreacion INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                database.execSQL("ALTER TABLE movimientos ADD COLUMN fechaActualizacion INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                database.execSQL("ALTER TABLE movimientos ADD COLUMN metodoActualizacion TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE movimientos ADD COLUMN daoResponsable TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE movimientos ADD COLUMN usuarioResponsable TEXT NOT NULL DEFAULT 'SYSTEM'")
            }
        }
        
        // Migración de la versión 13 a la 14: agregar tabla de auditoría
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
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
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "control_finanzas_db"
                )
                .addMigrations(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 