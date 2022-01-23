plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

configurations {
    val plugin = create("plugin")
    implementation.extendsFrom(plugin)
}

tasks.getByName<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    val cfg = project.configurations.getByName("plugin")
    configurations = listOf(cfg)
}


dependencies {
    implementation("org.key_project:key.core:2.11.0-SNAPSHOT")
    implementation("org.key_project:key.util:2.11.0-SNAPSHOT")
    implementation("org.key_project:key.ui:2.11.0-SNAPSHOT")
    implementation("org.slf4j:slf4j-api:1.7.33")

    testImplementation("org.slf4j:slf4j-simple:1.7.32")

    val plugin by configurations
    plugin("com.atlassian.commonmark:commonmark:0.17.0")
    plugin("com.atlassian.commonmark:commonmark-ext-gfm-tables:0.17.0")
    plugin("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    plugin("com.fasterxml.jackson.core:jackson-databind:2.13.1")
    plugin("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.13.1")
    plugin("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.13.1")
    plugin("org.ocpsoft.prettytime:prettytime:5.0.2.Final")
}