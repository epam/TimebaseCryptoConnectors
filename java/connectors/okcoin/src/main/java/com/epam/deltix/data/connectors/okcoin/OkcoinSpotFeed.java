package com.epam.deltix.data.connectors.okcoin;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.universal.AggressorSide;

import java.util.Arrays;

public class OkcoinSpotFeed extends MdSingleWsFeed {
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();
    private final Iso8601DateTimeParser dtParser = new Iso8601DateTimeParser();

    public OkcoinSpotFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final String... symbols) {

        super("OKCOIN",
                uri,
                depth,
                60000,
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
                args.addString(String.format("spot/depth:%s", s));
            }

            if (selected().trades()) {
                args.addString(String.format("spot/trade:%s", s));
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
        String type = object.getString("table");
        if ("spot/depth".equalsIgnoreCase(type)) {
            JsonArray jsonData = object.getArray("data");
            if (jsonData.size() == 0) {
                return;
            }

            String action = object.getStringRequired("action");
            if ("partial".equalsIgnoreCase(action)) {
                for (int i = 0; i < jsonData.size(); ++i) {
                    JsonObject snapshot = jsonData.getObject(i);
                    String instrument = snapshot.getStringRequired("instrument_id");
                    long timestamp = dtParser.set(snapshot.getStringRequired("timestamp")).millis();

                    QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(instrument, timestamp);
                    processSnapshotSide(quotesListener, snapshot.getArray("bids"), false);
                    processSnapshotSide(quotesListener, snapshot.getArray("asks"), true);
                    quotesListener.onFinish();
                }
            } else if ("update".equalsIgnoreCase(action)) {
                for (int i = 0; i < jsonData.size(); ++i) {
                    JsonObject updates = jsonData.getObject(i);
                    String instrument = updates.getStringRequired("instrument_id");
                    long timestamp = dtParser.set(updates.getStringRequired("timestamp")).millis();

                    QuoteSequenceProcessor quotesListener = processor().onBookUpdate(instrument, timestamp);
                    processChanges(quotesListener, updates.getArray("bids"), false);
                    processChanges(quotesListener, updates.getArray("asks"), true);
                    quotesListener.onFinish();
                }
            }
        } else if ("spot/trade".equalsIgnoreCase(type)) {
            JsonArray jsonData = object.getArray("data");
            for (int i = 0; i < jsonData.size(); ++i) {
                JsonObject trade = jsonData.getObject(i);

                String tradeSide = trade.getString("side");
                AggressorSide side = "buy".equalsIgnoreCase(tradeSide) ? AggressorSide.BUY : AggressorSide.SELL;

                processor().onTrade(
                    trade.getString("instrument_id"),
                    dtParser.set(trade.getStringRequired("timestamp")).millis(),
                    trade.getDecimal64Required("price"),
                    trade.getDecimal64Required("size"),
                    side
                );
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


    private void processChanges(
        final QuoteSequenceProcessor quotesListener, final JsonArray changes, final boolean ask) {

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
                ask
            );
        }
    }

}
