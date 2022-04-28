package com.epam.deltix.data.connectors.coinbase;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.JsonArray;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.commons.json.JsonValue;
import com.epam.deltix.data.connectors.commons.json.JsonValueParser;
import com.epam.deltix.data.connectors.commons.json.JsonWriter;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.util.collections.generated.LongToObjectHashMap;

public class PoloniexFeed extends MdSingleWsFeed {
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();
    private final LongToObjectHashMap<String> channelToInstrument = new LongToObjectHashMap<>();

    public PoloniexFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final String... symbols) {

        super("POLONIEX", uri, depth, 20000, selected, output, errorListener, symbols);
    }

    @Override
    protected void subscribe(final JsonWriter jsonWriter, final String... symbols) {
        channelToInstrument.clear();
        if (selected().level1() || selected().level2() || selected().trades()) {
            JsonValue subscriptionJson = JsonValue.newObject();
            JsonObject body = subscriptionJson.asObject();
            // hearthbeat
            body.putString("command", "subscribe");
            body.putString("channel", String.join(",", symbols));
            subscriptionJson.toJsonAndEoj(jsonWriter);
        }

        JsonValue subscriptionJson = JsonValue.newObject();
        JsonObject body = subscriptionJson.asObject();
        // hearthbeat
        body.putString("command", "subscribe");
        body.putLong("channel", 1010);
        subscriptionJson.toJsonAndEoj(jsonWriter);
    }

    @Override
    protected void onJson(final CharSequence data, final boolean last,  final JsonWriter jsonWriter) {
        jsonParser.parse(data);

        if (!last) {
            return;
        }

        JsonValue jsonValue = jsonParser.eoj();
        JsonArray array = jsonValue.asArray();
        if (array == null || array.size() < 3) {
            return;
        }

        long channelId = array.getLong(0);
        JsonArray messageArray = array.getArray(2);
        if (messageArray == null) {
            return;
        }

        for (int i = 0; i < messageArray.size(); ++i) {
            JsonArray messagePartArray = messageArray.getArray(i);
            if (messagePartArray != null && messagePartArray.size() > 1) {
                String type = messagePartArray.getString(0);
                long timestamp = getTimestamp(
                    messagePartArray.getString(messagePartArray.size() - 1)
                );
                if ("i".equalsIgnoreCase(type)) {
                    JsonObject snapshot = messagePartArray.getObject(1);
                    if (snapshot != null) {
                        String instrument = snapshot.getStringRequired("currencyPair");
                        channelToInstrument.put(channelId, instrument);
                        JsonArray orderBook = snapshot.getArrayRequired("orderBook");
                        QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(instrument, timestamp);
                        processSnapshotSide(quotesListener, orderBook.getObject(0), true);
                        processSnapshotSide(quotesListener, orderBook.getObject(1), false);
                        quotesListener.onFinish();
                    }
                } else if ("o".equalsIgnoreCase(type)) {
                    String instrument = channelToInstrument.get(channelId, null);
                    if (instrument != null) {
                        QuoteSequenceProcessor quotesListener = processor().onBookUpdate(instrument, timestamp);
                        boolean ask = messagePartArray.getLong(1) == 0;
                        long price = messagePartArray.getDecimal64Required(2);
                        long size = messagePartArray.getDecimal64Required(3);
                        if (Decimal64Utils.isZero(size)) {
                            size = TypeConstants.DECIMAL_NULL; // means delete the price
                        }
                        quotesListener.onQuote(price, size, ask);
                        quotesListener.onFinish();
                    }
                } else if ("t".equalsIgnoreCase(type)) {
                    String instrument = channelToInstrument.get(channelId, null);
                    if (instrument != null) {
                        long price = messagePartArray.getDecimal64Required(3);
                        long size = messagePartArray.getDecimal64Required(4);
                        processor().onTrade(instrument, timestamp, price, size);
                    }
                }
            }
        }
    }

    private long getTimestamp(String timestampStr) {
        try {
            return Long.parseLong(timestampStr);
        } catch (Throwable t) {
            return 0;
        }
    }

    private void processSnapshotSide(QuoteSequenceProcessor quotesListener, JsonObject quotePairs, boolean ask) {
        if (quotePairs == null) {
            return;
        }

        quotePairs.forEachString((price, size) -> {
            quotesListener.onQuote(
                Decimal64Utils.parse(price),
                Decimal64Utils.parse(size),
                ask
            );
        });
    }

}
