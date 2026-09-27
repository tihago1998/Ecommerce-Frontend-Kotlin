package com.ecommerce.ecommerceapp.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // Celular físico: IP del PC en la red Wi-Fi (ipconfig). El celular y el PC deben estar en la misma red.
    //private const val BASE_URL = "http://192.168.0.100:5050/"
    // Emulador: 10.0.2.2 es el "localhost" del PC visto desde el emulador
    private const val BASE_URL = "http://10.0.2.2:5050/"
    // Backend desplegado
    //private const val BASE_URL = "https://ecommerce-api-python.onrender.com/"



    private val debugInterceptor = Interceptor { chain ->
        val request = chain.request()
        Log.d("API_DEBUG", "URL: ${request.url}")
        Log.d("API_DEBUG", "Method: ${request.method}")
        Log.d("API_DEBUG", "Headers: ${request.headers}")

        val response = chain.proceed(request)

        Log.d("API_DEBUG", "Response Code: ${response.code}")
        Log.d("API_DEBUG", "Response Message: ${response.message}")

        response
    }

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("HTTP_LOG", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(debugInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}