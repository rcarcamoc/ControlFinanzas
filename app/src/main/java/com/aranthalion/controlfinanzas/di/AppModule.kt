package com.aranthalion.controlfinanzas.di

import android.content.Context
import com.aranthalion.controlfinanzas.data.local.AppDatabase
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.ClasificacionAutomaticaDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoManualDao
import com.aranthalion.controlfinanzas.data.repository.CategoriaRepository
import com.aranthalion.controlfinanzas.data.repository.CategoriaRepositoryImpl
import com.aranthalion.controlfinanzas.data.repository.ClasificacionAutomaticaRepositoryImpl
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.MovimientoManualRepositoryImpl
import com.aranthalion.controlfinanzas.data.util.MovimientoManualMapper
import com.aranthalion.controlfinanzas.domain.categoria.CategoriaRepository as CategoriaRepositoryDomain
import com.aranthalion.controlfinanzas.domain.clasificacion.ClasificacionAutomaticaRepository
import com.aranthalion.controlfinanzas.domain.movimiento.MovimientoManualRepository
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
    ): CategoriaRepositoryDomain

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
        fun provideMovimientoManualMapper(): MovimientoManualMapper {
            return MovimientoManualMapper()
        }

        @Provides
        @Singleton
        fun provideCategoriaRepository(categoriaDao: CategoriaDao): CategoriaRepository {
            return CategoriaRepository(categoriaDao)
        }

        @Provides
        @Singleton
        fun provideMovimientoRepository(movimientoDao: MovimientoDao, categoriaDao: CategoriaDao): MovimientoRepository {
            return MovimientoRepository(movimientoDao, categoriaDao)
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
        fun provideConfiguracionPreferences(@ApplicationContext context: Context): ConfiguracionPreferences {
            return ConfiguracionPreferences(context)
        }
    }
} 