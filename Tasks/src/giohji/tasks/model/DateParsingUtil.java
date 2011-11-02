package giohji.tasks.model;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TimeZone;

import android.util.Log;
/**
 * Utility class used to parse strings formated in the ISO format to Date objects.
 */
public final class DateParsingUtil {
    /**
     * TAG for logging.
     */
    private static final String TAG = "DateParsingUtil";
    /**
     * private constructor.
     */
    private DateParsingUtil() {
        super();
    }
    /**
     * Parse the given string in ISO 8601 format and build a Date object.
     *
     * @param isodate
     *            the date in ISO 8601 format
     * @return a Date instance
     */
    public static Date parseDate(final String isodate) {
        Date parsedDate = null;
        if (isodate != null) {
            final Calendar calendar = getCalendar(isodate);
            parsedDate = calendar.getTime();
        }
        return parsedDate;
    }

    /**
     * Check if the next token in the stringTokenizer equals the token.
     *
     * @param stringTokenizer
     *            the stringTokenizer
     * @param token
     *            the token
     * @return true, if token is equal the next token in stringTokenizer.
     */
    private static boolean check(final StringTokenizer stringTokenizer, final String token) {
        try {
            if (stringTokenizer.nextToken().equals(token)) {
                return true;
            } else {
                throw new NoSuchElementException("Missing [" + token + "]");
            }
        } catch (NoSuchElementException ex) {
            return false;
        }
    }

    /**
     * Parses the String and returns a Calendar object.
     *
     * @param isodate
     *            the String in ISO format.
     * @return the calendar
     */
    private static Calendar getCalendar(final String isodate) {
        // YYYY-MM-DDThh:mm:ss.sTZD
        final StringTokenizer stringTokenizer = new StringTokenizer(isodate, "-T:.+Z", true);

        final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        try {
            // Year
            if (stringTokenizer.hasMoreTokens()) {
                final int year = Integer.parseInt(stringTokenizer.nextToken());
                calendar.set(Calendar.YEAR, year);
            } else {
                return calendar;
            }
            // Month
            if (check(stringTokenizer, "-") && (stringTokenizer.hasMoreTokens())) {
                final int month = Integer.parseInt(stringTokenizer.nextToken()) - 1;
                calendar.set(Calendar.MONTH, month);
            } else {
                return calendar;
            }
            // Day
            if (check(stringTokenizer, "-") && (stringTokenizer.hasMoreTokens())) {
                final int day = Integer.parseInt(stringTokenizer.nextToken());
                calendar.set(Calendar.DAY_OF_MONTH, day);
            } else {
                return calendar;
            }
            // Hour
            if (check(stringTokenizer, "T") && (stringTokenizer.hasMoreTokens())) {
                final int hour = Integer.parseInt(stringTokenizer.nextToken());
                calendar.set(Calendar.HOUR_OF_DAY, hour);
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                return calendar;
            }
            // Minutes
            if (check(stringTokenizer, ":") && (stringTokenizer.hasMoreTokens())) {
                final int minutes = Integer.parseInt(stringTokenizer.nextToken());
                calendar.set(Calendar.MINUTE, minutes);
            } else {
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                return calendar;
            }

            // Seconds
            if (!stringTokenizer.hasMoreTokens()) {
                return calendar;
            }
            String tok = stringTokenizer.nextToken();
            if (tok.equals(":")) { // seconds
                if (stringTokenizer.hasMoreTokens()) {
                    final int secondes = Integer.parseInt(stringTokenizer.nextToken());
                    calendar.set(Calendar.SECOND, secondes);
                    if (!stringTokenizer.hasMoreTokens()) {
                        return calendar;
                    }
                    // frac sec
                    tok = stringTokenizer.nextToken();
                    if (tok.equals(".")) {

                        final StringBuffer buf = new StringBuffer();
                        String nextToken = stringTokenizer.nextToken();
                        buf.append(nextToken);
                        while (nextToken.length() < 3) {
                            buf.append('0');
                        }

                        nextToken = buf.toString();

                        nextToken = nextToken.substring(0, 3); // Cut trailing chars..
                        final int millisec = Integer.parseInt(nextToken);
                        calendar.set(Calendar.MILLISECOND, millisec);
                        if (!stringTokenizer.hasMoreTokens()) {
                            return calendar;
                        }
                        tok = stringTokenizer.nextToken();
                    } else {
                        calendar.set(Calendar.MILLISECOND, 0);
                    }
                } else {
                    throw new NoSuchElementException("No secondes specified");
                }
            } else {
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
            }
            // Timezone
            if (!tok.equals("Z")) { // UTC
                if (!(tok.equals("+") || tok.equals("-"))) {
                    throw new NumberFormatException("only Z, + or - allowed");
                }
                final boolean plus = tok.equals("+");
                if (!stringTokenizer.hasMoreTokens()) {
                    throw new NoSuchElementException("Missing hour field");
                }
                final int tzhour = Integer.parseInt(stringTokenizer.nextToken());
                int tzmin = 0;
                if (check(stringTokenizer, ":") && (stringTokenizer.hasMoreTokens())) {
                    tzmin = Integer.parseInt(stringTokenizer.nextToken());
                } else {
                    throw new NoSuchElementException("Missing minute field");
                }
                if (plus) {
                    calendar.add(Calendar.HOUR, -tzhour);
                    calendar.add(Calendar.MINUTE, -tzmin);
                } else {
                    calendar.add(Calendar.HOUR, tzhour);
                    calendar.add(Calendar.MINUTE, tzmin);
                }
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, e.getClass().getName() + e.getMessage());
        }
        return calendar;
    }
}