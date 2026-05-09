package com.example.eldercare;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MedicationDao {
    @Insert
    void insert(MedicationEntity medication);

    @Query("SELECT * FROM medications WHERE syncedToFirestore = 0")
    List<MedicationEntity> getUnsynced();

    @Update
    void update(MedicationEntity medication);
}
