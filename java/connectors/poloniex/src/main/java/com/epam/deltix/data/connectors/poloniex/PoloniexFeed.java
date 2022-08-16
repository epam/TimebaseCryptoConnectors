package com.epam.deltix.data.connectors.poloniex;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.util.collections.generated.LongToObjectHashMap;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PoloniexFeed extends MdSingleWsFeed {
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();
    private final LongToObjectHashMap<String> channelToInstrument = new LongToObjectHashMap<>();
    private long lastPingTime;
    private long pingTimeout = 15000;

    public PoloniexFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final String... symbols) {

        super("POLONIEX",
                uri,
                depth,
                20000,
                selected,
                output,
                errorListener,
                logger,
                symbols);
    }

    @Override
    protected void subscribe(final JsonWriter jsonWriter, final String... symbols) {
        channelToInstrument.clear();
        if (selected().level1() || selected().level2() || selected().trades()) {
            JsonValue subscriptionJson = JsonValue.newObject();
            JsonObject body = subscriptionJson.asObject();

            body.putString("event", "subscribe");

            JsonArray channelsArray = body.putArray("channel");
            channelsArray.addString("book_lv2");
            channelsArray.addString("trades");

            JsonArray symbolsArray = body.putArray("symbols");
            Arrays.stream(symbols).forEach(symbol -> {
                symbolsArray.addString(symbol);
            });

            lastPingTime = System.currentTimeMillis();

            subscriptionJson.toJsonAndEoj(jsonWriter);
        }
    }

    @Override
    protected void onJson(final CharSequence data, final boolean last, final JsonWriter jsonWriter) {
        jsonParser.parse(data);

        if (!last) {
            return;
        }

        JsonValue jsonValue = jsonParser.eoj();
        JsonObject object = jsonValue.asObject();
        String event = object.getString("event");
        String channel = object.getString("channel");

        if ("book_lv2".equals(channel) && !"subscribe".equals(event)) {
            String action = object.getString("action");
            if (action != null) {
                JsonObject jasonData = object.getArray("data").
                        items().collect(Collectors.toList()).get(0).asObject();

                if ("snapshot".equals(action)) {
                    String symbol = jasonData.getString("symbol");
                    long timestamp = jasonData.getLong("createTime");

                    QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(symbol, timestamp);

                    processSnapshotSide(quotesListener, jasonData.getArray("asks"), true);
                    processSnapshotSide(quotesListener, jasonData.getArray("bids"), false);

                    quotesListener.onFinish();
                } else if ("update".equals(action)) {
                    String symbol = jasonData.getString("symbol");
                    long timestamp = jasonData.getLong("createTime");

                    QuoteSequenceProcessor quotesListener = processor().onBookUpdate(symbol, timestamp);

                    processChanges(quotesListener, jasonData.getArray("bids"), false);
                    processChanges(quotesListener, jasonData.getArray("asks"), true);

                    quotesListener.onFinish();
                }

                //ping server
                long now = System.currentTimeMillis();
                long timeDelta = now - lastPingTime;

                if (timeDelta > pingTimeout) {
                    lastPingTime = now;
                    JsonValue pingJson = JsonValue.newObject();
                    JsonObject pingBody = pingJson.asObject();

                    pingBody.putString("event", "ping");

                    pingJson.toJsonAndEoj(jsonWriter);
                }
            }
        } else if ("trades".equals(channel) && !"subscribe".equals(event)) {
            JsonArray dataArray = object.getArrayRequired("data");
            for (int i = 0; i < dataArray.size(); ++i) {
                JsonObject trade = dataArray.getObjectRequired(i);
                long timestamp = trade.getLong("ts");
                String symbol = trade.getString("symbol");
                long price = trade.getDecimal64Required("price");
                long size = trade.getDecimal64Required("quantity");
                processor().onTrade(symbol, timestamp, price, size);
            }
        }
    }

    protected void processSnapshotSide(QuoteSequenceProcessor quotesListener, JsonArray quotePairs, boolean ask) {
        if (quotePairs == null) {
            return;
        }

        for (int i = 0; i < quotePairs.size(); i++) {
            final JsonArray pair = quotePairs.getArrayRequired(i);
            if (pair.size() < 2) {
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
            if (change.size() < 2) {
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
