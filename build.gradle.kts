plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    application
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-server-status-pages:2.3.7")
    
    // JWT Authentication
    implementation("io.ktor:ktor-server-auth:2.3.7")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.7")
    
    // WebSocket
    implementation("io.ktor:ktor-server-websockets:2.3.7")
    
    // JSON Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    
    // Swagger / OpenAPI
    implementation("io.ktor:ktor-server-openapi:2.3.7")
    implementation("io.ktor:ktor-server-swagger:2.3.7")
    
    // Logging middleware
    implementation("io.ktor:ktor-server-call-logging:2.3.7")
    
    // CORS
    implementation("io.ktor:ktor-server-cors:2.3.7")
    
    // PostgreSQL + Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:0.45.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.45.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.45.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.45.0")
    implementation("org.postgresql:postgresql:42.7.1")
    implementation("com.zaxxer:HikariCP:5.1.0")
    
    // Password hashing
    implementation("org.mindrot:jbcrypt:0.4")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    // Testing
    testImplementation("io.ktor:ktor-server-tests:2.3.7")
    testImplementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.22")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.22")
    testImplementation("com.h2database:h2:2.2.224")
}

application {
    mainClass.set("com.example.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnit()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.example.ApplicationKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
