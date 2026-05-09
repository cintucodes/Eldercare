package com.example.eldercare;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface PendingSosAlertDao {
    @Insert
    void insert(PendingSosAlert alert);

    @Query("SELECT * FROM pending_sos_alerts")
    List<PendingSosAlert> getAllPending();

    @Query("DELETE FROM pending_sos_alerts")
    void deleteAll();
}
