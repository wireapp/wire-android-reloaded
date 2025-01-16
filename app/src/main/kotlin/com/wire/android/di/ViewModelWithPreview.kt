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

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel

/**
 * Custom implementation of [hiltViewModel] that provides dedicated object by default for compose Preview.
 *
 * [ViewModel] needs to implement an interface annotated with [ViewModelPreview] and with default
 * implementations.
 */
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
@Composable
inline fun <reified VMI, reified VM> hiltViewModelWithPreview(
    key: String? = null
): VM where VMI : ViewModel, VMI : VM = when {
    LocalInspectionMode.current -> ViewModelPreviews.firstNotNullOf { it as? VM }
    try {
        Class.forName("androidx.test.espresso.Espresso")
        true
    } catch (e: ClassNotFoundException) {
        false
    } -> ViewModelPreviews.firstNotNullOf { it as? VM }
    else -> hiltViewModel<VMI>(key = key)
}

/**
 * Custom implementation of [hiltViewModel] for AssistedInject that provides dedicated object by default for compose Preview.
 *
 * [ViewModel] needs to implement an interface annotated with [ViewModelPreview] and with default
 * implementations.
 */
@Suppress("BOUNDS_NOT_ALLOWED_IF_BOUNDED_BY_TYPE_PARAMETER")
@Composable
inline fun <reified VMI, reified VM, reified VMF> hiltViewModelWithPreview(
    key: String? = null,
    noinline creationCallback: (VMF) -> VMI
): VM where VMI : ViewModel, VMI : VM = when {
    LocalInspectionMode.current -> ViewModelPreviews.firstNotNullOf { it as? VM }
    try {
        Class.forName("androidx.test.espresso.Espresso")
        true
    } catch (e: ClassNotFoundException) {
        false
    } -> ViewModelPreviews.firstNotNullOf { it as? VM }
    else -> hiltViewModel<VMI, VMF>(key = key, creationCallback = creationCallback)
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ViewModelPreview
