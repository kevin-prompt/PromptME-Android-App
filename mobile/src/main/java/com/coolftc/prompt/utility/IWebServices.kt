package com.coolftc.prompt.utility

interface IWebServices {
    fun <U> callGetApi(path: String, typeU: Class<U>, token: String): U?
    fun <T> callPostApi(path: String, input: T, token: String)
    fun <T, U> callPostApi(path: String, input: T, typeU: Class<U>, token: String): U?
    fun <T> callPutApi(path: String, input: T, token: String)
    fun <T, U> callPutApi(path: String, input: T, typeU: Class<U>, token: String): U?
    fun callPostFormApi(path: String, params: HashMap<String, String>, token: String)
    fun <U> callPostFormApi(path: String, params: HashMap<String, String>, typeU: Class<U>, token: String): U?
}