plugins {
    kotlin("jvm") version "1.9.21"
}

group = "edu.shamalov.os" // shamalov stands for "Shamaro & Sokolov"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(20)
}