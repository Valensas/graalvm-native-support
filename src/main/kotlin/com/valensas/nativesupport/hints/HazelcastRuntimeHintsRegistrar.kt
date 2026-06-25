package com.valensas.nativesupport.hints

import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder
import org.slf4j.LoggerFactory
import org.springframework.aot.hint.ExecutableMode
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.aot.hint.TypeReference

/**
 * Hints required to start a Hazelcast member inside a GraalVM native image:
 *  - destroy/shutdown method of the HazelcastInstance bean
 *    (see https://github.com/spring-projects/spring-framework/issues/29545#issuecomment-1462460562)
 *  - serialization hints for Hazelcast Throwable subtypes
 *  - reflection (incl. declared fields) for classes whose static initializer resolves a field
 *    VarHandle via tpcengine ReflectionUtil.findVarHandle; the native image strips the field (the
 *    name is a runtime string), causing NoSuchFieldError on first load (e.g.
 *    CompletableFutureTask.runner at cluster join)
 *  - @Probe metric-source classes discovered on the classpath
 */
class HazelcastRuntimeHintsRegistrar : RuntimeHintsRegistrar {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun registerHints(
        hints: RuntimeHints,
        classLoader: ClassLoader?
    ) {
        val cl = classLoader ?: ClassLoader.getSystemClassLoader()

        registerShutdown(hints, cl, "com.hazelcast.instance.impl.HazelcastInstanceProxy")
        HintUtils.registerSerializationHints(hints, StackTraceElement::class.java, cl)
        Reflections(HAZELCAST_PACKAGE).getSubTypesOf(Throwable::class.java).forEach {
            HintUtils.registerSerializationHints(hints, it, cl)
        }

        registerStartupTypes(hints)
        registerProbeMetricSources(hints, cl)
    }

    private fun registerStartupTypes(hints: RuntimeHints) {
        STARTUP_TYPES.forEach { type ->
            handlingMissingClass {
                hints.reflection().registerType(TypeReference.of(type), *MEMBER_CATEGORIES)
            }
        }
    }

    private fun registerProbeMetricSources(
        hints: RuntimeHints,
        classLoader: ClassLoader
    ) {
        @Suppress("UNCHECKED_CAST")
        val probeAnnotation = handlingMissingClass {
            classLoader.loadClass(PROBE_ANNOTATION) as Class<out Annotation>
        } ?: return

        val reflections = Reflections(
            ConfigurationBuilder()
                .forPackage(HAZELCAST_PACKAGE, classLoader)
                .addScanners(Scanners.MethodsAnnotated, Scanners.FieldsAnnotated)
        )

        val metricSources = buildSet {
            reflections.getMethodsAnnotatedWith(probeAnnotation).forEach { add(it.declaringClass) }
            reflections.getFieldsAnnotatedWith(probeAnnotation).forEach { add(it.declaringClass) }
        }

        val registered = metricSources
            .filterNot { clazz -> EXCLUDED_PREFIXES.any { clazz.name.startsWith(it) } }
            .onEach { hints.reflection().registerType(it, *MEMBER_CATEGORIES) }

        logger.info("Registered {} Hazelcast @Probe metric-source classes for native reflection.", registered.size)
    }

    private fun registerShutdown(
        hints: RuntimeHints,
        classLoader: ClassLoader,
        className: String
    ) {
        handlingMissingClass {
            val clazz = classLoader.loadClass(className)
            val shutdownMethod = clazz.getMethod("shutdown")
            hints.reflection().registerMethod(shutdownMethod, ExecutableMode.INVOKE)
        }
    }

    private fun <T> handlingMissingClass(f: () -> T): T? = try {
        f()
    } catch (e: ClassNotFoundException) {
        null
    } catch (e: NoClassDefFoundError) {
        null
    }

    private companion object {
        private const val HAZELCAST_PACKAGE = "com.hazelcast"
        private const val PROBE_ANNOTATION = "com.hazelcast.internal.metrics.Probe"
        private val MEMBER_CATEGORIES = MemberCategory.entries.toTypedArray()

        private val EXCLUDED_PREFIXES = listOf(
            "com.hazelcast.jet.",
            "com.hazelcast.wan.",
            "com.hazelcast.client."
        )

        // Hazelcast 5.7.0 classes whose static init resolves a field VarHandle (ReflectionUtil.findVarHandle); native image strips the field -> NoSuchFieldError on first load (e.g. CompletableFutureTask.runner at join). From a bytecode scan of findVarHandle-in-`static {}` (member-side); re-derive when bumping Hazelcast.
        private val VAR_HANDLE_CLINIT_TYPES = listOf(
            "com.hazelcast.internal.util.executor.CompletableFutureTask",
            "com.hazelcast.spi.impl.AbstractInvocationFuture",
            "com.hazelcast.spi.impl.DelegatingCompletableFuture",
            "com.hazelcast.spi.impl.operationservice.Operation",
            "com.hazelcast.spi.impl.operationservice.impl.Invocation",
            "com.hazelcast.executor.impl.ExecutionCallbackAdapterFactory",
            "com.hazelcast.internal.networking.nio.AbstractChannel",
            "com.hazelcast.internal.locksupport.operations.AbstractLockOperation",
            "com.hazelcast.internal.util.counters.SwCounter",
            "com.hazelcast.internal.util.LatencyDistribution",
            "com.hazelcast.internal.monitor.impl.LocalMapStatsImpl",
            "com.hazelcast.internal.monitor.impl.LocalRecordStoreStatsImpl",
            "com.hazelcast.internal.monitor.impl.LocalListStatsImpl",
            "com.hazelcast.internal.monitor.impl.LocalSetStatsImpl",
            "com.hazelcast.internal.monitor.impl.LocalReplicatedMapStatsImpl",
            "com.hazelcast.internal.monitor.impl.PartitionIndexesStats",
            "com.hazelcast.internal.monitor.impl.PartitionPerIndexStats",
            "com.hazelcast.map.impl.record.CachedDataRecordWithStats",
            "com.hazelcast.map.impl.record.CachedSimpleRecord",
            "com.hazelcast.cache.impl.CacheStatisticsImpl",
            "com.hazelcast.internal.crdt.pncounter.PNCounterProxy",
            "com.hazelcast.flakeidgen.impl.AutoBatcher\$Block",
            "com.hazelcast.internal.nearcache.impl.invalidation.MetaDataContainer",
            "com.hazelcast.internal.nearcache.impl.record.AbstractNearCacheRecord",
            "com.hazelcast.internal.util.collection.ReadOptimizedLruCache\$ValueAndTimestamp",
            "com.hazelcast.internal.tpcengine.Reactor",
            "com.hazelcast.internal.tpcengine.net.AsyncServerSocketMetrics",
            "com.hazelcast.internal.tpcengine.net.AsyncSocketMetrics"
        )

        private val STARTUP_TYPES = listOf(
            "com.hazelcast.instance.GeneratedBuildProperties",
            "com.hazelcast.internal.util.counters.MwCounter",
            "com.hazelcast.shaded.org.jctools.queues.MpmcArrayQueueProducerIndexField",
            "com.hazelcast.shaded.org.jctools.queues.MpmcArrayQueueConsumerIndexField"
        ) + VAR_HANDLE_CLINIT_TYPES
    }
}
