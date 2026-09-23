package com.wivernz.itera
import com.wivernz.itera.core.common.Logger
class TestLogger : Logger {
    val warnings = mutableListOf<String>()
    override fun v(tag: String, message: String) = Unit
    override fun d(tag: String, message: String) = Unit
    override fun i(tag: String, message: String) = Unit
    override fun w(tag: String, message: String, t: Throwable?) {
        check(t == null)
        warnings += message
    }
    override fun e(tag: String, message: String, t: Throwable?) {
        warnings += message
    }
}
