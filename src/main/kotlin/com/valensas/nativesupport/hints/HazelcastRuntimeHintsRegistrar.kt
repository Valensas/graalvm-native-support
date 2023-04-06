package com.valensas.nativesupport.hints

import org.springframework.aot.hint.ExecutableMode
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import java.io.Serializable

/**
 * Register hints for destroy method when a HazelcastInstance is exposed as a spring bean
 * (see https://github.com/spring-projects/spring-framework/issues/29545#issuecomment-1462460562)
 * and RetryableHazelcastException for serialization when handling cluster rebalance errors.
 */
class HazelcastRuntimeHintsRegistrar : RuntimeHintsRegistrar {

    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        registerShutdown(hints, classLoader, "com.hazelcast.instance.impl.HazelcastInstanceProxy")
        handlingMissingClass {
            val retryableException = classLoader?.loadClass("com.hazelcast.spi.exception.RetryableHazelcastException") ?: return@handlingMissingClass
            registerForSerializationRecursive(hints, retryableException)
        }
    }

    private fun registerShutdown(hints: RuntimeHints, classLoader: ClassLoader?, className: String) {
        handlingMissingClass {
            val clazz = classLoader?.loadClass(className) ?: return@handlingMissingClass
            val shutdownMethod = clazz.getMethod("shutdown")
            hints.reflection().registerMethod(shutdownMethod, ExecutableMode.INVOKE)
        }
    }

    private fun registerForSerializationRecursive(hints: RuntimeHints, clazz: Class<*>) {
        @Suppress("UNCHECKED_CAST")
        hints.serialization().registerType(clazz as Class<Serializable>)

        if (clazz.superclass != null && clazz.superclass != Any::class.java) {
            registerForSerializationRecursive(hints, clazz.superclass)
        }
    }

    private fun handlingMissingClass(f: () -> Unit) {
        try {
            f()
        } catch (e: ClassNotFoundException) {
            // ignored
        } catch (e: NoClassDefFoundError) {
            // ignored
        }
    }
}
