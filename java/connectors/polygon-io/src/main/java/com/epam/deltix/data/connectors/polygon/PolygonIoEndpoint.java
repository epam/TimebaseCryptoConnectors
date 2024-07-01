package com.epam.deltix.data.connectors.polygon;

public enum PolygonIoEndpoint {
    Stocks,
    Options,
    Indices,
    Forex,
    Crypto;

    public static PolygonIoEndpoint typeFromAddress(String wsEndpoint) {
        if (wsEndpoint == null || wsEndpoint.isEmpty()) {
            return null;
        }

        wsEndpoint = wsEndpoint.toLowerCase();
        if (wsEndpoint.contains("/stocks")) {
            return Stocks;
        } else if (wsEndpoint.contains("/options")) {
            return Options;
        } else if (wsEndpoint.contains("/indices")) {
            return Indices;
        } else if (wsEndpoint.contains("/forex")) {
            return Forex;
        } else if (wsEndpoint.contains("/crypto")) {
            return Crypto;
        }

        return null;
    }

    public static String getEndpoint(PolygonIoEndpoint endpoint) {
        String result = null;

        switch (endpoint) {
            case Stocks:
                result = "stocks";
                break;

            case Options:
                result = "options";
                break;

            case Indices:
                result = "indices";
                break;

            case Forex:
                result = "forex";
                break;

            case Crypto:
                result = "crypto";
                break;
        }

        return result;
    }
}
