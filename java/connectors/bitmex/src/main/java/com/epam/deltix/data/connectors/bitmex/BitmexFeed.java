package com.epam.deltix.data.connectors.bitmex;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.util.collections.generated.LongToLongHashMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BitmexFeed extends MdSingleWsFeed {
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();
    private final Iso8601DateTimeParser dtParser = new Iso8601DateTimeParser();
    private final Map<String, LongToLongHashMap> priceLevels = new HashMap<>();

    public BitmexFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final String... symbols) {

        super("BITMEX", uri, depth, 5000, selected, output, errorListener, symbols);
    }

    @Override
    protected void prepareSubscription(JsonWriter jsonWriter, String... symbols) {
        JsonValue subscriptionJson = JsonValue.newObject();
        JsonObject body = subscriptionJson.asObject();

        body.putString("op", "subscribe");

        JsonArray pairs = body.putArray("args");
        if (selected().level1() || selected().level2()) {
            Arrays.asList(symbols).forEach(s -> {
                pairs.addString("orderBookL2:" + s);
            });
        }

        if (selected().trades()) {
            Arrays.asList(symbols).forEach(s -> {
                pairs.addString("trade:" + s);
            });
        }

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
        if (object == null) {
            return;
        }

        String type = object.getString("table");
        if ("orderBookL2".equalsIgnoreCase(type)) {
            String action = object.getString("action");
            JsonArray bookData = object.getArray("data");
            if (bookData != null && bookData.size() > 0) {
                JsonObject firstObject = bookData.getObject(0);
                String instrument = firstObject.getString("symbol");
                LongToLongHashMap idToPrice = getPriceLevels(instrument);
                if ("partial".equalsIgnoreCase(action)) {
                    QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(instrument, getTimestamp(bookData));
                    processSnapshot(instrument, quotesListener, idToPrice, bookData);
                    quotesListener.onFinish();
                } else {
                    QuoteSequenceProcessor quotesListener = processor().onBookUpdate(instrument, getTimestamp(bookData));
                    processChanges(instrument, quotesListener, idToPrice, action, bookData);
                    quotesListener.onFinish();
                }
            }
        } else if ("trade".equalsIgnoreCase(type)) {
            String action = object.getString("action");
            if ("insert".equalsIgnoreCase(action)) {
                JsonArray tradeData = object.getArray("data");
                if (tradeData != null) {
                    for (int i = 0; i < tradeData.size(); ++i) {
                        JsonObject trade = tradeData.getObject(i);
                        long price = trade.getDecimal64Required("price");
                        long size = trade.getDecimal64Required("size");
                        String symbol = trade.getStringRequired("symbol");
                        long timestamp = dtParser.set(trade.getStringRequired("timestamp")).millis();

                        processor().onTrade(symbol, timestamp, price, size);
                    }
                }
            }
        }
    }

    private LongToLongHashMap getPriceLevels(String instrument) {
        return priceLevels.computeIfAbsent(instrument, k -> new LongToLongHashMap());
    }

    private long getTimestamp(JsonArray quotePairs) {
        long timestamp = TimeConstants.TIMESTAMP_UNKNOWN;
        if (quotePairs == null) {
            return timestamp;
        }

        for (int i = 0; i < quotePairs.size(); i++) {
            JsonObject pair = quotePairs.getObjectRequired(i);
            String timeString = pair.getString("timestamp");
            if (timeString != null) {
                timestamp = Math.max(timestamp, dtParser.set(timeString).millis());
            }
        }

        return timestamp;
    }

    private void processSnapshot(
            String instrument, QuoteSequenceProcessor quotesListener, LongToLongHashMap idToPrice, JsonArray quotePairs) {

        if (quotePairs == null) {
            return;
        }

        idToPrice.clear();

        for (int i = 0; i < quotePairs.size(); i++) {
            JsonObject pair = quotePairs.getObjectRequired(i);
            String symbol = pair.getString("symbol");
            if (!instrument.equalsIgnoreCase(symbol)) {
                throw new RuntimeException("Invalid symbol in snapshot: " + symbol);
            }

            long id = pair.getLong("id");
            long size = pair.getDecimal64Required("size");
            long price = pair.getDecimal64Required("price");
            boolean isOffer = "sell".equalsIgnoreCase(pair.getStringRequired("side"));

            idToPrice.put(id, price);

            quotesListener.onQuote(price, size, isOffer);
        }
    }

    private void processChanges(
        String instrument, QuoteSequenceProcessor quotesListener,
        LongToLongHashMap idToPrice, String action, JsonArray changes) {

        if (changes == null) {
            return;
        }

        for (int i = 0; i < changes.size(); i++) {
            JsonObject change = changes.getObjectRequired(i);

            String symbol = change.getString("symbol");
            if (!instrument.equalsIgnoreCase(symbol)) {
                throw new RuntimeException("Invalid symbol in snapshot: " + symbol);
            }

            long id = change.getLong("id");
            long size = TypeConstants.DECIMAL_NULL;
            long price = TypeConstants.DECIMAL_NULL;
            if ("insert".equalsIgnoreCase(action)) {
                price = change.getDecimal64Required("price");
                size = change.getDecimal64Required("size");
                idToPrice.put(id, price);
            } else if ("delete".equalsIgnoreCase(action)) {
                price = idToPrice.remove(id, Long.MIN_VALUE);
                if (price == Long.MIN_VALUE) {
                    throw new RuntimeException("Unknown price with id: " + id);
                }
            } else if ("update".equalsIgnoreCase(action)) {
                price = idToPrice.get(id, Long.MIN_VALUE);
                size = change.getDecimal64Required("size");
                if (price == Long.MIN_VALUE) {
                    throw new RuntimeException("Unknown price with id: " + id);
                }
            }
            boolean isOffer = "sell".equalsIgnoreCase(change.getStringRequired("side"));

            quotesListener.onQuote(price, size, isOffer);
        }
    }
}
