import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.71"
    `maven-publish`
}

group = "org.mechdancer"
version = "0.4.0"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    // 矩阵运算
    implementation("org.mechdancer", "linearalgebra", "0.2.8-snapshot-3")

    testImplementation("junit", "junit", "+")
    testImplementation(kotlin("test-junit"))

    // 支持网络工具
    testImplementation(kotlin("reflect"))
    testImplementation("org.mechdancer", "dependency", "+")
    testImplementation("org.mechdancer", "remote", "+")
    testImplementation("org.slf4j", "slf4j-api", "+")
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
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-Xinline-classes")
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/MechDancer/symbolcalculator")
            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("library") {
            from(components["kotlin"])
        }
    }
}
