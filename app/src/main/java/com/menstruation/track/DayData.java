package com.menstruation.track;

public class DayData {
    private int year; //儲存年份
    private int month; //儲存月份
    private int day;
    private boolean hasData; // 新增：標記是否有資料
    private boolean isOvulationDay; // 新增：標記是否排卵
    private boolean isPredictedPeriod; //月經預測
    private boolean isToday; //標記是否為今天的日期
    private boolean isSelected; //標記是否被使用者選中

    public DayData(int year, int month, int day, boolean hasData, boolean isOvulationDay, boolean isToday, boolean isSelected) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.hasData = hasData;
        this.isOvulationDay = isOvulationDay;
        this.isToday = isToday;
        this.isSelected = isSelected;
    } //初始化所有屬性
    //獲取物件屬性值:
    public boolean hasData() {
        return hasData;
    }

    public void setHasData(boolean hasData) {
        this.hasData = hasData;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public int getDay() {
        return day;
    }

    public boolean isToday() {
        return isToday;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }

    // Getter 和 Setter 方法
    public boolean isOvulationDay() {
        return isOvulationDay;
    }

    public void setOvulationDay(boolean ovulationDay) {
        isOvulationDay = ovulationDay;
    }

    public boolean isPredictedPeriod() {
        return isPredictedPeriod;
    }

    public void setPredictedPeriod(boolean predictedPeriod) {
        isPredictedPeriod = predictedPeriod;
    }
}

