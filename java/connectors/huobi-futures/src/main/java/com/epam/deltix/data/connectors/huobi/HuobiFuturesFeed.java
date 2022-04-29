package com.epam.deltix.data.connectors.huobi;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class HuobiFuturesFeed extends MdSingleWsFeed {
    private static final AtomicLong ID_GENERATOR = new AtomicLong();
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();

    private final int depth;

    public HuobiFuturesFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final String... symbols) {

        super("HUOBI", uri, depth, 5000, selected, output, errorListener, null, true, symbols);

        this.depth = depth;
    }

    @Override
    protected void subscribe(JsonWriter jsonWriter, String... symbols) {
        if (selected().level1() || selected().level2()) {
            Arrays.asList(symbols).forEach(s -> {
                JsonValue subscriptionJson = JsonValue.newObject();
                JsonObject body = subscriptionJson.asObject();

                body.putString("sub",
                    String.format("market.%s.depth.size_%d.high_freq", s.toLowerCase(Locale.ROOT), getHuobiDepth())
                );
                body.putString("data_type", "incremental");
                body.putString("id", String.valueOf(ID_GENERATOR.incrementAndGet()));

                subscriptionJson.toJsonAndEoj(jsonWriter);
            });
        }

        if (selected().trades()) {
            Arrays.asList(symbols).forEach(s -> {
                JsonValue subscriptionJson = JsonValue.newObject();
                JsonObject body = subscriptionJson.asObject();

                body.putString("sub",
                    String.format("market.%s.trade.detail", s.toLowerCase(Locale.ROOT))
                );
                body.putString("id", String.valueOf(ID_GENERATOR.incrementAndGet()));

                subscriptionJson.toJsonAndEoj(jsonWriter);
            });
        }
    }

    private int getHuobiDepth() {
        return depth <= 20 ? 20 : 150;
    }

    @Override
    protected void onJson(final CharSequence data, final boolean last, final JsonWriter jsonWriter) {
        jsonParser.parse(data);

        if (!last) {
            return;
        }

        JsonValue jsonValue = jsonParser.eoj();

        JsonObject object = jsonValue.asObject();
        if (object == null) {
            return;
        }

        long ping = object.getLong("ping");
        if (ping != 0L) {
            jsonWriter.startObject();
            jsonWriter.objectMember("pong");
            jsonWriter.numberValue(ping);
            jsonWriter.endObject();
            jsonWriter.eoj();
            return;
        }

        String topic = object.getString("ch");
        if (topic == null) {
            return;
        }

        String[] topicElements = topic.split("\\.");
        if (topicElements.length != 4 && topicElements.length != 5) {
            return;
        }

        long timestamp = object.getLong("ts");
        String instrument = topicElements[1];
        if ("depth".equalsIgnoreCase(topicElements[2])) {
            JsonObject tick = object.getObject("tick");
            if (tick == null) {
                return;
            }
            String event = tick.getStringRequired("event");
            if ("snapshot".equalsIgnoreCase(event)) {
                QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(instrument, timestamp);
                processSnapshotSide(quotesListener, tick.getArray("bids"), false);
                processSnapshotSide(quotesListener, tick.getArray("asks"), true);
                quotesListener.onFinish();
            } else if ("update".equalsIgnoreCase(event)) {
                QuoteSequenceProcessor quotesListener = processor().onBookUpdate(instrument, timestamp);
                processChanges(quotesListener, tick.getArray("bids"), false);
                processChanges(quotesListener, tick.getArray("asks"), true);
                quotesListener.onFinish();
            }
        } else if ("trade".equalsIgnoreCase(topicElements[2]) && "detail".equalsIgnoreCase(topicElements[3])) {
            JsonObject tick = object.getObject("tick");
            if (tick != null) {
                JsonArray dataJson = tick.getArray("data");
                if (dataJson != null) {
                    for (int i = 0; i < dataJson.size(); ++i) {
                        JsonObject trade = dataJson.getObject(i);
                        long price = trade.getDecimal64Required("price");
                        long size = trade.getDecimal64Required("amount");

                        processor().onTrade(instrument, timestamp, price, size);
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

    private void processChanges(QuoteSequenceProcessor quotesListener, JsonArray changes, boolean ask) {
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
                ask
            );
        }
    }

}
