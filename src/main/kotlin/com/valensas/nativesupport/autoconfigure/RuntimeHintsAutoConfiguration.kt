package com.valensas.nativesupport.autoconfigure

import com.valensas.nativesupport.hints.CustomRuntimeHintsRegistrar
import com.valensas.nativesupport.hints.HazelcastRuntimeHintsRegistrar
import com.valensas.nativesupport.hints.KotlinRuntimeHintsRegistrar
import com.valensas.nativesupport.hints.MinioRuntimeHintsRegistrar
import com.valensas.nativesupport.hints.SpringDocRuntimeHintsRegistrar
import com.valensas.nativesupport.hints.VaultRuntimeHintsRegistrar
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportRuntimeHints

@Configuration
@ImportRuntimeHints(
    CustomRuntimeHintsRegistrar::class,
    KotlinRuntimeHintsRegistrar::class,
    HazelcastRuntimeHintsRegistrar::class,
    MinioRuntimeHintsRegistrar::class,
    SpringDocRuntimeHintsRegistrar::class,
    VaultRuntimeHintsRegistrar::class
)
class RuntimeHintsAutoConfiguration
