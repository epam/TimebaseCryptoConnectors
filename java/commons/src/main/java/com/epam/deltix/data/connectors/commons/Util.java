package com.epam.deltix.data.connectors.commons;

import java.util.regex.Pattern;

public abstract class Util {
    public static final String NATIVE_LINE_BREAK = System.getProperty("line.separator");

    public static final String INSTRUMENTS_SEPARATOR = ",";

    public static <T extends AutoCloseable> T closeQuiet(final T resource) {
        if (resource == null) {
            return null;
        }
        try {
            resource.close();
        } catch (final Throwable t) {
        }
        return null;
    }

    public static String[] splitInstruments(String instruments) {
        return instruments.split("(?<!\\\\)" + Pattern.quote(INSTRUMENTS_SEPARATOR));
    }

    private Util() {
    }
}
