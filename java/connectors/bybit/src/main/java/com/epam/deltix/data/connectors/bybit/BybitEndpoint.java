package com.epam.deltix.data.connectors.bybit;

public enum BybitEndpoint {
    Spot,
    Linear,
    Inverse,
    Option;

    public static BybitEndpoint typeFromAddress(String wsEndpoint) {
        if (wsEndpoint == null || wsEndpoint.length() == 0) {
            return null;
        }

        wsEndpoint = wsEndpoint.toLowerCase();

        if (wsEndpoint.contains("/spot")) {
            return Spot;
        }
        else if (wsEndpoint.contains("/linear")) {
            return Linear;
        }
        else if (wsEndpoint.contains("/inverse")) {
            return Inverse;
        }
        else if (wsEndpoint.contains("/option")) {
            return Option;
        }

        return null;
    }

    public static String getEndpoint(BybitEndpoint endpoint) {
        String result = null;

        switch (endpoint) {
            case Spot:
                result = "spot";
                break;

            case Linear:
                result = "linear";
                break;

            case Inverse:
                result = "inverse";
                break;

            case Option:
                result = "option";
                break;
        }

        return result;
    }
}
