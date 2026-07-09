package com.notikeep.data.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-process source of truth for whether the system currently has our
 * NotificationListenerService bound. Written only by the service itself
 * (onListenerConnected/Disconnected); read by health checks and the rebinder.
 *
 * Kept in memory on purpose: the service lives in the same process, and a fresh
 * process start means "not connected yet" — exactly the default we want.
 */
@Singleton
class ListenerConnectionState @Inject constructor() {

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    val isConnected: Boolean get() = _connected.value

    fun onConnected() {
        _connected.value = true
    }

    fun onDisconnected() {
        _connected.value = false
    }
}
