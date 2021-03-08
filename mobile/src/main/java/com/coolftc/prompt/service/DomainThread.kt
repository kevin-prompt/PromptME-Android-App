package com.coolftc.prompt.service

import android.content.Context
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import com.coolftc.prompt.source.BaseCamp
import com.coolftc.prompt.utility.Connection
import com.coolftc.prompt.utility.Constants.*
import com.coolftc.prompt.utility.ExpClass
import com.coolftc.prompt.utility.WebServices
import com.google.gson.Gson

/**
 *  This thread is used to acquire the domain needed to access the API.  The idea is
 *  that this approach allows the location of the API to change if needed.
 */
class DomainThread(private val Context: Context, private val Callback: Messenger? = null) : Thread() {

    override fun run() {
        Connection(Context).use {
            if (it.isOnline()) {
                try {
                    val apiService = WebServices(Gson())
                    val baseURL = apiService.callGetApi(FTI_BASE_CAMP_URL, BaseCamp::class.java, "")
                    if (baseURL == null || baseURL.Host.isNullOrBlank()) throw ExpClass()
                    apiService.saveBaseURL(Context, baseURL.Host)
                    returnOK()
                } catch (kx: ExpClass) {
                    ExpClass.logEXP(kx, "Failed to get basecamp url.")
                    returnErr(kx.Status)
                }
            }
        }
    }

    /*
        If using the message stack to communicate back to caller, use this function
        in the positive case.  Use the msg.what flag identify the message at the
        at the caller.  It is also possible to send data back.
     */
    private fun returnOK() {
        try {
            val msg = Message.obtain()
            msg.what = WHAT_WS_DOMAIN
            Callback?.send(msg)
        } catch (ex: RemoteException) {
            ExpClass.logEX(ex, this.javaClass.name + ".run")
        }
    }

    /*
        If using the message stack to communicate back to caller, use this function
        in the exception case.  Use the msg.what flag identify the message at the
        at the caller.  It is also possible to send data back.
     */
    private fun returnErr(status: Int) {
        try {
            val msg = Message.obtain()
            msg.what = WHAT_WS_DOMAIN_ERR
            msg.arg1 = status
            Callback?.send(msg)
        } catch (ex: RemoteException) {
            ExpClass.logEX(ex, this.javaClass.name + ".run")
        }
    }

}