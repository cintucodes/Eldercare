package com.example.eldercare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

class ElderHealthViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    
    private val _elderUid = MutableStateFlow<String?>(null)
    private val _metricType = MutableStateFlow("heartRate")
    private val _daysRange = MutableStateFlow(7)

    fun setElder(uid: String?) { _elderUid.value = uid }
    fun setMetric(type: String) { _metricType.value = type }
    fun setDaysRange(days: Int) { _daysRange.value = days }

    @OptIn(ExperimentalCoroutinesApi::class)
    val healthData: StateFlow<List<HealthPoint>> = combine(_elderUid, _metricType, _daysRange) { uid, type, days ->
        Triple(uid, type, days)
    }.flatMapLatest { (uid, type, days) ->
        if (uid == null) return@flatMapLatest flowOf(emptyList<HealthPoint>())
        
        callbackFlow {
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -days)
            val startDate = cal.time

            val subscription = db.collection("users").document(uid).collection("vitals")
                .whereEqualTo("type", type)
                .addSnapshotListener { value, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    val points = value?.mapNotNull { doc ->
                        val ts = doc.getTimestamp("timestamp")
                        val valObj = doc.get("value")
                        if (ts != null && valObj is Number) {
                            val recordDate = ts.toDate()
                            if (recordDate.after(startDate)) {
                                HealthPoint(recordDate.time, valObj.toFloat())
                            } else null
                        } else null
                    }?.sortedBy { it.time } ?: emptyList()
                    trySend(points)
                }
            awaitClose { subscription.remove() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    data class HealthPoint(val time: Long, val value: Float)
}
