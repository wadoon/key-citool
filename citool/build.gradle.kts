import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "com.github.wadoon.keytools"
version = "1.4.0"


dependencies {
    implementation("org.key_project:key.core:2.10.0")
    implementation("org.key_project:key.util:2.10.0")

    val plugin by configurations
    plugin("org.slf4j:slf4j-simple:1.7.36")
}

val plugin by configurations

tasks.register<ShadowJar>("miniShadowJar") {
    group = "shadow"
    archiveClassifier.set("mini")
    /*dependencies {
        exclude(dependency("org.key_project:key.core"))
        exclude(dependency("org.key_project:key.util"))
    }*/
    from(sourceSets.getByName("main").output)
    configurations = listOf(plugin)
    manifest {
        this.attributes(
            "Main-Class" to "de.uka.ilkd.key.CheckerKt"
        )
    }
}

application {
    mainClassName = "de.uka.ilkd.key.CheckerKt"
}


run {}
