package com.menstruation.track;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<DayData> days; //儲存所有要顯示的日期資料。
    private final OnDayClickListener onDayClickListener; //自定義的點擊監聽器，用於通知外部點擊了哪個日期。
    private int selectedPosition = -1; //記錄當前選中的項目位置，預設為 -1（未選中任何項目）。

    // 建構函式
    public CalendarAdapter(List<DayData> days, OnDayClickListener onDayClickListener) {
        this.days = days;
        this.onDayClickListener = onDayClickListener;
    } //接收一個日期列表和一個點擊監聽器，初始化資料

    // 返回項目數量
    @Override
    public int getItemCount() {
        return days.size();
    } //返回資料列表的大小，告訴 RecyclerView 要顯示多少個項目

    // 創建 ViewHolder
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view);
    } // 作用：1.使用 LayoutInflater 將 item_calendar_day 佈局檔案轉換成一個 View。
      //      2.建立 CalendarViewHolder，用於管理每個項目的視圖。

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        CalendarViewHolder calendarViewHolder = (CalendarViewHolder) holder;
        DayData day = days.get(holder.getAdapterPosition()); // 使用 getAdapterPosition 獲取位置設定日期文字
        calendarViewHolder.dayTextView.setText(String.valueOf(day.getDay()));

        // 清除背景，避免重複套用
        calendarViewHolder.dayTextView.setBackground(null);

        // 設置選中樣式
        if (holder.getAdapterPosition() == selectedPosition) {
            calendarViewHolder.dayTextView.setBackgroundResource(R.drawable.bg_selected_circle); // 黑色圓框
        }  //如果當前項目的位置等於 selectedPosition，則套用選中背景（bg_selected_circle）。

        // 點擊事件
        calendarViewHolder.itemView.setOnClickListener(v -> {
            int previousSelectedPosition = selectedPosition; // 記住之前選中的位置
            selectedPosition = holder.getAdapterPosition(); // 更新選中的位置
            notifyItemChanged(previousSelectedPosition); // 刷新之前選中的項目
            notifyItemChanged(selectedPosition); // 刷新新選中的項目
            onDayClickListener.onDayClick(day.getDay()); // 通知日期變化
        });  //更新 selectedPosition 並刷新相關項目（notifyItemChanged），透過 onDayClickListener.onDayClick(day.getDay()) 將點擊事件回傳給外部。
    }

    public void setSelectedDate(int year, int month, int day) {
        for (DayData dayData : days) {
            dayData.setSelected(dayData.getYear() == year && dayData.getMonth() == month && dayData.getDay() == day);
        }
        notifyDataSetChanged();
    } //根據給定的年月日，更新所有日期的選中狀態，並刷新整個列表。


    public void setSelectedPosition(int position) {
        int previousSelectedPosition = selectedPosition; // 記住之前選中的位置
        selectedPosition = position;
        if (previousSelectedPosition != -1) {
            notifyItemChanged(previousSelectedPosition); // 刷新之前選中的項目
        }
        notifyItemChanged(selectedPosition); // 刷新新選中的項目
    }  //更新當前選中位置，並只刷新舊位置和新位置的項目，避免整個列表重繪。

    // 自定義 ViewHolder
    public static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView dayTextView;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            dayTextView = itemView.findViewById(R.id.dayText); //顯示日期數字
        }
    }

    // 點擊事件接口
    public interface OnDayClickListener {
        void onDayClick(int day);
    } //定義一個回調接口，被點擊時，會將該天的數字傳遞給外部使用者
}


