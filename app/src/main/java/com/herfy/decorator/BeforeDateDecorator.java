package com.herfy.decorator;

import android.graphics.drawable.Drawable;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

import java.util.Calendar;

public class BeforeDateDecorator implements DayViewDecorator {

    private CalendarDay date;
    private CalendarDay max;
    private final Drawable highlightDrawable;

    public BeforeDateDecorator(Drawable drawable, int maxDays) {
        highlightDrawable = drawable;
        date = CalendarDay.today();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, maxDays);
        max = CalendarDay.from(calendar.get(Calendar.YEAR), (calendar.get(Calendar.MONTH) + 1), calendar.get(Calendar.DATE));
    }

    @Override
    public boolean shouldDecorate(final CalendarDay day) {
        return date != null && (day.isBefore(date) || day.isAfter(max));
    }

    @Override
    public void decorate(final DayViewFacade view) {
        view.setBackgroundDrawable(highlightDrawable);
    }
}


