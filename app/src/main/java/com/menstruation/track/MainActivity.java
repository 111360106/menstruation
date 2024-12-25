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

    // 檢查是否有資料並更新按鈕圖示及顯示文字
    private void checkAndUpdateAddButton() {
        AppDatabase db = AppDatabase.getInstance(this);

        new Thread(() -> {
            String date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay);
            DailyData existingData = db.dailyDataDao().getByDate(date);
            List<String> recordedDates = db.dailyDataDao().getAllRecordedDates();

            // 正常處理有資料的情況
            List<LocalDate> periodStartDates = getPeriodStartDates(new HashSet<>(recordedDates));
            Set<String> predictedPeriodDays = calculatePredictedPeriodDays(new HashSet<>(recordedDates));
            Set<String> ovulationDays = calculateOvulationDays(periodStartDates);

            boolean isPredictedPeriod = predictedPeriodDays.contains(date);
            boolean isOvulationDay = ovulationDays.contains(date);

            runOnUiThread(() -> {
                FloatingActionButton btnAdd = findViewById(R.id.btn_add);
                TextView tvHint = findViewById(R.id.tv_hint);

                if (existingData != null) {
                    // 當天有資料，顯示月經量
                    btnAdd.setImageResource(android.R.drawable.ic_menu_edit);
                    if (existingData.temperature > 0) {
                        tvHint.setText("月經量：" + existingData.menstruationLevel + "  體溫：" + existingData.temperature);
                    } else {
                        tvHint.setText("月經量：" + existingData.menstruationLevel);
                    }
                    tvHint.setVisibility(View.VISIBLE);
                } else if (isPredictedPeriod) {
                    // 預估月經日
                    btnAdd.setImageResource(android.R.drawable.ic_input_add);
                    tvHint.setText("預估月經日");
                    tvHint.setVisibility(View.VISIBLE);
                } else if (isOvulationDay) {
                    // 排卵日
                    btnAdd.setImageResource(android.R.drawable.ic_input_add);
                    tvHint.setText("排卵日（高懷孕機率）");
                    tvHint.setVisibility(View.VISIBLE);
                } else {
                    // 無資料
                    btnAdd.setImageResource(android.R.drawable.ic_input_add);
                    tvHint.setVisibility(View.GONE);
                }
                btnAdd.setImageTintList(getResources().getColorStateList(android.R.color.white, null));
            });
        }).start();
    }

    private void showDailyTipDialog() {
        AppDatabase db = AppDatabase.getInstance(this);

        new Thread(() -> {
            String date = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay);
            DailyData existingData = db.dailyDataDao().getByDate(date);

            runOnUiThread(() -> {
                // 載入布局

                // 初始化 Dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                AlertDialog dialog = builder.create();

                // 綁定 UI 元素

                // 根據當天狀態設定內容
                if (existingData != null) {
                    // 當天有資料
                    dialog.show();

                    // 按鈕關閉功能
                } else {
                    //tipContent.setText("今天沒有月經紀錄，請保持健康的生活習慣！");
                }
            });
        }).start();
    }

    private List<MonthData> generateMultiMonthData(int startYear, int startMonth, Set<String> recordedDateSet) {
        List<MonthData> months = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, startYear);
        calendar.set(Calendar.MONTH, startMonth - 1); // 月份從0開始

        // 預生成兩年的數據
        for (int i = 0; i < 24; i++) { // 預設生成24個月
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH) + 1;
            months.add(new MonthData(year, month, generateMonthData(year, month, recordedDateSet)));
            calendar.add(Calendar.MONTH, 1); // 向後推一個月
            Log.d("DataDebug", "Generated month: " + year + "-" + month);
        }
        return months;
    } //日曆數據生成

    private String getDayOfWeek(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.SUNDAY:
                return "日";
            case Calendar.MONDAY:
                return "一";
            case Calendar.TUESDAY:
                return "二";
            case Calendar.WEDNESDAY:
                return "三";
            case Calendar.THURSDAY:
                return "四";
            case Calendar.FRIDAY:
                return "五";
            case Calendar.SATURDAY:
                return "六";
            default:
                return "";
        }
    }

    private List<DayData> generateMonthData(int year, int month, Set<String> recordedDateSet) {
        List<DayData> days = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1); // 設置為該月份的第一天
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH); // 獲取該月份的天數
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // 獲取該月份第一天是星期幾

        // 添加空白占位符（使日曆排列正確）
        for (int i = 1; i < firstDayOfWeek; i++) {
            days.add(new DayData(year, month, 0, false,false,false, false)); // 使用 day=0 作為占位符
        }
        // 添加實際日期
        for (int day = 1; day <= daysInMonth; day++) {
            boolean isToday = (year == selectedYear && month == selectedMonth && day == selectedDay);
            boolean hasData = recordedDateSet.contains(String.format("%04d-%02d-%02d", year, month, day));
            days.add(new DayData(year, month, day, hasData, false, isToday, false));
        }
        // 補充空白占位符，讓最後一行補齊
        int remainingDays = 7 - (days.size() % 7);
        if (remainingDays < 7) {
            for (int i = 0; i < remainingDays; i++) {
                days.add(new DayData(year, month, 0, false,false,false, false));
            }
        }
        return days;
    }  //每日數據生成

    private void loadPreviousMonths(Set<String> recordedDateSet) {
        if (adapter == null) return;

        // 獲取現有數據的最早月份
        MonthData firstMonthData = adapter.getFirstMonthData();
        if (firstMonthData == null) return;

        int year = firstMonthData.getYear();
        int month = firstMonthData.getMonth();

        // 計算前12個月
        List<MonthData> newMonths = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1); // 月份從0開始
        for (int i = 0; i < 12; i++) { // 加載前12個月
            calendar.add(Calendar.MONTH, -1);
            int newYear = calendar.get(Calendar.YEAR);
            int newMonth = calendar.get(Calendar.MONTH) + 1;
            newMonths.add(0, new MonthData(newYear, newMonth, generateMonthData(newYear, newMonth, recordedDateSet))); // 插入到前面
        }

        // 更新適配器數據
        adapter.addPreviousMonths(newMonths);

        // 確保滾動到正確的位置
        LinearLayoutManager layoutManager = (LinearLayoutManager) calendarRecyclerView.getLayoutManager();
        if (layoutManager != null) {
            layoutManager.scrollToPositionWithOffset(newMonths.size(), 0); // 偏移量保持不變
        }

        // 設置當前日期選中
        adapter.setSelectedDate(selectedYear, selectedMonth, selectedDay);
        updateSelectedDate(selectedYear, selectedMonth, selectedDay);
    }

    private void showAddRecordDialog() {
        // 加載自定義視圖
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_record, null);

        // 初始化視圖中的元素
        TextView tvSelectedDate = dialogView.findViewById(R.id.tv_selected_date);
        tvSelectedDate.setText("選擇日期：" + selectedYear + "/" + selectedMonth + "/" + selectedDay);

        RadioGroup rgMenstruationFlow = dialogView.findViewById(R.id.rg_menstruation_flow);
        LinearLayout llSymptoms = dialogView.findViewById(R.id.ll_symptoms);
        LinearLayout llMoods = dialogView.findViewById(R.id.ll_moods);
        EditText etTemperature = dialogView.findViewById(R.id.et_temperature);
        EditText etNotes = dialogView.findViewById(R.id.et_notes);
        Button btnDelete = dialogView.findViewById(R.id.btn_delete);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);

        AppDatabase db = AppDatabase.getInstance(this);

        // 查詢當天紀錄
        new Thread(() -> {
            DailyData existingData = db.dailyDataDao().getByDate(
                    String.format("%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay)
            );
            runOnUiThread(() -> {
                if (existingData != null) {
                    // 初始化畫面資料
                    if (existingData.menstruationLevel != null) {
                        for (int i = 0; i < rgMenstruationFlow.getChildCount(); i++) {
                            RadioButton radioButton = (RadioButton) rgMenstruationFlow.getChildAt(i);
                            if (radioButton.getText().toString().equals(existingData.menstruationLevel)) {
                                radioButton.setChecked(true);
                                break;
                            }
                        }
                    }
                    if (existingData.symptoms != null) {
                        for (int i = 0; i < llSymptoms.getChildCount(); i++) {
                            CheckBox checkBox = (CheckBox) llSymptoms.getChildAt(i);
                            if (existingData.symptoms.contains(checkBox.getText().toString())) {
                                checkBox.setChecked(true);
                            }
                        }
                    }
                    if (existingData.moods != null) {
                        for (int i = 0; i < llMoods.getChildCount(); i++) {
                            CheckBox checkBox = (CheckBox) llMoods.getChildAt(i);
                            if (existingData.moods.contains(checkBox.getText().toString())) {
                                checkBox.setChecked(true);
                            }
                        }
                    }
                    if (existingData.temperature != 0) {
                        etTemperature.setText(String.valueOf(existingData.temperature));
                    }
                    etNotes.setText(existingData.notes);
                }
                // 啟用刪除按鈕
                btnDelete.setEnabled(existingData != null);
            });
        }).start();

        // 動態新增心情選項
        String[] moods = {"愉悅", "平靜", "開心", "一般", "疲憊", "生氣", "悲傷", "不穩定", "消沉", "緊張", "焦慮"};
        for (String mood : moods) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(mood);
            llMoods.addView(checkBox);
        }

        // 動態新增症狀選項
        String[] symptoms = {"健康", "頭痛", "腹痛", "倦怠", "噁心", "抽筋", "背疼", "失眠", "胸部敏感", "腫脹", "便秘", "腹瀉", "發熱", "發冷"};
        for (String symptom : symptoms) {
            CheckBox checkBox = new CheckBox(this);
            checkBox.setText(symptom);
            llSymptoms.addView(checkBox);
        }

        // 彈出對話框
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // 確認按鈕邏輯
        dialogView.findViewById(R.id.btn_confirm).setOnClickListener(v -> {
            // 檢查必填項
            int selectedFlowId = rgMenstruationFlow.getCheckedRadioButtonId();
            if (selectedFlowId == -1) {
                Toast.makeText(this, "請選擇月經量！", Toast.LENGTH_SHORT).show();
                return;
            }

            // 獲取選擇的流量
            String flow = ((RadioButton) dialogView.findViewById(selectedFlowId)).getText().toString();

            // 獲取症狀與心情
            List<String> symptomsList = new ArrayList<>();
            for (int i = 0; i < llSymptoms.getChildCount(); i++) {
                CheckBox checkBox = (CheckBox) llSymptoms.getChildAt(i);
                if (checkBox.isChecked()) {
                    symptomsList.add(checkBox.getText().toString());
                }
            }

            List<String> moodsList = new ArrayList<>();
            for (int i = 0; i < llMoods.getChildCount(); i++) {
                CheckBox checkBox = (CheckBox) llMoods.getChildAt(i);
                if (checkBox.isChecked()) {
                    moodsList.add(checkBox.getText().toString());
                }
            }

            // 獲取體溫
            String temperatureInput = etTemperature.getText().toString();
            float temperature = 0.0f; // 預設為 0（未輸入）
            if (!temperatureInput.isEmpty())
            {
                try {
                    temperature = Float.parseFloat(temperatureInput);
                    if (temperature < 35.0 || temperature > 41.9) {
                        Toast.makeText(this, "請輸入 35.0 至 41.9 的有效體溫範圍！", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "請輸入正確的體溫格式！", Toast.LENGTH_SHORT).show();
                    return;
                }
            }  //新增資料--->體溫部分

            // 獲取備註
            String notes = etNotes.getText().toString();

            // 保存或更新資料
            saveOrUpdateRecord(selectedYear, selectedMonth, selectedDay, flow, symptomsList, moodsList, temperature, notes);

            // 關閉對話框
            dialog.dismiss();
        });

        // 刪除按鈕邏輯，日常記錄，並更新日曆界面與按鈕狀態
        btnDelete.setOnClickListener(v -> {
            new Thread(() -> {
                db.dailyDataDao().deleteByDate(
                        String.format("%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay)
                );
                runOnUiThread(() -> {
                    Toast.makeText(this, "資料已刪除！", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    checkAndUpdateAddButton(); // 更新按鈕圖示
                    refreshCalendar(selectedYear, selectedMonth); // 僅更新選中的月份

                    updateDayBackground(selectedYear, selectedMonth, selectedDay);
                    fetchRecordedDates(recordedDateSet -> {
                        // 更新排卵期，並在完成後刷新畫面
                        runOnUiThread(() -> updatePredictionsAndUI()); //更新排卵、預測月經資料
                    });
                });

            }).start();
        });

        // 關閉按鈕邏輯
        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    // 在新增資料時更新按鈕圖示
    private void saveOrUpdateRecord(int year, int month, int day, String flow, List<String> symptoms, List<String> moods, float temperature, String notes) {
        AppDatabase db = AppDatabase.getInstance(this);
        new Thread(() -> {
            String date = String.format("%04d-%02d-%02d", year, month, day);
            DailyData existingData = db.dailyDataDao().getByDate(date);

            if (existingData == null) {
                // 新增資料
                DailyData newData = new DailyData();
                newData.date = date;
                newData.menstruationLevel = flow;
                newData.symptoms = String.join(",", symptoms);
                newData.moods = String.join(",", moods);
                newData.temperature = temperature; // 無輸入時為 0
                newData.notes = notes;

                db.dailyDataDao().insert(newData);
            } else {
                // 更新資料
                existingData.menstruationLevel = flow;
                existingData.symptoms = String.join(",", symptoms);
                existingData.moods = String.join(",", moods);
                existingData.temperature = temperature; // 無輸入時為 0
                existingData.notes = notes;

                db.dailyDataDao().update(existingData);
            }

            runOnUiThread(() -> {
                Toast.makeText(this, "資料已保存！", Toast.LENGTH_SHORT).show();
                refreshCalendar(selectedYear, selectedMonth); // 僅刷新當月日曆
                checkAndUpdateAddButton();
                fetchRecordedDates(recordedDateSet -> {
                    //List<String> periodStartDates = calculatePeriodStartDates(recordedDateSet);
                    //updateOvulationDays(periodStartDates); // 更新排卵日
                    updatePredictionsAndUI();  //更新排卵、預測月經資料
                });
            });
        }).start();
    }  //新增或更新數據:接收來自用戶界面輸入的數據（如月經量、症狀、心情等），檢查數據是否已存在於數據庫中：

    private void showSettingsDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null);

        // 初始化 NumberPicker
        NumberPicker npPeriodLength = dialogView.findViewById(R.id.np_period_length);
        NumberPicker npCycleLength = dialogView.findViewById(R.id.np_cycle_length);

        npPeriodLength.setMinValue(1);
        npPeriodLength.setMaxValue(30);
        npCycleLength.setMinValue(20);
        npCycleLength.setMaxValue(90);

        // 加載設定值
        AppDatabase db = AppDatabase.getInstance(this);
        new Thread(() -> {
            Settings settings = db.settingsDao().getSettings();
            runOnUiThread(() -> {
                if (settings != null) {
                    npPeriodLength.setValue(settings.getPeriodLength());
                    npCycleLength.setValue(settings.getCycleLength());
                } else {
                    npPeriodLength.setValue(5);
                    npCycleLength.setValue(28);
                }
            });
        }).start();

        // 彈出對話框
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // 綁定清除資料按鈕
        Button btnClearAllData = dialogView.findViewById(R.id.btn_clear_all_data);
        btnClearAllData.setOnClickListener(v -> {
            showClearAllDataDialog(dialog); // 傳遞對話框實例
        });

        dialogView.findViewById(R.id.btn_save).setOnClickListener(v -> {
            int periodLength = npPeriodLength.getValue();
            int cycleLength = npCycleLength.getValue();
            saveSettings(periodLength, cycleLength);
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void saveSettings(int periodLength, int cycleLength) {
        AppDatabase db = AppDatabase.getInstance(this);
        if (periodLength > 0 && cycleLength > 0) {
            new Thread(() -> {
                Settings settings = new Settings();
                settings.setId(1); // 確保更新固定 ID 的記錄
                settings.setPeriodLength(periodLength);
                settings.setCycleLength(cycleLength);
                db.settingsDao().insertOrUpdate(settings);
                Log.d("MainActivity", "Settings saved: Period = " + periodLength + ", Cycle = " + cycleLength);

                // 更新畫面
                fetchRecordedDates(recordedDateSet -> {
                    this.cycleLength = cycleLength; // 更新全局變數
                    this.periodLength = periodLength; // 更新全局變數

                    Toast.makeText(this, "設定已保存！", Toast.LENGTH_SHORT).show();
                    // 即時更新畫面和排卵期
                    //refreshCalendarWithNewSettings();
                    updatePredictionsAndUI();  //更新排卵、預測月經資料
                });
            }).start();
        } else {
            Log.e("MainActivity", "NumberPicker is null. Cannot save settings.");
        }
    }  //設置週期與經期:儲存用戶自定義的週期與經期長度，並即時更新日曆和預測結果

    // 顯示清除資料確認對話框
    private void showClearAllDataDialog(AlertDialog parentDialog) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("清除資料")
                .setMessage("您確定要清除所有資料嗎？此操作無法恢復！")
                .setPositiveButton("確定", null) // 暫時設置為 null，稍後設定邏輯
                .setNegativeButton("取消", null)
                .create();

        // 設置正向按鈕行為
        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                // 執行清除資料
                clearAllData(() -> {
                    dialog.dismiss(); // 清除資料後關閉彈窗
                    if (parentDialog != null) {
                        parentDialog.dismiss(); // 關閉設定視窗
                    }
                    Toast.makeText(this, "所有資料已清除！", Toast.LENGTH_SHORT).show();
                });
            });
        });

        dialog.show();
    }

    // 清除所有資料
    private void clearAllData(Runnable onComplete) {
        AppDatabase db = AppDatabase.getInstance(this);

        new Thread(() -> {
            db.dailyDataDao().deleteAll(); // 清空每日資料表
            //db.settingsDao().deleteAll(); // 清空設定資料表

            runOnUiThread(() -> {
                Set<String> emptySet = new HashSet<>();
                // 清空適配器數據
                if (adapter != null) {
                    adapter.clearAll(); // 清除UI上的所有背景
                    adapter.updatePredictedPeriodDays(emptySet); // 清空預測月經日
                    adapter.updateOvulationDays(emptySet); // 清空排卵日
                    adapter.notifyDataSetChanged(); // 確保畫面清空
                }

                // 確保畫面同步
                fetchRecordedDates(recordedDateSet -> {
                    updatePredictionsAndUI(); // 更新畫面和預測數據
                    checkAndUpdateAddButton();
                });

                // 執行回呼
                if (onComplete != null) {
                    onComplete.run();
                }
            });
            //refreshCalendar(selectedYear, selectedMonth); // 僅更新選中的月份
        }).start();
    } //刪除所有數據記錄並刷新界面

    /* 呼叫顯示數據分析視窗！ */
    private void showStatisticsDialog() {
        // 在主執行緒初始化對話框和其佈局
        runOnUiThread(() -> {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_statistics, null);

            TextView averageCycleTextView = dialogView.findViewById(R.id.averageCycleLength);
            TextView averagePeriodTextView = dialogView.findViewById(R.id.averagePeriodLength);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("統計資料")
                    .setView(dialogView)
                    .setPositiveButton("關閉", null)
                    .create();

            new Thread(() -> {
                // 從資料庫中獲取數據
                AppDatabase db = AppDatabase.getInstance(this);
                List<String> recordedDates = db.dailyDataDao().getAllRecordedDates();

                if (recordedDates.size() < 2) {
                    runOnUiThread(() -> {
                        averageCycleTextView.setText("資料過少");
                        averagePeriodTextView.setText("資料過少");

                        new AlertDialog.Builder(this)
                                .setTitle("統計")
                                .setMessage("資料不足。請新增更多資料。")
                                .setPositiveButton("關閉", null)
                                .show();
                    });
                    return;
                } //記錄數量小於 2，顯示「資料過少」的警告

                // 計算數據
                List<LocalDate> periodStartDates = getPeriodStartDates(new HashSet<>(recordedDates)); //計算歷史月經開始日
                List<Integer> cycleLengths = calculateCycleLengths(periodStartDates); //計算各月經週期的天數
                List<Integer> periodLengths = calculatePeriodLengths(recordedDates); //計算各次月經的持續天數

                // 計算平均值
                float averageCycle = calculateAverage(cycleLengths);
                float averagePeriod = calculateAverage(periodLengths);

                // 更新 UI
                runOnUiThread(() -> {
                    averageCycleTextView.setText(averageCycle > 0 ? averageCycle + " 天" : "資料不足");
                    averagePeriodTextView.setText(averagePeriod > 0 ? averagePeriod + " 天" : "資料不足");
                    dialog.show();
                });
            }).start();
        });
    }

    /* 平均資料 */
    private float calculateAverage(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0; // 避免除以零
        }
        int sum = 0;
        for (int value : values) {
            sum += value;
        }
        return (float) sum / values.size();
    }

    private List<Integer> calculateCycleLengths(List<LocalDate> periodStartDates) {
        List<Integer> cycleLengths = new ArrayList<>();
        if (periodStartDates.size() < 2) {
            return cycleLengths; // 如果資料不足，返回空列表
        }
        for (int i = 1; i < periodStartDates.size(); i++) {
            int cycleLength = (int) ChronoUnit.DAYS.between(periodStartDates.get(i - 1), periodStartDates.get(i));
            if (cycleLength > 0) { // 確保周期長度為正
                cycleLengths.add(cycleLength);
            }
        }
        return cycleLengths;
    }

    private List<Integer> calculatePeriodLengths(List<String> recordedDates) {
        List<LocalDate> sortedDates = recordedDates.stream()
                .map(LocalDate::parse)
                .sorted()
                .collect(Collectors.toList());
        List<Integer> periodLengths = new ArrayList<>();
        int currentPeriodLength = 1;

        for (int i = 1; i < sortedDates.size(); i++) {
            if (sortedDates.get(i).minusDays(1).equals(sortedDates.get(i - 1))) {
                currentPeriodLength++;
            } else {
                periodLengths.add(currentPeriodLength);
                currentPeriodLength = 1;
            }
        }
        if (currentPeriodLength > 1) {
            periodLengths.add(currentPeriodLength);
        }
        return periodLengths;
    }

    private void refreshCalendar(int year, int month) {
        if (calendarRecyclerView == null || adapter == null) return;

        // 獲取更新的單月數據
        fetchRecordedDates(recordedDateSet -> {
            MonthData updatedMonth = new MonthData(year, month, generateMonthData(year, month, recordedDateSet));

            // 更新適配器中特定月份數據
            adapter.updateSingleMonth(updatedMonth);

            // 保持滾動位置
            RecyclerView.LayoutManager layoutManager = calendarRecyclerView.getLayoutManager();
            if (layoutManager instanceof LinearLayoutManager) {
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
                linearLayoutManager.scrollToPositionWithOffset(adapter.getMonthPosition(year, month), 0);
            }

            //Log.d("ScrollDebug", "Restoring position: " + firstVisibleItemPosition + ", offset: " + offset);
        });
    }

    private void fetchRecordedDates(DataCallback callback) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<String> recordedDates = db.dailyDataDao().getAllRecordedDates();
            Set<String> recordedDateSet = new HashSet<>(recordedDates);

            runOnUiThread(() -> callback.onDataFetched(recordedDateSet));
        }).start();
    }

    public interface DataCallback {
        void onDataFetched(Set<String> recordedDates);
    }

    /* 計算每次月經的第一天並標記 */
    private List<String> calculatePeriodStartDates(Set<String> recordedDateSet) {
        List<String> sortedDates = new ArrayList<>(recordedDateSet);
        Collections.sort(sortedDates); // 排序日期

        List<String> periodStartDates = new ArrayList<>();

        for (int i = 0; i < sortedDates.size(); i++) {
            if (i == 0) {
                // 第一天一定是新週期的開始
                periodStartDates.add(sortedDates.get(i));
            } else {
                LocalDate current = LocalDate.parse(sortedDates.get(i));
                LocalDate previous = LocalDate.parse(sortedDates.get(i - 1));

                // 判斷日期是否不連續（即差距超過一天）
                if (ChronoUnit.DAYS.between(previous, current) > 3) {
                    periodStartDates.add(sortedDates.get(i));
                }
            }
        }

        Log.d("OvulationDaysDebug", "Period Start Dates: " + periodStartDates);
        return periodStartDates;
    }

    /* 更新單日背景標記 */
    private void updateDayBackground(int year, int month, int day) {
        if (adapter == null) return;

        String date = String.format("%04d-%02d-%02d", year, month, day);
        AppDatabase db = AppDatabase.getInstance(this);

        new Thread(() -> {
            DailyData dailyData = db.dailyDataDao().getByDate(date);
            runOnUiThread(() -> {
                boolean hasData = dailyData != null;
                adapter.updateDayBackground(year, month, day, hasData);
            });
        }).start();
    }

    /* 更新畫面以包含所有排卵期 */
    private void updateOvulationDays(List<String> periodStartDates) {
        Set<String> ovulationDays = new HashSet<>();
        Set<String> predictedPeriodDays = new HashSet<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        try {
            for (String periodStartDate : periodStartDates) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(dateFormat.parse(periodStartDate));

                // 計算排卵日（週期第 (cycleLength - 14) 天）
                calendar.add(Calendar.DAY_OF_YEAR, cycleLength - 14);

                for (int j = 0; j < 5; j++) { // 排卵期持續 5 天
                    String ovulationDay = dateFormat.format(calendar.getTime());
                    if (!predictedPeriodDays.contains(ovulationDay)) { // 不與預測月經重疊
                        ovulationDays.add(ovulationDay);
                    }
                    calendar.add(Calendar.DAY_OF_YEAR, 1); // 下一天
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // 更新背景顯示
        if (adapter != null) {
            adapter.updateOvulationDays(ovulationDays);
        }

        Log.d("OvulationDaysDebug", "Ovulation Days: " + ovulationDays);
    }

    private Set<String> calculatePredictedPeriodDays(Set<String> recordedDates) {
        Set<String> predictedPeriodDays = new HashSet<>();
        List<LocalDate> periodStartDates = getPeriodStartDates(recordedDates);
        int cycleLengthN = cycleLength;


        if (!periodStartDates.isEmpty()) {
            // 以最後的月經開始日進行預測
            LocalDate lastPeriodStart = !periodStartDates.isEmpty() ?
                    periodStartDates.get(periodStartDates.size() - 1) :
                    LocalDate.now().minusDays(cycleLengthN);

            for (int i = 1; i <= 3; i++) { // 預測 3 個月
                LocalDate nextPeriodStart = lastPeriodStart.plusDays(cycleLengthN * i);

                // 添加預測期間內的日期
                for (int j = 0; j < periodLength; j++) { // 動態控制月經持續天數
                    predictedPeriodDays.add(nextPeriodStart.plusDays(j).toString());
                }
            }
        }

        return predictedPeriodDays;
    }  //月經日期預測:算未來 3 個月的月經日期範圍

    private Set<String> calculateOvulationDays(List<LocalDate> periodStartDates) {
        Set<String> ovulationDays = new HashSet<>();
        if (periodStartDates.isEmpty()) return ovulationDays;

        for (LocalDate startDate : periodStartDates) {
            LocalDate ovulationStart = startDate.plusDays(cycleLength - 14); // 排卵日估算
            for (int j = 0; j < 5; j++) { // 排卵期為 5 天
                ovulationDays.add(ovulationStart.plusDays(j).toString());
            }
        }

        return ovulationDays;
    }  //排卵期計算:基於週期長度，計算每次月經的排卵期（周期中間減去 14 天）

    private void updatePredictionsAndUI() {
        fetchRecordedDates(recordedDates -> {
            if (recordedDates.isEmpty()) {
                // 如果無任何資料，清空預測日並刷新 UI
                adapter.updatePredictedPeriodDays(new HashSet<>());
                adapter.updateOvulationDays(new HashSet<>());
                adapter.notifyDataSetChanged();
                return;
            }

            // 正常處理有資料的情況
            List<LocalDate> periodStartDates = getPeriodStartDates(recordedDates);
            Set<String> predictedPeriodDays = calculatePredictedPeriodDays(recordedDates);
            Set<String> ovulationDays = calculateOvulationDays(periodStartDates);

            runOnUiThread(() -> {
                adapter.updatePredictedPeriodDays(predictedPeriodDays);
                adapter.updateOvulationDays(ovulationDays);
                adapter.notifyDataSetChanged();
            });

            checkAndUpdateAddButton();
        });
    }

    private List<LocalDate> getPeriodStartDates(Set<String> recordedDates) {
        List<LocalDate> periodStartDates = new ArrayList<>();

        // 將 `recordedDates` 轉換為 LocalDate 並排序
        List<LocalDate> sortedDates = recordedDates.stream()
                .map(LocalDate::parse)
                .sorted()
                .collect(Collectors.toList());

        // 遍歷日期並找出月經開始日（第一天）
        for (int i = 0; i < sortedDates.size(); i++) {
            if (i == 0 || !sortedDates.get(i).minusDays(1).equals(sortedDates.get(i - 1))) {
                // 如果與前一天不連續，則為新的一次月經開始日
                periodStartDates.add(sortedDates.get(i));
            }
        }
        Log.d("PeriodPrediction", "Predicted Period Dayss: " + periodStartDates);
        return periodStartDates;
    }
}
