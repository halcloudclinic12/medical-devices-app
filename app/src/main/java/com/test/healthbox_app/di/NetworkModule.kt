package com.test.healthbox_app.di

import android.content.Context
import android.util.Log
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.test.healthbox_app.BuildConfig
import com.test.healthbox_app.data.network.ApiService
import com.test.healthbox_app.data.repository.OnBoardingAPIRepositoryImpl
import com.test.healthbox_app.data.repository.PatientsAPIRepositoryImpl
import com.test.healthbox_app.domain.repository.OnboardingAPIRepository
import com.test.healthbox_app.domain.repository.PatientsAPIRepository
import com.test.healthbox_app.domain.use_cases.OnBoardingAPIUseCases
import com.test.healthbox_app.domain.use_cases.PatientsAPIUseCases
import com.test.healthbox_app.domain.use_cases.SharedPreferenceUseCases
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val ENABLE_LOGS = BuildConfig.ENABLE_LOGS

@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {

    const val DEFAULT_TIMEOUT_MIN = 2L
    const val HEADER_AUTHORIZATION = "Authorization"


    @Provides
    @Singleton
    fun provideConfiguredOkHttpClient(
        @ApplicationContext context: Context,
        sharedPreferenceUseCases: SharedPreferenceUseCases,
    ): OkHttpClient {

        val client = OkHttpClient.Builder().connectTimeout(DEFAULT_TIMEOUT_MIN, TimeUnit.MINUTES).readTimeout(DEFAULT_TIMEOUT_MIN, TimeUnit.MINUTES)
            .writeTimeout(DEFAULT_TIMEOUT_MIN, TimeUnit.MINUTES)


        client.addInterceptor { chain ->

            var token = ""
            sharedPreferenceUseCases.getToken()?.let {
                token = "Bearer ${it}"
            }

            Log.e("barerTokenLogs", " : token : $token")
            Log.e("EnableLogsLogsNetworkModule", " : ENABLE_LOGS : $ENABLE_LOGS")

            val request = chain.request().newBuilder().apply {
                addHeader(HEADER_AUTHORIZATION, token)
            }.build()

            chain.proceed(request)
        }

//        if (ENABLE_LOGS) {
            val loggingInterceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
            client.addInterceptor(loggingInterceptor)
//        }

//        if (ENABLE_LOGS) {
            val chuckerInterceptor = ChuckerInterceptor.Builder(context).collector(ChuckerCollector(context))
                // The max body content length in bytes, after this responses will be truncated.
                .maxContentLength(250000L)
                // Read the whole response body even when the client does not consume the response completely.
                .alwaysReadResponseBody(true)
                // List of headers to replace with ** in the Chucker UI
                .redactHeaders(emptySet()).build()
            client.addInterceptor(chuckerInterceptor)
//        }
        return client.build()
    }

    @Provides
    @Singleton
    fun providerRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder().baseUrl(BuildConfig.BASE_API).client(okHttpClient).addConverterFactory(GsonConverterFactory.create()).build()

    @Provides
    @Singleton
    fun providerApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideOnboardingRepositoryImpl(apiService: ApiService): OnboardingAPIRepository {
        return OnBoardingAPIRepositoryImpl(apiService = apiService)
    }

    @Provides
    @Singleton
    fun provideOnBoardingAPIUseCases(
        onboardingAPIRepository: OnboardingAPIRepository
    ): OnBoardingAPIUseCases {
        return OnBoardingAPIUseCases(onboardingAPIRepository = onboardingAPIRepository)
    }

    @Provides
    @Singleton
    fun providePatientsRepositoryImpl(apiService: ApiService): PatientsAPIRepository {
        return PatientsAPIRepositoryImpl(apiService = apiService)
    }

    @Provides
    @Singleton
    fun providePatientsAPIUseCases(
        onboardingAPIRepository: PatientsAPIRepository
    ): PatientsAPIUseCases {
        return PatientsAPIUseCases(patientsAPIRepository = onboardingAPIRepository)
    }
}