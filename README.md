# GraalVM Native Support

This library contains a set of classes an auto-configurations to facilitate using GraalVM Native Images with
Spring Boot.

## Runtime Hints

There are several Runtime Hints provided by this library:

- Kotlin internal types (`KotlinRuntimeHintsRegistrar`), for internal types used by Kotlin.
- Hazelcast types (`HazelcastRuntimeHintsRegistrar`), for registering destroy method when a `HazelcastInstance` is exposed as a Spring Bean.
- Custom types (`CustomRuntimeHintsRegistrar`), for automatically registering all types in packages given by the user. This is known to work well for spring-data entity classes and WebClient models.

The `CustomRuntimeHintsRegistrar` can be used by setting the `com.valensas.nativesupport.reflect-packages` system property
to include the required packages. Reflection configuration will be added for all classes found in these packages. Additionally,
serialization configuration will be added for classes implementing `Serializable` in a recursive manner (recursion includes fields
and superclasses).

Example gradle configuration:

```kotlin
tasks.processAot {
    doFirst {
        systemProperty("com.valensas.nativesupport.reflect-packages", """
            org.example.myproject.model
            org.example.myproject.entity
            """.trimIndent())
    }
}
```

Warning: When using interfaces in classes within these packages, the automatic reflection config will try as a best-effort
to provide serialization configuration for known interfaces such as `Map` or `List`. Many cases are not handled automatically.

Warning: When using classes with `@java.io.Serial` methods, the automatic reflection config will try as a best-effort
to provide serialization configuration for known classes. Many cases are handled automatically.

## Pseudo-Conditional bean creation

This module allows for pseudo-conditional bean creation at runtime that can be used with AOT processing. There are
two different techniques used allow this, each with its own pros and cons.

Warning: As conditional bean creation is not supported while using AOT processing, the techniques used *always* create
a bean but uses workarounds that work for specific use-cases. Each technique has its own use-case and pitfall.

### Creating beans as list

This technique consists of creating a `List` type bean instead of a single type. This technique can be used
when the beans are injected using a `List`. As this technique exposes a bean of type `List`, annotations on
the actual object such `@Transactional`, `@Scheduled`, `@Cacheable`, `@PostConstruct` will *not* work.
This technique is known to work well for defining consumers for `kafka-webflux`.

Example configuration:

```kotlin
enum class MyCommandLineArgs(
    override val value: String
): ConfigurationPredicateValue {
    Schedule("schedule")
}

@Configuration
class KafkaConfig(
    private val consumerService: MyConsumerService,
    @Value("\${spring.kafka.consumer.enabled:#{null}}")
    private val consumerEnabled: Boolean?,
    override val args: ApplicationArguments,
): CommandLineArgConfiguration {
    fun myDescriptor(): KafkaConsumerDescriptor =
        PayloadKafkaConsumerDescriptor(
            topic = "my-topic",
            modelType = MyKafkaModel::class,
            consumer = consumerService::process,
            wildcard = false
        )

    @Bean
    fun consumers() = conditionalListOnPredicate(
        MyCommandLineArgs.Schedule.doesNotExist().masterSwitch(consumerEnabled),
        listOf(myDescriptor())
    )
}
```

### Creating beans trough a proxy

This technique consists of creating bean through a proxy that conditionally relay calls to the methods to the actual
implementation. If not enabled, the proxy doesn't call the underlying implementation and returns `null`. Here, the
bean must be exposed as an interface. Spring annotations will not work on the concrete implementation but will work when
added to the interface. This is because all Spring can see is the interface and not the concrete implementation. This is
known to work well with Spring `@Scheduled` tasks.

Example configuration:

```kotlin
interface ISchedulingService {
    // All Spring annotations needs to be placed on the interface
    @Scheduled(fixedDelayString = "1000")
    fun myScheduledJob()
}

// This class needs to be open and not annotated with @Service/@Component
open class MySchedulingService: ISchedulingService {
    @Timed("cron_job", extraTags = ["name", "My Scheduled Job"]) // This annotation will not work
    override fun myScheduledJob() = runBlocking {
        println("Running job")
    }
}

enum class MyCommandLineArgs(
    override val value: String
): ConfigurationPredicateValue {
    Schedule("schedule")
}

@Configuration
@EnableScheduling
class SchedulingConfig(
    override val args: ApplicationArguments,
    @Value("\${exchange.scheduling.enabled:#{null}}")
    private val schedulingEnabled: Boolean?
): CommandLineArgConfiguration {
    @Bean
    fun schedulingService(): ISchedulingService {
        return conditionalProxyOnPredicate<ISchedulingService, MySchedulingService>(
                OrderGroupCommandLineArg.Schedule.exists().masterSwitch(schedulingEnabled),
            MySchedulingService()
        )
    }
}

```
