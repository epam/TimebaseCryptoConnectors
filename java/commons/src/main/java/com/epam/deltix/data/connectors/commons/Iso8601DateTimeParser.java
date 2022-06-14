package com.epam.deltix.data.connectors.commons;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Parses timestamps like "2021-01-01T00:10:24.391550209Z" fast in case
 * when the time increases or decreases monotonically (for real-time cases)
 */
public class Iso8601DateTimeParser {
    private final Calendar lastValueDayMidnightCalendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    private long lastValueDayMidnight = Long.MIN_VALUE;
    private long lastValueDay = Long.MIN_VALUE;
    private long millis;
    private int nanos;

    public Iso8601DateTimeParser() {
    }

    public Iso8601DateTimeParser(final CharSequence time) {
        set(time);
    }

    public final Iso8601DateTimeParser set(final CharSequence from) {
        int idx = 0;

        final int year = (from.charAt(idx++) - '0') * 1000 +
                (from.charAt(idx++) - '0') * 100 +
                (from.charAt(idx++) - '0') * 10
                + (from.charAt(idx++) - '0');

        if (from.charAt(idx++) != '-') {
            throw new IllegalArgumentException("Expected '-' at " + idx);
        }

        final int month = (from.charAt(idx++) - '0') * 10 +
                (from.charAt(idx++) - '0');

        if (from.charAt(idx++) != '-') {
            throw new IllegalArgumentException("Expected '-' at " + idx);
        }

        final int day = (from.charAt(idx++) - '0') * 10 +
                (from.charAt(idx++) - '0');

        long valueDay = year * 10000L + month * 100 + day;

        if (lastValueDay != valueDay) {
            lastValueDayMidnightCalendar.set(Calendar.YEAR, year);
            lastValueDayMidnightCalendar.set(Calendar.MONTH, month - 1);
            lastValueDayMidnightCalendar.set(Calendar.DAY_OF_MONTH, day);
            lastValueDayMidnightCalendar.set(Calendar.HOUR_OF_DAY, 0);
            lastValueDayMidnightCalendar.set(Calendar.MINUTE, 0);
            lastValueDayMidnightCalendar.set(Calendar.SECOND, 0);
            lastValueDayMidnightCalendar.set(Calendar.MILLISECOND, 0);

            lastValueDayMidnight = lastValueDayMidnightCalendar.getTimeInMillis();

            lastValueDay = valueDay;
        }

        if (from.charAt(idx++) != 'T') {
            throw new IllegalArgumentException("Expected 'T' at " + idx);
        }
        millis = lastValueDayMidnight +
                TimeUnit.HOURS.toMillis((from.charAt(idx++) - '0') * 10 +
                        (from.charAt(idx++) - '0'));

        if (from.charAt(idx++) != ':') {
            throw new IllegalArgumentException("Expected ':' at " + idx);
        }
        millis += TimeUnit.MINUTES.toMillis((from.charAt(idx++) - '0') * 10 +
                (from.charAt(idx++) - '0'));

        if (from.charAt(idx++) != ':') {
            throw new IllegalArgumentException("Expected ':' at " + idx);
        }
        millis += TimeUnit.SECONDS.toMillis((from.charAt(idx++) - '0') * 10 +
                (from.charAt(idx++) - '0'));

        nanos = 0;

        if (from.length() > 20) {    // .SSS
            if (from.charAt(idx++) != '.') {
                throw new IllegalArgumentException("Expected '.' at " + idx);
            }
            millis = millis +
                    (from.charAt(idx++) - '0') * 100 +
                    (from.charAt(idx++) - '0') * 10 +
                    (from.charAt(idx++) - '0');

            if (from.length() > 24) { // .SSSSSS
                nanos = (from.charAt(idx++) - '0') * 100_000 +
                        (from.charAt(idx++) - '0') * 10_000 +
                        (from.charAt(idx++) - '0') * 1_000;

                if (from.length() > 27) { // .SSSSSSSSS
                    nanos += (from.charAt(idx++) - '0') * 100 +
                            (from.charAt(idx++) - '0') * 10 +
                            (from.charAt(idx++) - '0');
                }
            }
        }

        // check the timezone if exists
        if (from.length() > idx) {
            if (from.charAt(idx) != 'Z') {
                throw new IllegalArgumentException("Expected 'Z' timezone at " + idx);
            }
        }

        return this;
    }

    public final long millis() {
        return millis;
    }

    public int nanos() {
        return nanos;
    }

    @Override
    public String toString() {
        return "Iso8601DateTimeParser{" +
                "millis=" + millis +
                ", nanos=" + nanos +
                '}';
    }
}
