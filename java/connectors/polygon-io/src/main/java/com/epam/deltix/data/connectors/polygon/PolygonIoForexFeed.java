package com.epam.deltix.data.connectors.polygon;

import com.epam.deltix.data.connectors.commons.CloseableMessageOutput;
import com.epam.deltix.data.connectors.commons.ErrorListener;
import com.epam.deltix.data.connectors.commons.Logger;
import com.epam.deltix.data.connectors.commons.MdModel;
import com.epam.deltix.data.connectors.commons.json.JsonArray;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.commons.json.JsonValue;
import com.epam.deltix.data.connectors.commons.json.JsonWriter;
import com.epam.deltix.dfp.Decimal64Utils;

public class PolygonIoForexFeed extends PolygonIoFeed {


    public PolygonIoForexFeed(final PolygonIoConnectorSettings settings,
                              MdModel.Options selected,
                              CloseableMessageOutput output,
                              ErrorListener errorListener,
                              Logger logger,
                              String... symbols) {

        super(settings, selected, output, errorListener, logger, symbols);
    }

    public void subscribe(JsonWriter jsonWriter, String... symbols) {
        super.subscribe(jsonWriter, symbols);

        JsonValue json = JsonValue.newObject();
        JsonObject body = json.asObject();
        body.putString("action", "subscribe");
        body.putString("params", buildParams(symbols));
        json.toJsonAndEoj(jsonWriter);
    }

    private String buildParams(String[] symbols) {
        StringBuilder params = new StringBuilder();
        for (String symbol : symbols) {
            if (selected().level1()) {
                params.append("C.").append(symbol).append(",");
            }
        }
        params.setLength(params.length() - 1);
        return params.toString();
    }

    @Override
    protected void processPolygonData(JsonArray array) {
        for (int i = 0; i < array.size(); ++i) {
            JsonObject obj = array.getObject(i);
            String event = obj.getString("ev");
            if ("C".equalsIgnoreCase(event)) {
                String instrument = obj.getString("p");
                long timestamp = obj.getLong("t");
                long askPrice = obj.getDecimal64Required("a");
                long bidPrice = obj.getDecimal64Required("b");
                String exchange = obj.getString("x");

                processor().onL1Snapshot(
                    instrument, timestamp,
                    bidPrice, Decimal64Utils.ZERO, exchange,
                    askPrice, Decimal64Utils.ZERO, exchange
                );
            }
        }
    }

}
