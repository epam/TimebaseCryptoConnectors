package com.epam.deltix.data.connectors.deribit;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.timebase.messages.universal.AggressorSide;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DeribitFeed extends MdSingleWsFeed {
    private final JsonValueParser jsonParser = new JsonValueParser();
    private Map<String, Long> changeIdMap = new HashMap<>();

    public DeribitFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final String... symbols) {

        super("DERIBIT", uri, depth, 5000, selected, output, errorListener, logger, symbols);
    }

    @Override
    protected void subscribe(JsonWriter jsonWriter, String... symbols) {
        JsonValue updatesJson = prepareMessage("public/subscribe", symbols);
        updatesJson.toJsonAndEoj(jsonWriter);
    }

    private void unsubscribe(JsonWriter jsonWriter, String... symbols) {
        JsonValue updatesJson = prepareMessage("public/unsubscribe", symbols);
        updatesJson.toJsonAndEoj(jsonWriter);
    }

    private JsonValue prepareMessage(String method, String[] symbols) {
        JsonValue updatesJson = JsonValue.newObject();
        JsonObject updatesBody = updatesJson.asObject();

        updatesBody.putString("jsonrpc", "2.0");
        updatesBody.putString("method", method);
        updatesBody.putInteger("id", 1);

        JsonObject params = updatesBody.putObject("params");
        JsonArray channels = params.putArray("channels");

        Arrays.asList(symbols).forEach(symbol -> {
            channels.addString("book." + symbol.toUpperCase() + ".100ms");
            channels.addString("trades." + symbol.toUpperCase() + ".100ms");
        });

        return updatesJson;
    }

    @Override
    protected void onJson(final CharSequence data, final boolean last, final JsonWriter jsonWriter) {
        jsonParser.parse(data);

        if (!last) {
            return;
        }

        JsonValue jsonValue = jsonParser.eoj();
        JsonObject object = jsonValue.asObject();
        JsonObject params = object.getObject("params");

        if (params != null) {
            String channel = params.getString("channel");

            if (channel != null) {
                if (channel.contains("book.")) {
                    JsonObject jsonData = params.getObject("data");
                    String instrument = jsonData.getString("instrument_name").toLowerCase();
                    long timestamp = jsonData.getLong("timestamp");
                    String type = jsonData.getString("type");
                    Long changeId = jsonData.getLong("change_id");

                    if ("snapshot".equals(type)) {
                        QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(instrument, timestamp);

                        processSnapshotSide(quotesListener, jsonData.getArray("bids"), false);
                        processSnapshotSide(quotesListener, jsonData.getArray("asks"), true);

                        quotesListener.onFinish();
                        changeIdMap.put(instrument, changeId);

                    } else if ("change".equals(type)) {
                        long previousChangeId = jsonData.getLong("prev_change_id");

                        if (previousChangeId == changeIdMap.get(instrument)) {
                            changeIdMap.put(instrument, changeId);

                            QuoteSequenceProcessor quotesListenerUpdate = processor().onBookUpdate(instrument, timestamp);
                            processChanges(quotesListenerUpdate, jsonData.getArray("bids"), false);
                            processChanges(quotesListenerUpdate, jsonData.getArray("asks"), true);

                            quotesListenerUpdate.onFinish();
                        } else {
                            unsubscribe(jsonWriter, instrument);
                            changeIdMap.remove(instrument);

                            subscribe(jsonWriter, instrument);
                        }
                    }
                } else if (channel.contains("trade.")) {
                    JsonArray dataArray = object.getArrayRequired("data");
                    for (int i = 0; i < dataArray.size(); ++i) {
                        JsonObject trade = dataArray.getObjectRequired(i);
                        long timestamp = trade.getLong("timestamp");
                        long price = trade.getDecimal64Required("price");
                        long size = trade.getDecimal64Required("amount");
                        String instrument = trade.getString("instrument_name");
                        String tradeDirection = trade.getString("direction");

                        AggressorSide side = "buy".equalsIgnoreCase(tradeDirection) ? AggressorSide.BUY : AggressorSide.SELL;

                        processor().onTrade(instrument, timestamp, price, size, side);
                    }
                }
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
                    pair.getDecimal64Required(1),
                    pair.getDecimal64Required(2),
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
            long size = change.getDecimal64Required(2);

            if (Decimal64Utils.isZero(size)) {
                size = TypeConstants.DECIMAL_NULL; // means delete the price
            }

            try {
                quotesListener.onQuote(
                        change.getDecimal64Required(1),
                        size,
                        ask
                );
            } catch (Throwable t) {
                int k = 3;
            }
        }
    }
}
