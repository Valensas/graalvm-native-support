package com.valensas.nativesupport.autoconfigure

import com.valensas.nativesupport.hints.CustomRuntimeHintsRegistrar
import com.valensas.nativesupport.hints.HazelcastRuntimeHintsRegistrar
import com.valensas.nativesupport.hints.KotlinRuntimeHintsRegistrar
import com.valensas.nativesupport.hints.SpringDocRuntimeHintsRegistrar
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportRuntimeHints

@Configuration
@ImportRuntimeHints(
    CustomRuntimeHintsRegistrar::class,
    KotlinRuntimeHintsRegistrar::class,
    HazelcastRuntimeHintsRegistrar::class,
    SpringDocRuntimeHintsRegistrar::class
)
class RuntimeHintsAutoConfiguration
