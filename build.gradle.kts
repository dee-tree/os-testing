plugins {
    kotlin("jvm") version "1.9.21" apply false
    jacoco
}

group = "edu.shamalov.os" // shamalov stands for "Shamaro & Sokolov"
version = "0.1.0"

repositories {
    mavenCentral()
}

buildscript {
    subprojects {
        repositories {
            mavenCentral()
        }
    }
}



