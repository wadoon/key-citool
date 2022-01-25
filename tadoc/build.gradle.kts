plugins {
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

application {
    mainClassName = "org.key_project.core.doc.AppKt"
}

dependencies {
    implementation("org.key_project:key.core:2.10.0")
    implementation("org.key_project:key.util:2.10.0")

    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.3")
    implementation("com.atlassian.commonmark:commonmark:0.17.0")
    implementation("com.atlassian.commonmark:commonmark-ext-gfm-tables:0.17.0")
    implementation("com.atlassian.commonmark:commonmark-ext-autolink:0.17.0")
    implementation("com.atlassian.commonmark:commonmark-ext-gfm-strikethrough:0.17.0")
    implementation("com.atlassian.commonmark:commonmark-ext-ins:0.17.0")
    implementation("com.atlassian.commonmark:commonmark-ext-heading-anchor:0.17.0")
//    compile group: 'org.eclipse.jgit', name: 'org.eclipse.jgit', version: '5.7.0.202003110725-r'
}