import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("org.jetbrains.dokka") version "1.7.20"
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "io.github.wadoon"
version = "1.5.0-snapshot"


repositories {
    mavenCentral()
}

val plugin: Configuration by configurations.creating
configurations {
    implementation.get().extendsFrom(plugin)
}


repositories {
    maven("https://git.key-project.org/api/v4/projects/35/packages/maven")
}

dependencies {
    val implementation by configurations

    plugin(platform("org.jetbrains.kotlin:kotlin-bom"))
    plugin("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    plugin("com.github.ajalt:clikt:2.8.0")
    plugin("org.jetbrains:annotations:23.0.0")
    plugin("org.slf4j:slf4j-api:1.7.33")
    plugin("org.slf4j:slf4j-simple:1.7.36")
    plugin("com.google.code.gson:gson:2.9.0")

    val testImplementation by configurations
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("com.google.truth:truth:1.1.3")
    testImplementation("org.slf4j:slf4j-simple:1.7.33")

    implementation("org.key_project:key.core:2.10.0")
    implementation("org.key_project:key.util:2.10.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<JavaCompile> {
    options.release.set(11)
}

tasks.withType<Test> {
    useJUnitPlatform()
    reports.html.required.set(false)
    reports.junitXml.required.set(true)
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<Javadoc> {
    isFailOnError = false
}

tasks.register<ShadowJar>("miniShadowJar") {
    group = "shadow"
    archiveClassifier.set("mini")
    from(sourceSets.getByName("main").output)
    configurations = listOf(plugin)
    manifest {
        this.attributes(
            "Main-Class" to "de.uka.ilkd.key.CheckerKt"
        )
    }
}

application {
    mainClass.set("de.uka.ilkd.key.CheckerKt")
}
