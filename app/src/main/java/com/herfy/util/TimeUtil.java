package com.herfy.util;

import com.herfy.utils.helper.CacheUtil;
import com.herfy.utils.helper.LocaleUtil;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class TimeUtil {

    public static String convertTimeUtc(String date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            Date nDate = dateFormat.parse(date);
            SimpleDateFormat outputFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            outputFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            return outputFmt.format(nDate);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;

    }

    public static String dateToHourOnly(Date selectedDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", new Locale(LocaleUtil.getInstance().getLanguage()));
            sdf.format(selectedDate);
            return sdf.format(selectedDate);
        } catch (Exception pe) {
            return selectedDate.toString();
        }
    }

    public static String formatDate(String selectedDate) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            Date newDate = format.parse(selectedDate);
            format = new SimpleDateFormat("MMMM dd, yyyy");
            return format.format(newDate);
        } catch (ParseException pe) {
            return selectedDate;
        }
    }

    public static String formatDate(String selectedDate,String pattern) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
            Date newDate = format.parse(selectedDate);
            format = new SimpleDateFormat("MMMM dd, yyyy");
            return format.format(newDate);
        } catch (ParseException pe) {
            return selectedDate;
        }
    }

    public static String properSchedDate(String selectedDate) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.ENGLISH);
            Date newDate = format.parse(selectedDate);
            format = new SimpleDateFormat("MMM d, h:mm a", new Locale(LocaleUtil.getInstance().getLanguage()));
            return format.format(newDate);
        } catch (ParseException pe) {
            return selectedDate;
        }
    }

    public static String dateToMonthAndDayOnly(String selectedDate) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date newDate = format.parse(selectedDate);
            format = new SimpleDateFormat("MMM d", new Locale(LocaleUtil.getInstance().getLanguage()));
            return format.format(newDate);
        } catch (ParseException pe) {
            return selectedDate;
        }
    }

    public static String dateSelectedToNoTimeDate(String selectedDate) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date newDate = format.parse(selectedDate);
            format = new SimpleDateFormat("yyyy-MM-dd", new Locale(LocaleUtil.getInstance().getLanguage()));
            return format.format(newDate);
        } catch (ParseException pe) {
            return selectedDate;
        }
    }

    public static String serverToDisplay(String selectedDate) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", new Locale(LocaleUtil.getInstance().getLanguage()));
            format.setTimeZone(TimeZone.getTimeZone("GMT+0"));
            Date newDate = format.parse(selectedDate);
            format = new SimpleDateFormat("MMM d, h:mm a", new Locale(LocaleUtil.getInstance().getLanguage()));
            format.setTimeZone(TimeZone.getDefault());
            return format.format(newDate);
        } catch (ParseException pe) {
            return selectedDate;
        }
    }

    public static long addDays(int year, int month, int day, int hour, int min, int add) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR, hour);
        calendar.set(Calendar.MINUTE, min);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis() + (1000 * 60 * 60 * 24 * add);
    }

    public static long addMinutes(int year, int month, int day, int hour, int min, int add) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR, hour);
        calendar.set(Calendar.MINUTE, min);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis() + (add * 60000);
    }

    public static long getTimeDifferenceInMillis(String expiresAt) {
        long value = 0L;
        String timeZone = "GMT+3";
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            Date parseExpirationTime = format.parse(expiresAt);
            DateFormat convertExpiryFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
            Date expirationDateTime = convertExpiryFormat.parse(convertExpiryFormat.format(parseExpirationTime));
            DateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
            format.setTimeZone(TimeZone.getTimeZone(timeZone));
            String conceptTime = convertToConceptTimeZone();
            Date currentTime = newFormat.parse(conceptTime);
            value = expirationDateTime.getTime() - currentTime.getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return value;
    }

    private static String convertToConceptTimeZone() {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
        format.setTimeZone(TimeZone.getTimeZone(CacheUtil.getInstance().getConcept().getDefaultTimezone()));
        return format.format(new Date(System.currentTimeMillis()));
    }

    public static long getDateDiff(String expiresAt) {
        try {

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            DateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
            String conceptTime = convertToConceptTimeZone();
            Date currentTime = newFormat.parse(conceptTime);
            return TimeUnit.DAYS.convert(currentTime.getTime() - format.parse(expiresAt).getTime(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static String formatDateNoComma(String selectedDate) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            Date newDate = format.parse(selectedDate);
            format = new SimpleDateFormat("MMMM dd yyyy");
            return format.format(newDate);
        } catch (ParseException pe) {
            return selectedDate;
        }
    }

}
