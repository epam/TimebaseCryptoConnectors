package com.epam.deltix.data.connectors.kraken;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.universal.AggressorSide;

import java.util.Arrays;

public class KrakenFuturesFeed extends MdSingleWsFeed {
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();

    public KrakenFuturesFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final String... symbols) {

        super("KRAKEN",
                uri,
                depth,
                5000,
                selected,
                output,
                errorListener,
                logger,
                symbols);
    }

    @Override
    protected void subscribe(JsonWriter jsonWriter, String... symbols) {
        if (selected().level1() || selected().level2()) {
            JsonValue subscriptionJson = JsonValue.newObject();
            JsonObject body = subscriptionJson.asObject();

            body.putString("event", "subscribe");
            body.putString("feed", "book");
            JsonArray pairs = body.putArray("product_ids");
            Arrays.asList(symbols).forEach(pairs::addString);

            subscriptionJson.toJsonAndEoj(jsonWriter);
        }

        if (selected().trades()) {
            JsonValue subscriptionJson = JsonValue.newObject();
            JsonObject body = subscriptionJson.asObject();

            body.putString("event", "subscribe");
            body.putString("feed", "trade");
            JsonArray pairs = body.putArray("product_ids");
            Arrays.asList(symbols).forEach(pairs::addString);

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
        if (object == null) {
            return;
        }

        String instrument = object.getString("product_id");
        if (instrument == null) {
            return;
        }

        String feed = object.getString("feed");
        if ("book_snapshot".equalsIgnoreCase(feed)) {
            long timestamp = object.getLong("timestamp");

            QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(instrument, timestamp);
            processSnapshotSide(quotesListener, object.getArray("bids"), false);
            processSnapshotSide(quotesListener, object.getArray("asks"), true);
            quotesListener.onFinish();
        } else if ("book".equalsIgnoreCase(feed)) {
            long timestamp = object.getLong("timestamp");

            QuoteSequenceProcessor quotesListener = processor().onBookUpdate(instrument, timestamp);
            long size = object.getDecimal64Required("qty");
            if (Decimal64Utils.isZero(size)) {
                size = TypeConstants.DECIMAL_NULL; // means delete the price
            }
            quotesListener.onQuote(
                object.getDecimal64Required("price"),
                size,
                "sell".equalsIgnoreCase(object.getStringRequired("side"))
            );
            quotesListener.onFinish();
        } else if ("trade".equalsIgnoreCase(feed)) {
            String tradeDirection = object.getString("side");
            AggressorSide side = "buy".equalsIgnoreCase(tradeDirection) ? AggressorSide.BUY : AggressorSide.SELL;

            processor().onTrade(instrument,
                object.getLong("time"),
                object.getDecimal64Required("price"),
                object.getDecimal64Required("qty"),
                side
            );
        }
    }

    private void processSnapshotSide(QuoteSequenceProcessor quotesListener, JsonArray quotePairs, boolean ask) {
        if (quotePairs == null) {
            return;
        }

        for (int i = 0; i < quotePairs.size(); i++) {
            JsonObject pair = quotePairs.getObjectRequired(i);
            quotesListener.onQuote(
                pair.getDecimal64Required("price"),
                pair.getDecimal64Required("qty"),
                ask
            );
        }
    }
}
