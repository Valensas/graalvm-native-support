package com.valensas.nativesupport.hints

import org.slf4j.LoggerFactory
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.data.util.TypeScanner

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
        val packages = packageNames.split(",", " ", "\n").filter { it.isNotBlank() }

        logger.info("Setting reflection hints for classes in packages: {}", packages.joinToString(", "))

        TypeScanner
            .typeScanner(classLoader!!)
            .scanPackages(packages)
            .forEach {
                hints.reflection().registerType(it, *MemberCategory.values())
                HintUtils.registerSerializationHints(hints, it, classLoader)
            }
    }
}
