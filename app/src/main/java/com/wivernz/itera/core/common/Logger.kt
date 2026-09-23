package com.wivernz.itera.core.common
import android.util.Log
interface Logger {
    fun v(tag: String, message: String)
    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, t: Throwable? = null)
    fun e(tag: String, message: String, t: Throwable? = null)
}
class DebugLogger : Logger {
    override fun v(tag: String, message: String) {
        Log.v(tag, message)
    }
    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }
    override fun i(tag: String, message: String) {
        Log.i(tag, message)
    }
    override fun w(tag: String, message: String, t: Throwable?) {
        Log.w(tag, message, t)
    }
    override fun e(tag: String, message: String, t: Throwable?) {
        Log.e(tag, message, t)
    }
}
class ReleaseLogger : Logger {
    override fun v(tag: String, message: String) = Unit
    override fun d(tag: String, message: String) = Unit
    override fun i(tag: String, message: String) {
        Log.i(tag, message)
    }
    override fun w(tag: String, message: String, t: Throwable?) {
        Log.w(tag, message, t)
    }
    override fun e(tag: String, message: String, t: Throwable?) {
        Log.e(tag, message, t)
    }
}
