package com.aranthalion.controlfinanzas.di

import android.content.Context
import com.aranthalion.controlfinanzas.data.local.AppDatabase
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.data.local.dao.AuditoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.CategoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.ClasificacionAutomaticaDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoDao
import com.aranthalion.controlfinanzas.data.local.dao.MovimientoManualDao
import com.aranthalion.controlfinanzas.data.local.dao.PresupuestoCategoriaDao
import com.aranthalion.controlfinanzas.data.local.dao.SueldoDao
import com.aranthalion.controlfinanzas.data.local.dao.UsuarioDao
import com.aranthalion.controlfinanzas.data.local.dao.CuentaPorCobrarDao
import com.aranthalion.controlfinanzas.data.repository.AuditoriaService
import com.aranthalion.controlfinanzas.data.repository.CategoriaRepositoryImpl
import com.aranthalion.controlfinanzas.data.repository.ClasificacionAutomaticaRepositoryImpl
import com.aranthalion.controlfinanzas.data.repository.MovimientoRepository
import com.aranthalion.controlfinanzas.data.repository.MovimientoManualRepositoryImpl
import com.aranthalion.controlfinanzas.data.repository.PresupuestoCategoriaRepository
import com.aranthalion.controlfinanzas.data.repository.PresupuestoCategoriaRepositoryImpl
import com.aranthalion.controlfinanzas.data.repository.SueldoRepository
import com.aranthalion.controlfinanzas.data.repository.SueldoRepositoryImpl
import com.aranthalion.controlfinanzas.data.repository.UsuarioRepositoryImpl
import com.aranthalion.controlfinanzas.data.repository.CuentaPorCobrarRepositoryImpl
import com.aranthalion.controlfinanzas.data.repository.TinderClasificacionService
import com.aranthalion.controlfinanzas.data.repository.NormalizacionService
import com.aranthalion.controlfinanzas.data.repository.MigracionInicialService
import com.aranthalion.controlfinanzas.data.repository.CacheService
import com.aranthalion.controlfinanzas.data.util.MovimientoManualMapper
import com.aranthalion.controlfinanzas.domain.categoria.CategoriaRepository
import com.aranthalion.controlfinanzas.domain.clasificacion.ClasificacionAutomaticaRepository
import com.aranthalion.controlfinanzas.domain.movimiento.MovimientoManualRepository
import com.aranthalion.controlfinanzas.domain.usuario.UsuarioRepository
import com.aranthalion.controlfinanzas.domain.cuenta.CuentaPorCobrarRepository
import com.aranthalion.controlfinanzas.domain.usecase.AnalisisFinancieroUseCase
import com.aranthalion.controlfinanzas.domain.usecase.AnalisisGastoPorCategoriaUseCase
import com.aranthalion.controlfinanzas.domain.usecase.AporteProporcionalUseCase
import com.aranthalion.controlfinanzas.domain.usecase.GestionarPresupuestosUseCase
import com.aranthalion.controlfinanzas.domain.usecase.GestionarMovimientosUseCase
import com.aranthalion.controlfinanzas.domain.usecase.GestionarUsuariosUseCase
import com.aranthalion.controlfinanzas.domain.usecase.GestionarCuentasPorCobrarUseCase
import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import com.aranthalion.controlfinanzas.domain.movimiento.GestionarMovimientosManualesUseCase
import com.aranthalion.controlfinanzas.domain.categoria.GestionarCategoriasUseCase
import com.aranthalion.controlfinanzas.domain.usecase.InsightsAvanzadosUseCase
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

    @Binds
    @Singleton
    abstract fun bindUsuarioRepository(
        usuarioRepository: UsuarioRepositoryImpl
    ): UsuarioRepository

    @Binds
    @Singleton
    abstract fun bindCuentaPorCobrarRepository(
        cuentaPorCobrarRepository: CuentaPorCobrarRepositoryImpl
    ): CuentaPorCobrarRepository

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
        fun provideAuditoriaDao(database: AppDatabase): AuditoriaDao {
            return database.auditoriaDao()
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
        fun provideUsuarioDao(database: AppDatabase): UsuarioDao {
            return database.usuarioDao()
        }

        @Provides
        @Singleton
        fun provideCuentaPorCobrarDao(database: AppDatabase): CuentaPorCobrarDao {
            return database.cuentaPorCobrarDao()
        }

        @Provides
        @Singleton
        fun provideMovimientoManualMapper(): MovimientoManualMapper {
            return MovimientoManualMapper()
        }

        @Provides
        @Singleton
        fun provideAuditoriaService(
            auditoriaDao: AuditoriaDao
        ): AuditoriaService {
            return AuditoriaService(auditoriaDao)
        }

        @Provides
        @Singleton
        fun provideMovimientoRepository(
            movimientoDao: MovimientoDao, 
            categoriaDao: CategoriaDao,
            @ApplicationContext context: Context,
            auditoriaService: AuditoriaService,
            normalizacionService: NormalizacionService,
            cacheService: CacheService
        ): MovimientoRepository {
            return MovimientoRepository(movimientoDao, categoriaDao, context, auditoriaService, normalizacionService, cacheService)
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
            movimientoDao: MovimientoDao,
            @ApplicationContext context: Context
        ): ClasificacionAutomaticaRepositoryImpl {
            return ClasificacionAutomaticaRepositoryImpl(clasificacionDao, categoriaDao, movimientoDao, context)
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
        fun provideUsuarioRepository(
            usuarioDao: UsuarioDao
        ): UsuarioRepositoryImpl {
            return UsuarioRepositoryImpl(usuarioDao)
        }

        @Provides
        @Singleton
        fun provideCuentaPorCobrarRepository(
            cuentaPorCobrarDao: CuentaPorCobrarDao,
            usuarioDao: UsuarioDao
        ): CuentaPorCobrarRepositoryImpl {
            return CuentaPorCobrarRepositoryImpl(cuentaPorCobrarDao, usuarioDao)
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
        fun provideGestionarUsuariosUseCase(
            usuarioRepository: UsuarioRepository
        ): GestionarUsuariosUseCase {
            return GestionarUsuariosUseCase(usuarioRepository)
        }

        @Provides
        @Singleton
        fun provideGestionarCuentasPorCobrarUseCase(
            cuentaPorCobrarRepository: CuentaPorCobrarRepository,
            usuarioRepository: UsuarioRepository
        ): GestionarCuentasPorCobrarUseCase {
            return GestionarCuentasPorCobrarUseCase(cuentaPorCobrarRepository, usuarioRepository)
        }

        @Provides
        @Singleton
        fun provideInsightsAvanzadosUseCase(
            movimientoRepository: MovimientoRepository,
            presupuestoRepository: PresupuestoCategoriaRepository
        ): InsightsAvanzadosUseCase {
            return InsightsAvanzadosUseCase(movimientoRepository, presupuestoRepository)
        }

        @Provides
        @Singleton
        fun provideAnalisisGastoPorCategoriaUseCase(
            movimientoRepository: MovimientoRepository,
            presupuestoRepository: PresupuestoCategoriaRepository,
            categoriaRepository: CategoriaRepository
        ): AnalisisGastoPorCategoriaUseCase {
            return AnalisisGastoPorCategoriaUseCase(movimientoRepository, presupuestoRepository, categoriaRepository)
        }

        @Provides
        @Singleton
        fun provideConfiguracionPreferences(@ApplicationContext context: Context): ConfiguracionPreferences {
            return ConfiguracionPreferences(context)
        }

        @Provides
        fun providePresupuestoCategoriaRepository(
            dao: PresupuestoCategoriaDao,
            auditoriaService: AuditoriaService
        ): PresupuestoCategoriaRepository =
            PresupuestoCategoriaRepositoryImpl(dao, auditoriaService)

        @Provides
        @Singleton
        fun providePresupuestoCategoriaDao(database: AppDatabase): PresupuestoCategoriaDao {
            return database.presupuestoCategoriaDao()
        }

        @Provides
        @Singleton
        fun provideGestionarClasificacionAutomaticaUseCase(
            clasificacionRepository: ClasificacionAutomaticaRepository
        ): GestionarClasificacionAutomaticaUseCase {
            return GestionarClasificacionAutomaticaUseCase(clasificacionRepository)
        }

        @Provides
        @Singleton
        fun provideGestionarMovimientosManualesUseCase(
            movimientoManualRepository: MovimientoManualRepository
        ): GestionarMovimientosManualesUseCase {
            return GestionarMovimientosManualesUseCase(movimientoManualRepository)
        }

        @Provides
        @Singleton
        fun provideGestionarCategoriasUseCase(
            categoriaRepository: CategoriaRepository
        ): GestionarCategoriasUseCase {
            return GestionarCategoriasUseCase(categoriaRepository)
        }

        @Provides
        @Singleton
        fun provideCategoriaRepositoryImpl(
            categoriaDao: CategoriaDao,
            auditoriaService: AuditoriaService
        ): CategoriaRepositoryImpl {
            return CategoriaRepositoryImpl(categoriaDao, auditoriaService)
        }

        @Provides
        @Singleton
        fun provideTinderClasificacionService(
            movimientoDao: MovimientoDao,
            clasificacionUseCase: GestionarClasificacionAutomaticaUseCase
        ): TinderClasificacionService {
            return TinderClasificacionService(movimientoDao, clasificacionUseCase)
        }
        
        @Provides
        @Singleton
        fun provideNormalizacionService(
            movimientoDao: MovimientoDao
        ): NormalizacionService {
            return NormalizacionService(movimientoDao)
        }
        
        @Provides
        @Singleton
        fun provideMigracionInicialService(
            normalizacionService: NormalizacionService
        ): MigracionInicialService {
            return MigracionInicialService(normalizacionService)
        }
        
        @Provides
        @Singleton
        fun provideCacheService(): CacheService {
            return CacheService()
        }
    }
} 