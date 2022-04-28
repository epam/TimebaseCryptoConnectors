package com.epam.deltix.data.connectors.cryptofacilities;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.JsonArray;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.commons.json.JsonValue;
import com.epam.deltix.data.connectors.commons.json.JsonValueParser;
import com.epam.deltix.data.connectors.commons.json.JsonWriter;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;

import java.util.Arrays;

public class CryptofacilitiesFeed extends MdSingleWsFeed {
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();

    public CryptofacilitiesFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final String... symbols) {

        super("CRYPTOFS", uri, depth, 5000, selected, output, errorListener, symbols);
    }

    @Override
    protected void subscribe(JsonWriter jsonWriter, String... symbols) {
        if (selected().level1() || selected().level2()) {
            JsonValue subscriptionJson = JsonValue.newObject();
            JsonObject body = subscriptionJson.asObject();

            body.putString("event", "subscribe");
            body.putString("feed", "book");

            JsonArray productIds = body.putArray("product_ids");
            Arrays.stream(symbols).forEach(productIds::addString);

            subscriptionJson.toJsonAndEoj(jsonWriter);
        }

        if (selected().trades()) {
            JsonValue subscriptionJson = JsonValue.newObject();
            JsonObject body = subscriptionJson.asObject();

            body.putString("event", "subscribe");
            body.putString("feed", "trade");

            JsonArray productIds = body.putArray("product_ids");
            Arrays.stream(symbols).forEach(productIds::addString);

            subscriptionJson.toJsonAndEoj(jsonWriter);
        }

        JsonValue subscriptionJson = JsonValue.newObject();
        JsonObject body = subscriptionJson.asObject();

        body.putString("event", "subscribe");
        body.putString("feed", "heartbeat");

        subscriptionJson.toJsonAndEoj(jsonWriter);
    }

    @Override
    protected void onJson(CharSequence data, boolean last, JsonWriter jsonWriter) {
        jsonParser.parse(data);

        if (!last) {
            return;
        }

        JsonValue jsonValue = jsonParser.eoj();
        JsonObject object = jsonValue.asObjectRequired();

        String feed = object.getString("feed");
        String instrument = object.getString("product_id");
        if (instrument == null) {
            return;
        }

        long timestamp = object.getLong("timestamp");
        if ("book_snapshot".equalsIgnoreCase(feed)) {
            QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(instrument, timestamp);
            processSnapshotSide(quotesListener, object.getArray("bids"), false);
            processSnapshotSide(quotesListener, object.getArray("asks"), true);
            quotesListener.onFinish();
        } else if ("book".equalsIgnoreCase(feed)) {
            QuoteSequenceProcessor quotesListener = processor().onBookUpdate(instrument, timestamp);
            long size = object.getDecimal64Required("qty");
            if (Decimal64Utils.isZero(size)) {
                size = TypeConstants.DECIMAL_NULL; // means delete the price
            }
            quotesListener.onQuote(
                object.getDecimal64Required("price"),
                size,
                "sell".equals(object.getStringRequired("side"))
            );
            quotesListener.onFinish();
        } else if ("trade".equalsIgnoreCase(feed)) {
            processor().onTrade(
                instrument,
                timestamp,
                object.getDecimal64Required("price"),
                object.getDecimal64Required("qty")
            );
        }
    }

    private void processSnapshotSide(QuoteSequenceProcessor quotesListener, JsonArray quotePairs, boolean ask) {
        if (quotePairs == null) {
            return;
        }

        for (int i = 0; i < quotePairs.size(); i++) {
            JsonObject quote = quotePairs.getObjectRequired(i);
            quotesListener.onQuote(
                quote.getDecimal64Required("price"),
                quote.getDecimal64Required("qty"),
                ask
            );
        }
    }

}
