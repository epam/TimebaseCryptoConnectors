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

public class PolygonIoStocksFeed extends PolygonIoFeed {


    public PolygonIoStocksFeed(final PolygonIoConnectorSettings settings,
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
                params.append("Q.").append(symbol).append(",");
            }
            if (selected().trades()) {
                params.append("T.").append(symbol).append(",");
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
            if ("T".equalsIgnoreCase(event)) {
                String instrument = obj.getString("sym");
                long timestamp = obj.getLong("t");
                long price = obj.getDecimal64Required("p");
                long size = obj.getDecimal64Required("s");
                long ex = obj.getLong("x");
                String exName = exchanges.get(ex);

                processor().onTrade(
                    instrument, timestamp, price, size
                );

                if (logger().isDebugEnabled()) {
                    logger().debug(() ->
                        "TRADE: " + instrument +
                            " | t: " + timestamp +
                            " | p: " + Decimal64Utils.toString(price) +
                            ", s: " + Decimal64Utils.toString(size) +
                            ", ex: " + (exName != null ? exName : "?")
                    );
                }
            } else if ("Q".equalsIgnoreCase(event)) {
                String instrument = obj.getString("sym");
                long timestamp = obj.getLong("t");
                long askPrice = obj.getDecimal64Required("ap");
                long askSize = obj.getDecimal64Required("as");
                long askEx = obj.getLong("ax");
                long bidPrice = obj.getDecimal64Required("bp");
                long bidSize = obj.getDecimal64Required("bs");
                long bidEx = obj.getLong("bx");
                String askExName = exchanges.get(askEx);
                String bidExName = exchanges.get(bidEx);

                processor().onL1Snapshot(
                    instrument, timestamp, bidPrice, bidSize, askPrice, askSize
                );

                if (logger().isDebugEnabled()) {
                    logger().debug(() ->
                        "QUOTE: " + instrument +
                            " | t: " + timestamp +
                            " | bidP: " + Decimal64Utils.toString(bidPrice) +
                            ", bidS: " + Decimal64Utils.toString(bidSize) +
                            ", bidEx: " + (bidExName != null ? bidExName : "?") +
                            "| askP: " + Decimal64Utils.toString(askPrice) +
                            ", askS: " + Decimal64Utils.toString(askSize) +
                            ", askEx: " + (askExName != null ? askExName : "?")
                    );
                }
            } else if ("status".equalsIgnoreCase(event)) {
                processStatusEvent(obj);
            }
        }
    }

}
