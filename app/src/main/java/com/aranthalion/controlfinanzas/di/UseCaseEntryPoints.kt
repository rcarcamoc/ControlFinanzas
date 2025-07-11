package com.aranthalion.controlfinanzas.di

import com.aranthalion.controlfinanzas.domain.clasificacion.GestionarClasificacionAutomaticaUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ClasificacionUseCaseEntryPoint {
    fun gestionarClasificacionAutomaticaUseCase(): GestionarClasificacionAutomaticaUseCase
} 