package com.epam.deltix.data.connectors.bybit;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.data.connectors.commons.l2.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.ExchangeCodec;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.timebase.messages.TypeConstants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BybitFuturesFeed extends SingleWsFeed {
    private static final long BYBIT_EXCHANGE_CODE = ExchangeCodec.codeToLong("BYBIT");
    private static final long PING_PERIOD = 5000;

    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();
    private final Iso8601DateTimeParser dtParser = new Iso8601DateTimeParser();
    private final Map<String, L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent>>
            l2Processors = new HashMap<>();
    private final DefaultEvent priceBookEvent = new DefaultEvent();
    private final TradeProducer tradeProducer;

    private final int depth;

    private long lastPingTime;

    private enum UpdateType {
        delete,
        update,
        insert
    }

    public BybitFuturesFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final String... symbols)
    {
        super(uri, 30000, selected, output, errorListener, symbols);

        this.depth = depth;
        tradeProducer = new TradeProducer(BYBIT_EXCHANGE_CODE, output);
    }

    @Override
    protected void prepareSubscription(JsonWriter jsonWriter, String... symbols) {
        JsonValue subscriptionJson = JsonValue.newObject();
        JsonObject body = subscriptionJson.asObject();

        body.putString("op", "subscribe");
        JsonArray args = body.putArray("args");
        Arrays.asList(symbols).forEach(s -> {
            if (selected().level1() || selected().level2()) {
                args.addString(
                    String.format(
                        depth <= 25 ? "orderBookL2_25.%s" : "orderBook_200.100ms.%s",
                        s)
                );
            }
            if (selected().trades()) {
                args.addString(String.format("trade.%s", s));
            }
        });

        subscriptionJson.toJsonAndEoj(jsonWriter);
    }

    @Override
    protected void onJson(final CharSequence data, final boolean last, final JsonWriter jsonWriter) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPingTime > PING_PERIOD) {
            lastPingTime = currentTime;
            jsonWriter.startObject();
            jsonWriter.objectMember("op");
            jsonWriter.stringValue("ping");
            jsonWriter.endObject();
            jsonWriter.eoj();
        }

        jsonParser.parse(data);

        if (!last) {
            return;
        }

        JsonValue jsonValue = jsonParser.eoj();
        JsonObject object = jsonValue.asObject();

        String topic = object.getString("topic");
        if (topic == null) {
            return;
        }

        String[] topicElements = topic.split("\\.");
        if (topicElements.length != 2 && topicElements.length != 3) {
            return;
        }

        String instrument = topicElements[topicElements.length - 1];
        if (topic.startsWith("orderBook")) {
            L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor
                = getPriceBookProcessor(instrument);
            String type = object.getString("type");
            if ("snapshot".equalsIgnoreCase(type)) {
                long timestamp = getTimestamp(object,"timestamp_e6") / 1000;
                JsonObject dataObject = object.getObject("data");
                JsonArray dataArray = object.getArray("data");
                if (dataObject != null) {
                    l2Processor.onSnapshotPackageStarted(TimeConstants.TIMESTAMP_UNKNOWN, timestamp);
                    processSnapshot(l2Processor, dataObject.getArray("order_book"));
                    l2Processor.onPackageFinished();
                } else if (dataArray != null) {
                    l2Processor.onSnapshotPackageStarted(TimeConstants.TIMESTAMP_UNKNOWN, timestamp);
                    processSnapshot(l2Processor, dataArray);
                    l2Processor.onPackageFinished();
                }
            } else if ("delta".equalsIgnoreCase(type)) {
                long timestamp = getTimestamp(object,"timestamp_e6") / 1000;
                JsonObject dataObject = object.getObject("data");
                if (dataObject == null) {
                    return;
                }
                l2Processor.onIncrementalPackageStarted(timestamp);
                processChanges(l2Processor, dataObject.getArray("delete"), UpdateType.delete);
                processChanges(l2Processor, dataObject.getArray("update"), UpdateType.update);
                processChanges(l2Processor, dataObject.getArray("insert"), UpdateType.insert);
                l2Processor.onPackageFinished();
            }
        } else if (topic.startsWith("trade.")) {
            JsonArray trades = object.getArray("data");
            if (trades != null) {
                for (int i = 0; i < trades.size(); ++i) {
                    JsonObject trade = trades.getObject(i);

                    long price = getDecimal(trade, "price");
                    long size = getDecimal(trade, "size");
                    long timestamp = dtParser.set(trade.getStringRequired("timestamp")).millis();

                    tradeProducer.onTrade(timestamp, instrument, price, size);
                }
            }
        }
    }

    private L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent>
        getPriceBookProcessor(String instrument)
    {
        L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent>
                result = l2Processors.get(instrument);
        if (result == null) {
            ChainedL2Listener.Builder<DefaultItem<DefaultEvent>, DefaultEvent> builder =
                ChainedL2Listener.builder();

            if (selected().level1()) {
                builder.with(new BestBidOfferProducer<>(this));
            }
            if (selected().level2()) {
                builder.with(new L2Producer<>(this));
            }

            result = L2Processor.builder()
                .withInstrument(instrument)
                .withSource(BYBIT_EXCHANGE_CODE)
                .withBookOutputSize(depth)
                .buildWithPriceBook(
                    builder.build()
                );
            l2Processors.put(instrument, result);
        }
        return result;
    }

    private void processSnapshot(
            L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor,
            JsonArray quotes)
    {
        if (quotes == null) {
            return;
        }

        for (int i = 0; i < quotes.size(); i++) {
            JsonObject quote = quotes.getObjectRequired(i);
            priceBookEvent.reset();
            priceBookEvent.set(
                    "sell".equalsIgnoreCase(quote.getStringRequired("side")),
                    getDecimal(quote, "price"),
                    getDecimal(quote, "size")
            );
            l2Processor.onEvent(priceBookEvent);
        }
    }

    private void processChanges(
            L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor,
            JsonArray changes, UpdateType updateType)
    {
        if (changes == null) {
            return;
        }

        for (int i = 0; i < changes.size(); i++) {
            final JsonObject change = changes.getObjectRequired(i);

            priceBookEvent.reset();

            long size = TypeConstants.DECIMAL_NULL;;
            if (updateType != UpdateType.delete) {
                size = getDecimal(change, "size");
            }

            priceBookEvent.set(
                "sell".equalsIgnoreCase(change.getStringRequired("side")),
                getDecimal(change, "price"),
                size
            );
            l2Processor.onEvent(priceBookEvent);
        }
    }

    private long getDecimal(JsonObject object, String fieldName) {
        String stringValue = object.getString(fieldName);
        if (stringValue != null) {
            return Decimal64Utils.parse(stringValue);
        } else {
            return Decimal64Utils.fromBigDecimal(
                object.getDecimalRequired(fieldName)
            );
        }
    }

    private long getTimestamp(JsonObject object, String fieldName) {
        String stringValue = object.getString(fieldName);
        if (stringValue != null) {
            return Long.parseLong(stringValue);
        } else {
            return object.getLong(fieldName);
        }
    }
}
