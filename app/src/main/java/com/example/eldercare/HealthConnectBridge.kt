package com.example.eldercare

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * HealthConnectBridge provides a Java-friendly API for Health Connect suspend functions.
 */
object HealthConnectBridge {

    private val scope = CoroutineScope(Dispatchers.IO)

    interface SuccessCallback<T> {
        fun onSuccess(result: T)
    }

    interface ErrorCallback {
        fun onError(e: Exception)
    }

    @JvmStatic
    fun readSteps(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
        callback: SuccessCallback<Long>,
        onError: ErrorCallback
    ) {
        scope.launch {
            try {
                val request = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
                val response = client.readRecords(request)
                val totalSteps = response.records.sumOf { it.count }
                withContext(Dispatchers.Main) {
                    callback.onSuccess(totalSteps)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError.onError(e)
                }
            }
        }
    }

    @JvmStatic
    fun readHeartRate(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
        callback: SuccessCallback<ReadRecordsResponse<HeartRateRecord>>,
        onError: ErrorCallback
    ) {
        scope.launch {
            try {
                val request = ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
                val response = client.readRecords(request)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError.onError(e)
                }
            }
        }
    }

    @JvmStatic
    fun readBloodPressure(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
        callback: SuccessCallback<ReadRecordsResponse<BloodPressureRecord>>,
        onError: ErrorCallback
    ) {
        scope.launch {
            try {
                val request = ReadRecordsRequest(
                    recordType = BloodPressureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
                val response = client.readRecords(request)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError.onError(e)
                }
            }
        }
    }

    @JvmStatic
    fun readBloodGlucose(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
        callback: SuccessCallback<ReadRecordsResponse<BloodGlucoseRecord>>,
        onError: ErrorCallback
    ) {
        scope.launch {
            try {
                val request = ReadRecordsRequest(
                    recordType = BloodGlucoseRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
                val response = client.readRecords(request)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError.onError(e)
                }
            }
        }
    }

    @JvmStatic
    fun readBodyTemperature(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
        callback: SuccessCallback<ReadRecordsResponse<BodyTemperatureRecord>>,
        onError: ErrorCallback
    ) {
        scope.launch {
            try {
                val request = ReadRecordsRequest(
                    recordType = BodyTemperatureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
                val response = client.readRecords(request)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError.onError(e)
                }
            }
        }
    }

    @JvmStatic
    fun readWeight(
        client: HealthConnectClient,
        start: Instant,
        end: Instant,
        callback: SuccessCallback<ReadRecordsResponse<WeightRecord>>,
        onError: ErrorCallback
    ) {
        scope.launch {
            try {
                val request = ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
                val response = client.readRecords(request)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError.onError(e)
                }
            }
        }
    }
}
