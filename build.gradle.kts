import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Duration

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.10"
    id("org.jetbrains.dokka") version "2.0.0"
    `java-library`
    id("application")
    id("com.gradleup.shadow") version "9.1.0"

    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

group = "io.github.wadoon.key"
version = "1.7.0-SNAPSHOT"
description = "Tool for continuous integration of KeY proof files."

repositories {
    mavenCentral()
    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
}

val plugin: Configuration by configurations.creating
configurations {
    implementation.get().extendsFrom(plugin)
}

repositories {
    mavenCentral()
}

val keyVersion = System.getenv("KEY_VERSION") ?: "2.12.4-SNAPSHOT"

dependencies {
    val implementation by configurations

    //plugin(platform("org.jetbrains.kotlin:kotlin-bom:2.2.0"))
    plugin("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.10")
    plugin("com.github.ajalt.clikt:clikt:5.0.3")
    plugin("org.jetbrains:annotations:26.0.2")
    plugin("org.slf4j:slf4j-api:2.0.17")
    plugin("org.slf4j:slf4j-simple:2.0.17")
    plugin("com.google.code.gson:gson:2.13.1")

    //plugin("org.apache.maven:maven-resolver-provider:3.9.10")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("org.slf4j:slf4j-simple:2.0.17")

    when {
        keyVersion.startsWith("2.10.") ->
            implementation("org.key_project:key.core:$keyVersion")

        else ->
            implementation("org.key-project:key.core:$keyVersion")
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
    description = "Build a small fatJar w/o key"
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

            repositories{
                maven {
                    name = "folder"
                    url = uri("$rootDir/release")
                }
            }


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
    repositories {
        create("central") {
            nexusUrl = uri("https://ossrh-staging-api.central.sonatype.com/service/local/")
            snapshotRepositoryUrl = uri("https://central.sonatype.com/repository/maven-snapshots/")

            stagingProfileId.set("org.key-project")
            val user: String = project.properties.getOrDefault("ossrhUsername", "").toString()
            val pwd: String = project.properties.getOrDefault("ossrhPassword", "").toString()

            username.set(user)
            password.set(pwd)
        }
    }
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
            remoteUrl("https://github.com/wadoon/key-citool/blob/main")
            remoteLineSuffix.set("#L")
        }
    }
    pluginsConfiguration.html {
        //customStyleSheets.from("styles.css")
        //customAssets.from("logo.png")
        footerMessage.set("GPL-v2-only")
    }
}
