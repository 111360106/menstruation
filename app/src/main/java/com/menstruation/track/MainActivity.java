package com.menstruation.track;

import android.icu.util.Calendar;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;

import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private CalendarMonthAdapter adapter; // 將 adapter 提升為成員變數
    private RecyclerView calendarRecyclerView;
    private int selectedYear;
    private int selectedMonth;
    private int selectedDay;
    private int cycleLength = 28; // 月經週期默認值
    private int periodLength = 5; // 月經天數默認值
    private int averageCycleLength = -1; // 默認為 -1 表示未計算，平均月經週期
    private int averagePeriodLength = -1; // 默認為 -1 表示未計算，平均月經天數


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom_mood);
        // 初始化 RecyclerView，用於顯示日曆

        ImageButton btnChart = findViewById(R.id.btn_chart);
        ImageButton btnSettings = findViewById(R.id.btn_settings);
        FloatingActionButton btnAdd = findViewById(R.id.btn_add);

        // 初始化 RecyclerView
        // 初始化 RecyclerView，確保使用類成員變數
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView);
        calendarRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchRecordedDates(recordedDateSet -> {
            List<String> periodStartDates = calculatePeriodStartDates(recordedDateSet);

            // 更新排卵日和取得預測月經資料
            //updateOvulationDays(periodStartDates);
            calculatePredictedPeriodDays(recordedDateSet);

            // 初始化週期長度
            AppDatabase db = AppDatabase.getInstance(this);
            new Thread(() -> {
                Settings settings = db.settingsDao().getSettings();
                if (settings != null) {
                    // 初始化平均值
                    cycleLength = settings.getCycleLength(); // 從設定中加載
                    periodLength = settings.getPeriodLength(); // 從設定中加載
                }
            }).start();

            // 獲取當前日期
            Calendar today = Calendar.getInstance();
            selectedYear = today.get(Calendar.YEAR);
            selectedMonth = today.get(Calendar.MONTH) + 1; // Calendar 的月份從 0 開始
            selectedDay = today.get(Calendar.DAY_OF_MONTH);

            // 生成多月數據
            List<MonthData> months = generateMultiMonthData(selectedYear, selectedMonth, recordedDateSet);

            // 初始化適配器，在 CalendarMonthAdapter 的點擊回調中添加資料檢查與按鈕更新
            adapter = new CalendarMonthAdapter(months, (year, month, day) -> {
                selectedYear = year;
                selectedMonth = month;
                selectedDay = day;

                updateSelectedDate(year, month, day); // 更新頂部顯示
                adapter.setSelectedDate(year, month, day); // 更新選中狀態
                checkAndUpdateAddButton(); // 新增檢查資料並更新按鈕圖示
            });
            calendarRecyclerView.setAdapter(adapter);

            // 設置當前日期選中
            adapter.setSelectedDate(selectedYear, selectedMonth, selectedDay);
            updateSelectedDate(selectedYear, selectedMonth, selectedDay);

            // 滾動到當前月份
            int currentMonthIndex = 0;
            for (int i = 0; i < months.size(); i++) {
                if (months.get(i).getYear() == selectedYear && months.get(i).getMonth() == selectedMonth) {
                    currentMonthIndex = i;
                    break;
                }
            }
            calendarRecyclerView.scrollToPosition(currentMonthIndex);

            // 更新頂部日期
            updateSelectedDate(selectedYear, selectedMonth, selectedDay);

            // 設置滾動監聽器
            calendarRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (!recyclerView.canScrollVertically(-1)) { // 檢查是否滾動到頂部
                        loadPreviousMonths(recordedDateSet); // 加載之前的月份
                    }
                }
            });

            updatePredictionsAndUI();  //更新排卵、預測月經資料
            showDailyTipDialog(); //飲食提醒
        });

        btnChart.setOnClickListener(v -> {
            // 處理月經週期資料按鈕點擊事件
            // 在點擊按鈕時重新計算統計數據
            new Thread(() -> {
                showStatisticsDialog(); // 獲取平均天數預測 計算統計數據
                //runOnUiThread(this::updateStatisticsUI); // 更新統計 UI
            }).start();
        });

        btnSettings.setOnClickListener(v -> {
            // 處理設定按鈕點擊事件
            showSettingsDialog();
        });

        btnAdd.setOnClickListener(v -> {
            // 處理新增資料按鈕點擊事件
            showAddRecordDialog();
        });

        // 檢查當前日期的資料並更新按鈕圖示
        checkAndUpdateAddButton();
    }

    // 更新日期顯示
    private void updateSelectedDate(int year, int month, int day) {
        TextView selectedDateTextView = findViewById(R.id.tv_selected_date);
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(year, month - 1, day);
        String dayOfWeek = getDayOfWeek(selectedDate.get(Calendar.DAY_OF_WEEK));
        selectedDateTextView.setText(String.format("%d/%02d/%02d (%s)", year, month, day, dayOfWeek));
    }



  