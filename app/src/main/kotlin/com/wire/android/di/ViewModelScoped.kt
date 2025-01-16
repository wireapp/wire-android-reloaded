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

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.sebaslogen.resaca.hilt.hiltViewModelScoped
import dev.ahmedmourad.bundlizer.Bundlizer
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * Returns a proper scoped arguments instance from the given [SavedStateHandle] for the scoped [ViewModel].
 */
inline fun <reified R : ScopedArgs> SavedStateHandle.scopedArgs(): R =
    scopedArgs(R::class, this)

/**
 * Returns a proper scoped arguments instance from the given [SavedStateHandle].
 *
 * @param argsClass the class of the arguments, must implement [ScopedArgs] and be serializable
 * @param argsContainer the [SavedStateHandle] to get the arguments from
 */
@OptIn(InternalSerializationApi::class)
fun <R : ScopedArgs> scopedArgs(argsClass: KClass<R>, argsContainer: SavedStateHandle): R =
    Bundlizer.unbundle(argsClass.serializer(), argsContainer.toBundle())

/**
 * Custom implementation of [hiltViewModelScoped] that takes proper scoped serializable arguments that implement [ScopedArgs]
 * and provides them into scoped [ViewModel] converting it automatically to [Bundle] using [Bundlizer].
 *
 * [ViewModel] needs to implement an interface annotated with [ViewModelPreview] and with default
 * implementations.
 *
 * Proper key will be taken from the [ScopedArgs.key] property.
 *
 * @param arguments The arguments that will be provided to the [ViewModel], must implement [ScopedArgs] and be serializable
 */
@OptIn(InternalSerializationApi::class)
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
@Composable
inline fun <reified T, reified S, reified R : ScopedArgs> hiltViewModelScoped(arguments: R): S where T : ViewModel, T : S = when {
    LocalInspectionMode.current -> ViewModelPreviews.firstNotNullOf { it as? S }
    try {
        Class.forName("androidx.test.espresso.Espresso")
        true
    } catch (e: ClassNotFoundException) {
        false
    } -> ViewModelPreviews.firstNotNullOf { it as? S }
    else -> hiltViewModelScoped<T>(key = arguments.key, defaultArguments = Bundlizer.bundle(R::class.serializer(), arguments))
}

/**
 * Creates a [Bundle] with all key-values from the given [SavedStateHandle].
 */
@Suppress("SpreadOperator")
fun SavedStateHandle.toBundle(): Bundle = bundleOf(*(keys().map { it to get<Any>(it) }.toTypedArray()))

/**
 * Interface for arguments for scoped ViewModels.
 * It is used to provide a unique key for the scoped ViewModel.
 */
interface ScopedArgs {
    val key: Any?
}
