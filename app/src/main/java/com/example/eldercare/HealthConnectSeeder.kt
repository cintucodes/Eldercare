package com.example.eldercare

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.units.*
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/**
 * HealthConnectSeeder generates and inserts mock health data into Health Connect.
 * Optimized for Health Connect version 1.0.0-alpha11.
 */
class HealthConnectSeeder(private val healthConnectClient: HealthConnectClient) {

    suspend fun seedAllData() {
        val records = mutableListOf<Record>()
        val now = Instant.now()

        for (i in 0..6) {
            val daysAgo = i.toLong()
            val dayStart = now.minus(daysAgo, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS)
            val morning = dayStart.plus(8, ChronoUnit.HOURS)
            val midday = dayStart.plus(12, ChronoUnit.HOURS)
            val evening = dayStart.plus(20, ChronoUnit.HOURS)

            // 1. Steps (4,000–12,000)
            records.add(
                StepsRecord(
                    count = Random.nextLong(4000, 12001),
                    startTime = dayStart,
                    endTime = evening,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC
                )
            )

            // 2. Heart Rate (3 samples/day at 60–100 bpm)
            val heartRateSamples = List(3) { index ->
                HeartRateRecord.Sample(
                    time = morning.plus(index.toLong() * 4, ChronoUnit.HOURS),
                    beatsPerMinute = Random.nextLong(60, 101)
                )
            }
            records.add(
                HeartRateRecord(
                    startTime = morning,
                    endTime = evening,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC,
                    samples = heartRateSamples
                )
            )

            // 3. Sleep (6–9 hours)
            val sleepStart = dayStart.minus(2, ChronoUnit.HOURS) // 10 PM previous night
            val sleepEnd = sleepStart.plus(Random.nextLong(6, 10), ChronoUnit.HOURS)
            records.add(
                SleepSessionRecord(
                    startTime = sleepStart,
                    endTime = sleepEnd,
                    startZoneOffset = ZoneOffset.UTC,
                    endZoneOffset = ZoneOffset.UTC
                )
            )

            // 4. Weight (65–80 kg)
            records.add(
                WeightRecord(
                    time = morning,
                    zoneOffset = ZoneOffset.UTC,
                    weight = Mass.kilograms(Random.nextDouble(65.0, 80.1))
                )
            )

            // 5. Blood Pressure
            records.add(
                BloodPressureRecord(
                    time = midday,
                    zoneOffset = ZoneOffset.UTC,
                    systolic = Pressure.millimetersOfMercury(Random.nextDouble(110.0, 140.0)),
                    diastolic = Pressure.millimetersOfMercury(Random.nextDouble(70.0, 90.0))
                )
            )

            // 6. Blood Glucose
            records.add(
                BloodGlucoseRecord(
                    time = midday.plus(1, ChronoUnit.HOURS),
                    zoneOffset = ZoneOffset.UTC,
                    level = BloodGlucose.millimolesPerLiter(Random.nextDouble(4.0, 8.0))
                )
            )
        }

        healthConnectClient.insertRecords(records)
    }
}
