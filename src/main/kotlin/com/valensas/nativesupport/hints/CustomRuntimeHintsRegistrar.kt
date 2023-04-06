package com.valensas.nativesupport.hints

import org.reflections.Reflections
import org.slf4j.LoggerFactory
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar

/**
 * Registers runtime hints for all classes in packages defined in the
 * `com.valensas.nativesupport.reflect-packages` system property.
 * Serializable classes are recursively processed and added for
 * serialization.
 */
class CustomRuntimeHintsRegistrar : RuntimeHintsRegistrar {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        val packageNames = System.getProperty("com.valensas.nativesupport.reflect-packages") ?: return
        val packages = packageNames.split(",", " ", "\n").filter { it.isNotBlank() }.toTypedArray()

        logger.info("Setting reflection hints for classes in packages: {}", packages.joinToString(", "))

        val reflections = Reflections(*packages)
        reflections
            .getSubTypesOf(java.lang.Object::class.java)
            .forEach {
                hints.reflection().registerType(it, *MemberCategory.values())
                HintUtils.registerSerializationHints(hints, it, classLoader!!)
            }
    }
}
