package com.example.cycle_link.model

import android.util.Log
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

fun getUnsafeOkHttpClient(): OkHttpClient {
    val trustAll = arrayOf<TrustManager>(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    )
    val sslContext = SSLContext.getInstance("SSL").apply {
        init(null, trustAll, SecureRandom())
    }
    return OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustAll[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()
}

class UserRepository(private val token: String) {
    private val service: UserApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://back-cyclelink.vercel.app/api/")
            .client(getUnsafeOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(UserApiService::class.java)
    }


    suspend fun getProfile(): UserDto? =
        runCatching { service.getProfile("Bearer $token") }
            .onFailure { Log.e("UserRepo", "Erreur fetchProfile", it) }
            .getOrNull()

}
