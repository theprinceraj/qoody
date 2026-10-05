package com.qoody.shared.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** How long upstream flows stay active after the last UI subscriber leaves (survives rotation). */
private const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L

/** Standard conversion from a cold upstream to the [StateFlow] a ViewModel exposes to its screen. */
fun <T> Flow<T>.stateInViewModel(
    scope: CoroutineScope,
    initialValue: T,
): StateFlow<T> = stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS), initialValue)
