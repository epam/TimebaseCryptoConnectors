package com.epam.deltix.data.connectors.coinflex;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.timebase.messages.universal.AggressorSide;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CoinflexSpotFeed extends MdSingleWsFeed {
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();

    private final Set<String> skipSnapshotsSet = new HashSet<>();

    public CoinflexSpotFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final String... symbols) {

        super("COINFLEX",
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
        skipSnapshotsSet.clear();

        JsonValue subscriptionJson = JsonValue.newObject();
        JsonObject body = subscriptionJson.asObject();

        body.putString("op", "subscribe");
        JsonArray args = body.putArray("args");
        Arrays.asList(symbols).forEach(s -> {
            if (selected().level1() || selected().level2()) {
                args.addString(String.format("depth:%s", s));
            }

            if (selected().trades()) {
                args.addString(String.format("trade:%s", s));
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
        if ("depth".equalsIgnoreCase(type)) {
            JsonArray jsonData = object.getArray("data");
            if (jsonData.size() == 0) {
                return;
            }

            JsonObject snapshot = jsonData.getObject(0);
            String instrument = snapshot.getStringRequired("instrumentId");
            String timestampStr = snapshot.getStringRequired("timestamp");
            long timestamp = Long.parseLong(timestampStr);

            QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(instrument, timestamp);
            processSnapshotSide(quotesListener, snapshot.getArray("bids"), false);
            processSnapshotSide(quotesListener, snapshot.getArray("asks"), true);
            quotesListener.onFinish();
        } else if ("trade".equalsIgnoreCase(type)) {
            JsonArray jsonData = object.getArray("data");
            for (int i = 0; i < jsonData.size(); ++i) {
                JsonObject trade = jsonData.getObject(i);
                String tradeDirection = trade.getString("side");

                AggressorSide side = "buy".equalsIgnoreCase(tradeDirection) ? AggressorSide.BUY : AggressorSide.SELL;

                processor().onTrade(
                    trade.getString("marketCode"),
                    Long.parseLong(trade.getStringRequired("timestamp")),
                    trade.getDecimal64Required("price"),
                    trade.getDecimal64Required("quantity"),
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

}
