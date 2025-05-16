package com.valensas.nativesupport.hints

import org.springframework.aot.hint.BindingReflectionHintsRegistrar
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter

class HibernateRuntimeHintsRegistrar : RuntimeHintsRegistrar {
    override fun registerHints(
        hints: RuntimeHints,
        classLoader: ClassLoader?
    ) {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AssignableTypeFilter(Any::class.java))

        val basePackage = "org.hibernate.validator.internal.constraintvalidators"
        val components = scanner.findCandidateComponents(basePackage)

        components.forEach { beanDefinition ->
            val clazz = classLoader?.loadClass(beanDefinition.beanClassName)
            BindingReflectionHintsRegistrar().registerReflectionHints(hints.reflection(), clazz)
        }
    }
}
