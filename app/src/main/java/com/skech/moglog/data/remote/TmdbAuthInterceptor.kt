package com.skech.moglog.data.remote

import com.skech.moglog.data.security.SecureStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attaches the user's TMDB v4 Bearer token (`Authorization: Bearer ...`) or v3 API key (`?api_key=...`)
 * to outgoing TMDB API requests without embedding secrets in URLs across call sites.
 */
@Singleton
class TmdbAuthInterceptor @Inject constructor(
    private val secureStore: SecureStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val credential = secureStore.getTmdbCredential()

        val requestBuilder = originalRequest.newBuilder()
            .header("Accept", "application/json")

        if (credential.isNotBlank()) {
            if (secureStore.isBearerToken(credential)) {
                requestBuilder.header("Authorization", "Bearer $credential")
            } else {
                val urlWithKey = originalRequest.url.newBuilder()
                    .setQueryParameter("api_key", credential)
                    .build()
                requestBuilder.url(urlWithKey)
            }
        }

        return chain.proceed(requestBuilder.build())
    }
}
