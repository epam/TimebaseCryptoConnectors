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

public class BybitSpotFeed extends SingleWsFeed {
    private static final long BYBIT_EXCHANGE_CODE = ExchangeCodec.codeToLong("BYBIT");
    private static final long PING_PERIOD = 10000;

    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();
    private final Map<String, L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent>>
            l2Processors = new HashMap<>();
    private final DefaultEvent priceBookEvent = new DefaultEvent();
    private final TradeProducer tradeProducer;

    private final int depth;

    private long lastPingTime;

    public BybitSpotFeed(
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
        Arrays.asList(symbols).forEach(s -> {
            if (selected().level1() || selected().level2()) {
                JsonValue subscriptionJson = JsonValue.newObject();
                JsonObject body = subscriptionJson.asObject();
                body.putString("topic", "diffDepth");
                body.putString("event", "sub");
                body.putString("symbol", s);
                JsonObject params = body.putObject("params");
                params.putBoolean("binary", false);

                subscriptionJson.toJsonAndEoj(jsonWriter);
            }
            if (selected().trades()) {
                JsonValue subscriptionJson = JsonValue.newObject();
                JsonObject body = subscriptionJson.asObject();
                body.putString("topic", "trade");
                body.putString("event", "sub");
                body.putString("symbol", s);
                JsonObject params = body.putObject("params");
                params.putBoolean("binary", false);

                subscriptionJson.toJsonAndEoj(jsonWriter);
            }
        });
    }

    @Override
    protected void onJson(final CharSequence data, final boolean last, final JsonWriter jsonWriter) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPingTime > PING_PERIOD) {
            lastPingTime = currentTime;
            jsonWriter.startObject();
            jsonWriter.objectMember("ping");
            jsonWriter.numberValue(currentTime);
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
        String instrument = object.getString("symbol");
        boolean first = object.getBoolean("f");
        if ("diffDepth".equalsIgnoreCase(topic)) {
            L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor
                = getPriceBookProcessor(instrument);

            JsonArray jsonDataArray = object.getArrayRequired("data");
            for (int i = 0; i < jsonDataArray.size(); ++i) {
                JsonObject jsonData = jsonDataArray.getObjectRequired(i);
                long timestamp = jsonData.getLong("t");
                if (first) {
                    l2Processor.onSnapshotPackageStarted(TimeConstants.TIMESTAMP_UNKNOWN, timestamp);
                    processSnapshotSide(l2Processor, jsonData.getArray("b"), false);
                    processSnapshotSide(l2Processor, jsonData.getArray("a"), true);
                    l2Processor.onPackageFinished();
                } else {
                    l2Processor.onIncrementalPackageStarted(timestamp);
                    processChanges(l2Processor, jsonData.getArray("b"), false);
                    processChanges(l2Processor, jsonData.getArray("a"), true);
                    l2Processor.onPackageFinished();
                }
            }
        } else if ("trade".equalsIgnoreCase(topic)) {
            if (!first) { // skip snapshots
                JsonArray jsonDataArray = object.getArrayRequired("data");
                for (int i = 0; i < jsonDataArray.size(); ++i) {
                    JsonObject trade = jsonDataArray.getObjectRequired(i);
                    long timestamp = trade.getLong("t");
                    long price = Decimal64Utils.parse(trade.getStringRequired("p"));
                    long size = Decimal64Utils.parse(trade.getStringRequired("q"));

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

    private void processSnapshotSide(
        L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor,
        JsonArray quotePairs, boolean ask)
    {
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
            priceBookEvent.reset();
            priceBookEvent.set(
                ask,
                Decimal64Utils.parse(pair.getStringRequired(0)),
                Decimal64Utils.parse(pair.getStringRequired(1))
            );
            l2Processor.onEvent(priceBookEvent);
        }
    }

    private void processChanges(
        L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor,
        JsonArray changes, boolean isAsk)
    {
        if (changes == null) {
            return;
        }

        for (int i = 0; i < changes.size(); i++) {
            final JsonArray change = changes.getArrayRequired(i);
            if (change.size() != 2) {
                throw new IllegalArgumentException("Unexpected size of a change :" + change.size());
            }
            priceBookEvent.reset();

            long size = Decimal64Utils.parse(change.getStringRequired(1));
            if (Decimal64Utils.isZero(size)) {
                size = TypeConstants.DECIMAL_NULL; // means delete the price
            }

            priceBookEvent.set(
                isAsk,
                Decimal64Utils.parse(change.getStringRequired(0)),
                size
            );

            l2Processor.onEvent(priceBookEvent);
        }
    }

}
