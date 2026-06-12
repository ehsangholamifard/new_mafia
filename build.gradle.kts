plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

tasks.register<Copy>("copyLicenses") {
    from("/opt/android/sdk/licenses")
    into(file("android_sdk/licenses"))
}

