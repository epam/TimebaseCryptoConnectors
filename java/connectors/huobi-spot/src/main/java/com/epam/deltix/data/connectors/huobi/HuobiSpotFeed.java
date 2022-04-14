package com.epam.deltix.data.connectors.huobi;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class HuobiSpotFeed extends SingleWsFeed {
    private static final AtomicLong ID_GENERATOR = new AtomicLong();
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();
    private final MarketDataProcessor dataProcessor;

    private final int depth;

    public HuobiSpotFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final String... symbols)
    {
        super(uri, 5000, selected, output, errorListener, symbols);

        this.depth = depth;
        this.dataProcessor = MarketDataProcessorImpl.create("HUOBI", this, selected(), depth);
    }

    @Override
    protected void prepareSubscription(JsonWriter jsonWriter, String... symbols) {
        if (selected().level1() || selected().level2()) {
            Arrays.asList(symbols).forEach(s -> {
                JsonValue subscriptionJson = JsonValue.newObject();
                JsonObject body = subscriptionJson.asObject();

                body.putString("sub", "market." + s.toLowerCase(Locale.ROOT) + ".depth.step0");
                body.putString("id", String.valueOf(ID_GENERATOR.incrementAndGet()));

                subscriptionJson.toJsonAndEoj(jsonWriter);
            });
        }

        if (selected().trades()) {
            Arrays.asList(symbols).forEach(s -> {
                JsonValue subscriptionJson = JsonValue.newObject();
                JsonObject body = subscriptionJson.asObject();

                body.putString("sub", "market." + s.toLowerCase(Locale.ROOT) + ".trade.detail");
                body.putString("id", String.valueOf(ID_GENERATOR.incrementAndGet()));

                subscriptionJson.toJsonAndEoj(jsonWriter);
            });
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
        if (topicElements.length != 4) {
            return;
        }

        long timestamp = object.getLong("ts");
        String instrument = topicElements[1];
        if ("depth".equalsIgnoreCase(topicElements[2]) && "step0".equalsIgnoreCase(topicElements[3])) {
            JsonObject tick = object.getObject("tick");
            if (tick != null) {
                L2BookProcessor l2BookProcessor = dataProcessor.onBookSnapshot(instrument, timestamp);
                processSnapshotSide(l2BookProcessor, tick.getArray("bids"), false);
                processSnapshotSide(l2BookProcessor, tick.getArray("asks"), true);
                l2BookProcessor.onFinish();
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

                        dataProcessor.onTrade(instrument, timestamp, price, size);
                    }
                }
            }
        }
    }

    private void processSnapshotSide(
            final L2BookProcessor l2BookProcessor, final JsonArray quotePairs, final boolean ask) {

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

            l2BookProcessor.onQuote(
                pair.getDecimal64Required(0),
                pair.getDecimal64Required(1),
                ask
            );
        }
    }

}
