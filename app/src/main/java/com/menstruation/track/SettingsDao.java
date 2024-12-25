package com.menstruation.track;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(Settings settings);

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    Settings getSettings();

    @Query("DELETE FROM Settings")
    void deleteAll();
}

