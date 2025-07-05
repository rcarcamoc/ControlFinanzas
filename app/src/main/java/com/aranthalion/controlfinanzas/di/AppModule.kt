package com.aranthalion.controlfinanzas.di

import android.content.Context
import com.aranthalion.controlfinanzas.data.local.AppDatabase
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.ClasificacionAutomaticaDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoManualDao
import com.aranthalion.controlfinanzas.data.local.dao.PresupuestoCategoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.SueldoDao
import com.aranthalion.controlfinanzas.data.repository.CategoriaRepositoryImpl
import com.aranthalion.controlfinanzas.data.repository.ClasificacionAutomaticaRepositoryImpl
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.MovimientoManualRepositoryImpl
import com.aranthalion.controlfinanzas.data.repository.PresupuestoCategoriaRepository
import com.aranthalion.controlfinanzas.data.repository.PresupuestoCategoriaRepositoryImpl
import com.aranthalion.controlfinanzas.data.repository.SueldoRepository
import com.aranthalion.controlfinanzas.data.repository.SueldoRepositoryImpl
import com.aranthalion.controlfinanzas.data.util.MovimientoManualMapper
import com.aranthalion.controlfinanzas.domain.categoria.CategoriaRepository
import com.aranthalion.controlfinanzas.domain.clasificacion.ClasificacionAutomaticaRepository
import com.aranthalion.controlfinanzas.domain.movimiento.MovimientoManualRepository
import com.aranthalion.controlfinanzas.domain.usecase.AnalisisFinancieroUseCase
import com.aranthalion.controlfinanzas.domain.usecase.AporteProporcionalUseCase
import com.aranthalion.controlfinanzas.domain.usecase.GestionarPresupuestosUseCase
import com.aranthalion.controlfinanzas.domain.usecase.GestionarMovimientosUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindCategoriaRepository(
        categoriaRepository: CategoriaRepositoryImpl
    ): CategoriaRepository

    @Binds
    @Singleton
    abstract fun bindMovimientoManualRepository(
        movimientoManualRepository: MovimientoManualRepositoryImpl
    ): MovimientoManualRepository

    @Binds
    @Singleton
    abstract fun bindClasificacionAutomaticaRepository(
        clasificacionRepository: ClasificacionAutomaticaRepositoryImpl
    ): ClasificacionAutomaticaRepository

    @Binds
    @Singleton
    abstract fun bindSueldoRepository(
        sueldoRepository: SueldoRepositoryImpl
    ): SueldoRepository

    companion object {
        @Provides
        @Singleton
        fun provideAppDatabase(
            @ApplicationContext context: Context
        ): AppDatabase {
            return AppDatabase.getDatabase(context)
        }

        @Provides
        @Singleton
        fun provideCategoriaDao(database: AppDatabase): CategoriaDao {
            return database.categoriaDao()
        }

        @Provides
        @Singleton
        fun provideMovimientoDao(database: AppDatabase): MovimientoDao {
            return database.movimientoDao()
        }

        @Provides
        @Singleton
        fun provideMovimientoManualDao(database: AppDatabase): MovimientoManualDao {
            return database.movimientoManualDao()
        }

        @Provides
        @Singleton
        fun provideClasificacionAutomaticaDao(database: AppDatabase): ClasificacionAutomaticaDao {
            return database.clasificacionAutomaticaDao()
        }

        @Provides
        @Singleton
        fun provideSueldoDao(database: AppDatabase): SueldoDao {
            return database.sueldoDao()
        }

        @Provides
        @Singleton
        fun provideMovimientoManualMapper(): MovimientoManualMapper {
            return MovimientoManualMapper()
        }

        @Provides
        @Singleton
        fun provideMovimientoRepository(
            movimientoDao: MovimientoDao, 
            categoriaDao: CategoriaDao,
            @ApplicationContext context: Context
        ): MovimientoRepository {
            return MovimientoRepository(movimientoDao, categoriaDao, context)
        }

        @Provides
        @Singleton
        fun provideMovimientoManualRepository(
            movimientoManualDao: MovimientoManualDao,
            mapper: MovimientoManualMapper
        ): MovimientoManualRepositoryImpl {
            return MovimientoManualRepositoryImpl(movimientoManualDao, mapper)
        }

        @Provides
        @Singleton
        fun provideClasificacionAutomaticaRepository(
            clasificacionDao: ClasificacionAutomaticaDao,
            categoriaDao: CategoriaDao,
            @ApplicationContext context: Context
        ): ClasificacionAutomaticaRepositoryImpl {
            return ClasificacionAutomaticaRepositoryImpl(clasificacionDao, categoriaDao, context)
        }

        @Provides
        @Singleton
        fun provideSueldoRepository(
            sueldoDao: SueldoDao
        ): SueldoRepositoryImpl {
            return SueldoRepositoryImpl(sueldoDao)
        }

        @Provides
        @Singleton
        fun provideAnalisisFinancieroUseCase(
            movimientoRepository: MovimientoRepository,
            presupuestoRepository: PresupuestoCategoriaRepository
        ): AnalisisFinancieroUseCase {
            return AnalisisFinancieroUseCase(movimientoRepository, presupuestoRepository)
        }

        @Provides
        @Singleton
        fun provideAporteProporcionalUseCase(
            sueldoRepository: SueldoRepository,
            movimientoRepository: MovimientoRepository
        ): AporteProporcionalUseCase {
            return AporteProporcionalUseCase(sueldoRepository, movimientoRepository)
        }

        @Provides
        @Singleton
        fun provideGestionarPresupuestosUseCase(
            presupuestoRepository: PresupuestoCategoriaRepository,
            categoriaRepository: CategoriaRepository,
            movimientoRepository: MovimientoRepository
        ): GestionarPresupuestosUseCase {
            return GestionarPresupuestosUseCase(presupuestoRepository, categoriaRepository, movimientoRepository)
        }

        @Provides
        @Singleton
        fun provideGestionarMovimientosUseCase(
            movimientoRepository: MovimientoRepository
        ): GestionarMovimientosUseCase {
            return GestionarMovimientosUseCase(movimientoRepository)
        }



        @Provides
        @Singleton
        fun provideConfiguracionPreferences(@ApplicationContext context: Context): ConfiguracionPreferences {
            return ConfiguracionPreferences(context)
        }

        @Provides
        fun providePresupuestoCategoriaRepository(
            dao: PresupuestoCategoriaDao
        ): PresupuestoCategoriaRepository =
            PresupuestoCategoriaRepositoryImpl(dao)

        @Provides
        @Singleton
        fun providePresupuestoCategoriaDao(database: AppDatabase): PresupuestoCategoriaDao {
            return database.presupuestoCategoriaDao()
        }
    }
} 