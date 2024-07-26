package io.github.jeadyx.simplenetmanager

import android.os.Looper
import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

private val TAG = "[ServerManager]"
data class ServerData(
    @JvmField var id: java.lang.Double = java.lang.Double(-1.0),
    @JvmField var name: String="",
    @JvmField var gender: String="",
    @JvmField var phone: String=""
){
    constructor(src: Map<*, *>) : this() {
        this.javaClass.fields.forEach { field->
            if(!field.name.startsWith("$")){
                if(src[field.name]?.javaClass == field.type){
                    field.set(this, src[field.name])
                }
            }
        }
    }
}
/** 基于OKHttp的接口封装
 * @sample SimpleNetManager.getInstance("https://example.com")
 */
class SimpleNetManager(var baseUrl: String, var timeout: Long=10) {
    private val TAG = "[ServerManager] "
    private var client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(timeout, TimeUnit.SECONDS)
        .writeTimeout(timeout, TimeUnit.SECONDS)
        .callTimeout(timeout, TimeUnit.SECONDS)
        .build()

    companion object {
        private lateinit var instance: SimpleNetManager
        private fun initInstance(url: String, timeout: Long){
            if(!Companion::instance.isInitialized){
                instance = SimpleNetManager(url, timeout)
            }
            instance.baseUrl = url
            instance.timeout = timeout
        }

        /**
         * Get instance of ServerManager
         * @param baseUrl ex: http://localhost:3000
         * @return serverManager
         */
        fun getInstance(baseUrl: String, timeout: Long=10): SimpleNetManager {
            initInstance(baseUrl.trimEnd('/'), timeout)
            return instance
        }
    }

    /**
     * 发起GET请求并返回 T 类型
     */
    fun <T> get(path: String="", query: String="", classType: Class<T>?=null, onResult: (T?, String?)->Unit){
        val targetUrl = baseUrl.let {
            if(path.trim('/').isNotEmpty()){
                return@let "$it/$path"
            }
            it
        }.let {
            if(query.trim('?').isNotEmpty()){
                return@let "$it?$query"
            }
            it
        }
        Log.w(TAG, "get: get target url $targetUrl")
        val request = Request.Builder()
            .url(targetUrl)
            .build()
        thread {
            Looper.prepare()
            val response: Response
            try {
                response = client.newCall(request).execute()
            }catch (e: Exception){
                onResult(null, e.message)
                return@thread
            }
            try{
                val result = handleResult(response.body?.string(), classType)
                onResult(result, response.message)
            }catch (e: Exception){
                Log.e(TAG, "get: catch a exception on handle result $e")
                onResult(null, e.localizedMessage)
            }
        }
    }
    /**
     * 发起GET请求并下载其内容
     * @query 请求参数 ex: name=xx&age=xx
     */
    fun download(path: String="", query: String="", downloadPath: String, onResult: (state: String?)->Unit){
        val targetUrl = baseUrl.let {
            if(path.trim('/').isNotEmpty()){
                return@let "$it/$path"
            }
            it
        }.let {
            if(query.trim('?').isNotEmpty()){
                return@let "$it?$query"
            }
            it
        }
        Log.w(TAG, "download: get target url $targetUrl")
        val request = Request.Builder()
            .url(targetUrl)
            .build()
        thread {
            Looper.prepare()
            val response: Response
            try {
                response = client.newCall(request).execute()
                response.body?.let {body->
                    body.byteStream().use {inStream->
                        val fos = FileOutputStream(downloadPath)
                        fos.use {
                            val buffer = ByteArray(1024)
                            val total = body.contentLength()
                            var len = 0
                            while (true){
                                len = inStream.read(buffer)
                                if(len==-1){
                                    break
                                }
                                fos.write(buffer, 0, len)
                                onResult("$len/$total")
                            }
                        }
                    }
                }
            }catch (e: Exception){
                onResult(e.localizedMessage)
                return@thread
            }
        }
    }

    /**
     * 发起POST请求并返回 T 类型
     * @param body 请求体 ex: "{\"name\": \"test\"}"
     */
    fun <T> post(path: String="", body: String="", classType: Class<T>?=null, onResult: (T?, String?)->Unit){
        val targetUrl = baseUrl.let {
            val pathCat = path.trim('/')
            if(pathCat.isNotEmpty()){
                return@let "$it/$pathCat"
            }
            it
        }
        Log.w(TAG, "post: post target url $targetUrl")
        val requestBody = body.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url(targetUrl)
                .post(requestBody)
                .build()
        thread {
            Looper.prepare()
            val response: Response
            try {
                response = client.newCall(request).execute()
            }catch (e: Exception){
                Log.e(TAG, "post: cat exception $e", )
                onResult(null, e.message)
                return@thread
            }
            val bodyStr = response.body?.string()
            val result = handleResult(bodyStr, classType)
//            Log.i(TAG, "post: response ${response.code} ${response.isSuccessful}, ${response.message}", )
            if(response.isSuccessful){
                onResult(result, null)
            }else{
                onResult(result, response.message)
            }
        }
    }

    private inline fun <V>handleResult(body: String?, type: Class<V>?): V? {
//        Log.w(TAG, "handleResult: result $body", )
        if(type==null){
            return null
        }else if(type == String::class.java){
            return body as? V
        }
        return body?.let {content->
            try {
                val json = Gson().fromJson(content, type)
//                Log.d(TAG, "handleResult: after parse result $json")
                return@let json
            }catch (e: Exception){
                Log.e(TAG, "handleResult: cat err $e", )
                null
            }
        }
    }
}