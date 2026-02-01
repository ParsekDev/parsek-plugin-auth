import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("kapt") version "2.3.0"
    id("com.gradleup.shadow") version "9.3.1"
    id("org.jreleaser") version "1.14.0"
    `maven-publish`
    signing
}

group = "dev.parsek"
version =
    (if (project.hasProperty("version") && project.findProperty("version") != "unspecified") project.findProperty("version") else "local-build")!!

val pf4jVersion: String by project
val vertxVersion: String by project
val handlebarsVersion: String by project
val gsonVersion: String by project
val springContextVersion: String by project

val bootstrap = (project.findProperty("bootstrap") as String?)?.toBoolean() ?: false
val pluginsDir: File? by rootProject.extra

repositories {
    mavenCentral()
}

dependencies {
    if (bootstrap) {
        listOf(
            ":Parsek" to "Parsek*.jar",
            ":plugins:parsek-plugin-database" to "parsek-plugin-database*.jar",
            ":plugins:parsek-plugin-token" to "parsek-plugin-token*.jar",
            ":plugins:parsek-plugin-mail" to "parsek-plugin-mail*.jar",
            ":plugins:parsek-plugin-system-property" to "parsek-plugin-system-property*.jar"
        ).forEach { (path, pattern) ->
            if (findProject(path) != null) {
                compileOnly(project(path))
            } else {
                compileOnly(fileTree(rootDir) { include(pattern) })
            }
        }
    } else {
        compileOnly("dev.parsek:core:1.0.0-beta.19")
        compileOnly("dev.parsek:parsek-plugin-database:1.0.0-dev.5")
        compileOnly("dev.parsek:parsek-plugin-token:1.0.0-dev.9")
        compileOnly("dev.parsek:parsek-plugin-mail:1.0.0-dev.6")
        compileOnly("dev.parsek:parsek-plugin-system-property:1.0.0-dev.4")
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
    implementation("io.vertx:vertx-web-client:$vertxVersion")

    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    compileOnly("com.google.code.gson:gson:$gsonVersion")

    // https://mvnrepository.com/artifact/com.auth0/java-jwt
    compileOnly("com.auth0:java-jwt:4.4.0")

    // https://mvnrepository.com/artifact/commons-codec/commons-codec
    compileOnly(group = "commons-codec", name = "commons-codec", version = "1.16.0")

    // https://mvnrepository.com/artifact/commons-validator/commons-validator
    compileOnly("commons-validator:commons-validator:1.8.0")

    // https://mvnrepository.com/artifact/org.springframework/spring-context
    compileOnly("org.springframework:spring-context:$springContextVersion")
}

tasks {
    build {
        dependsOn(shadowJar)
        dependsOn("copyJar")
        // Ensure standard jar is built for Maven publishing (stays in build/libs)
        dependsOn(jar)
    }

    shadowJar {
        val pluginId: String by project
        val pluginClass: String by project
        val pluginProvider: String by project
        val pluginDependencies: String by project

        manifest {
            attributes["main-class"] = pluginClass
            attributes["id"] = pluginId
            attributes["version"] = version
            attributes["developer"] = pluginProvider
            attributes["dependencies"] = pluginDependencies
        }

        if (version != "unspecified") {
            archiveFileName.set("$pluginId-v${version}.jar")
        } else {
            archiveFileName.set("$pluginId.jar")
        }

        if (project.gradle.startParameter.taskNames.contains("publish")) {
            archiveFileName.set(archiveFileName.get().lowercase())
        }

        dependencies {
            exclude(dependency("io.vertx:vertx-core"))
            exclude {
                it.moduleGroup == "io.netty" || it.moduleGroup == "org.slf4j"
            }
        }
    }

    register("copyJar") {
        outputs.upToDateWhen { false }
        mustRunAfter(shadowJar)

        pluginsDir?.let {
            doLast {
                copy {
                    from(shadowJar.get().archiveFile.get().asFile.absolutePath)
                    into(it)
                }
            }
        }
    }
}

tasks.named<Jar>("jar") {
    // Keep jar enabled for Maven publishing
    enabled = true
    // Add custom naming: v before version and -api before .jar
    if (version != "unspecified") {
        archiveFileName.set("${rootProject.name}-v${version}-api.jar")
    } else {
        archiveFileName.set("${rootProject.name}-api.jar")
    }
}

java {
    // Use Java 21 for compilation
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }

    withJavadocJar()
    withSourcesJar()
}

// Configure sources and javadoc jars after they are created
tasks.named<Jar>("sourcesJar") {
    // Add custom naming: v before version and -api before .jar
    if (version != "unspecified") {
        archiveFileName.set("${rootProject.name}-v${version}-api-sources.jar")
    } else {
        archiveFileName.set("${rootProject.name}-api-sources.jar")
    }
}

tasks.named<Jar>("javadocJar") {
    // Add custom naming: v before version and -api before .jar
    if (version != "unspecified") {
        archiveFileName.set("${rootProject.name}-v${version}-api-javadoc.jar")
    } else {
        archiveFileName.set("${rootProject.name}-api-javadoc.jar")
    }
}

kotlin {
    jvmToolchain(21) // Ensure Kotlin uses the Java 21 toolchain
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

// Publishing configuration
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "dev.parsek"
            artifactId = "parsek-plugin-auth"
            version = project.version.toString()

            // Use the standard jar task output
            artifact(tasks.named("jar"))
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))

            pom {
                name.set("Parsek Auth Plugin")
                description.set("Auth system for Parsek")
                url.set("https://github.com/ParsekDev/parsek-plugin-auth")
                inceptionYear.set("2025")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("Statu")
                        name.set("Statu")
                        email.set("info@statu.co")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/ParsekDev/parsek-plugin-auth.git")
                    developerConnection.set("scm:git:ssh://github.com/ParsekDev/parsek-plugin-auth.git")
                    url.set("https://github.com/ParsekDev/parsek-plugin-auth")
                }
            }
        }
    }

    repositories {
        maven {
            url = layout.buildDirectory.dir("staging-deploy").get().asFile.toURI()
        }
    }
}

// Signing configuration
signing {
    val signingKey = System.getenv("GPG_PRIVATE_KEY")
    val signingPassword = System.getenv("GPG_PASSPHRASE")

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["maven"])
    }
}

// JReleaser configuration
jreleaser {
    project {
        name.set("parsek-plugin-auth")
        description.set("Auth system for Parsek")
        authors.add("Statu")
        license.set("MIT")
        links {
            homepage.set("https://github.com/ParsekDev/parsek-plugin-auth")
        }
        inceptionYear.set("2025")
    }

    // Configure GitHub release provider (required by JReleaser even for deploy-only)
    release {
        github {
            overwrite.set(false)
            skipTag.set(true)
            skipRelease.set(true)
            changelog {
                enabled.set(false)
            }
        }
    }

    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
    }

    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(org.jreleaser.model.Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}