package com.aranthalion.controlfinanzas.di

import android.content.Context
import com.aranthalion.controlfinanzas.data.local.AppDatabase
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoManualDao
import com.aranthalion.controlfinanzas.data.repository.CategoriaRepository
import com.aranthalion.controlfinanzas.data.repository.CategoriaRepositoryImpl
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.MovimientoManualRepositoryImpl
import com.aranthalion.controlfinanzas.data.util.MovimientoManualMapper
import com.aranthalion.controlfinanzas.domain.categoria.CategoriaRepository as CategoriaRepositoryDomain
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
        fun provideConfiguracionPreferences(@ApplicationContext context: Context): ConfiguracionPreferences {
            return ConfiguracionPreferences(context)
        }
    }
} 