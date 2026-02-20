package com.valensas.nativesupport.hints

import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar

class MinioRuntimeHintsRegistrar : RuntimeHintsRegistrar {
    override fun registerHints(
        hints: RuntimeHints,
        classLoader: ClassLoader?
    ) {
        registerByName(
            hints,
            classLoader,
            "org.simpleframework.xml.core.TextLabel",
            "io.minio.messages.LocationConstraint",
            "io.minio.BaseArgs",
            "io.minio.BaseArgs\$Builder",
            "io.minio.GetObjectArgs",
            "io.minio.GetObjectArgs\$Builder",
            "io.minio.PutObjectArgs",
            "io.minio.PutObjectArgs\$Builder",
            "io.minio.StatObjectArgs",
            "io.minio.StatObjectArgs\$Builder"
        )
    }

    private fun registerByName(
        hints: RuntimeHints,
        classLoader: ClassLoader?,
        vararg classNames: String
    ) {
        classNames.forEach { name ->
            handlingMissingClass {
                val clazz = classLoader?.loadClass(name) ?: return@handlingMissingClass
                hints.reflection().registerType(
                    clazz,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS,
                    MemberCategory.DECLARED_FIELDS
                )
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
