package com.coolftc.prompt.utility

import android.content.Context
import java.time.LocalDate

interface IWebServices {
    fun <U> callGetApi(path: String, typeU: Class<U>, token: String): U?
    fun <T> callPostApi(path: String, input: T, token: String)
    fun <T, U> callPostApi(path: String, input: T, typeU: Class<U>, token: String): U?
    fun <T> callPutApi(path: String, input: T, token: String)
    fun <T, U> callPutApi(path: String, input: T, typeU: Class<U>, token: String): U?
    fun callPostFormApi(path: String, params: HashMap<String, String>, token: String)
    fun <U> callPostFormApi(path: String, params: HashMap<String, String>, typeU: Class<U>, token: String): U?
    fun callDeleteApi(path: String, token: String)

    fun saveBaseURL(context: Context, url: String)
    fun baseUrl(context: Context): String?
    fun baseUrlAge(context: Context): LocalDate?
}