import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.js.translate.context.Namer.kotlin


buildscript {
    var kotlinVersion: String by extra
    kotlinVersion = "1.2.61"

    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", kotlinVersion))
    }
}

group = "org.hangman"
version = "0.1.0"


plugins {
    java
    kotlin("jvm") version "1.2.61"
}

repositories {
    mavenCentral()
}

dependencies {
    val kotlinVersion: String by extra

    compile(kotlin("stdlib-jdk8", kotlinVersion))
    compile("io.reactivex.rxjava2:rxkotlin:2.3.0")
    testCompile("junit", "junit", "4.12")
    testCompile("io.kotlintest:kotlintest-runner-junit5:3.1.9")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

val test by tasks.getting(Test::class) {
    useJUnitPlatform { }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
