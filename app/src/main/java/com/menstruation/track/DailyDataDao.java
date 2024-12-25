package com.menstruation.track;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DailyDataDao {
    @Insert
    void insert(DailyData dailyData);

    @Update
    void update(DailyData dailyData);

    @Query("SELECT * FROM daily_data WHERE date = :date")
    DailyData getByDate(String date);

    @Query("SELECT * FROM daily_data WHERE date = :year || '-' || :month || '-' || :day LIMIT 1")
    DailyData getDailyDataByDate(int year, int month, int day);

    @Query("DELETE FROM daily_data")
    void deleteAll();

    @Query("SELECT * FROM daily_data")
    List<DailyData> getAll();

    @Query("DELETE FROM daily_data WHERE date = :date")
    void deleteByDate(String date);

    @Query("SELECT date FROM daily_data")
    List<String> getAllRecordedDates();

    @Query("SELECT date FROM daily_data WHERE strftime('%Y-%m', date) = :formattedDate")
    List<String> getDatesByMonth(String formattedDate);
}

