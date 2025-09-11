package com.kovcom.konnect.logger

import android.util.Log

/**
 * A default implementation of the [Logger] interface that uses the Android Log class.
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
