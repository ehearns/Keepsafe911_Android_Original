package com.keepSafe911.webservices

import android.content.Context
import com.google.gson.GsonBuilder
import com.keepSafe911.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

class WebApiClient constructor()//just an empty constructor for now
{
    private var webApi: WebApi? = null
    private var token = ""


    val webApi_without: WebApi?
        get() {

            System.setProperty("http.keepAlive", "false")

            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            val interceptor = Interceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(newRequest)
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(interceptor)
                .readTimeout(90000, TimeUnit.MILLISECONDS)
                .connectTimeout(90000, TimeUnit.MILLISECONDS)
                .build()

            client.connectTimeoutMillis

            val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            retrofit_new = retrofit

            webApi = retrofit.create(WebApi::class.java)

            return webApi

        }


    val webApi_without_new: WebApi?
        get() {

            System.setProperty("http.keepAlive", "false")

            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            val interceptor = Interceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader(
                        "Content-Type",
                        "application/json"
                    )
                    .build()
                chain.proceed(newRequest)
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(interceptor)
                .readTimeout(90000, TimeUnit.MILLISECONDS)
                .connectTimeout(90000, TimeUnit.MILLISECONDS)
                .build()

            client.connectTimeoutMillis

            val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            retrofit_new = retrofit

            webApi = retrofit.create(WebApi::class.java)

            return webApi

        }

    val webApi_yelp: WebApi?
        get() {

            System.setProperty("http.keepAlive", "false")

            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            val interceptor = Interceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader(
                        "Authorization",
                        "Bearer ${BuildConfig.yelpClientApiKey}"
                    )
                    .build()
                chain.proceed(newRequest)
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(interceptor)
                .readTimeout(90000, TimeUnit.MILLISECONDS)
                .connectTimeout(90000, TimeUnit.MILLISECONDS)
                .build()

            client.connectTimeoutMillis

            val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.YELP_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            retrofit_new = retrofit

            webApi = retrofit.create(WebApi::class.java)

            return webApi

        }


    val webApi_without_header: WebApi?
        get() {

            System.setProperty("http.keepAlive", "false")

            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            val interceptor = Interceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .build()
                chain.proceed(newRequest)
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(interceptor)
                .readTimeout(90000, TimeUnit.MILLISECONDS)
                .connectTimeout(90000, TimeUnit.MILLISECONDS)
                .build()

            client.connectTimeoutMillis

            val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            retrofit_new = retrofit

            webApi = retrofit.create(WebApi::class.java)

            return webApi

        }

    val webApi_with_MultiPart: WebApi?
        get() {

            try {
//                this.token = Methods.getLoginUser().getToken()

                val logging = HttpLoggingInterceptor()
                logging.level = HttpLoggingInterceptor.Level.BODY

                val httpClient = OkHttpClient.Builder()

                val interceptor = Interceptor { chain ->
                    val newRequest = chain.request().newBuilder()
                        .removeHeader("token")
                        .addHeader("Content-Type", "multipart/form-data")
                        .build()

                    chain.proceed(newRequest)
                }
                httpClient.addInterceptor(interceptor)
                httpClient.addInterceptor(logging)
                httpClient.readTimeout(90000, TimeUnit.MILLISECONDS)
                httpClient.connectTimeout(90000, TimeUnit.MILLISECONDS)

                val client = httpClient.build()
                val retrofit = Retrofit.Builder()
                    .baseUrl(BuildConfig.BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                retrofit_new = retrofit

                webApi = retrofit.create(WebApi::class.java)
                return webApi

            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

        }

    val webApi_with_MultiPart_Find_Child: WebApi?
        get() {

            try {
//                this.token = Methods.getLoginUser().getToken()

                val logging = HttpLoggingInterceptor()
                logging.level = HttpLoggingInterceptor.Level.BODY

                val httpClient = OkHttpClient.Builder()

                val interceptor = Interceptor { chain ->
                    val newRequest = chain.request().newBuilder()
                        .removeHeader("token")
                        .addHeader("Content-Type", "multipart/form-data")
                        .build()

                    chain.proceed(newRequest)
                }
                httpClient.addInterceptor(interceptor)
                httpClient.addInterceptor(logging)
                httpClient.readTimeout(90000, TimeUnit.MILLISECONDS)
                httpClient.connectTimeout(90000, TimeUnit.MILLISECONDS)

                val client = httpClient.build()
                val retrofit = Retrofit.Builder()
                    .baseUrl(BuildConfig.FIND_CHILD_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()

                retrofit_new = retrofit

                webApi = retrofit.create(WebApi::class.java)
                return webApi

            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

        }


    val webApi_payment: WebApi?
        get() {

            System.setProperty("http.keepAlive", "false")

            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            val interceptor = Interceptor { chain ->
                val newRequest = chain.request().newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(newRequest)
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(interceptor)
                .readTimeout(90000, TimeUnit.MILLISECONDS)
                .connectTimeout(90000, TimeUnit.MILLISECONDS)
                .build()

            client.connectTimeoutMillis

            val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.PAYMENT_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

            retrofit_new = retrofit

            webApi = retrofit.create(WebApi::class.java)

            return webApi

        }

    val webApi_HaveIBeenPwndService: WebApi?
        get() {

            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            val client = OkHttpClient.Builder().addInterceptor(logging).addInterceptor { chain ->
                val request = chain.request().newBuilder().addHeader("User-Agent", BuildConfig.DEFAULT_USER_AGENT).addHeader("hibp-api-key",
                    BuildConfig.HIBP_API_KEY
                ).build()
                chain.proceed(request)
            }.readTimeout(90000, TimeUnit.MILLISECONDS)
                .connectTimeout(90000, TimeUnit.MILLISECONDS)
                .build()
            val gson = GsonBuilder()
                .setLenient()
                .create()

            val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.HIBP_REST_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build()

            val hibpService = retrofit.create(WebApi::class.java)

            return hibpService
        }


    val webApi_PwnedPasswordsService: WebApi?
        get() {

            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            val client = OkHttpClient.Builder().addInterceptor(logging).addInterceptor { chain ->
                val request = chain.request().newBuilder().addHeader("User-Agent", BuildConfig.DEFAULT_USER_AGENT).addHeader("hibp-api-key",
                    BuildConfig.HIBP_API_KEY
                ).build()
                chain.proceed(request)
            }.readTimeout(90000, TimeUnit.MILLISECONDS)
                .connectTimeout(90000, TimeUnit.MILLISECONDS)
                .build()
            val gson = GsonBuilder()
                .setLenient()
                .create()

            val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.PPW_REST_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .client(client)
                .build()
            val ppwService = retrofit.create(WebApi::class.java)

            return ppwService
        }


    companion object {
        private var webApiClient: WebApiClient? = null
        private var mcontext: Context? = null
        var retrofit_new: Retrofit? = null

        fun getInstance(context: Context): WebApiClient {
            if (webApiClient == null)
                webApiClient = WebApiClient()
            mcontext = context
            return webApiClient as WebApiClient
        }
    }

}
