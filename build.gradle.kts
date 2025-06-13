plugins {
    java
    application
}

group = "cn.njit"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

application {
    mainClass.set("cn.njit.Main") // 后续创建主类
}

// 依赖配置
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8") // Kotlin标准库
    testImplementation("junit:junit:4.13.2") // 测试框架
    // 后续如需SQLite，添加：implementation("org.xerial:sqlite-jdbc:3.42.0.0")
}

// 编译选项
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("--enable-preview") // JDK21预览特性
}
