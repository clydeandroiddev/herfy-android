package com.herfy.util;

import android.content.Context;
import android.text.SpannedString;
import android.util.Patterns;

import com.herfy.R;
import com.herfy.data.constants.Text;
import com.herfy.data.constants.Values;
import com.skylinedynamics.solosdk.api.models.objects.Store;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class StringUtil {

    public static boolean isValidName(String name) {
        Pattern pattern = Pattern.compile("^[a-zA-Z\\u0600-\\u06FF \\\\s]*$");
        return name != null && !name.isEmpty() && pattern.matcher(name).matches();
    }

    public static boolean isValidPhoneNumber(String phone, int mobileFormatIndex) {
        boolean valid;
        if (CacheUtil.getInstance().getConcept().getAllowedCountries() != null && !CacheUtil.getInstance().getConcept().getAllowedCountries().isEmpty() && CacheUtil.getInstance().getConcept().getAllowedCountries().size() >= mobileFormatIndex ) {
            Pattern pattern = Pattern.compile(CacheUtil.getInstance().getConcept().getAllowedCountries().get(mobileFormatIndex).getMobileFormat());
            valid = pattern.matcher(toEnglish(phone)).matches();
        } else {
            valid = Patterns.PHONE.matcher(phone).matches();
        }
        return phone != null && !phone.isEmpty() && valid;
    }

    public static boolean isValidPhoneNumber(String phone) {
        boolean valid;

        if (CacheUtil.getInstance().getConcept().getMobileFormat() != null && !CacheUtil.getInstance().getConcept().getMobileFormat().isEmpty()) {
            Pattern pattern = Pattern.compile(CacheUtil.getInstance().getConcept().getMobileFormat());
            valid = pattern.matcher(toEnglish(phone)).matches();
        } else if (CacheUtil.getInstance().getConcept().getCountry().equalsIgnoreCase("sa")) {
            Pattern pattern = Pattern.compile("([0-9\\s\\-]{7,})(?:\\s*(?:#|x\\.?|ext\\.?|extension)\\s*(\\d+))?$");
            valid = pattern.matcher(phone).matches();
        } else {
            valid = Patterns.PHONE.matcher(phone).matches();
        }

        return phone != null && !phone.isEmpty() && valid;
    }

    public static boolean isValidMobileNumber(String mobileNumber) {
        return android.util.Patterns.PHONE.matcher(mobileNumber).matches();
    }

    public static boolean isNumeric(String mobileNumber) {
        try {
            long mobileValue = Long.parseLong(mobileNumber);
            return true;
        } catch (NumberFormatException e) {}
        return false;
    }

    public static boolean isValidEmail(String email) {
        return email != null && !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static String localize(String number) {
        if (LocaleUtil.getInstance().isEnglish() || LocaleUtil.getInstance().isFrench()) {
            return number;
        } else {
            return number
                    .replace("١", "1").replace("٢", "2")
                    .replace("٣", "3").replace("٤", "4")
                    .replace("٥", "5").replace("٦", "6")
                    .replace("٧", "7").replace("٨", "8")
                    .replace("٩", "9").replace("٠", "0")
                    .replace(",", ".").replace("٫", ".");
        }
    }

    public static String toArabic(String number) {
        if (LocaleUtil.getInstance().isEnglish() || LocaleUtil.getInstance().isFrench()) {
            return number;
        } else {
            return number
                    .replace("1", "١" ).replace("2","٢")
                    .replace("3", "٣").replace("4","٤")
                    .replace("5", "٥").replace("6", "٦")
                    .replace("7", "٧").replace("8", "٨")
                    .replace("9", "٩").replace("0", "٠")
                    .replace(".", ",").replace(".", "٫");
        }
    }

    public static String toEnglish(String number) {
        if (LocaleUtil.getInstance().isEnglish() || LocaleUtil.getInstance().isFrench()) {
            return number;
        } else {
            return number
                    .replace("١", "1").replace("٢", "2")
                    .replace("٣", "3").replace("٤", "4")
                    .replace("٥", "5").replace("٦", "6")
                    .replace("٧", "7").replace("٨", "8")
                    .replace("٩", "9").replace("٠", "0")
                    .replace(",", ".").replace("٫", ".");
        }
    }

    public static String getDistance(double distance) {

        if (distance >= 0) {
            if (distance >= 10000) {
                DecimalFormat formatter = new DecimalFormat("#,###");
                //return localize(String.format(LocaleUtil.getLocale(), "%d ", (int) (distance / 1000.0f))) + CacheUtil.getInstance().getTranslations(Text.KILOMETERS);
                return localize(formatter.format((int) (distance / 1000.0f))) + " " + CacheUtil.getInstance().getTranslations(Text.KILOMETERS);
            } else if (distance >= 1000) {
                DecimalFormat formatter = new DecimalFormat("#,###.0");
                //return localize(String.format(LocaleUtil.getLocale(), "%.1f ", distance / 1000.0f)) + CacheUtil.getInstance().getTranslations(Text.KILOMETERS);
                return localize(formatter.format(distance / 1000.0f)) + " " + CacheUtil.getInstance().getTranslations(Text.KILOMETERS);
            } else if (distance >= 100) {
                return localize(String.format(LocaleUtil.getLocale(), "%d ", (int) distance)) + CacheUtil.getInstance().getTranslations(Text.METERS);
            } else {
                return localize(String.format(LocaleUtil.getLocale(), "%.1f ", distance)) + CacheUtil.getInstance().getTranslations(Text.METERS);
            }
        }

        return "";

    }

    public static String getOpeningHours(Store store) {

        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;

        if (store.getAttributes().getOpeningHours() != null && day < store.getAttributes().getOpeningHours().size()) {

            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                //simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+3"));

                Date openDate = simpleDateFormat.parse(store.getAttributes().getOpeningHours().get(day).getOpen());
                Date closedDate = simpleDateFormat.parse(store.getAttributes().getOpeningHours().get(day).getClosed());

                simpleDateFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                //simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+3"));

                return localize(simpleDateFormat.format(openDate).replace("AM", CacheUtil.getInstance().getTranslations(Text.AM)).replace("PM", CacheUtil.getInstance().getTranslations(Text.PM)) + " - " + simpleDateFormat.format(closedDate).replace("AM", CacheUtil.getInstance().getTranslations(Text.AM)).replace("PM", CacheUtil.getInstance().getTranslations(Text.PM)));

            } catch (ParseException e) {
                e.printStackTrace();
            }

            return localize(store.getAttributes().getOpeningHours().get(day).getOpen() + " - " + store.getAttributes().getOpeningHours().get(day).getClosed());

        }

        return "";

    }

    public static String formatOpenHours(String open, String close) {
        String formattedOpenHours = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date nowDate = new Date();
            String currentDate = sdf.format(nowDate);

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.ENGLISH);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date openDate = simpleDateFormat.parse(open);
            Date closedDate = simpleDateFormat.parse(close);

            SimpleDateFormat timeSdf = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            TimeZone localTimeZone = TimeZone.getDefault();
            timeSdf.setTimeZone(localTimeZone);

            if(openDate != null && closedDate != null) {
                if (localTimeZone.inDaylightTime(nowDate)) {
                    simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
                    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    openDate = simpleDateFormat.parse(currentDate + " " + open);
                    closedDate = simpleDateFormat.parse(currentDate + " " + close);
                    if(openDate != null && closedDate != null) {
                        boolean isCloseNextDay = closedDate.getTime() < openDate.getTime();
                        if (isCloseNextDay) {
                            Calendar closedCalendar = Calendar.getInstance();
                            closedCalendar.setTime(closedDate);
                            closedCalendar.add(Calendar.DATE, 1);
                            String tomorrowDate = sdf.format(closedCalendar.getTime());
                            closedDate = simpleDateFormat.parse(tomorrowDate + " " + close);
                        }
                        if (closedDate != null) {
                            long openTime = openDate.getTime();
                            long closedTime = closedDate.getTime();
                            Date openDateWithOffset = new Date(openTime);
                            Date closedDateWithOffset = new Date(closedTime);
                            formattedOpenHours = timeSdf.format(openDateWithOffset) + " - " + timeSdf.format(closedDateWithOffset);
                        }
                    }
                } else {
                    formattedOpenHours = timeSdf.format(openDate) + " - " + timeSdf.format(closedDate);
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return formattedOpenHours;
    }

    public static String getClosingHours(Store store) {

        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK) - 1;

        if (store.getAttributes().getOpeningHours() != null && day < store.getAttributes().getOpeningHours().size()) {

            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                //simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+3"));

                //Date openDate = simpleDateFormat.parse(store.getAttributes().getOpeningHours().get(day).getOpen());
                Date closedDate = simpleDateFormat.parse(store.getAttributes().getOpeningHours().get(day).getClosed());

                simpleDateFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                //simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+3"));

                //return localize(simpleDateFormat.format(openDate) + " - " + simpleDateFormat.format(closedDate));

                if (closedDate != null) {
                    Calendar nowCalendar = Calendar.getInstance();
                    nowCalendar.set(Calendar.SECOND, 0);

                    Calendar closedCalendar = Calendar.getInstance();
                    closedCalendar.setTime(closedDate);
                    closedCalendar.set(Calendar.YEAR, nowCalendar.get(Calendar.YEAR));
                    closedCalendar.set(Calendar.MONTH, nowCalendar.get(Calendar.MONTH));
                    closedCalendar.set(Calendar.DAY_OF_MONTH, nowCalendar.get(Calendar.DAY_OF_MONTH));

                    long difference = closedCalendar.getTime().getTime() - nowCalendar.getTime().getTime();
                    if (difference <= 60 * 60 * 2 * 1000) {
                        return CacheUtil.getInstance().getTranslations(Text.CLOSING_SOON, "Closing soon - order before (x)").replace("(x)", localize(simpleDateFormat.format(closedDate).replace("AM", CacheUtil.getInstance().getTranslations(Text.AM)).replace("PM", CacheUtil.getInstance().getTranslations(Text.PM))));
                    } else {
                        return CacheUtil.getInstance().getTranslations(Text.OPEN_UNTIL, "Open until (x)").replace("(x)", localize(simpleDateFormat.format(closedDate).replace("AM", CacheUtil.getInstance().getTranslations(Text.AM)).replace("PM", CacheUtil.getInstance().getTranslations(Text.PM))));
                    }

                }

            } catch (ParseException e) {
                e.printStackTrace();
            }

        }

        return "";

    }

    public static SpannedString getFormattedPriceWithText(double price) {
        return CurrencyUtils.Companion.updateCurrencyWithText(price, true);
    }

    public static SpannedString getFormattedPrice(double price) {
        return CurrencyUtils.Companion.updateCurrency(price, true);
    }

    public static String getFormattedAmount(double amount) {
        int decimalPlaces = CacheUtil.getInstance().getConcept().getDecimalPlaces();
        if (decimalPlaces > 0 && amount != (int) amount) {
            String format = "%." + decimalPlaces + "f";
            return localize(String.format(Locale.ENGLISH, format, amount));
        }
        return localize("" + (int) amount);
    }

    public static String colorIntToString(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }

    public static SpannedString getDigitalCouponPrice(double price, boolean withSpace) {
        return CurrencyUtils.Companion.updateCurrency(price, withSpace);
    }


    public static SpannedString getNewFormattedPrice(double price, boolean withSpace) {
        return CurrencyUtils.Companion.updateCurrency(price, withSpace);
    }

    public static SpannedString getNewFormattedPrice(double price) {
        return getNewFormattedPrice(price, true);
    }

    public static String getCloudinaryUrl(int width, int height, String imageUrl) {
        //return imageUrl;
        return Values.CLOUDINARY_IMAGE_URL.replace("(w)", String.valueOf(width)).replace("(h)", String.valueOf(height)) + imageUrl;
    }

    public static String getCloudflareUrl(int width, int height, String imageUrl) {
        if (imageUrl.contains(Values.CDN_HOST)) {
            int index = imageUrl.lastIndexOf("/");
            String start = imageUrl.substring(0, index);
            String end = imageUrl.substring(index);
            return start + Values.CLOUDFLARE_IMAGE_URL.replace("(w)", String.valueOf(width)).replace("(h)", String.valueOf(height)) + end;
        }
        return imageUrl;
    }

    public static String getCloudflareSliderUrl(int width, int height, String imageUrl) {
        if (imageUrl.contains(Values.CDN_HOST)) {
            int index = imageUrl.lastIndexOf("/");
            String start = imageUrl.substring(0, index);
            String end = imageUrl.substring(index);
            return start + Values.CLOUDFLARE_SLIDER_URL.replace("(w)", String.valueOf(width)).replace("(h)", String.valueOf(height)) + end;
        }
        return imageUrl;
    }

    public static String generateRandomEmail() {
        Random r = new Random();
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        int number = r.nextInt(1000);
        String num = String.format(Locale.ENGLISH, "%02d", number);
        return "dev" + System.currentTimeMillis() + letters.charAt(r.nextInt(letters.length())) + letters.charAt(r.nextInt(letters.length())) + StringUtil.toEnglish(num) + "@burgerizzr.com";
    }

    public static String getCountDownTime(long timeLeft) {
        if (timeLeft > 0) {
            String m = String.format(Locale.getDefault(), "%d", TimeUnit.MILLISECONDS.toMinutes(timeLeft));
            String s = String.format(Locale.getDefault(), "%d", TimeUnit.MILLISECONDS.toSeconds(timeLeft) - TimeUnit.MINUTES.toSeconds(
                            TimeUnit.MILLISECONDS.toMinutes(
                                    timeLeft
                            )
                    ));

            if(s.length() == 1){
                s = "0"+s;
            }

            if(m.length() == 1){
                m = "0"+m;
            }

            return toArabic(m)+":"+toArabic(s);
        } else {
            return toArabic("00")+":"+toArabic("00");
        }
    }

    public static String getTimeLeft(long timeLeft) {
        if (timeLeft > 0) {
            String m = localize(String.format(Locale.getDefault(), "%d", TimeUnit.MILLISECONDS.toMinutes(timeLeft)));
            String s = localize(String.format(Locale.getDefault(), "%d", TimeUnit.MILLISECONDS.toSeconds(timeLeft) -
                    TimeUnit.MINUTES.toSeconds(
                            TimeUnit.MILLISECONDS.toMinutes(
                                    timeLeft
                            )
                    )));

            return CacheUtil.getInstance().getTranslations(Text.MINUTES_SECONDS_LEFT, "(x)m (y)s left").replace("(x)", m).replace("(y)", localize(s));
        } else {
            return CacheUtil.getInstance().getTranslations(Text.MINUTES_SECONDS_LEFT, "(x) m (y) s left").replace("(x)", localize("0")).replace("(y)", localize("0"));
        }
    }

    public static boolean isNullOrEmpty(String label) {
        if (label != null) {
            return label.isEmpty();
        }
        return true;
    }

    public static String trimEventAttribute(String attribute) {
        if (attribute.length() > 100) {
            return attribute.substring(0, 100);
        }
        return attribute;
    }

    public static boolean hasOrderLatLong() {
        return !CacheUtil.getInstance().getOrderLatitude().isEmpty() && !CacheUtil.getInstance().getOrderLongitude().isEmpty();
    }

    public static String getAppName(Context context) {
        String appName = context.getResources().getString(R.string.app_name);

        if(CacheUtil.getInstance().getCountry() != null) {
            return CacheUtil.getInstance().getCountry().applicationName;
        } else {
            return appName;
        }
    }
}
