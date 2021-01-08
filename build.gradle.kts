plugins {
    java
    application
    id("com.google.protobuf") apply false
    kotlin("jvm") version "1.4.21"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit", "junit", "4.12")
    implementation(kotlin("stdlib"))
}

subprojects {
    apply {
        plugin("java")
        plugin("kotlin")
        plugin("application")
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("com.google.protobuf:protobuf-java:3.13.0")
        implementation("com.google.code.gson:gson:2.8.6")
        testImplementation("junit", "junit", "4.12")
    }
}
