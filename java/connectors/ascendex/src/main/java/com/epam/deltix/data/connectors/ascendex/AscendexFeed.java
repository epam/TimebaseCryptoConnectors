package com.epam.deltix.data.connectors.ascendex;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.util.collections.CharSequenceSet;

import java.util.Arrays;

public class AscendexFeed extends MdSingleWsFeed {
    private final JsonValueParser jsonParser = new JsonValueParser();
    private CharSequenceSet snapshotInitialization = new CharSequenceSet();

    public AscendexFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final String... symbols) {

        super("ASCENDEX", uri, depth, 5000, selected, output, errorListener, logger, symbols);
    }

    @Override
    protected void subscribe(JsonWriter jsonWriter, String... symbols) {
        StringBuilder symbolsStringBuilder = new StringBuilder();
        Arrays.stream(symbols).forEach(symbol -> symbolsStringBuilder.append(symbol).append(","));
        String symbolsString = symbolsStringBuilder.deleteCharAt(symbolsStringBuilder.length() - 1).toString();

        //order book updates subscription
        JsonValue updatesJson = JsonValue.newObject();
        JsonObject updatesBody = updatesJson.asObject();

        String updatesChannel = "depth:" + symbolsString;

        updatesBody.putString("op", "sub");
        updatesBody.putString("ch", updatesChannel);

        updatesJson.toJsonAndEoj(jsonWriter);

        //trades subscription
        String tradesChannel = "trades:" + symbolsString;
        JsonValue tradesJson = JsonValue.newObject();
        JsonObject tradesBody = tradesJson.asObject();

        tradesBody.putString("op", "sub");
        tradesBody.putString("ch", tradesChannel);

        tradesJson.toJsonAndEoj(jsonWriter);

        //snapshot request
        Arrays.stream(symbols).forEach((symbol) -> {
            JsonValue snapshotJson = JsonValue.newObject();
            JsonObject snapshotBody = snapshotJson.asObject();

            snapshotBody.putString("op", "req");
            snapshotBody.putString("action", "depth-snapshot");
            JsonObject snapshotArgs = snapshotBody.putObject("args");
            snapshotArgs.putString("symbol", symbol);

            snapshotJson.toJsonAndEoj(jsonWriter);
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
        JsonObject jsonData = object.getObject("data");
        String channel = object.getString("m");
        String symbol = object.getString("symbol");

        if("ping".equals(channel)) {
            JsonValue pongJson = JsonValue.newObject();

            JsonObject pongBody = pongJson.asObject();
            pongBody.putString("op", "pong");

            pongJson.toJsonAndEoj(jsonWriter);
        } else if("depth-snapshot".equals(channel)) {
            long timeStamp = jsonData.getLong("ts");

            QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(symbol, timeStamp);
            processSnapshotSide(quotesListener, jsonData.getArray("bids"), false);
            processSnapshotSide(quotesListener, jsonData.getArray("asks"), true);

            quotesListener.onFinish();
            snapshotInitialization.addCharSequence(symbol);
        } else if("depth".equals(channel) && snapshotInitialization.containsCharSequence(symbol)) {
            long timeStamp = jsonData.getLong("ts");

            QuoteSequenceProcessor quotesListenerUpdate = processor().onBookUpdate(symbol, timeStamp);
            processChanges(quotesListenerUpdate, jsonData.getArray("bids"), false);
            processChanges(quotesListenerUpdate, jsonData.getArray("asks"), true);

            quotesListenerUpdate.onFinish();
        } else if("trades".equals(channel)) {
            JsonArray dataArray = object.getArrayRequired("data");
            for (int i = 0; i < dataArray.size(); ++i) {
                JsonObject trade = dataArray.getObjectRequired(i);
                long timestamp = getTimestamp(trade.getString("ts"));
                long price = trade.getDecimal64Required("p");
                long size = trade.getDecimal64Required("q");

                processor().onTrade(symbol, timestamp, price, size);
            }
        }
    }

    private void processSnapshotSide(QuoteSequenceProcessor quotesListener, JsonArray quotePairs, boolean ask) {
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

    private void processChanges(final QuoteSequenceProcessor quotesListener, final JsonArray changes, final boolean ask) {
        if (changes == null) {
            return;
        }

        for (int i = 0; i < changes.size(); i++) {
            final JsonArray change = changes.getArrayRequired(i);
            long size = change.getDecimal64Required(1);

            if (Decimal64Utils.isZero(size)) {
                size = TypeConstants.DECIMAL_NULL; // means delete the price
            }

            try {
                quotesListener.onQuote(
                        change.getDecimal64Required(0),
                        size,
                        ask
                );
            } catch (Throwable t) {
                int k = 3;
            }
        }
    }

    private long getTimestamp(String tsString) {
        long timestamp = TimeConstants.TIMESTAMP_UNKNOWN;

        if (tsString != null) {
            timestamp = Long.parseLong(tsString);
        }

        return timestamp;
    }
}
