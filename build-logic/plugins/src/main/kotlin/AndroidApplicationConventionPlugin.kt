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
import com.android.build.api.dsl.ApplicationExtension
import com.wire.android.gradle.configureAndroidKotlinTests
import com.wire.android.gradle.configureCompose
import com.wire.android.gradle.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        with(pluginManager) {
            apply("com.android.application")
            apply("org.jetbrains.kotlin.android")
            apply("org.jetbrains.kotlin.plugin.compose")
        }

        extensions.configure<ApplicationExtension> {
            // TODO: Handle flavors. Currently implemented in `variants.gradle.kts` script

            namespace = AndroidApp.id
            configureKotlinAndroid(this)

            val isFDroidRelease = (project.properties["isFDroidRelease"] as? String)?.toBoolean() ?: false

            defaultConfig {
                AndroidApp.setRootDir(project.projectDir)

                val resolvedVersionName = if (isFDroidRelease) {
                    AndroidApp.versionName
                } else {
                    "${AndroidApp.versionName}-${AndroidApp.leastSignificantVersionCode}"
                }

                applicationId = AndroidApp.id
                defaultConfig.targetSdk = AndroidSdk.target
                versionCode = AndroidApp.versionCode
                versionName = resolvedVersionName
                setProperty("archivesBaseName", "$applicationId-v$versionName")
            }

            configureCompose(this)

            packaging {
                resources {
                    pickFirsts.add("META-INF/AL2.0")
                    pickFirsts.add("META-INF/LGPL2.1")
                    excludes.add("MANIFEST.MF")
                    excludes.add("LICENSE.txt")
                    excludes.add("META-INF/DEPENDENCIES")
                    excludes.add("META-INF/ASL2.0")
                    excludes.add("META-INF/NOTICE")
                    excludes.add("META-INF/licenses/ASM")
                    excludes.add("META-INF/versions/9/previous-compilation-data.bin")
                }
            }

            configureAndroidKotlinTests()
        }
    }
}
