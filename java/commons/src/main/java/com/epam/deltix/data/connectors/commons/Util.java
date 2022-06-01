package com.epam.deltix.data.connectors.commons;

import java.io.IOException;
import java.util.regex.Pattern;

public abstract class Util {
    public static final String NATIVE_LINE_BREAK = System.getProperty("line.separator");

    public static final String INSTRUMENTS_SEPARATOR = ",";

    private static final String[] TABS_BY_2 = new String[] {
            "",
            "  ",
            "    ",
            "      ",
            "        ",
            "          "
    };

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

    public static void tabBy2(final Appendable to, final int level) throws IOException {
        final int lastTab = TABS_BY_2.length - 1;
        for (int i = 0; i < (level / lastTab); i++) {
            to.append(TABS_BY_2[lastTab]);
        }
        to.append(TABS_BY_2[level % lastTab]);
    }

    public static boolean equals(final CharSequence c1, CharSequence c2) {
        if (c1 == null) {
            return c2 == null;
        }
        if (c2 == null) {
            return false;
        }
        return CharSequence.compare(c1, c2) == 0;
    }

    private Util() {
    }
}
