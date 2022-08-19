package com.epam.deltix.data.connectors.hitbtc;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.universal.AggressorSide;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public class HitbtcFeed extends MdSingleWsFeed {
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();

    private final AtomicLong idGenerator = new AtomicLong();

    public HitbtcFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final String... symbols) {

        super("HITBTC",
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
    protected void subscribe(JsonWriter jsonWriter, String... symbols) {
        if (selected().level1() || selected().level2()) {
            JsonValue subscriptionJson = JsonValue.newObject();
            JsonObject body = subscriptionJson.asObject();

            body.putString("method", "subscribe");
            body.putString("ch", "orderbook/full");
            body.putLong("id", idGenerator.incrementAndGet());

            JsonObject params = body.putObject("params");
            JsonArray array = params.putArray("symbols");
            Arrays.asList(symbols).forEach(array::addString);

            subscriptionJson.toJsonAndEoj(jsonWriter);
        }

        if (selected().trades()) {
            JsonValue subscriptionJson = JsonValue.newObject();
            JsonObject body = subscriptionJson.asObject();

            body.putString("method", "subscribe");
            body.putString("ch", "trades");
            body.putLong("id", idGenerator.incrementAndGet());

            JsonObject params = body.putObject("params");
            JsonArray array = params.putArray("symbols");
            Arrays.asList(symbols).forEach(array::addString);

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

        String channel = object.getString("ch");
        if ("orderbook/full".equalsIgnoreCase(channel)) {
            JsonObject snapshot = object.getObject("snapshot");
            JsonObject update = object.getObject("update");
            if (snapshot != null) {
                snapshot.forEachObject((instrument, symbolSnapshot) -> {
                    long timestamp = symbolSnapshot.getLong("t");
                    QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(instrument, timestamp);
                    processSnapshotSide(quotesListener, symbolSnapshot.getArray("b"), false);
                    processSnapshotSide(quotesListener, symbolSnapshot.getArray("a"), true);
                    quotesListener.onFinish();
                });
            } else if (update != null) {
                update.forEachObject((instrument, symbolUpdate) -> {
                    long timestamp = symbolUpdate.getLong("t");
                    QuoteSequenceProcessor quotesListener = processor().onBookUpdate(instrument, timestamp);
                    processUpdates(quotesListener, symbolUpdate.getArray("b"), false);
                    processUpdates(quotesListener, symbolUpdate.getArray("a"), true);
                    quotesListener.onFinish();
                });
            }
        } else if ("trades".equalsIgnoreCase(channel)) {
            JsonObject update = object.getObject("update");
            if (update != null) {
                update.forEachArray((instrument, trades) -> {
                    for (int i = 0; i < trades.size(); ++i) {
                        JsonObject trade = trades.getObject(i);
                        String tradeDirection = trade.getString("s");
                        AggressorSide side = "buy".equalsIgnoreCase(tradeDirection) ? AggressorSide.BUY : AggressorSide.SELL;

                        processor().onTrade(instrument,
                            trade.getLongRequired("t"),
                            trade.getDecimal64Required("p"),
                            trade.getDecimal64Required("q"),
                            side
                        );
                    }
                });
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

    private void processUpdates(
        final QuoteSequenceProcessor quotesListener, final JsonArray quotePairs, final boolean ask) {

        if (quotePairs == null) {
            return;
        }

        for (int i = 0; i < quotePairs.size(); i++) {
            final JsonArray pair = quotePairs.getArrayRequired(i);
            if (pair.size() != 2) {
                throw new IllegalArgumentException("Unexpected size of quote: " + pair.size());
            }

            long size = pair.getDecimal64Required(1);
            if (Decimal64Utils.isZero(size)) {
                size = TypeConstants.DECIMAL_NULL; // means delete the price
            }

            quotesListener.onQuote(
                pair.getDecimal64Required(0),
                size,
                ask
            );
        }
    }

}
