plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "io.github.s1mar"
version = "0.1.0"

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.7"
val kotlinxSerializationVersion = "1.6.2"

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.0")
    // HTTP Client (Ktor)
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // JSON Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.9")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()

    coordinates("io.github.s1mar", "kompletions", version.toString())

    pom {
        name.set("Kompletions")
        description.set("A lightweight Kotlin DSL client for OpenAI-compatible chat completion APIs")
        inceptionYear.set("2025")
        url.set("https://github.com/s1mar/Kompletions")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id.set("s1mar")
                name.set("s1mar")
                url.set("https://github.com/s1mar")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/s1mar/Kompletions.git")
            developerConnection.set("scm:git:ssh://git@github.com/s1mar/Kompletions.git")
            url.set("https://github.com/s1mar/Kompletions")
        }
    }
}
