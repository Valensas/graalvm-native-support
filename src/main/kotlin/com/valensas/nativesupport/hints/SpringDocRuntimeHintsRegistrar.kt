package com.valensas.nativesupport.hints

import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar

class SpringDocRuntimeHintsRegistrar : RuntimeHintsRegistrar {

    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        val classNames = listOf(
            "io.swagger.v3.oas.models.PathItem\$HttpMethod",
            "io.swagger.v3.oas.models.PathItem"
        )
        classNames.forEach { className ->
            val clazz = classLoader?.loadClass(className) ?: return@forEach
            hints.reflection().registerType(clazz)
        }
    }
}
