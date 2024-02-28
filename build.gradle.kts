plugins {
    kotlin("jvm") version "1.9.20"
    kotlin("kapt") version "1.9.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    `maven-publish`
}

group = "co.statu.parsek"
version =
    (if (project.hasProperty("version") && project.findProperty("version") != "unspecified") project.findProperty("version") else "local-build")!!

val pf4jVersion: String by project
val vertxVersion: String by project
val handlebarsVersion: String by project
val gsonVersion: String by project

val bootstrap = (project.findProperty("bootstrap") as String?)?.toBoolean() ?: false
val pluginsDir: File? by rootProject.extra

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    if (bootstrap) {
        compileOnly(project(mapOf("path" to ":Parsek")))
        compileOnly(project(mapOf("path" to ":plugins:parsek-plugin-database")))
        compileOnly(project(mapOf("path" to ":plugins:parsek-plugin-token")))
        compileOnly(project(mapOf("path" to ":plugins:parsek-plugin-mail")))
        compileOnly(project(mapOf("path" to ":plugins:parsek-plugin-system-property")))
        compileOnly(project(mapOf("path" to ":plugins:parsek-plugin-i18n")))
    } else {
        compileOnly("com.github.StatuParsek:Parsek:main-SNAPSHOT")
        compileOnly("com.github.StatuParsek:parsek-plugin-database:main-SNAPSHOT")
        compileOnly("com.github.StatuParsek:parsek-plugin-token:main-SNAPSHOT")
        compileOnly("com.github.StatuParsek:parsek-plugin-mail:main-SNAPSHOT")
        compileOnly("com.github.StatuParsek:parsek-plugin-system-property:main-SNAPSHOT")
        compileOnly("com.github.StatuParsek:parsek-plugin-i18n:main-SNAPSHOT")
    }

    compileOnly(kotlin("stdlib-jdk8"))

    compileOnly("org.pf4j:pf4j:${pf4jVersion}")
    kapt("org.pf4j:pf4j:${pf4jVersion}")

    compileOnly("io.vertx:vertx-web:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin:$vertxVersion")
    compileOnly("io.vertx:vertx-lang-kotlin-coroutines:$vertxVersion")
    compileOnly("io.vertx:vertx-jdbc-client:$vertxVersion")
    compileOnly("io.vertx:vertx-json-schema:$vertxVersion")
    compileOnly("io.vertx:vertx-web-validation:$vertxVersion")
    compileOnly("io.vertx:vertx-web-client:$vertxVersion")

    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    compileOnly("com.google.code.gson:gson:$gsonVersion")

    // https://mvnrepository.com/artifact/com.auth0/java-jwt
    compileOnly("com.auth0:java-jwt:4.4.0")

    // https://mvnrepository.com/artifact/commons-codec/commons-codec
    compileOnly(group = "commons-codec", name = "commons-codec", version = "1.16.0")

    // https://mvnrepository.com/artifact/commons-validator/commons-validator
    compileOnly("commons-validator:commons-validator:1.8.0")
}

tasks {
    shadowJar {
        val pluginId: String by project
        val pluginClass: String by project
        val pluginProvider: String by project
        val pluginDependencies: String by project

        manifest {
            attributes["Plugin-Class"] = pluginClass
            attributes["Plugin-Id"] = pluginId
            attributes["Plugin-Version"] = version
            attributes["Plugin-Provider"] = pluginProvider
            attributes["Plugin-Dependencies"] = pluginDependencies
        }

        archiveFileName.set("$pluginId-$version.jar")

        dependencies {
            exclude(dependency("io.vertx:vertx-core"))
            exclude {
                it.moduleGroup == "io.netty" || it.moduleGroup == "org.slf4j"
            }
        }
    }

    register("copyJar") {
        pluginsDir?.let {
            doLast {
                copy {
                    from(shadowJar.get().archiveFile.get().asFile.absolutePath)
                    into(it)
                }
            }
        }

        outputs.upToDateWhen { false }
        mustRunAfter(shadowJar)
    }

    jar {
        enabled = false
        dependsOn(shadowJar)
        dependsOn("copyJar")
    }
}

publishing {
    repositories {
        maven {
            name = "parsek-plugin-auth"
            url = uri("https://maven.pkg.github.com/StatuParsek/parsek-plugin-auth")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME_GITHUB")
                password = project.findProperty("gpr.token") as String? ?: System.getenv("TOKEN_GITHUB")
            }
        }
    }

    publications {
        create<MavenPublication>("shadow") {
            project.extensions.configure<com.github.jengelman.gradle.plugins.shadow.ShadowExtension> {
                artifactId = "parsek-plugin-auth"
                component(this@create)
            }
        }
    }
}