package com.epam.deltix.data.connectors.bybit;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.universal.AggressorSide;

import java.util.HashSet;
import java.util.Set;

public class BybitFeed extends MdSingleWsFeed {
    private static final long PING_PERIOD = 10000;

    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();

    private final BybitEndpoint endpoint;

    private int subscriptionLimit;
    private int subscriptionBookDepth = 50;

    public BybitFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final String... symbols) {

        super("BYBIT",
                uri,
                depth,
                30000,
                selected,
                output,
                errorListener,
                logger,
                getPeriodicalJsonTask(),
                symbols);

        endpoint = BybitEndpoint.typeFromAddress(uri);
        subscriptionLimit = 100;
        if (endpoint != null) {
            if (endpoint == BybitEndpoint.Spot) {
                subscriptionLimit = 5;

                if (depth == 1) {
                    subscriptionBookDepth = 1;
                } else if (depth <= 50) {
                    subscriptionBookDepth = 50;
                } else {
                    subscriptionBookDepth = 200;
                }
            } else if (endpoint == BybitEndpoint.Linear || endpoint == BybitEndpoint.Inverse) {
                if (depth == 1) {
                    subscriptionBookDepth = 1;
                } else if (depth <= 50) {
                    subscriptionBookDepth = 50;
                } else if (depth <= 200) {
                    subscriptionBookDepth = 200;
                } else {
                    subscriptionBookDepth = 500;
                }
            } else if (endpoint == BybitEndpoint.Option) {
                if (depth <= 25) {
                    subscriptionBookDepth = 25;
                } else {
                    subscriptionBookDepth = 100;
                }
            }
        }
    }

    private static PeriodicalJsonTask getPeriodicalJsonTask() {
        return new PeriodicalJsonTask() {
            @Override
            public long delayMillis() {
                return PING_PERIOD;
            }

            @Override
            public void execute(JsonWriter jsonWriter) {
                jsonWriter.startObject();
                jsonWriter.objectMember("op");
                jsonWriter.stringValue("ping");
                jsonWriter.endObject();
                jsonWriter.eoj();
            }
        };
    }

    @Override
    protected void subscribe(JsonWriter jsonWriter, String... symbols) {
        Set<String> partial = new HashSet<>();
        int limit = subscriptionLimit;
        int counter = 0;

        if (symbols.length < subscriptionLimit) {
            limit = symbols.length;
        }

        for (String symbol : symbols) {
            partial.add(symbol);

            if (partial.size() == limit) {
                subscribePartial(partial, jsonWriter);

                counter = counter + limit;
                partial.clear();
                if (counter + limit > symbols.length) {
                    limit = symbols.length - counter;
                }
            }
        }
    }

    private void subscribePartial(Set<String> symbols, JsonWriter jsonWriter) {
        JsonValue subscriptionJson = JsonValue.newObject();
        JsonObject body = subscriptionJson.asObject();
        body.putString("op", "subscribe");
        JsonArray args = body.putArray("args");
        symbols.forEach(s -> {
            if (selected().level1() || selected().level2()) {
                args.addString("orderbook." + subscriptionBookDepth + "." + s);
            }
            if (selected().trades()) {
                args.addString("publicTrade." + s);
            }
        });
        subscriptionJson.toJsonAndEoj(jsonWriter);
    }

    @Override
    protected void onJson(final CharSequence data, final boolean last, final JsonWriter jsonWriter) {
        jsonParser.parse(data);

        if (!last) {
            return;
        }

        JsonValue jsonValue = jsonParser.eoj();
        JsonObject object = jsonValue.asObject();

        String topic = object.getString("topic");
        if (topic == null) {
            return;
        }

        if (topic.startsWith("orderbook")) {
            String type = object.getString("type");
            long timestamp = object.getLong("ts");
            JsonObject jsonData = object.getObject("data");
            String symbol = jsonData.getString("s");
            if (type.equalsIgnoreCase("snapshot")) {
                QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(symbol, timestamp);
                processSnapshotSide(quotesListener, jsonData.getArray("b"), false);
                processSnapshotSide(quotesListener, jsonData.getArray("a"), true);
                quotesListener.onFinish();
            } else if (type.equalsIgnoreCase("delta")) {
                QuoteSequenceProcessor quotesListener = processor().onBookUpdate(symbol, timestamp);
                processChanges(quotesListener, jsonData.getArray("b"), false);
                processChanges(quotesListener, jsonData.getArray("a"), true);
                quotesListener.onFinish();
            }
        } else if (topic.startsWith("publicTrade")) {
            String type = object.getString("type");
            if (type.equalsIgnoreCase("snapshot")) {
                long timestamp = object.getLong("ts");
                JsonArray jsonDataArray = object.getArrayRequired("data");
                for (int i = 0; i < jsonDataArray.size(); ++i) {
                    JsonObject trade = jsonDataArray.getObjectRequired(i);
                    String symbol = trade.getString("s");
                    long price = trade.getDecimal64Required("p");
                    long size = trade.getDecimal64Required("v");
                    String sideStr = trade.getString("S");

                    AggressorSide side = sideStr.equalsIgnoreCase("buy") ? AggressorSide.BUY : AggressorSide.SELL;

                    processor().onTrade(symbol, timestamp, price, size, side);
                }
            }
        }
    }

    private void processSnapshotSide(QuoteSequenceProcessor quotesListener, JsonArray quotePairs, boolean ask) {
        if (quotePairs == null) {
            return;
        }

        for (int i = 0; i < quotePairs.size(); i++) {
            final JsonArray pair = quotePairs.getArrayRequired(i);
            if (pair.size() != 2) {
                throw new IllegalArgumentException("Unexpected size of "
                    + (ask ? "an ask" : "a bid")
                    + " quote: "
                    + pair.size());
            }
            quotesListener.onQuote(
                pair.getDecimal64Required(0),
                pair.getDecimal64Required(1),
                ask
            );
        }
    }

    private void processChanges(QuoteSequenceProcessor quotesListener, JsonArray changes, boolean isAsk) {
        if (changes == null) {
            return;
        }

        for (int i = 0; i < changes.size(); i++) {
            final JsonArray change = changes.getArrayRequired(i);
            if (change.size() != 2) {
                throw new IllegalArgumentException("Unexpected size of a change :" + change.size());
            }

            long size = change.getDecimal64Required(1);
            if (Decimal64Utils.isZero(size)) {
                size = TypeConstants.DECIMAL_NULL; // means delete the price
            }

            quotesListener.onQuote(
                change.getDecimal64Required(0),
                size,
                isAsk
            );
        }
    }

}
