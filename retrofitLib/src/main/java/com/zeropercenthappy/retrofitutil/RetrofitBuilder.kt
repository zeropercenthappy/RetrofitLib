package com.zeropercenthappy.retrofitutil

import android.content.Context
import android.text.TextUtils
import me.jessyan.progressmanager.ProgressManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.CallAdapter
import retrofit2.Converter
import retrofit2.Retrofit
import java.util.*
import java.util.concurrent.TimeUnit

class RetrofitBuilder {
    private var baseUrl: String = ""
    private var handleCookie: Boolean = true
    private var extraParamMap = TreeMap<String, String>()
    private val extraHeaderMap = TreeMap<String, String>()
    private val extraInterceptorList = arrayListOf<Interceptor>()
    private var connectTimeoutMs: Long = 10_000
    private var readTimeoutMs: Long = 10_000
    private var writeTimeoutMs: Long = 10_000
    private val okHttpClientBuilder = ProgressManager.getInstance().with(OkHttpClient.Builder())
    private val converterFactoryList = arrayListOf<Converter.Factory>()
    private val callAdapterFactoryList = arrayListOf<CallAdapter.Factory>()

    fun baseUrl(baseUrl: String): RetrofitBuilder {
        this.baseUrl = baseUrl
        return this
    }

    fun handleCookie(handleCookie: Boolean): RetrofitBuilder {
        this.handleCookie = handleCookie
        return this
    }

    fun addParam(key: String, value: String): RetrofitBuilder {
        extraParamMap[key] = value
        return this
    }

    fun addParams(params: Map<String, String>): RetrofitBuilder {
        for (key in params.keys) {
            val value = params[key]
            if (value != null && value != "") {
                extraParamMap[key] = value
            }
        }
        return this
    }

    fun addHeader(key: String, value: String): RetrofitBuilder {
        extraHeaderMap[key] = value
        return this
    }

    fun addHeaders(headers: Map<String, String>): RetrofitBuilder {
        for (key in headers.keys) {
            val value = headers[key]
            if (value != null && value != "") {
                extraHeaderMap[key] = value
            }
        }
        return this
    }

    fun addInterceptor(interceptor: Interceptor): RetrofitBuilder {
        extraInterceptorList.add(interceptor)
        return this
    }

    fun addInterceptors(interceptorList: List<Interceptor>): RetrofitBuilder {
        extraInterceptorList.addAll(interceptorList)
        return this
    }

    fun connectTimeout(ms: Long): RetrofitBuilder {
        connectTimeoutMs = ms
        return this
    }

    fun readTimeout(ms: Long): RetrofitBuilder {
        readTimeoutMs = ms
        return this
    }

    fun writeTimeout(ms: Long): RetrofitBuilder {
        writeTimeoutMs = ms
        return this
    }

    fun okHttpClientBuilderOption(option: (OkHttpClient.Builder) -> Unit): RetrofitBuilder {
        option(okHttpClientBuilder)
        return this
    }

    fun addConverterFactory(factory: Converter.Factory): RetrofitBuilder {
        converterFactoryList.add(factory)
        return this
    }

    fun addCallAdapterFactory(factory: CallAdapter.Factory): RetrofitBuilder {
        callAdapterFactoryList.add(factory)
        return this
    }

    fun build(context: Context): Retrofit {
        // 检测baseUrl
        if (TextUtils.isEmpty(baseUrl)) {
            throw Exception("base url can not be empty")
        } else if (!baseUrl.endsWith("/")) {
            throw Exception("base url must end with /")
        }
        // 默认Interceptor
        val defaultInterceptor = DefaultInterceptor(
            context.applicationContext,
            handleCookie,
            extraParamMap,
            extraHeaderMap
        )
        extraInterceptorList.add(0, defaultInterceptor)
        // 构造OkHttpClient
        val okHttpClient = okHttpClientBuilder.apply {
            // 拦截器
            for (interceptor in extraInterceptorList) {
                addInterceptor(interceptor)
            }
            // 超时
            connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
            readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            writeTimeout(writeTimeoutMs, TimeUnit.MILLISECONDS)
            // Cookie管理
            if (handleCookie) {
                cookieJar(CookieJarImpl(context))
            }
        }.build()
        // 构造Retrofit
        val builder = Retrofit.Builder()
        builder.baseUrl(baseUrl)
        builder.client(okHttpClient)
        for (factory in converterFactoryList) {
            builder.addConverterFactory(factory)
        }
        for (factory in callAdapterFactoryList) {
            builder.addCallAdapterFactory(factory)
        }
        return builder.build()
    }
}