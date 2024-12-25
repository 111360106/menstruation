package com.menstruation.track;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;

@Database(entities = {DailyData.class, Settings.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase
{
    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context)
    {
        if (instance == null)
        {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "menstruation_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }

    public abstract SettingsDao settingsDao();
    public abstract DailyDataDao dailyDataDao();
}
//負責：
//定義一個 Room 資料庫，儲存和管理月經追蹤應用程式中的數據。
//提供單例設計模式，確保資料庫實例的唯一性。
//定義 DAO 介面，提供與資料表進行操作的方法。
//支援資料庫版本升級（Migration），新增了資料表的欄位，確保資料結構的變更不會摧毀現有數據（除了 fallbackToDestructiveMigration 情況）。


