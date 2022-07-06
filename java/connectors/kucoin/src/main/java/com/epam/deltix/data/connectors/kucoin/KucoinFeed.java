package com.epam.deltix.data.connectors.kucoin;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.timebase.messages.TypeConstants;

import java.util.*;

public class KucoinFeed extends MdSingleWsRestFeed {
    private final JsonValueParser jsonParser = new JsonValueParser();
    private Map<String, Queue<JsonObject>> updatesBufferMap = new HashMap<>();
    private Map<String, Long> sequenceIdMap = new HashMap<>();
    private long lastPingTime;
    private long pingTimeout = 16000;

    public KucoinFeed(
            final String wsUrl,
            final String restUrl,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final boolean isAuthRequired,
            final String... symbols) {

        super("KUCOIN",
                wsUrl,
                restUrl,
                depth,
                30000,
                selected,
                output,
                errorListener,
                logger,
                isAuthRequired,
                symbols);
    }

    @Override
    protected void subscribe(JsonWriter jsonWriter, String... symbols) {
        StringBuilder symbolsStringBuilder = new StringBuilder();
        Arrays.stream(symbols).forEach(symbol -> symbolsStringBuilder.append(symbol).append(","));
        String symbolsString = symbolsStringBuilder.deleteCharAt(symbolsStringBuilder.length() - 1)
                .toString().toUpperCase();

        JsonValue subscriptionJson = JsonValue.newObject();
        JsonObject body = subscriptionJson.asObject();

        body.putInteger("id", 1);
        body.putString("type", "subscribe");
        body.putString("topic", "/market/level2:" + symbolsString);
        body.putBoolean("response", false);

        Arrays.asList(symbols).forEach(symbol -> {
            Queue<JsonObject> updatesQueue = new LinkedList<>();
            updatesBufferMap.put(symbol, updatesQueue);
        });

        subscriptionJson.toJsonAndEoj(jsonWriter);
        lastPingTime = System.currentTimeMillis();
        initBookSnapshots(symbols);
    }

    @Override
    protected void onWsJson(final CharSequence data, final boolean last, final JsonWriter jsonWriter) {
        jsonParser.parse(data);

        if (!last) {
            return;
        }

        JsonValue jsonValue = jsonParser.eoj();
        JsonObject object = jsonValue.asObject();

        if ("trade.l2update".equals(object.getString("subject"))) {
            JsonObject jasonData = object.getObject("data");
            String symbol = jasonData.getString("symbol").toLowerCase();

            Queue<JsonObject> buffer = updatesBufferMap.get(symbol);
            buffer.add(jasonData);

            if (sequenceIdMap.containsKey((symbol))) {
                while (buffer.size() > 0) {
                    JsonObject updateItem = buffer.poll();
                    if (sequenceIdMap.get(symbol) + 1 == updateItem.getLong("sequenceStart")) {
                        processBookUpdate(updateItem);
                        sequenceIdMap.put(symbol, updateItem.getLong("sequenceEnd"));
                    } else if (sequenceIdMap.get(symbol) + 1 < updateItem.getLong("sequenceStart")) {
                        sequenceIdMap.remove(symbol);
                        buffer.clear();
                        initBookSnapshots(symbol);
                    }
                }
            }
        }

        long now = System.currentTimeMillis();
        long timeDelta = now - lastPingTime;

        if (timeDelta > pingTimeout) {
            JsonValue pingJson = JsonValue.newObject();
            JsonObject pingBody = pingJson.asObject();

            pingBody.putString("id", "1");
            pingBody.putString("type", "ping");

            lastPingTime = now;
            pingJson.toJsonAndEoj(jsonWriter);
        }
    }

    @Override
    protected void onRestJson(String symbol, CharSequence body) {
        processBookSnapshot(body, symbol);
    }

    @Override
    protected String authenticate(String wsUrl) {
        String resultUrl = wsUrl;
        CharSequence body = post("/bullet-public");

        JsonValueParser jsonParser = new JsonValueParser();
        jsonParser.parse(body);
        JsonValue jsonValue = jsonParser.eoj();
        JsonObject authObject = jsonValue.asObject();

        if ("200000".equals(authObject.getString("code"))) {
            String token = authObject.getObject("data").getString("token");
            resultUrl = resultUrl + "?token=" + token;
        }

        return resultUrl;
    }

    private void initBookSnapshots(String... symbols) {
        Arrays.asList(symbols).stream().forEach(symbol -> {
            String target = "/market/orderbook/level2_100?symbol=" + symbol.toUpperCase();
            getAsync(symbol, target);
        });
    }

    private void processBookUpdate(JsonObject object) {
        JsonObject changes = object.getObject("changes");

        QuoteSequenceProcessor quotesListener = processor().onBookUpdate(object.getString("symbol").toLowerCase(),
                TimeConstants.TIMESTAMP_UNKNOWN);

        processChanges(quotesListener, changes.getArray("bids"), false);
        processChanges(quotesListener, changes.getArray("asks"), true);

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
        JsonObject data = object.getObject("data");

        long sequence = Long.parseLong(data.getString("sequence"));

        if (sequence != 0) {
            QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(symbol, data.getLong("time"));

            processSnapshotSide(quotesListener, data.getArray("bids"), false);
            processSnapshotSide(quotesListener, data.getArray("asks"), true);
            quotesListener.onFinish();

            sequenceIdMap.put(symbol, sequence);
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
