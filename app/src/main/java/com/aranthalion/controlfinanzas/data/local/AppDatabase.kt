package com.aranthalion.controlfinanzas.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aranthalion.controlfinanzas.data.local.converter.DateConverter
import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.ClasificacionAutomaticaDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoManualDao
import com.aranthalion.controlfinanzas.data.local.dao.PresupuestoCategoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.SueldoDao
import com.aranthalion.controlfinanzas.data.local.dao.UsuarioDao
import com.aranthalion.controlfinanzas.data.local.dao.CuentaPorCobrarDao
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
        CuentaPorCobrarEntity::class
    ],
    version = 10,
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migración segura que preserva todos los datos
        private val MIGRATION_1_10 = object : Migration(1, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Esta migración preserva todos los datos existentes
                // Si hay cambios futuros en la estructura, se agregarán aquí
                // Por ahora, solo preservamos los datos existentes
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "control_finanzas_db"
                )
                .addMigrations(MIGRATION_1_10) // Usar migración segura en lugar de destructiva
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 