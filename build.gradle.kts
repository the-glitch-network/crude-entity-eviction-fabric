/* Copyright (c) 2021 KJP12
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

import java.net.URI

plugins {
  java
  `java-library`
  id("fabric-loom")
  `maven-publish`
  id("com.diffplug.spotless")
  id("io.github.juuxel.loom-quiltflower")
}

val minecraft_version: String by project
val yarn_mappings: String by project
val loader_version: String by project
val fabric_api_version: String by project
val lazydfu_version: String by project
val ledger_version: String by project
val fl_kotlin_version: String by project
val h2_version: String by project
val project_version: String by project

val isPublish = System.getenv("GITHUB_EVENT_NAME") == "release"
val isRelease = System.getenv("BUILD_RELEASE").toBoolean()
val isActions = System.getenv("GITHUB_ACTIONS").toBoolean()
val baseVersion: String = "$project_version+mc.$minecraft_version"

group = "net.kjp12"

version =
  when {
    isRelease -> baseVersion
    isActions ->
      "$baseVersion-build.${System.getenv("GITHUB_RUN_NUMBER")}-commit.${System.getenv("GITHUB_SHA").substring(0, 7)}-branch.${System.getenv("GITHUB_REF")?.substring(11)?.replace('/', '.') ?: "unknown"}"
    else -> "$baseVersion-build.local"
  }

java {
  sourceCompatibility = JavaVersion.VERSION_16
  targetCompatibility = JavaVersion.VERSION_16
}

repositories {
  mavenCentral()
  maven {
    name = "Nexus Repository OSS"
    url = URI("https://oss.sonatype.org/content/repositories/snapshots")
  }
  maven {
    name = "Modrinth"
    url = URI("https://api.modrinth.com/maven")
  }
}

dependencies {
  minecraft("com.mojang", "minecraft", minecraft_version)
  mappings("net.fabricmc", "yarn", yarn_mappings, classifier = "v2")
  include(implementation("com.h2database", "h2", h2_version))
  modImplementation("net.fabricmc", "fabric-loader", loader_version)
  modImplementation("maven.modrinth", "ledger", ledger_version)
  modImplementation(fabricApi.module("fabric-lifecycle-events-v1", fabric_api_version))
  modRuntime("net.fabricmc", "fabric-language-kotlin", fl_kotlin_version)
  modRuntime("net.fabricmc.fabric-api", "fabric-api", fabric_api_version)
  modRuntime("maven.modrinth", "lazydfu", lazydfu_version)
}

spotless {
  val licenseHeader = rootDir.resolve(".internal/license-header.java")
  java {
    importOrderFile(rootDir.resolve(".internal/spotless.importorder"))

    val eclipse = eclipse()
    val eclipseConfig = rootDir.resolve(".internal/spotless.xml")
    if (eclipseConfig.exists()) eclipse.configFile(eclipseConfig)

    licenseHeaderFile(licenseHeader)
  }
  kotlinGradle {
    target("*.gradle.kts")
    ktfmt().googleStyle()
    licenseHeaderFile(licenseHeader, "(import|plugins|rootProject)")
  }
}

tasks {
  withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isDeprecation = true
    options.isWarnings = true
  }
  register<Jar>("sourcesJar") {
    dependsOn("classes")
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
  }
  processResources {
    val map =
      mapOf(
        "version" to project.version,
        "project_version" to project_version,
        "loader_version" to loader_version,
        "minecraft_required" to project.property("minecraft_required")?.toString()
      )
    inputs.properties(map)

    filesMatching("fabric.mod.json") { expand(map) }
  }
  withType<Jar> { from("LICENSE") }
  build { dependsOn(remapSourcesJar) }
  register<Copy>("poolBuilds") {
    dependsOn(build)
    from(remapJar, remapSourcesJar)
    into(buildDir.resolve("pool"))
  }
}
