package com.epam.deltix.data.connectors.ftx;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;

public class FtxFeed extends MdSingleWsFeed {
    private static final long PING_PERIOD = 5000;
    private static final BigDecimal TIME_MILLIS_SCALE = new BigDecimal(1000);
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();

    public FtxFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final String... symbols) {

        super("FTX", uri, depth, 15000, selected, output, errorListener, getPeriodicalJsonTask(), symbols);
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
    protected void prepareSubscription(JsonWriter jsonWriter, String... symbols) {
        if (selected().level1() || selected().level2()) {
            Arrays.asList(symbols).forEach(s -> {
                JsonValue subscriptionJson = JsonValue.newObject();
                JsonObject body = subscriptionJson.asObject();

                body.putString("op", "subscribe");
                body.putString("channel", "orderbook");
                body.putString("market", s);

                subscriptionJson.toJsonAndEoj(jsonWriter);
            });
        }

        if (selected().trades()) {
            Arrays.asList(symbols).forEach(s -> {
                JsonValue subscriptionJson = JsonValue.newObject();
                JsonObject body = subscriptionJson.asObject();

                body.putString("op", "subscribe");
                body.putString("channel", "trades");
                body.putString("market", s);

                subscriptionJson.toJsonAndEoj(jsonWriter);
            });
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

        String channel = object.getString("channel");
        if ("orderbook".equalsIgnoreCase(channel)) {
            String instrument = object.getString("market");
            String type = object.getString("type");
            JsonObject jsonData = object.getObject("data");
            if ("partial".equalsIgnoreCase(type)) {
                long timestamp = getTimestamp(jsonData.getDecimal("time"));
                QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(instrument, timestamp);
                processSnapshotSide(quotesListener, jsonData.getArrayRequired("bids"), false);
                processSnapshotSide(quotesListener, jsonData.getArrayRequired("asks"), true);
                quotesListener.onFinish();
            } else if ("update".equalsIgnoreCase(type)) {
                JsonArray bids = jsonData.getArray("bids");
                JsonArray asks = jsonData.getArray("asks");
                if ((bids != null && bids.size() > 0) || (asks != null && asks.size() > 0)) {
                    long timestamp = getTimestamp(jsonData.getDecimal("time"));
                    QuoteSequenceProcessor quotesListener = processor().onBookUpdate(instrument, timestamp);
                    processChanges(quotesListener, bids, false);
                    processChanges(quotesListener, asks, true);
                    quotesListener.onFinish();
                }
            }
        } else if ("trades".equalsIgnoreCase(channel)) {
            String instrument = object.getString("market");
            JsonArray trades = object.getArray("data");
            if (trades != null) {
                for (int i = 0; i < trades.size(); ++i) {
                    JsonObject trade = trades.getObject(i);
                    long price = Decimal64Utils.fromBigDecimal(trade.getDecimalRequired("price"));
                    long size = Decimal64Utils.fromBigDecimal(trade.getDecimalRequired("size"));
                    long timestamp = OffsetDateTime.parse(trade.getString("time")).toInstant().toEpochMilli();
                    processor().onTrade(instrument, timestamp, price, size);
                }
            }
        }
    }

    private void processSnapshotSide(
            final QuoteSequenceProcessor quotesListener, final JsonArray quotePairs, final boolean ask) {

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

    private void processChanges(
            final QuoteSequenceProcessor quotesListener, final JsonArray changes, final boolean ask) {

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
                ask
            );
        }
    }

    private long getTimestamp(BigDecimal time) {
        return time.multiply(TIME_MILLIS_SCALE).longValue();
    }
}
