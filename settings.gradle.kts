/* Copyright (c) 2021 KJP12
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

rootProject.name = "crude-entity-eviction"

pluginManagement {
  repositories {
    maven {
      name = "Fabric"
      url = java.net.URI("https://maven.fabricmc.net/")
    }
    maven {
      name = "Cotton"
      url = java.net.URI("https://server.bbkr.space/artifactory/libs-release")
    }
    gradlePluginPortal()
  }
  plugins {
    id("fabric-loom") version System.getProperty("loom_version")!!
    id("com.diffplug.spotless") version System.getProperty("spotless_version")!!
    id("com.modrinth.minotaur") version System.getProperty("minotaur_version")!!
    id("io.github.juuxel.loom-quiltflower") version System.getProperty("quiltflower_version")!!
  }
}
