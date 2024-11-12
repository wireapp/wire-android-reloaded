/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

buildscript {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
        maven(url = "https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath(libs.hilt.gradlePlugin)
        val fdroidBuild = (System.getenv("flavor")
            ?: System.getenv("FLAVOR")
            ?: System.getenv("CUSTOM_FLAVOR")
            ?: gradle.startParameter.taskRequests.toString())
            .lowercase()
            .contains("fdroid")

        if (fdroidBuild) {
            println("Not including gms")
        } else {
            println("Including gms")
            classpath(libs.googleGms.gradlePlugin)
        }
        classpath(libs.aboutLibraries.gradlePlugin)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    }
}

plugins {
    id(ScriptPlugins.infrastructure)
    alias(libs.plugins.ksp) apply false // https://github.com/google/dagger/issues/3965
    alias(libs.plugins.compose.compiler) apply false
}

