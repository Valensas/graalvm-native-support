package com.valensas.nativesupport.config

import org.springframework.boot.ApplicationArguments
import java.lang.reflect.Method
import java.lang.reflect.Proxy

interface CommandLineArgConfiguration {
    val args: ApplicationArguments

    /**
     * Conditionally create a bean based on the given predicate. The created
     * bean will be a List type.
     *
     * WARNING: Because this is a list type, it might not be suitable for all cases.
     *          This is known to work well will kafka-webflux consumers.
     */
    fun <T> conditionalListOnPredicate(
        predicate: ConfigurationPredicate<Collection<String>>,
        values: List<T>
    ): List<T> =
        if (predicate.test(args.sourceArgs.toList())) {
            values
        } else {
            emptyList()
        }

    /**
     * Conditionally create a bean based on the given predicate. The created
     * bean will be a List type.
     *
     * WARNING: Because this is a list type, it might not be suitable for all cases.
     *          This is known to work well will kafka-webflux consumers.
     */
    fun <T> conditionalListOnPredicate(
        predicate: ConfigurationPredicate<Collection<String>>,
        value: T
    ): List<T> = conditionalListOnPredicate(predicate, listOf(value))
}

/**
 * Conditionally create a bean based on the predicate. The bean will be enabled or
 * disabled trough com.valensas.nativesupport.config.OptionalBeanInvocationHandler.
 *
 * WARNING: The given class will always be created but methods will return null when
 *          disabled. This might not work well for all cases. This is known to work well
 *          with @Scheduled tasks.
 */
inline fun <reified I, T : I> CommandLineArgConfiguration.conditionalProxyOnPredicate(
    predicate: ConfigurationPredicate<Collection<String>>,
    value: T,
    methodsToDisable: Set<Method> = I::class.java.methods.toSet()
): I {
    val enabled = predicate.test(args.sourceArgs.toList())
    val invocationHandler = OptionalBeanInvocationHandler(enabled, value, methodsToDisable)
    return Proxy.newProxyInstance(
        javaClass.classLoader,
        arrayOf(I::class.java),
        invocationHandler
    ) as I
}
