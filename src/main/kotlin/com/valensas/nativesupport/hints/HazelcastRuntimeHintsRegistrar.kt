package com.valensas.nativesupport.hints

import org.springframework.aot.hint.ExecutableMode
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar

/**
 * Register destroy method when a HazelcastInstance is exposed as a spring bean.
 * See https://github.com/spring-projects/spring-framework/issues/29545#issuecomment-1462460562.
 */
class HazelcastRuntimeHintsRegistrar : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        registerShutdown(hints, classLoader, "com.hazelcast.instance.impl.HazelcastInstanceProxy")
        hints.serialization().registerType(java.lang.Throwable::class.java)
    }

    private fun registerShutdown(hints: RuntimeHints, classLoader: ClassLoader?, className: String) {
        try {
            val clazz = classLoader?.loadClass(className) ?: return
            val shutdownMethod = clazz.getMethod("shutdown")
            hints.reflection().registerMethod(shutdownMethod, ExecutableMode.INVOKE)
        } catch (e: ClassNotFoundException) {
            // ignored
        } catch (e: NoClassDefFoundError) {
            // ignored
        }
    }
}
