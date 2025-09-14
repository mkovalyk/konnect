package com.kovcom.konnect.logger

/**
 * An interface for logging messages.
 * This allows for custom logger implementations to be used with Konnect.
 */
interface Logger {
    /**
     * Sends a debug log message.
     * @param tag Used to identify the source of a log message.
     * @param message The message you would like logged.
     */
    fun d(tag: String, message: String)

    /**
     * Sends an info log message.
     * @param tag Used to identify the source of a log message.
     * @param message The message you would like logged.
     */
    fun i(tag: String, message: String)

    /**
     * Sends an error log message.
     * @param tag Used to identify the source of a log message.
     * @param message The message you would like logged.
     * @param throwable An exception to log.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
