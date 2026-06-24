package com.valensas.nativesupport.hints

import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar

class MinioRuntimeHintsRegistrar : RuntimeHintsRegistrar {
    override fun registerHints(
        hints: RuntimeHints,
        classLoader: ClassLoader?
    ) {
        listOf("io.minio", "org.simpleframework.xml").forEach { pkg ->
            val reflections =
                Reflections(
                    ConfigurationBuilder()
                        .forPackage(pkg, classLoader ?: ClassLoader.getSystemClassLoader())
                        .addScanners(Scanners.SubTypes.filterResultsBy { true })
                )
            reflections.getAll(Scanners.SubTypes).forEach { className ->
                handlingMissingClass {
                    val clazz = (classLoader ?: ClassLoader.getSystemClassLoader()).loadClass(className)
                    hints.reflection().registerType(
                        clazz,
                        MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                        MemberCategory.INVOKE_DECLARED_METHODS,
                        MemberCategory.DECLARED_FIELDS
                    )
                }
            }
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
