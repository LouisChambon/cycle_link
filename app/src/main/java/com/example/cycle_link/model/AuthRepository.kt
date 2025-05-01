package com.example.cycle_link.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AuthRepository {
    private val service: AuthApiService

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, trustAllCerts, SecureRandom())
        }
        val sslSocketFactory = sslContext.socketFactory

        val client = OkHttpClient.Builder()
            .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://back-cyclelink.vercel.app/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        service = retrofit.create(AuthApiService::class.java)
    }

    suspend fun login(email: String, password: String): AuthResponse? = withContext(Dispatchers.IO) {
        runCatching { service.login(LoginRequest(email, password)) }
            .getOrNull()
    }

    suspend fun signup(
        name: String, email: String, password: String,
        phone: String = "", address: String = ""
    ): AuthResponse? = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            service.signup(SignupRequest(name, email, password, phone, address))
        }
            .onFailure { t ->
                android.util.Log.e("AuthRepository", "signup failed", t)
            }
            .getOrNull()
    }
}
