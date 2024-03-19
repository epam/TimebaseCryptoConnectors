package com.epam.deltix.data.connectors.bitmart;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.timebase.messages.universal.AggressorSide;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BitmartSpotFeed extends MdSingleWsFeed {
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();

    private final int depth;

    private final Set<String> skipSnapshotsSet = new HashSet<>();

    public BitmartSpotFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final String... symbols) {

        super("BITMART",
                uri,
                depth,
                10000,
                selected,
                output,
                errorListener,
                logger,
                symbols);
        this.depth = depth;
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
                args.addString(String.format("spot/depth%s:%s", getBitmartAvailableDepth(), s));
            }

            if (selected().trades()) {
                args.addString(String.format("spot/trade:%s", s));
            }
        });

        subscriptionJson.toJsonAndEoj(jsonWriter);
    }

    private int getBitmartAvailableDepth() {
        if (depth <= 5) {
            return 5;
        } else if (depth <= 20) {
            return 20;
        } else {
            return 50;
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
        String type = object.getString("table");
        if (type == null) {
            return;
        }
        String[] typeSplitted = type.split("/");
        if (typeSplitted.length < 2) {
            return;
        }

        if (typeSplitted[1].startsWith("depth")) {
            JsonArray jsonData = object.getArray("data");
            for (int i = 0; i < jsonData.size(); ++i) {
                JsonObject snapshot = jsonData.getObject(i);
                String instrument = snapshot.getStringRequired("symbol");
                long timestamp = snapshot.getLong("ms_t");
                QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(instrument, timestamp);
                processSnapshotSide(quotesListener, snapshot.getArray("bids"), false);
                processSnapshotSide(quotesListener, snapshot.getArray("asks"), true);
                quotesListener.onFinish();
            }
        } else if (typeSplitted[1].startsWith("trade")) {
            JsonArray jsonData = object.getArray("data");
            for (int i = 0; i < jsonData.size(); ++i) {
                JsonObject trade = jsonData.getObject(i);
                String symbol = trade.getString("symbol");
                if (skipSnapshotsSet.contains(symbol)) {
                    String tradeDirection = trade.getString("side");
                    AggressorSide side = "buy".equalsIgnoreCase(tradeDirection) ? AggressorSide.BUY : AggressorSide.SELL;

                    processor().onTrade(
                        symbol, trade.getLong("s_t"),
                        trade.getDecimal64Required("price"),
                        trade.getDecimal64Required("size"),
                        side
                    );
                } else {
                    // skip trade snapshot
                    skipSnapshotsSet.add(symbol);
                    return;
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

}
