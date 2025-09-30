package ru.gdemuzei.bot.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class GeoUtil {
    private static final Pattern COORDS = Pattern.compile(
            "^\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*,\\s*([+-]?\\d+(?:\\.\\d+)?)\\s*$");

    private GeoUtil() {
    }

    public static boolean parseLatLon(String text, double[] out) {
        var m = COORDS.matcher(text);
        if (!m.matches()) {
            return false;
        }
        double lat = Double.parseDouble(m.group(1));
        double lon = Double.parseDouble(m.group(2));
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            return false;
        }
        out[0] = lat;
        out[1] = lon;
        return true;
    }

    public static String formatDistance(Double km) {
        if (km == null) {
            return "";
        }
        if (km >= 1.0) {
            return String.format(Locale.US, "%.1f км", km);
        }
        long m = Math.round(km * 1000.0);
        return m + " м";
    }
}
