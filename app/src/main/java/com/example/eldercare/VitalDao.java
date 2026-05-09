package com.example.eldercare;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface VitalDao {
    @Insert
    void insert(VitalEntity vital);

    @Query("SELECT * FROM vitals WHERE syncedToFirestore = 0")
    List<VitalEntity> getUnsynced();

    @Update
    void update(VitalEntity vital);

    @Query("SELECT * FROM vitals WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    List<VitalEntity> getRecentVitals(String userId, int limit);
}
