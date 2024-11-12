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

package scripts

import findVersion
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    id("com.android.application") apply false
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    val detektVersion = findVersion("detekt").requiredVersion
    detekt("io.gitlab.arturbosch.detekt:detekt-cli:$detektVersion")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:$detektVersion")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:$detektVersion")
    detektPlugins("com.wire:detekt-rules:1.0.0-1.23.6") {
        isChanging = true
    }
}

// Detekt Configuration
val detektAll by tasks.registering(Detekt::class) {
    group = "Quality"
    description = "Runs a detekt code analysis ruleset on the Wire Android codebase"
    parallel = true
    buildUponDefaultConfig = true

    val outputFile = "$buildDir/staticAnalysis/index.html"

    setSource(files(rootDir))
    config.setFrom("$rootDir/config/detekt/detekt.yml")

    include("**/*.kt")
    exclude("**/*.kts", "**/build/**", "/buildSrc", "/kalium", "/template")

    baseline.set(file("$rootDir/config/detekt/baseline.xml"))

    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(file(outputFile))
        txt.required.set(false)
    }

    val reportFile = "Static Analysis Report: $outputFile \n"
    doFirst { println(reportFile) }
    doLast { println(reportFile) }
}

tasks.withType(DetektCreateBaselineTask::class) {
    description = "Overrides current baseline."
    buildUponDefaultConfig.set(true)
    ignoreFailures.set(true)
    parallel.set(true)
    setSource(files(rootDir))
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline.set(file("$rootDir/config/detekt/baseline.xml"))

    include("**/*.kt")
    exclude("**/*.kts", "**/build/**", "/buildSrc", "/kalium", "/template")
}

tasks.register("staticCodeAnalysis") {
    description = "Analyses code within the Wire Android codebase"
    dependsOn(detektAll)
}

tasks.register("testCoverage") {
    group = "Quality"
    description = "Reports code coverage on tests within the Wire Android codebase."
    dependsOn("koverXmlReport")
}

//configurations.matching { it.name == "detekt" }.all {
//    resolutionStrategy.eachDependency {
//        if (requested.group == "org.jetbrains.kotlin") {
//            useVersion("1.9.23")
//        }
//    }
//}
