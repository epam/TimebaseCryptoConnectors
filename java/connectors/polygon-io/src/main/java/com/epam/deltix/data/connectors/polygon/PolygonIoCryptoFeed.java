package com.epam.deltix.data.connectors.polygon;

import com.epam.deltix.data.connectors.commons.CloseableMessageOutput;
import com.epam.deltix.data.connectors.commons.ErrorListener;
import com.epam.deltix.data.connectors.commons.Logger;
import com.epam.deltix.data.connectors.commons.MdModel;
import com.epam.deltix.data.connectors.commons.json.JsonArray;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.commons.json.JsonValue;
import com.epam.deltix.data.connectors.commons.json.JsonWriter;
import com.epam.deltix.timebase.messages.universal.AggressorSide;

public class PolygonIoCryptoFeed extends PolygonIoFeed {

    public PolygonIoCryptoFeed(final PolygonIoConnectorSettings settings,
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
                params.append("XQ.").append(symbol).append(",");
            }
            if (selected().trades()) {
                params.append("XT.").append(symbol).append(",");
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
            if ("XT".equalsIgnoreCase(event)) {
                String instrument = obj.getString("pair");
                long timestamp = obj.getLong("t");
                long price = obj.getDecimal64Required("p");
                long size = obj.getDecimal64Required("s");
                String ex = String.valueOf(obj.getLong("x"));
                JsonArray conditionsArray = obj.getArray("c");
                String conditions = readConditions(conditionsArray);
                AggressorSide side = null;
                if (conditionsArray.size() > 0) {
                    long condition = conditionsArray.getLong(0);
                    if (condition == 1) {
                        side = AggressorSide.SELL;
                    } else if (condition == 2) {
                        side = AggressorSide.BUY;
                    }
                }

                processor().onTrade(
                    instrument, timestamp, price, size, side, ex, conditions
                );
            } else if ("XQ".equalsIgnoreCase(event)) {
                String instrument = obj.getString("pair");
                long timestamp = obj.getLong("t");
                long askPrice = obj.getDecimal64Required("ap");
                long askSize = obj.getDecimal64Required("as");
                long bidPrice = obj.getDecimal64Required("bp");
                long bidSize = obj.getDecimal64Required("bs");
                String ex = String.valueOf(obj.getLong("x"));

                processor().onL1Snapshot(
                    instrument, timestamp, bidPrice, bidSize, ex, askPrice, askSize, ex
                );
            }
        }
    }

}
