package com.epam.deltix.data.connectors.kraken;

public class Util {
    static final long LONG_MAX_VALUE_DIV_10   = Long.MAX_VALUE / 10;

    public static long parseTime(CharSequence value) {
        int dot = indexOf(value, '.');

        if (dot == -1) {
            return parseLong(value) * 1000;
        }

        long sec  = parseLong(value, 0, dot);
        long mils = parseLong(value, dot + 1, value.length());

        return sec * 1000 + mils / 1000;
    }

    public static int indexOf(CharSequence data, char c) {
        if (data == null) {
            return -1;
        }

        int l = data.length();

        if (l == 0) {
            return -1;
        }

        int i = 0;

        while (--l >= 0) {
            if (data.charAt(i) == c) {
                return i;
            }

            i++;
        }

        return -1;
    }

    public static long parseLong(CharSequence sc) {
        return parseLong(sc, 0, sc.length());
    }

    public static long parseLong(final CharSequence sc, final int startIncl, final int endExcl) {
        if (startIncl == endExcl) {
            throw new NumberFormatException("Empty string");
        }

        int     pos      = startIncl;
        long    value    = 0;
        char    ch       = sc.charAt(pos);

        boolean negative = ch == '-';

        if ((ch == '+') || negative) {
            pos++;

            if (pos == endExcl) {
                throw new NumberFormatException(new StringBuilder().append(sc, startIncl, endExcl).toString());
            }

            ch = sc.charAt(pos);
        }

        for (;;) {
            if (ch != ',') {
                int digit = ch - '0';

                if ((digit < 0) || (digit > 9)) {
                    throw new NumberFormatException("Illegal digit at position " + (pos + 1) + " in: "
                        + new StringBuilder().append(sc,
                        startIncl,
                        endExcl).toString());
                }

                if (value < -LONG_MAX_VALUE_DIV_10) {
                    throw new NumberFormatException("Long integer (8-byte) too large: " + sc);
                }

                value = value * 10 - digit;

                if (value > 0)    // Overflow
                {
                    throw new NumberFormatException("Long integer (8-byte) too large: " + sc);
                }
            }

            pos++;

            if (pos == endExcl) {
                if (negative) {
                    return (value);
                } else if (value == Long.MIN_VALUE) {
                    throw new NumberFormatException("Long integer (8-byte) too large: " + sc);
                } else {
                    return (-value);
                }
            }

            ch = sc.charAt(pos);
        }
    }
}
