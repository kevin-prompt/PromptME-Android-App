package com.coolftc.prompt.utility

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

@Suppress("unused")
class ExpParseToCalendar(Desc: String, Source: Throwable) : ExpClass(TIME_EXP, "ParseToCalendarErr", Desc, Source)

/**
 * This can be used when internally throwing exceptions.
 * The DEBUG_RUN should be turned to false when releasing to production.
 */
@Suppress("unused")
open class ExpClass(
        var Number: Int = GENERAL_EXP,
        var Name: String = exceptionName(Number),
        var Status: Int = 0,
        var Desc: String = exceptionName(Number),
        var Extra: String = "",
        private var Source: Throwable? = null)
    : Exception(Desc, Source) {

    // Traditional encapsulation of exception, providing information on the source exception.
    constructor(Number: Int, Name: String, Desc: String, Source: Throwable?)
            : this(Number = Number, Name = Name, Desc = Desc, Source = Source, Status =  0, Extra = "")

    // Informational exception.
    constructor(Number: Int, Name: String, Desc: String, Extra: String)
            : this(Number = Number, Name = Name, Desc = Desc, Extra = Extra, Status =  0, Source = null)

    // HTTP error encapsulation for propagation of a request failure.
    constructor(Status: Int, Desc: String, Extra: String)
            : this(Status = Status, Desc = Desc, Extra = Extra, Number = HTTP_STATUS, Name =  httpStatusName(Status), Source =  null)

    // Class Constants and Methods available to outside world.
    companion object {
        // Exception Types
        const val GENERAL_EXP = 18000
        const val HTTP_STATUS = 18001
        const val PARSE_EXP = 18002
        const val FILE_ISSUES = 18003
        const val NETWORK_EXP = 18004
        const val TIME_EXP = 18005
        const val NO_DATA_FOUND = 18006
        @SuppressWarnings("WeakerAccess")
        const val AUDIT_LOG = 17001
        // Status Codes
        const val STATUS_CODE_NETWORK_DOWN = 99
        const val STATUS_UNAUTHORIZED = 401
        const val STATUS_NOTFOUND = 404
        const val STATUS_CODE_SERVER_ERR = 500
        const val STATUS_CODE_UNKNOWN = 98
        // Internal Constants
        private const val ERR_TAG = "Exception Sink"
        private const val DEBUG_RUN = true

        fun exceptionName(code: Int): String = when (code) {
            GENERAL_EXP -> "General Failure"
            HTTP_STATUS -> "Networking Failure"
            PARSE_EXP -> "Parse Failure"
            FILE_ISSUES -> "File Failure"
            NETWORK_EXP -> "Connection Failure"
            AUDIT_LOG -> "Audit Message"
            NO_DATA_FOUND -> "No Data Found"
            else -> "Unknown Failure"
        }

        fun audit(title: String?, msg: String?, keys: List<Pair<String, String>>) {
            for (tuple in keys) {
                FirebaseCrashlytics.getInstance().setCustomKey(tuple.first, tuple.second)
            }
            if (msg != null) {
                FirebaseCrashlytics.getInstance().log(msg)
            }
            FirebaseCrashlytics.getInstance().recordException(ExpClass(AUDIT_LOG, "Audit Message", title ?: "", "Custom Keys:" + keys.size.toString()))
        }

        fun logEXP(ex: ExpClass, key: String?) {
            FirebaseCrashlytics.getInstance().log(ex.Number.toString() + " Exception Name: " + ex.Name + "() :: (status=" + httpStatusName(ex.Status) + ") key=" + key + " :: " + ex.message + " -extra- " + ex.Extra)
            FirebaseCrashlytics.getInstance().recordException(ex)
            Log.e(ERR_TAG, ex.Number.toString() + " Exception Name: " + ex.Name + "() :: (status=" + httpStatusName(ex.Status) + ") key=" + key + " :: " + ex.Desc + " -extra- " + ex.Extra)
        }

        fun logEX(ex: Exception, key: String?) {
            FirebaseCrashlytics.getInstance().log(ERR_TAG + ex.stackTrace[0].className + "." + ex.stackTrace[0].methodName + "() :: (key=" + key + ") " + ex.message)
            FirebaseCrashlytics.getInstance().recordException(ex)
            Log.e(ERR_TAG, ex.stackTrace[0].className + "." + ex.stackTrace[0].methodName + "() :: (key=" + key + ") " + ex.message, ex)
        }

        fun logINFO(tag: String?, message: String) {
            if (DEBUG_RUN) Log.i(tag, message)
        }

        private fun httpStatusName(code: Int): String = when (code) {
              99 -> "NetworkDown" // This one is personal.
             100 -> "Continue"
             101 -> "SwitchingProtocols"
             300 -> "MultipleChoices"
             301 -> "MovedPermanently"
             302 -> "Found"
             303 -> "SeeOther"
             304 -> "NotModified"
             307 -> "TemporaryRedirect"
             308 -> "PermanentRedirect"
             400 -> "BadRequest"
             401 -> "Unauthorized"
             402 -> "PaymentRequired"
             403 -> "Forbidden"
             404 -> "NotFound"
             405 -> "MethodNotAllowed"
             406 -> "NotAcceptable"
             407 -> "ProxyAuthenticationRequired"
             408 -> "RequestTimeout"
             409 -> "Conflict"
             410 -> "Gone"
             411 -> "LengthRequired"
             412 -> "PreconditionFailed"
             413 -> "PayloadTooLarge"
             414 -> "URITooLong"
             415 -> "UnsupportedMediaType"
             416 -> "RangeNotSatisfiable"
             417 -> "ExpectationFailed"
             426 -> "UpgradeRequired"
             428 -> "PreconditionRequired"
             429 -> "TooManyRequests"
             431 -> "RequestHeaderFieldsTooLarge"
             451 -> "UnavailableForLegalReasons"
             500 -> "InternalServerError"
             501 -> "NotImplemented"
             502 -> "BadGateway"
             503 -> "ServiceUnavailable"
             504 -> "GatewayTimeout"
             505 -> "HTTPVersionNotSupported"
             511 -> "NetworkAuthenticationRequired"
             else -> "UnknownError"
        }
    }
}
