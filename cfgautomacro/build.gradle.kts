import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.github.wadoon.keytools"
version = "0.1"

val plugin by configurations

dependencies {
    implementation("org.key_project:key.core:2.10.0")
    implementation("org.key_project:key.util:2.10.0")
}

tasks.register<ShadowJar>("miniShadowJar") {
    group = "shadow"
    archiveClassifier.set("mini")
    from(sourceSets.getByName("main").output)
    configurations = listOf(plugin)
}