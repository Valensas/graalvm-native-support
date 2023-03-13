package com.valensas.nativesupport.hints

import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import kotlin.reflect.jvm.internal.ReflectionFactoryImpl

/**
 * Register runtime hints for Kotlin internal types.
 * See https://dev.to/jonastm/current-state-of-spring-boot-27-native-with-kotlin-graalvm-3dda
 */
class KotlinRuntimeHintsRegistrar : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        val kotlinInternalTypes = listOf(
            ReflectionFactoryImpl::class.java,
            KotlinVersion::class.java,
            Array<KotlinVersion>::class.java,
            KotlinVersion.Companion::class.java,
            Array<KotlinVersion.Companion>::class.java,
            // This class package-internal and cannot be imported here.
            // jdk8-named classes are used even with Java 17.
            classLoader!!.loadClass("kotlin.internal.jdk8.JDK8PlatformImplementations")
        )

        kotlinInternalTypes.forEach {
            hints.reflection().registerType(
                it,
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.DECLARED_FIELDS,
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS
            )
        }
    }
}
