package com.example.eldercare

import com.google.firebase.firestore.ListenerRegistration
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton to track all active Firestore listeners to ensure they are removed on logout.
 */
object ListenerRegistry {
    private val listeners = ConcurrentHashMap<String, ListenerRegistration>()

    fun register(key: String, registration: ListenerRegistration) {
        // Remove existing listener with same key if it exists
        listeners[key]?.remove()
        listeners[key] = registration
    }

    fun unregister(key: String) {
        listeners.remove(key)?.remove()
    }

    fun removeAll() {
        listeners.values.forEach { it.remove() }
        listeners.clear()
    }
}
