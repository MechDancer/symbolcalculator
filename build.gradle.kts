import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.61"
}

group = "org.mechdancer"
version = "0.2.0-dev"

repositories {
    mavenCentral()
    jcenter()
    maven("https://maven.aliyun.com/repository/central")
    maven("https://maven.aliyun.com/repository/google")
    maven("https://maven.aliyun.com/repository/gradle-plugin")
    maven("https://maven.aliyun.com/repository/jcenter")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    testImplementation("junit", "junit", "+")
    testImplementation(kotlin("test-junit"))
    testImplementation("org.mechdancer", "linearalgebra", "0.2.7-dev-2")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

// 源码导出任务
val sourceTaskName = "sourcesJar"
task<Jar>(sourceTaskName) {
    archiveClassifier.set("sources")
    group = "build"

    from(sourceSets["main"].allSource)
}
tasks["jar"].dependsOn(sourceTaskName)

// 默认内联类
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-Xinline-classes")
}
