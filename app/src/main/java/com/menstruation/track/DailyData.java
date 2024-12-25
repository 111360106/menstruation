package com.menstruation.track;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "daily_data")
public class DailyData {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String date; // 日期 (yyyy-MM-dd)
    public String menstruationLevel; // 月經量 (無、些許量、正常量、量多)
    public String symptoms; // 症狀 (以逗號分隔的多選值)
    public String moods; // 心情 (以逗號分隔的多選值)
    public float temperature; // 當天體溫
    @ColumnInfo(name = "notes")
    public String notes; // 筆記
}

