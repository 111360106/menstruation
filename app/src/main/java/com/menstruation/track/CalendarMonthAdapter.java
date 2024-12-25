package com.menstruation.track;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CalendarMonthAdapter extends RecyclerView.Adapter<CalendarMonthAdapter.MonthViewHolder> {

    private final List<MonthData> months; //保存所有月份的數據
    private List<DayData> days; // 升級為成員變數
    private final OnDateClickListener onDateClickListener;

    public CalendarMonthAdapter(List<MonthData> months, OnDateClickListener onDateClickListener) {
        this.months = months;
        this.onDateClickListener = onDateClickListener;
        this.days = new ArrayList<>();
        for (MonthData month : months) {
            days.addAll(month.getDays()); // 將所有日子匯總
        }
    }

    @NonNull
    @Override
    public MonthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_month, parent, false);
        return new MonthViewHolder(view);
    }  //加載布局 item_month，作為單個月份的容器視圖

    @Override
    public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
        List<DayData> days = months.get(position).getDays(); // 獲取當前月份的所有日期
        MonthData monthData = months.get(position);
        holder.dateContainer.removeAllViews(); // 清空舊的視圖(dateContainer)，避免重複渲染

        TextView monthTitle = new TextView(holder.itemView.getContext());
        monthTitle.setText(String.format("%04d年%02d月", monthData.getYear(), monthData.getMonth())); // 顯示年份和月份
        monthTitle.setTextSize(18);
        monthTitle.setGravity(Gravity.CENTER);
        monthTitle.setPadding(0, 16, 0, 16); // 增加上下間距
        holder.dateContainer.addView(monthTitle);

        for (int i = 0; i < days.size(); i += 7) { // 每次處理一周（7 天）
            LinearLayout weekRow = new LinearLayout(holder.itemView.getContext());
            weekRow.setOrientation(LinearLayout.HORIZONTAL);
            weekRow.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            // 處理一周的日期
            for (int j = 0; j < 7; j++) {
                DayData dayData = days.get(i + j); // 獲取每天的數據
                TextView dayTextView = (TextView) LayoutInflater.from(holder.itemView.getContext())
                        .inflate(R.layout.item_calendar_day, weekRow, false);

                if (dayData.getDay() != 0) { // 非占位符
                    dayTextView.setText(String.valueOf(dayData.getDay()));

                    // 設置背景優先級
                    if (dayData.isSelected() && dayData.hasData()) {
                        dayTextView.setBackgroundResource(R.drawable.bg_selected_and_has_data); // 選中且有數據：黑框 + 粉色背景
                    } else if (dayData.hasData()) {
                        dayTextView.setBackgroundResource(R.drawable.bg_selected_day); // 有數據：粉色背景
                    } else if (dayData.isSelected() && dayData.isOvulationDay()) {
                        dayTextView.setBackgroundResource(R.drawable.bg_selected_and_has_data_blue); // 選中且為排卵日：黑框 + 藍色背景
                    } else if (dayData.isOvulationDay()) {
                        dayTextView.setBackgroundResource(R.drawable.btn_rounded_blue); // 排卵日：藍色背景
                    } else if (dayData.isSelected() && dayData.isPredictedPeriod()) {
                        dayTextView.setBackgroundResource(R.drawable.bg_selected_and_predicted); // 選中且預測月經日：黑框 + 虛線背景
                    } else if (dayData.isPredictedPeriod()) {
                        dayTextView.setBackgroundResource(R.drawable.bg_predicted_period); // 預測月經日：虛線背景
                    } else if (dayData.isSelected()) {
                        dayTextView.setBackgroundResource(R.drawable.bg_selected_circle); // 僅選中：黑框
                    } else {
                        dayTextView.setBackground(null); // 無背景
                    }

                    // 動態縮小背景
                    dayTextView.setScaleX(0.7f); // 縮小寬度比例
                    dayTextView.setScaleY(0.7f); // 縮小高度比例
                } else {
                    dayTextView.setText(""); // 空白占位符
                    dayTextView.setBackground(null);
                }

                // 點擊事件
                dayTextView.setOnClickListener(v -> {
                    if (dayData.getDay() != 0) {
                        notifyDateSelection(dayData); // 更新選中狀態
                    }
                });

                weekRow.addView(dayTextView); // 添加到該行
            }

            holder.dateContainer.addView(weekRow); // 添加整周
        }
    }

    public void updateDaysBackground(int year, int month, int day, boolean hasData, boolean isPredictedPeriod, boolean isOvulationDay, boolean isSelected) {
        for (DayData dayData : days) {
            if (dayData.getYear() == year && dayData.getMonth() == month && dayData.getDay() == day) {
                dayData.setHasData(hasData);
                dayData.setPredictedPeriod(isPredictedPeriod);
                dayData.setOvulationDay(isOvulationDay);
                dayData.setSelected(isSelected);
                notifyDataSetChanged();
                break;
            }
        }
    } //根據年份、月份和日期更新背景屬性。

    public void updateDayBackground(int year, int month, int day, boolean hasData) {
        for (MonthData monthData : months) {
            if (monthData.getYear() == year && monthData.getMonth() == month) {
                for (DayData dayData : monthData.getDays()) {
                    if (dayData.getDay() == day) {
                        dayData.setHasData(hasData);
                        notifyItemChanged(months.indexOf(monthData));
                        return;
                    }
                }
            }
        }
    } //僅更新單個日期的 hasData 屬性。

    public void updateOvulationDays(Set<String> ovulationDays) {
        for (MonthData monthData : months) {
            for (DayData dayData : monthData.getDays()) {
                String formattedDate = String.format("%04d-%02d-%02d", dayData.getYear(), dayData.getMonth(), dayData.getDay());
                dayData.setOvulationDay(ovulationDays.contains(formattedDate));
            }
        }
        notifyDataSetChanged();
    } //批量設置排卵日

    public MonthData getFirstMonthData() {
        if (months == null || months.isEmpty()) return null;
        return months.get(0);
    } //返回第一個月份的數據。

    public void addPreviousMonths(List<MonthData> newMonths) {
        if (newMonths == null || newMonths.isEmpty()) return;
        months.addAll(0, newMonths); // 添加到列表的開頭
        notifyItemRangeInserted(0, newMonths.size()); // 通知 RecyclerView
    }

    @Override
    public int getItemCount() {
        return months.size();
    }

    private void notifyDateSelection(DayData selectedDay) {
        for (DayData day : days) {
            day.setSelected(day == selectedDay);
        }
        notifyDataSetChanged(); // 刷新所有項目

        if (onDateClickListener != null) {
            onDateClickListener.onDateClick(selectedDay.getYear(), selectedDay.getMonth(), selectedDay.getDay());
        }
    }

    public void setSelectedDate(int year, int month, int day) {
        for (MonthData monthData : months) {
            for (DayData dayData : monthData.getDays()) {
                dayData.setSelected(dayData.getYear() == year && dayData.getMonth() == month && dayData.getDay() == day);
            }
        }
        notifyDataSetChanged(); // 刷新所有月份視圖
    }

    public void updateMonths(List<MonthData> updatedMonths) {
        for (int i = 0; i < updatedMonths.size(); i++) {
            if (i < months.size()) {
                if (!months.get(i).equals(updatedMonths.get(i))) {
                    months.set(i, updatedMonths.get(i));
                    notifyItemChanged(i); // 僅通知必要的變更
                }
            } else {
                months.add(updatedMonths.get(i));
                notifyItemInserted(i);
            }
        }

        // 刪除多餘數據
        while (months.size() > updatedMonths.size()) {
            months.remove(months.size() - 1);
            notifyItemRemoved(months.size());
        }
    } //根據新的月份數據批量更新。

    public void updateSingleMonth(MonthData updatedMonth) {
        for (int i = 0; i < months.size(); i++) {
            MonthData existingMonth = months.get(i);
            if (existingMonth.getYear() == updatedMonth.getYear() && existingMonth.getMonth() == updatedMonth.getMonth()) {
                months.set(i, updatedMonth); // 替換該月份數據
                notifyItemChanged(i); // 僅通知該項更新
                break;
            }
        }
    } //僅更新特定月份。

    public int getMonthPosition(int year, int month) {
        for (int i = 0; i < months.size(); i++) {
            MonthData monthData = months.get(i);
            if (monthData.getYear() == year && monthData.getMonth() == month) {
                return i;
            }
        }
        return -1; // 找不到時返回 -1
    } //根據年份和月份獲取索引位置。

    public void clearAll() {
        for (MonthData monthData : months) {
            for (DayData dayData : monthData.getDays()) {
                dayData.setHasData(false);
                dayData.setOvulationDay(false);
                dayData.setSelected(false);
            }
        }
        notifyDataSetChanged();
    } //清除所有數據 (選中狀態、排卵日等)。

    public void updatePredictedPeriodDays(Set<String> predictedPeriodDays) {
        for (MonthData monthData : months) {
            for (DayData dayData : monthData.getDays()) {
                String formattedDate = String.format("%04d-%02d-%02d", dayData.getYear(), dayData.getMonth(), dayData.getDay());
                if (predictedPeriodDays.contains(formattedDate)) {
                    dayData.setPredictedPeriod(true); // 設置虛線背景
                } else {
                    dayData.setPredictedPeriod(false);
                }
            }
        }
        notifyDataSetChanged();
    } //批量設置預測月經日。


    /* 確保舊的背景清除，新的背景正確應用 */
    public void clearOvulationDays() {
        for (MonthData monthData : months) {
            for (DayData dayData : monthData.getDays()) {
                dayData.setOvulationDay(false);
            }
        }
        notifyDataSetChanged();
    } //清除所有排卵日標記。

    public static class MonthViewHolder extends RecyclerView.ViewHolder {
        TextView monthTitle;
        LinearLayout dateContainer;

        public MonthViewHolder(@NonNull View itemView) {
            super(itemView);
            monthTitle = itemView.findViewById(R.id.monthTitle); // 月份標題
            dateContainer = itemView.findViewById(R.id.dateContainer); // 日期容器,每個月的日期視圖
        }
    }
}

