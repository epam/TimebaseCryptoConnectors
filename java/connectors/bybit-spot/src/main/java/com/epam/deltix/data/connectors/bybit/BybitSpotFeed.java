package com.epam.deltix.data.connectors.bybit;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.universal.AggressorSide;

import java.util.Arrays;

public class BybitSpotFeed extends MdSingleWsFeed {
    private static final long PING_PERIOD = 10000;

    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();

    public BybitSpotFeed(
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
                jsonWriter.objectMember("ping");
                jsonWriter.numberValue(System.currentTimeMillis());
                jsonWriter.endObject();
                jsonWriter.eoj();
            }
        };
    }

    @Override
    protected void subscribe(JsonWriter jsonWriter, String... symbols) {
        Arrays.asList(symbols).forEach(s -> {
            if (selected().level1() || selected().level2()) {
                JsonValue subscriptionJson = JsonValue.newObject();
                JsonObject body = subscriptionJson.asObject();
                body.putString("topic", "diffDepth");
                body.putString("event", "sub");
                body.putString("symbol", s);
                JsonObject params = body.putObject("params");
                params.putBoolean("binary", false);

                subscriptionJson.toJsonAndEoj(jsonWriter);
            }
            if (selected().trades()) {
                JsonValue subscriptionJson = JsonValue.newObject();
                JsonObject body = subscriptionJson.asObject();
                body.putString("topic", "trade");
                body.putString("event", "sub");
                body.putString("symbol", s);
                JsonObject params = body.putObject("params");
                params.putBoolean("binary", false);

                subscriptionJson.toJsonAndEoj(jsonWriter);
            }
        });
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
        String instrument = object.getString("symbol");
        boolean first = object.getBoolean("f");
        if ("diffDepth".equalsIgnoreCase(topic)) {
            JsonArray jsonDataArray = object.getArrayRequired("data");
            for (int i = 0; i < jsonDataArray.size(); ++i) {
                JsonObject jsonData = jsonDataArray.getObjectRequired(i);
                long timestamp = jsonData.getLong("t");
                if (first) {
                    QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(instrument, timestamp);
                    processSnapshotSide(quotesListener, jsonData.getArray("b"), false);
                    processSnapshotSide(quotesListener, jsonData.getArray("a"), true);
                    quotesListener.onFinish();
                } else {
                    QuoteSequenceProcessor quotesListener = processor().onBookUpdate(instrument, timestamp);
                    processChanges(quotesListener, jsonData.getArray("b"), false);
                    processChanges(quotesListener, jsonData.getArray("a"), true);
                    quotesListener.onFinish();
                }
            }
        } else if ("trade".equalsIgnoreCase(topic)) {
            if (!first) { // skip snapshots
                JsonArray jsonDataArray = object.getArrayRequired("data");
                for (int i = 0; i < jsonDataArray.size(); ++i) {
                    JsonObject trade = jsonDataArray.getObjectRequired(i);
                    long timestamp = trade.getLong("t");
                    long price = trade.getDecimal64Required("p");
                    long size = trade.getDecimal64Required("q");
                    boolean isBuySide = trade.getBoolean("m");

                    AggressorSide side = isBuySide ? AggressorSide.BUY : AggressorSide.SELL;

                    processor().onTrade(instrument, timestamp, price, size, side);
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
