plugins {
    kotlin("jvm")
    application
}

group = "edu.shamalov.os"
version = "0.1.0"


dependencies {
    implementation(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.3")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.1.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(20)
}