package com.example.eldercare

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.*
import java.time.Instant
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.function.Consumer

/**
 * HealthConnectManager handles all interactions with the Health Connect SDK.
 * Optimized for Health Connect version 1.0.0-alpha11.
 */
class HealthConnectManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var healthConnectClient: HealthConnectClient? = null

    interface HealthConnectCallback<T> {
        fun onSuccess(result: T)
        fun onError(e: Exception)
    }

    fun isHealthConnectAvailable(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.google.android.apps.healthdata", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun getClient(): HealthConnectClient? {
        if (healthConnectClient == null) {
            try {
                if (isHealthConnectAvailable()) {
                    healthConnectClient = HealthConnectClient.getOrCreate(context)
                }
            } catch (e: Exception) {
                Log.e("HealthConnect", "Error getting client: ${e.message}")
            }
        }
        return healthConnectClient
    }

    // --- Data Reading Methods ---

    suspend fun readSteps(startTime: Instant, endTime: Instant): Long {
        val client = getClient() ?: return 0
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records.sumOf { it.count }
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading steps: ${e.message}")
            0
        }
    }

    suspend fun readHeartRate(startTime: Instant, endTime: Instant): Int {
        val client = getClient() ?: return 0
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            if (response.records.isEmpty()) return 0
            // Return latest heart rate
            response.records.last().samples.last().beatsPerMinute.toInt()
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading heart rate: ${e.message}")
            0
        }
    }

    suspend fun readBloodPressure(startTime: Instant, endTime: Instant): Pair<Int, Int>? {
        val client = getClient() ?: return null
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = BloodPressureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            if (response.records.isEmpty()) return null
            val last = response.records.last()
            Pair(last.systolic.inMillimetersOfMercury.toInt(), last.diastolic.inMillimetersOfMercury.toInt())
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading blood pressure: ${e.message}")
            null
        }
    }

    suspend fun readBloodGlucose(startTime: Instant, endTime: Instant): Double {
        val client = getClient() ?: return 0.0
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = BloodGlucoseRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            if (response.records.isEmpty()) return 0.0
            response.records.last().level.inMillimolesPerLiter
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading blood glucose: ${e.message}")
            0.0
        }
    }

    suspend fun readSleep(startTime: Instant, endTime: Instant): Double {
        val client = getClient() ?: return 0.0
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            var totalDuration = Duration.ZERO
            for (record in response.records) {
                totalDuration = totalDuration.plus(Duration.between(record.startTime, record.endTime))
            }
            totalDuration.toMinutes() / 60.0
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error reading sleep: ${e.message}")
            0.0
        }
    }

    // --- Sync Logic ---

    fun syncAllToFirestore(userId: String) {
        scope.launch {
            val endTime = Instant.now()
            val startTime = endTime.minus(24, ChronoUnit.HOURS)

            val steps = async { readSteps(startTime, endTime) }
            val heartRate = async { readHeartRate(startTime, endTime) }
            val bp = async { readBloodPressure(startTime, endTime) }
            val glucose = async { readBloodGlucose(startTime, endTime) }
            val sleep = async { readSleep(startTime, endTime) }

            val stepsVal = steps.await()
            val hrVal = heartRate.await()
            val bpVal = bp.await()
            val glucoseVal = glucose.await()
            val sleepVal = sleep.await()

            saveVitalsEntry(userId, stepsVal, hrVal, glucoseVal, sleepVal, bpVal)
            saveIndividualRecords(userId, stepsVal, hrVal, glucoseVal, sleepVal, bpVal)
        }
    }

    private fun saveVitalsEntry(userId: String, steps: Long, hr: Int, glucose: Double, sleep: Double, bp: Pair<Int, Int>?) {
        val db = FirebaseFirestore.getInstance()
        val entry = hashMapOf(
            "elderId" to userId,
            "type" to "vitals_entry",
            "steps" to steps,
            "heartRate" to hr,
            "bloodGlucose" to glucose,
            "sleepHours" to sleep,
            "systolicBP" to (bp?.first ?: 0),
            "diastolicBP" to (bp?.second ?: 0),
            "timestamp" to Timestamp.now()
        )

        db.collection("users").document(userId).collection("vitals")
            .add(entry)
            .addOnSuccessListener { Log.d("HealthConnect", "Summary vitals saved") }
    }

    private fun saveIndividualRecords(userId: String, steps: Long, hr: Int, glucose: Double, sleep: Double, bp: Pair<Int, Int>?) {
        val db = FirebaseFirestore.getInstance()
        val vitalsRef = db.collection("users").document(userId).collection("vitals")

        if (steps > 0) saveTypeRecord(vitalsRef, userId, "steps", steps.toDouble())
        if (hr > 0) saveTypeRecord(vitalsRef, userId, "heartRate", hr.toDouble())
        if (glucose > 0) saveTypeRecord(vitalsRef, userId, "bloodGlucose", glucose)
        if (sleep > 0) saveTypeRecord(vitalsRef, userId, "sleepHours", sleep)
        if (bp != null) {
            saveTypeRecord(vitalsRef, userId, "systolicBP", bp.first.toDouble())
            saveTypeRecord(vitalsRef, userId, "diastolicBP", bp.second.toDouble())
        }
    }

    private fun saveTypeRecord(ref: com.google.firebase.firestore.CollectionReference, userId: String, type: String, value: Double) {
        val record = hashMapOf(
            "elderId" to userId,
            "type" to type,
            "value" to value,
            "timestamp" to Timestamp.now()
        )
        ref.add(record)
    }

    // --- Permission Handlers ---

    fun redirectToInstallHealthConnect() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
            setPackage("com.android.vending")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error: ${e.message}")
        }
    }

    fun requestPermissionLauncher(activity: AppCompatActivity, onResult: Consumer<Boolean>): ActivityResultLauncher<Set<String>> {
        return activity.registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
            if (granted.containsAll(PERMISSIONS)) {
                onResult.accept(true)
            } else {
                onResult.accept(false)
            }
        }
    }

    fun requestPermissions(launcher: ActivityResultLauncher<Set<String>>) {
        launcher.launch(PERMISSIONS)
    }

    suspend fun checkPermissions(): Set<String> {
        val client = getClient() ?: return emptySet()
        return try {
            client.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error checking permissions: ${e.message}")
            emptySet()
        }
    }

    fun checkPermissionsAsync(callback: HealthConnectCallback<Set<String>>) {
        scope.launch {
            try {
                val granted = checkPermissions()
                callback.onSuccess(granted)
            } catch (e: Exception) {
                callback.onError(e)
            }
        }
    }

    companion object {
        @JvmField
        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getWritePermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(BloodPressureRecord::class),
            HealthPermission.getWritePermission(BloodPressureRecord::class),
            HealthPermission.getReadPermission(BloodGlucoseRecord::class),
            HealthPermission.getWritePermission(BloodGlucoseRecord::class),
            HealthPermission.getReadPermission(BodyTemperatureRecord::class),
            HealthPermission.getWritePermission(BodyTemperatureRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getWritePermission(StepsRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getWritePermission(WeightRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getWritePermission(SleepSessionRecord::class)
        )
    }
}
