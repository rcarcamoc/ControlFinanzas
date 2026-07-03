package com.aranthalion.controlfinanzas.data.remote.api

import android.util.Base64
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val prefs: ConfiguracionPreferences
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val email = prefs.syncEmail
        val password = prefs.syncPassword
        val credentials = Base64.encodeToString(
            "$email:$password".toByteArray(), Base64.NO_WRAP
        )
        val request = chain.request().newBuilder()
            .header("Authorization", "Basic $credentials")
            .build()
        return chain.proceed(request)
    }
}
