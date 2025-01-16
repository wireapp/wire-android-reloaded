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
package com.wire.android.di

import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.validate

class ViewModelPreviewProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        ViewModelPreviewProcessor(environment.codeGenerator)
}

internal class ViewModelPreviewProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val viewModelPreviews: List<KSClassDeclaration> = resolver
            .getSymbolsWithAnnotation("com.wire.android.di.ViewModelPreview")
            .filterIsInstance<KSClassDeclaration>()
            .toList()
        if (!viewModelPreviews.iterator().hasNext()) return emptyList()
        viewModelPreviews.forEach { preview ->
            require(preview.classKind == ClassKind.INTERFACE) {
                    "ViewModelScopedPreview can only be applied to interfaces, " +
                            "but ${preview.qualifiedName?.asString()} is a ${preview.classKind}"
            }
            require(!preview.getAllFunctions().any(KSFunctionDeclaration::isAbstract)) {
                    "ViewModelPreview can only be applied to interfaces with default implementations, " +
                            "but ${preview.qualifiedName?.asString()} has abstract functions"
            }
            require(!preview.getAllProperties().any(KSPropertyDeclaration::isAbstract)) {
                    "ViewModelPreview can only be applied to interfaces with default implementations, " +
                            "but ${preview.qualifiedName?.asString()} has abstract properties"
            }
            createObjectFile(preview)
        }
        createListFile(viewModelPreviews)
        return (viewModelPreviews).filterNot { it.validate() }.toList()
    }

    private fun KSClassDeclaration.previewName() = "${this.simpleName.asString()}Preview"

    private fun createObjectFile(item: KSClassDeclaration) {
        val packageName = item.packageName.asString()
        val name = item.previewName()
        val content = "package $packageName\n\n" +
                "import ${item.qualifiedName?.asString()}\n\n" +
                "data object $name : ${item.simpleName.asString()}"
        val dependencies = Dependencies(aggregating = true, *listOfNotNull(item.containingFile).toTypedArray())
        codeGenerator.createNewFile(dependencies, packageName, name, "kt")
            .use { it.write(content.toByteArray()) }
    }

    private fun createListFile(items: List<KSClassDeclaration>) {
        if (!items.iterator().hasNext()) return
        val packageName = "com.wire.android.di"
        val name = "ViewModelPreviews"
        val content = "package $packageName\n\n" +
                items.joinToString("\n") { "import ${it.packageName.asString()}.${it.previewName()}" } + "\n\n" +
                "val $name = listOf(\n\t" + items.joinToString(",\n\t") { it.previewName() } + "\n)"
        val dependencies = Dependencies(aggregating = true, *items.mapNotNull { it.containingFile }.toTypedArray())
        codeGenerator.createNewFile(dependencies, packageName, name, "kt")
        .use { it.write(content.toByteArray()) }
    }
}
