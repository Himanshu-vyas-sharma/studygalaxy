package app.studygalaxy.data.api

import android.content.Context
import app.studygalaxy.BuildConfig
import app.studygalaxy.data.local.TokenStore
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

object ApiModule {
    @Volatile private var impl: Api? = null

    fun get(ctx: Context): Api {
        impl?.let { return it }
        synchronized(this) {
            impl?.let { return it }
            val authInterceptor = Interceptor { chain ->
                val token = runBlocking { TokenStore.getJwt(ctx) }
                val req = chain.request().newBuilder().apply {
                    if (token != null) header("Authorization", "Bearer $token")
                }.build()
                chain.proceed(req)
            }
            val logging = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .build()
            val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
            val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE)
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
            val built = retrofit.create(Api::class.java)
            impl = built
            return built
        }
    }
}
