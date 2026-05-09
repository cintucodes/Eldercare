package com.example.eldercare;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface HealthSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(HealthSnapshot snapshot);

    @Query("SELECT * FROM health_snapshots WHERE uid = :uid ORDER BY timestamp DESC LIMIT 1")
    HealthSnapshot getLatestByUid(String uid);

    @Query("SELECT * FROM health_snapshots ORDER BY timestamp DESC")
    List<HealthSnapshot> getAllSnapshots();

    @Query("DELETE FROM health_snapshots")
    void deleteAll();
}
