package com.valensas.nativesupport.config

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

/**
 * An invocation handler that calls if enabled. Can be used
 * to help with conditionally running @Scheduled tasks with
 * native images.
 */
class OptionalBeanInvocationHandler<T>(
    private val enabled: Boolean,
    private val original: T
) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        if (!enabled) return null

        return if (args != null) {
            method.invoke(original, *args)
        } else {
            method.invoke(original)
        }
    }
}
