plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidx.room3) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

detekt {
    source.setFrom("shared/src", "androidApp/src")
    config.setFrom("config/detekt.yml")
    buildUponDefaultConfig = true
    parallel = true
}

// Targets name source folders explicitly: a `**` glob also walks build/ while other tasks write into it,
// which makes Spotless fail intermittently.
spotless {
    kotlin {
        target("shared/src/**/*.kt", "androidApp/src/**/*.kt")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("*.gradle.kts", "shared/*.gradle.kts", "androidApp/*.gradle.kts")
        ktlint(libs.versions.ktlint.get())
    }
}
