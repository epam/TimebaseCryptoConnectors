package com.epam.deltix.data.connectors.okex;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.timebase.messages.TypeConstants;

import java.util.Arrays;

public class OkexSpotFeed extends MdSingleWsFeed {
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();

    public OkexSpotFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final String... symbols) {

        super("OKEX",
                uri,
                depth,
                30000,
                selected,
                output,
                errorListener,
                logger,
                symbols);
    }

    @Override
    protected void subscribe(JsonWriter jsonWriter, String... symbols) {
        JsonValue subscriptionJson = JsonValue.newObject();
        JsonObject body = subscriptionJson.asObject();
        body.putString("op", "subscribe");
        JsonArray args = body.putArray("args");
        Arrays.asList(symbols).forEach(s -> {
            if (selected().level1() || selected().level2()) {
                JsonObject object = args.addObject();
                object.putString("channel", "books");
                object.putString("instId", s);
            }

            if (selected().trades()) {
                JsonObject object = args.addObject();
                object.putString("channel", "trades");
                object.putString("instId", s);
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

        JsonObject arg = object.getObject("arg");
        String channel = arg.getString("channel");
        String instrument = arg.getStringRequired("instId");

        JsonArray arrayData = object.getArray("data");
        if (arrayData == null || arrayData.size() < 1) {
            return;
        }

        if ("books".equalsIgnoreCase(channel)) {
            String action = object.getStringRequired("action");
            JsonObject jsonData = arrayData.getObject(0);
            long timestamp = getTimestamp(jsonData.getString("ts"));
            if ("snapshot".equalsIgnoreCase(action)) {
                QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(instrument, timestamp);
                processSnapshotSide(quotesListener, jsonData.getArray("bids"), false);
                processSnapshotSide(quotesListener, jsonData.getArray("asks"), true);
                quotesListener.onFinish();
            } else if ("update".equalsIgnoreCase(action)) {
                QuoteSequenceProcessor quotesListener = processor().onBookUpdate(instrument, timestamp);
                processChanges(quotesListener, jsonData.getArray("bids"), false);
                processChanges(quotesListener, jsonData.getArray("asks"), true);
                quotesListener.onFinish();
            }
        } else if ("trades".equalsIgnoreCase(channel)) {
            JsonArray jsonDataArray = object.getArrayRequired("data");
            for (int i = 0; i < jsonDataArray.size(); ++i) {
                JsonObject trade = jsonDataArray.getObjectRequired(i);
                long timestamp = getTimestamp(trade.getString("ts"));
                long price = trade.getDecimal64Required("px");
                long size = trade.getDecimal64Required("sz");

                processor().onTrade(instrument, timestamp, price, size);
            }
        }
    }

    private void processSnapshotSide(QuoteSequenceProcessor quotesListener, JsonArray quotePairs, boolean ask) {
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

    private long getTimestamp(String tsString) {
        long timestamp = TimeConstants.TIMESTAMP_UNKNOWN;
        if (tsString != null) {
            timestamp = Long.parseLong(tsString);
        }

        return timestamp;
    }

}
