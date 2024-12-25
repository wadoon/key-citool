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
