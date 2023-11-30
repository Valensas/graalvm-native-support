package com.valensas.nativesupport.hints

import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar

class VaultRuntimeHintsRegistrar : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        handlingMissingClass {
            val clazz = classLoader?.loadClass("org.springframework.vault.core.VersionedResponse") ?: return@handlingMissingClass
            hints.reflection().registerType(clazz, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)
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
