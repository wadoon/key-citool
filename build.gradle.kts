import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    mavenCentral()
}
plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.31" apply (false)
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
        maven("https://git.key-project.org/api/v4/projects/35/packages/maven")
    }

    dependencies {
        val implementation by configurations

        implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        implementation("com.github.ajalt:clikt:2.8.0")
        implementation("org.jetbrains:annotations:23.0.0")
        implementation("org.key_project:key.core:2.10.0")
        implementation("org.slf4j:slf4j-api:1.7.32")

        //    implementation("org.key_project:key.core")

        val testImplementation by configurations
        testImplementation("org.jetbrains.kotlin:kotlin-test")
        testImplementation("org.junit.jupiter:junit-jupiter:5.6.2")
        testImplementation("org.slf4j:slf4j-simple:1.7.32")
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
}

