package com.menstruation.track;

import java.util.List;

public class MonthData {
    private final int year;  // 年份
    private final int month; // 月份
    private final List<DayData> days; // 該月份的日期數據

    public MonthData(int year, int month, List<DayData> days) {
        this.year = year;
        this.month = month;
        this.days = days;
    }

    // Getter 方法
    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public List<DayData> getDays() {
        return days;
    }
}
