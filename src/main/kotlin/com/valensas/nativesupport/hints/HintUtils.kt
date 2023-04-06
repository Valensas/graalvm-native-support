package com.valensas.nativesupport.hints

import org.slf4j.LoggerFactory
import org.springframework.aot.hint.RuntimeHints
import java.io.Serializable
import java.lang.reflect.Modifier

object HintUtils {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var serializedClasses: MutableSet<Class<*>> = mutableSetOf()

    // Use explicit `java.lang` package to avoid Kotlin types
    private val primitiveToBoxed = mapOf(
        "byte" to java.lang.Byte::class.java,
        "short" to java.lang.Short::class.java,
        "char" to java.lang.Character::class.java,
        "int" to java.lang.Integer::class.java,
        "long" to java.lang.Long::class.java,
        "float" to java.lang.Float::class.java,
        "double" to java.lang.Double::class.java,
        "boolean" to java.lang.Boolean::class.java
    )

    fun registerSerializationHints(hints: RuntimeHints, clazz: Class<*>, classLoader: ClassLoader) {
        if (clazz.isArray) return logger.trace("{} is interface or array, skipping", clazz)
        if (!serializedClasses.add(clazz)) return logger.trace("{} is already processed, skipping", clazz)
        if (clazz.isInterface) return registerInterfaceSerializationHints(hints, clazz, classLoader)

        if (clazz.isPrimitive) {
            registerPrimitiveSerializationHints(clazz, hints)
            return
        }

        // Primitives are not serializable but their boxed types are. This case should be after primitive check
        if (!Serializable::class.java.isAssignableFrom(clazz)) return logger.trace("{} is not serializable, skipping", clazz)

        @Suppress("UNCHECKED_CAST")
        hints.serialization().registerType(clazz as Class<out Serializable>)

        (clazz.declaredFields + clazz.fields).forEach {
            // Transient properties are not serialized, no need to add serialization hints
            if (!Modifier.isTransient(it.modifiers)) {
                registerSerializationHints(hints, it.type, classLoader)
            } else {
                logger.trace("Field {} of {} is transient, skipping", it, clazz)
            }
        }

        clazz.superclass?.let { registerSerializationHints(hints, it, classLoader) }

        registerCustomSerializationHints(
            classLoader,
            clazz,
            hints
        )
    }

    /**
     * Using @java.io.Serial, the actual serialized class can be changed for a Serializable.
     * This can not be easily detected trough reflection. This method attempts to detect and
     * provide support for known cases with best-effort.
     */
    private fun registerCustomSerializationHints(
        classLoader: ClassLoader,
        clazz: Class<*>,
        hints: RuntimeHints
    ) {
        // Use lambdas to not eagerly load unnecessary classes in case they are not
        // present in the classpath.
        // Ordering of this list is relevant, use longer package names first.
        val customSerializationMapping = listOf(
            "java.time.zone" to { classLoader.loadClass("java.time.zone.Ser") },
            "java.time.chrono" to { classLoader.loadClass("java.time.chrono.Ser") },
            "java.time" to { classLoader.loadClass("java.time.Ser") }
        )

        val customSerializedClass =
            customSerializationMapping.find { clazz.packageName.startsWith(it.first) }?.second ?: return
        registerSerializationHints(hints, customSerializedClass(), classLoader)
    }

    private fun registerPrimitiveSerializationHints(clazz: Class<*>, hints: RuntimeHints) {
        // During serialization, primitive types are automatically boxed.
        // Use the boxed type instead of the primitive itself.
        val boxedType = primitiveToBoxed[clazz.canonicalName] ?: throw Exception(
            "Unknown primitive type $clazz"
        )
        logger.trace("Registering boxed type {} for primitive {}", boxedType, clazz)
        @Suppress("UNCHECKED_CAST")
        hints
            .serialization()
            .registerType(boxedType as Class<out Serializable>)
            // Always register Number as it's used as the superclass of boxed-types
            .registerType(Number::class.java)
    }

    /***
     * Adding serialization hints for interfaces does not work. Only concrete implementations can
     * be used. Searching for all implementations of an interface in the classpath is not reasonable.
     * This method tries to add serialization information for well-known interface/implementation
     * pairings with best-effort.
     */
    private fun registerInterfaceSerializationHints(hints: RuntimeHints, clazz: Class<*>, classLoader: ClassLoader) {
        val mapClasses = listOf(
            { java.util.HashMap::class.java },
            { java.util.LinkedHashMap::class.java },
            { classLoader.loadClass("kotlin.collections.EmptyMap") },
            { classLoader.loadClass("java.util.Collections\$SingletonMap") }
        )

        val listClasses = listOf(
            { java.util.ArrayList::class.java },
            { java.util.LinkedList::class.java }
        )

        val knownInterfaceImplementations = mapOf(
            Map::class.java to mapClasses,
            java.util.Map::class.java to mapClasses,
            List::class.java to listClasses,
            java.util.List::class.java to listClasses
        )

        knownInterfaceImplementations[clazz]?.forEach {
            try {
                @Suppress("UNCHECKED_CAST")
                hints.serialization().registerType(it() as Class<Serializable>)
            } catch (e: NoClassDefFoundError) {
                // ignored
            } catch (e: ClassNotFoundException) {
                // ignored
            }
        }
    }
}
