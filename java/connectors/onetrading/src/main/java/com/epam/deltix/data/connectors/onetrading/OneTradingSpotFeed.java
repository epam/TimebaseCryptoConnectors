package com.epam.deltix.data.connectors.onetrading;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.universal.AggressorSide;

import java.util.Arrays;

public class OneTradingSpotFeed extends MdSingleWsFeed {
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();

    private final Iso8601DateTimeParser dtParser = new Iso8601DateTimeParser();

    public OneTradingSpotFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final String... symbols) {

        super("BITPANDA",
                uri,
                depth,
                10000,
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

        body.putString("type", "SUBSCRIBE");
        JsonArray channels = body.putArray("channels");
        if (selected().level1() || selected().level2()) {
            JsonObject object = channels.addObject();
            object.putString("name", "ORDER_BOOK");
            JsonArray array = object.putArray("instrument_codes");
            Arrays.asList(symbols).forEach(array::addString);
        }

        if (selected().trades()) {
            JsonObject object = channels.addObject();
            object.putString("name", "PRICE_TICKS");
            JsonArray array = object.putArray("instrument_codes");
            Arrays.asList(symbols).forEach(array::addString);
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

        String type = object.getString("type");
        if ("ORDER_BOOK_SNAPSHOT".equalsIgnoreCase(type)) {
            String instrument = object.getString("instrument_code");
            long timestamp = object.getLong("time") / 1_000_000;
            QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(instrument, timestamp);
            processSnapshotSide(quotesListener, object.getArray("bids"), false);
            processSnapshotSide(quotesListener, object.getArray("asks"), true);
            quotesListener.onFinish();
        } else if ("ORDER_BOOK_UPDATE".equalsIgnoreCase(type)) {
            String instrument = object.getString("instrument_code");
            long timestamp = object.getLong("time") / 1_000_000;
            QuoteSequenceProcessor quotesListener = processor().onBookUpdate(instrument, timestamp);
            processUpdates(quotesListener, object.getArray("changes"));
            quotesListener.onFinish();
        } else if ("PRICE_TICK".equalsIgnoreCase(type)) {
            String instrument = object.getString("instrument_code");
            long timestamp = object.getLong("time") / 1_000_000;

            long price = object.getDecimal64Required("price");
            long size = object.getDecimal64Required("amount");
            String tradeDirection = object.getString("taker_side");

            AggressorSide side = "buy".equalsIgnoreCase(tradeDirection) ? AggressorSide.BUY : AggressorSide.SELL;

            processor().onTrade(instrument, timestamp, price, size, side);
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

    private void processUpdates(
        final QuoteSequenceProcessor quotesListener, final JsonArray quotePairs) {

        if (quotePairs == null) {
            return;
        }

        for (int i = 0; i < quotePairs.size(); i++) {
            final JsonArray pair = quotePairs.getArrayRequired(i);
            if (pair.size() != 3) {
                throw new IllegalArgumentException("Unexpected size of quote: " + pair.size());
            }

            long size = pair.getDecimal64Required(2);
            if (Decimal64Utils.isZero(size)) {
                size = TypeConstants.DECIMAL_NULL; // means delete the price
            }

            quotesListener.onQuote(
                pair.getDecimal64Required(1),
                size,
                "SELL".equalsIgnoreCase(pair.getStringRequired(0))
            );
        }
    }

}
