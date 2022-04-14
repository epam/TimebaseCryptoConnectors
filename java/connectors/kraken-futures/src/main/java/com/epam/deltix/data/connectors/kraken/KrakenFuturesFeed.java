package com.epam.deltix.data.connectors.kraken;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;

import java.util.Arrays;

public class KrakenFuturesFeed extends SingleWsFeed {
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();
    private final MarketDataProcessor dataProcessor;

    public KrakenFuturesFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final String... symbols)
    {
        super(uri, 5000, selected, output, errorListener, symbols);

        this.dataProcessor = new MarketDataProcessorImpl("KRAKEN", this, selected(), depth);
    }

    @Override
    protected void prepareSubscription(JsonWriter jsonWriter, String... symbols) {
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

            L2BookProcessor l2BookProcessor = dataProcessor.onBookSnapshot(instrument, timestamp);
            processSnapshotSide(l2BookProcessor, object.getArray("bids"), false);
            processSnapshotSide(l2BookProcessor, object.getArray("asks"), true);
            l2BookProcessor.onFinish();
        } else if ("book".equalsIgnoreCase(feed)) {
            long timestamp = object.getLong("timestamp");

            L2BookProcessor l2BookProcessor = dataProcessor.onBookUpdate(instrument, timestamp);
            long size = object.getDecimal64Required("qty");
            if (Decimal64Utils.isZero(size)) {
                size = TypeConstants.DECIMAL_NULL; // means delete the price
            }
            l2BookProcessor.onQuote(
                object.getDecimal64Required("price"),
                size,
                "sell".equalsIgnoreCase(object.getStringRequired("side"))
            );
            l2BookProcessor.onFinish();
        } else if ("trade".equalsIgnoreCase(feed)) {
            dataProcessor.onTrade(instrument,
                object.getLong("time"),
                object.getDecimal64Required("price"),
                object.getDecimal64Required("qty")
            );
        }
    }

    private void processSnapshotSide(L2BookProcessor l2BookProcessor, JsonArray quotePairs, boolean ask) {
        if (quotePairs == null) {
            return;
        }

        for (int i = 0; i < quotePairs.size(); i++) {
            JsonObject pair = quotePairs.getObjectRequired(i);
            l2BookProcessor.onQuote(
                pair.getDecimal64Required("price"),
                pair.getDecimal64Required("qty"),
                ask
            );
        }
    }
}
