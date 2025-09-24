package com.kovcom.konnect.core.logger

import android.util.Log
import com.kovcom.konnect.logger.Logger

/**
 * A default implementation of the [com.kovcom.konnect.logger.Logger] interface that uses the Android Log class.
 */
class AndroidLogger : Logger {
    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        Log.e(tag, message, throwable)
    }
}
