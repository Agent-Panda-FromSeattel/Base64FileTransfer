plugins {
    // 确保插件正确应用
    kotlin("jvm") version "1.8.22" apply false
    java
    application
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "cn.njit"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

application {
    mainClass.set("cn.njit.Main")
}

// 添加多个仓库以增加可用性
repositories {
    mavenCentral()
    maven { url = uri("https://repo.maven.apache.org/maven2") }
    maven { url = uri("https://plugins.gradle.org/m2/") }
}

dependencies {
    // 显式指定Kotlin版本
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.22")
    testImplementation("junit:junit:4.13.2")
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
}

// 禁用配置缓存以避免缓存问题
configurations.configureEach {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("--enable-preview")
}

// 清理任务
tasks.register("cleanCache") {
    doLast {
        delete(rootProject.buildDir)
        delete(file(".gradle"))
    }
}

// 配置客户端任务
tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("clientShadowJar") {
    archiveClassifier.set("client")
    manifest {
        attributes("Main-Class" to "cn.njit.client.Client")
    }
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())
}

// 配置服务器端任务
tasks.register<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("serverShadowJar") {
    archiveClassifier.set("server")
    manifest {
        attributes("Main-Class" to "cn.njit.server.Server")
    }
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())
}