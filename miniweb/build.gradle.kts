plugins {
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

val ktor_version = "1.6.7"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.ktor:ktor-server-netty:1.6.7")
    implementation("io.ktor:ktor-html-builder:1.6.7")
    implementation("io.github.microutils:kotlin-logging:2.1.21")
    implementation("org.slf4j:slf4j-simple:1.7.32")
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("org.key_project.web.Server")
}
