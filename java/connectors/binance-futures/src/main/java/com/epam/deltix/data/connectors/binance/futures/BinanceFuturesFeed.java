package com.epam.deltix.data.connectors.binance.futures;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.timebase.messages.TypeConstants;

import java.util.*;

public class BinanceFuturesFeed extends MdSingleWsRestFeed {
    private final JsonValueParser jsonParser = new JsonValueParser();
    private Map<String, Queue<JsonObject>> updatesBufferMap = new HashMap<>();
    private Map<String, Boolean> firstBookUpdate = new HashMap<>();
    private Map<String, Long> snapshotIdMap = new HashMap<>();
    private Map<String, Long> lastUpdateIdMap = new HashMap<>();

    public BinanceFuturesFeed(
            final String wsUrl,
            final String restUrl,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final String... symbols) {

        super("BINANCE",
                wsUrl,
                restUrl,
                depth,
                30000,
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
        body.putString("method", "SUBSCRIBE");

        JsonArray params = body.putArray("params");
        Arrays.asList(symbols).forEach(symbol -> {
            params.addString(symbol + "@depth");
            params.addString(symbol + "@trade");

            Queue<JsonObject> updatesQueue = new LinkedList<>();
            updatesBufferMap.put(symbol, updatesQueue);
        });

        //mandatory id for the response tracking
        body.putInteger("id", 1);

        subscriptionJson.toJsonAndEoj(jsonWriter);

        initBookSnapshots(Arrays.asList(symbols));
    }

    @Override
    protected void onWsJson(final CharSequence data, final boolean last, final JsonWriter jsonWriter) {
        jsonParser.parse(data);

        if (!last) {
            return;
        }

        JsonValue jsonValue = jsonParser.eoj();
        JsonObject object = jsonValue.asObject();
        JsonObject wsData = object.getObject("data");
        if (wsData != null) {
            if ("depthUpdate".equals(wsData.getString("e"))) {
                String symbol = wsData.getString("s").toLowerCase();

                Queue<JsonObject> buffer = updatesBufferMap.get(symbol);
                buffer.add(wsData);

                if (snapshotIdMap.containsKey((symbol))) {
                    if (firstBookUpdate.get(symbol)) {
                        while (buffer.size() > 0) {
                            JsonObject updateItem = buffer.poll();

                            long U = updateItem.getLong("U");
                            long u = updateItem.getLong("u");
                            long lastUpdateId = snapshotIdMap.get(symbol);
                            if (lastUpdateId >= U && lastUpdateId <= u) {
                                processBookUpdate(updateItem);
                                firstBookUpdate.put(symbol, false);
                                lastUpdateIdMap.put(symbol, u);
                                break;
                            } else if (U > lastUpdateId) {
                                firstBookUpdate.put(symbol, false);
                                snapshotIdMap.remove(symbol);
                                buffer.clear();
                                initBookSnapshots(Arrays.asList(symbol));
                            }
                        }
                    } else {
                        while (buffer.size() > 0) {
                            JsonObject updateItem = buffer.poll();
                            if (lastUpdateIdMap.get(symbol) == updateItem.getLong("pu")) {
                                processBookUpdate(updateItem);
                                lastUpdateIdMap.put(symbol, updateItem.getLong("u"));
                            } else {
                                firstBookUpdate.put(symbol, true);
                                snapshotIdMap.remove(symbol);
                                buffer.clear();
                                initBookSnapshots(Arrays.asList(symbol));
                            }
                        }
                    }
                }
            } else if ("trade".equals(wsData.getString("e"))) {
                long timestamp = wsData.getLong("E");

                long price = wsData.getDecimal64Required("p");
                long size = wsData.getDecimal64Required("q");

                processor().onTrade(wsData.getString("s").toLowerCase(), timestamp, price, size);
            }
        }
    }

    @Override
    protected void onRestJson(String symbol, CharSequence body) {
        processBookSnapshot(body, symbol);
    }

    private void initBookSnapshots(List<String> symbols) {
        symbols.stream().forEach(symbol -> {
            String target = "/depth?symbol=" + symbol.toUpperCase() + "&limit=1000";
            getAsync(symbol, target);
        });
    }

    private void processBookUpdate(JsonObject object) {
        long timestamp = object.getLong("E");

        QuoteSequenceProcessor quotesListener = processor().onBookUpdate(object.getString("s").toLowerCase(), timestamp);

        processChanges(quotesListener, object.getArray("b"), false);
        processChanges(quotesListener, object.getArray("a"), true);

        quotesListener.onFinish();
    }

    private void processChanges(QuoteSequenceProcessor quotesListener, JsonArray changes, boolean isAsk) {
        if (changes == null) {
            return;
        }

        for (int i = 0; i < changes.size(); i++) {
            final JsonArray change = changes.getArrayRequired(i);
            if (change.size() < 2) {
                throw new IllegalArgumentException("Unexpected size of a change :" + change.size());
            }

            long size = change.getDecimal64Required(1);
            if (Decimal64Utils.isZero(size)) {
                size = TypeConstants.DECIMAL_NULL; // means delete the price
            }

            quotesListener.onQuote(
                    change.getDecimal64Required(0),
                    size,
                    isAsk
            );
        }
    }

    protected void processBookSnapshot(CharSequence body, String symbol) {
        JsonValueParser jsonParser = new JsonValueParser();
        jsonParser.parse(body);

        JsonValue jsonValue = jsonParser.eoj();
        JsonObject object = jsonValue.asObject();

        long lastUpdateId = object.getLong("lastUpdateId");

        if (lastUpdateId != 0) {
            QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(symbol, TimeConstants.TIMESTAMP_UNKNOWN);

            processSnapshotSide(quotesListener, object.getArray("bids"), false);
            processSnapshotSide(quotesListener, object.getArray("asks"), true);
            quotesListener.onFinish();

            snapshotIdMap.put(symbol, lastUpdateId);
            firstBookUpdate.put(symbol, true);
        } else {
            String message = object.getString("msg");
            if (message != null && !message.isEmpty()) {
                logger().warning(() -> "Error: " + symbol + " - " + message);
            } else {
                logger().warning(() -> "Error: call from " + symbol + " doesn't return a value");
            }
        }
    }

    protected void processSnapshotSide(QuoteSequenceProcessor quotesListener, JsonArray quotePairs, boolean ask) {
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
