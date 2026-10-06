plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.android.sdk.common)
}

application {
    mainClass.set("MainKt")
}

tasks.named<JavaExec>("run") {
    args(rootProject.file("app/src/main/res/drawable").absolutePath)
}
