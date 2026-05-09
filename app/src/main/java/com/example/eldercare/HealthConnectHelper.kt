package com.example.eldercare

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import kotlinx.coroutines.runBlocking

/**
 * Helper object to bridge Java and Kotlin for Health Connect operations.
 */
object HealthConnectHelper {
    @JvmStatic
    fun seedData(context: Context) {
        try {
            val client = HealthConnectClient.getOrCreate(context)
            val seeder = HealthConnectSeeder(client)
            runBlocking {
                seeder.seedAllData()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
