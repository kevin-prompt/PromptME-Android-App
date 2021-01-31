package com.coolftc.prompt.utility

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonWriter
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import com.coolftc.prompt.utility.Constants.*

/**
 *  The WebServices Class holds methods that can be used to communicate with the
 *  JSON based REST Web Service APIs.
 */
class WebServices(private val Parser: Gson, private val Timeout: Int = API_TIMEOUT) : IWebServices {
    /*
     * Class specific to the error and return information from the API. If API
     * returns detailed error data, customize this class to support parsing it.
     */
    data class ErrorResponse(val message: String?)
    data class Nothing(val data: NoData?)
    data class NoData(val filler: String?)

    private fun checkErrResponse(message: String?): ErrorResponse =
        if (message.isNullOrBlank()) {
            ErrorResponse(ExpClass.exceptionName(ExpClass.HTTP_STATUS))
        } else {
            try {
                Parser.fromJson(message, ErrorResponse::class.java)
            } catch (ex: Exception) {
                ErrorResponse(message)
            }
        }

    private fun getPostDataString(params: HashMap<String, String>): String {
        val result = StringBuilder()
        var first = true
        for ((key, value) in params) {
            if (first) first = false else result.append("&")
            result.append(URLEncoder.encode(key, API_ENCODING))
            result.append("=")
            result.append(URLEncoder.encode(value, API_ENCODING))
        }
        return result.toString()
    }


    /*
        Retrieve a resource.  Since this is a GET, there is no request data other than the
        supplied path parameter.  Expectation is that dynamic information and parameters
        have already been included (in the path) by the time this is called.  For authentication,
        the token is provided separately and included in the Authorization header.  Since this
        is a GET, it requires that something be returned (use POST for side effect APIs).

        In the case that something goes wrong, when the Web Service call returns other than
        a success(2xx) code, the error response stream is parsed and an instance of the
        ExpClass is thrown. This will contain the HTTP status code and/or the actual exception.
    */
    override fun <U> callGetApi(path: String, typeU: Class<U>, token: String): U? {
        val methodName = this.javaClass.name + ".CallGetApi-" + path
        val bearer: String = if (token.isBlank()) token else API_BEARER + token
        var webC: HttpURLConnection? = null
        return try {
            // Set up the connection
            val web = URL(path)
            webC = web.openConnection() as HttpURLConnection
            webC.requestMethod = "GET" // Available: POST, PUT, DELETE, OPTIONS, HEAD and TRACE
            webC.setRequestProperty("Accept", API_HEADER_ACCEPT)
            if (bearer.isNotEmpty()) webC.setRequestProperty("Authorization", bearer)
            webC.useCaches = false
            webC.allowUserInteraction = false
            webC.connectTimeout = Timeout
            webC.readTimeout = Timeout
            webC.connect()
            // Process the response
            val status = webC.responseCode
            if (status in 200..299) {
                val br = BufferedReader(InputStreamReader(webC.inputStream, API_ENCODING))
                val sb = StringBuilder()
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    sb.append(line).append("\n")
                }
                br.close()
                try {
                    Parser.fromJson(sb.toString(), typeU)
                } catch (ex: Exception) {
                    throw ExpClass(ExpClass.PARSE_EXP, methodName, ex.toString(), ex)
                }
            } else {
                val br = BufferedReader(InputStreamReader(webC.errorStream, API_ENCODING))
                val sb = StringBuilder()
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    sb.append(line).append("\n")
                }
                br.close()
                val er = checkErrResponse(sb.toString())
                throw ExpClass(status, er.message ?: "", methodName)
            }
        } catch (ex: IOException) {
            throw ExpClass(ExpClass.NETWORK_EXP, methodName, ex.toString(), ex)
        } finally {
            if (webC != null) {
                try {
                    webC.disconnect()
                } catch (ex: Exception) {
                    throw ExpClass(ExpClass.GENERAL_EXP, methodName, ex.toString(), ex)
                }
            }
        }
    }

    /*
        The CallPostPutApi is a general method used to process Authenticated Rest POST (or PUT)
        calls. To use it, supply the path, a request object, authorization token and the TypeWS
        of the return object. This returns an instance of the specified type upon success.
        If no response is expected, the polymorphic wrapper method that has no defined return
        type can be used.

        In the case that the Web Service call returns other than a success(2xx) code, the error
        response stream is parsed and an instance of the ExpClass is thrown. This will contain
        the HTTP status code and/or the actual exception.
     */
    override fun <T> callPostApi(path: String, input: T, token: String) {
        callPostApi(
                path,
                input,
                Nothing::class.java,
                token)
    }

    override fun <T, U> callPostApi(path: String, input: T, typeU: Class<U>, token: String): U? {
        return callPostPutApi(
                path,
                input,
                typeU,
                token)
    }

    override fun <T> callPutApi(path: String, input: T, token: String) {
        callPutApi(
                path,
                input,
                Nothing::class.java,
                token)
    }

    override fun <T, U> callPutApi(path: String, input: T, typeU: Class<U>, token: String): U? {
        return callPostPutApi(
                path,
                input,
                typeU,
                token,
                false)
    }

    private fun <T, U> callPostPutApi(path: String, input: T, typeU: Class<U>, token: String, post: Boolean = true): U? {
        val methodName = this.javaClass.name + ".CallPostPutApi-" + path
        val bearer: String = if (token.isBlank()) token else API_BEARER + token
        var webC: HttpURLConnection? = null
        val typeOfRequest = object : TypeToken<T>() {}.type
        return try {
            // Set up the connection
            val web = URL(path)
            webC = web.openConnection() as HttpURLConnection
            webC.requestMethod = if(post) "POST" else "PUT" // Available: POST, PUT, DELETE, OPTIONS, HEAD and TRACE
            webC.setRequestProperty("Accept", API_HEADER_ACCEPT)
            if (bearer.isNotEmpty()) webC.setRequestProperty("Authorization", bearer)
            webC.setRequestProperty("Content-type", API_HEADER_CONTENT)
            webC.doOutput = true
            webC.useCaches = false
            webC.allowUserInteraction = false
            webC.connectTimeout = Timeout
            webC.readTimeout = Timeout
            webC.connect()
            // Create the payload
            val body: OutputStream = BufferedOutputStream(webC.outputStream)
            val jwrite = JsonWriter(OutputStreamWriter(body, API_ENCODING))
            Parser.toJson(input, typeOfRequest, jwrite)
            jwrite.flush()
            jwrite.close()
            // Process the response
            val status = webC.responseCode
            if (status in 200..299) {
                if (typeU == Nothing::class.java) { // No return data is expected, so none returned.
                    return null
                }
                val br = BufferedReader(InputStreamReader(webC.inputStream, API_ENCODING))
                val sb = StringBuilder()
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    sb.append(line).append("\n")
                }
                br.close()
                try {
                    Parser.fromJson(sb.toString(), typeU)
                } catch (ex: Exception) {
                    throw ExpClass(ExpClass.PARSE_EXP, methodName, ex.toString(), ex)
                }
            } else {
                val br = BufferedReader(InputStreamReader(webC.errorStream, API_ENCODING))
                val sb = StringBuilder()
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    sb.append(line).append("\n")
                }
                br.close()
                val er = checkErrResponse(sb.toString())
                throw ExpClass(status, er.message ?: "", methodName)
            }
        } catch (ex: IOException) {
            throw ExpClass(ExpClass.NETWORK_EXP, methodName, ex.toString(), ex)
        } finally {
            if (webC != null) {
                try {
                    webC.disconnect()
                } catch (ex: Exception) {
                    throw ExpClass(ExpClass.GENERAL_EXP, methodName, ex.toString(), ex)
                }
            }
        }
    }

    /*
        The CallPostFormApi is a general method used to process HTTP POST calls that still
        use the older "Form" based content method.  This type or call was common prior to
        the use of Javascript and is supported directly in HTML by browsers.  This format
        is sometimes still seen in login methods, even if the rest of the API has moved
        to JSON.  Interestingly, for the most part these return JSON, thankfully.

        The input data is a set of key/value pairs that are converted to a query string,
        but sent in the body instead of the URL.
     */
    override fun callPostFormApi(path: String, params: HashMap<String, String>, token: String) {
        callPostFormApi(
                path,
                params,
                Nothing::class.java,
                token)
    }

    override fun <U> callPostFormApi(path: String, params: HashMap<String, String>, typeU: Class<U>, token: String): U? {
        val methodName = this.javaClass.name + ".CallPostFormApi-" + path
        val bearer: String = if (token.isBlank()) token else API_BEARER + token
        var webC: HttpURLConnection? = null
        return try {
            // Set up the connection
            val web = URL(path)
            webC = web.openConnection() as HttpURLConnection
            webC.requestMethod = "POST"
            webC.setRequestProperty("Accept", API_HEADER_ACCEPT)
            if (bearer.isNotEmpty()) webC.setRequestProperty("Authorization", bearer)
            webC.setRequestProperty("Content-type", API_XFORM_CONTENT)
            webC.doOutput = true
            webC.useCaches = false
            webC.allowUserInteraction = false
            webC.connectTimeout = Timeout
            webC.readTimeout = Timeout
            webC.connect()
            // Create the payload
            val os = webC.outputStream
            val writer = BufferedWriter(OutputStreamWriter(os, API_ENCODING))
            writer.write(getPostDataString(params))
            writer.flush()
            writer.close()
            os.close()
            // Process the response
            val status = webC.responseCode
            if (status in 200..299) {
                if (typeU == Nothing::class.java) { // No return data is expected, so none returned.
                    return null
                }
                val br = BufferedReader(InputStreamReader(webC.inputStream, API_ENCODING))
                val sb = StringBuilder()
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    sb.append(line).append("\n")
                }
                br.close()
                try {
                    Parser.fromJson(sb.toString(), typeU)
                } catch (ex: Exception) {
                    throw ExpClass(ExpClass.PARSE_EXP, methodName, ex.toString(), ex)
                }
            } else {
                val br = BufferedReader(InputStreamReader(webC.errorStream, API_ENCODING))
                val sb = StringBuilder()
                var line: String?
                while (br.readLine().also { line = it } != null) {
                    sb.append(line).append("\n")
                }
                br.close()
                val er = checkErrResponse(sb.toString())
                throw ExpClass(status, er.message ?: "", methodName)
            }
        } catch (ex: IOException) {
            throw ExpClass(ExpClass.NETWORK_EXP, methodName, ex.toString(), ex)
        } finally {
            if (webC != null) {
                try {
                    webC.disconnect()
                } catch (ex: Exception) {
                    throw ExpClass(ExpClass.GENERAL_EXP, methodName, ex.toString(), ex)
                }
            }
        }
    }
}