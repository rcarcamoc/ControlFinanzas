package com.aranthalion.controlfinanzas.data.util

import kotlinx.coroutines.flow.*

inline fun <ResultType, RequestType> networkBoundResource(
    crossinline query: () -> Flow<ResultType>,
    crossinline fetch: suspend () -> RequestType,
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    crossinline shouldFetch: (ResultType) -> Boolean = { true },
    crossinline onFetchFailed: (Throwable) -> Unit = {}
): Flow<Resource<ResultType>> = flow {
    val data = query().first()
    emit(Resource.Loading(data))
    
    if (shouldFetch(data)) {
        try {
            val fetchedData = fetch()
            saveFetchResult(fetchedData)
            query().collect { emit(Resource.Success(it)) }
        } catch (t: Throwable) {
            onFetchFailed(t)
            query().collect { emit(Resource.Error(t, it)) }
        }
    } else {
        query().collect { emit(Resource.Success(it)) }
    }
}
