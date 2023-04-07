package com.valensas.nativesupport.hints

import org.reflections.Reflections
import org.springframework.aot.hint.ExecutableMode
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar

/**
 * Register hints for destroy method when a HazelcastInstance is exposed as a spring bean
 * (see https://github.com/spring-projects/spring-framework/issues/29545#issuecomment-1462460562)
 * and related exception classes for serialization.
 */
class HazelcastRuntimeHintsRegistrar : RuntimeHintsRegistrar {

    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        registerShutdown(hints, classLoader, "com.hazelcast.instance.impl.HazelcastInstanceProxy")
        HintUtils.registerSerializationHints(hints, StackTraceElement::class.java, classLoader!!)

        val reflections = Reflections("com.hazelcast")
        reflections.getSubTypesOf(Throwable::class.java).forEach {
            HintUtils.registerSerializationHints(hints, it, classLoader)
        }
    }

    private fun registerShutdown(hints: RuntimeHints, classLoader: ClassLoader?, className: String) {
        handlingMissingClass {
            val clazz = classLoader?.loadClass(className) ?: return@handlingMissingClass
            val shutdownMethod = clazz.getMethod("shutdown")
            hints.reflection().registerMethod(shutdownMethod, ExecutableMode.INVOKE)
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
