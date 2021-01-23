buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.13")
    }
}

rootProject.name = "server-architecture-comparison"
include("Client")
include("Common")
include("Server")
include("Experiment")
include("GUI")
