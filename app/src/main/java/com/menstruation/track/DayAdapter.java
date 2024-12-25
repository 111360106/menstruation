package com.menstruation.track;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DayAdapter extends RecyclerView.Adapter<DayAdapter.DayViewHolder> {

    private final List<DayData> days;  //儲存所有日期資料的清單，每個項目代表一個 DayData。
    private final OnDateClickListener onDateClickListener;  //用來處理日期點擊的回調。

    // 建構函式
    public DayAdapter(List<DayData> days, OnDateClickListener onDateClickListener) {
        this.days = days;
        this.onDateClickListener = onDateClickListener;
    } //初始化日期清單和點擊監聽器。

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        DayData dayData = days.get(position);

        if (dayData.getDay() == 0) {
            holder.dayTextView.setText("");
            holder.dayTextView.setClickable(false);
            holder.dayTextView.setBackground(null);  //清空文字，設置為不可點擊，且背景為空。
        } else {
            holder.dayTextView.setText(String.valueOf(dayData.getDay()));
            holder.dayTextView.setClickable(true);  //日期文字，允許點擊。

            if (dayData.isSelected()) {
                holder.dayTextView.setBackgroundResource(R.drawable.bg_selected_circle);
            } else {
                holder.dayTextView.setBackground(null);
            }

            holder.itemView.setOnClickListener(v -> {
                notifyDateSelection(dayData);   //設置點擊處理邏輯，調用 notifyDateSelection 更新選中狀態。
            });
        }
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    // 更新選中狀態
    private void notifyDateSelection(DayData selectedDay) {
        for (DayData dayData : days) {
            dayData.setSelected(dayData == selectedDay); // 更新選中狀態
        }

        if (onDateClickListener != null) {
            onDateClickListener.onDateClick(selectedDay.getYear(), selectedDay.getMonth(), selectedDay.getDay());
        }  //回傳點擊的日期資訊。

        notifyDataSetChanged(); // 刷新 RecyclerView
    }

    // ViewHolder 類別
    static class DayViewHolder extends RecyclerView.ViewHolder {
        TextView dayTextView;

        DayViewHolder(@NonNull View itemView) {
            super(itemView);
            dayTextView = itemView.findViewById(R.id.dayText); // 綁定 ID
        }
    }
}
