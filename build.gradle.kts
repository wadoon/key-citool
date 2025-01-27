import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import io.github.gradlenexus.publishplugin.NexusRepositoryContainer
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Duration

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.dokka") version "2.0.0"
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"

    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

group = "io.github.wadoon.key"
version = "1.6.0-dev"
description = "Tool for continuous integration of KeY proof files."

repositories {
    mavenCentral()
}

val plugin: Configuration by configurations.creating
configurations {
    implementation.get().extendsFrom(plugin)
}

repositories {
    mavenCentral()
    //maven("https://git.key-project.org/api/v4/projects/35/packages/maven")
    //maven("https://s01.oss.sonatype.org/content/repositories/orgkey-project-1029")
}

val key_version = System.getenv("KEY_VERSION") ?: "2.12.3"

dependencies {
    val implementation by configurations

    plugin(platform("org.jetbrains.kotlin:kotlin-bom"))
    plugin("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    plugin("com.github.ajalt:clikt:2.8.0")
    plugin("org.jetbrains:annotations:26.0.2")
    plugin("org.slf4j:slf4j-api:2.0.16")
    plugin("org.slf4j:slf4j-simple:2.0.16")
    plugin("com.google.code.gson:gson:2.11.0")

    plugin("org.apache.maven:maven-resolver-provider:3.9.9")

    val testImplementation by configurations
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("org.slf4j:slf4j-simple:2.0.16")

    when {
        key_version.startsWith("2.10.") ->
            implementation("org.key_project:key.core:$key_version")
        //key_version.startsWith("2.12.") ->
        else ->
            implementation("org.key-project:key.core:$key_version")
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JvmTarget.JVM_21
    }
}

tasks.withType<JavaCompile> {
    options.release = 21
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


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
//            from(components["kotlin"])

            pom {
                name = "key-ci-tool"
                description = project.description
                url = "https://github.com/wadoon/key-citool"
                licenses {
                    license {
                        name = "GNU General Public License (GPL), Version 2"
                        url = "https://www.gnu.org/licenses/old-licenses/gpl-2.0.html"
                    }
                }
                developers {
                    developer {
                        id = "wadoon"
                        name = "Alexander Weigl"
                        email = "weigl@kit.edu"
                    }
                }
                scm {
                    connection = "git@github.com:wadoon/key-citool.git"
                    url = "https://github.com/wadoon/key-citool"
                }
            }
        }
    }
}

nexusPublishing {
    repositories(Action<NexusRepositoryContainer> {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    })

    // these are not strictly required. The default timeouts are set to 1 minute. But Sonatype can be really slow.
    // If you get the error "java.net.SocketTimeoutException: timeout", these lines will help.
    connectTimeout = Duration.ofMinutes(3)
    clientTimeout = Duration.ofMinutes(3)
}


signing {
    useGpgCmd()
    sign(publishing.publications["mavenJava"])
}

dokka {
    moduleName.set("key-citool")
    dokkaSourceSets.main {
        includes.from("README.md")
        sourceLink {
            localDirectory.set(file("src/main/kotlin"))
            remoteUrl("https://example.com/src")
            remoteLineSuffix.set("#L")
        }
    }
    pluginsConfiguration.html {
        //customStyleSheets.from("styles.css")
        //customAssets.from("logo.png")
        footerMessage.set("(c) Your Company")
    }
}
