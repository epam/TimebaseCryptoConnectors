package com.epam.deltix.data.connectors.okex;

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

public class OkexSpotFeed extends SingleWsFeed {
    private static final long OKEX_EXCHANGE_CODE = ExchangeCodec.codeToLong("OKEX");
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();
    private final Map<String, L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent>>
            l2Processors = new HashMap<>();
    private final DefaultEvent priceBookEvent = new DefaultEvent();
    private final TradeProducer tradeProducer;

    private final int depth;

    public OkexSpotFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final String... symbols)
    {
        super(uri, 30000, selected, output, errorListener, symbols);

        this.depth = depth;
        tradeProducer = new TradeProducer(OKEX_EXCHANGE_CODE, output);
    }

    @Override
    protected void prepareSubscription(JsonWriter jsonWriter, String... symbols) {
        JsonValue subscriptionJson = JsonValue.newObject();
        JsonObject body = subscriptionJson.asObject();
        body.putString("op", "subscribe");
        JsonArray args = body.putArray("args");
        Arrays.asList(symbols).forEach(s -> {
            if (selected().level1() || selected().level2()) {
                JsonObject object = args.addObject();
                object.putString("channel", "books");
                object.putString("instId", s);
            }

            if (selected().trades()) {
                JsonObject object = args.addObject();
                object.putString("channel", "trades");
                object.putString("instId", s);
            }
        });

        subscriptionJson.toJsonAndEoj(jsonWriter);
    }

    @Override
    protected void onJson(final CharSequence data, final boolean last, final JsonWriter jsonWriter) {
        jsonParser.parse(data);

        if (!last) {
            return;
        }

        JsonValue jsonValue = jsonParser.eoj();
        JsonObject object = jsonValue.asObject();

        JsonObject arg = object.getObject("arg");
        String channel = arg.getString("channel");
        String instrument = arg.getStringRequired("instId");

        JsonArray arrayData = object.getArray("data");
        if (arrayData == null || arrayData.size() < 1) {
            return;
        }

        if ("books".equalsIgnoreCase(channel)) {
            L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor
                = getPriceBookProcessor(instrument);
            String action = object.getStringRequired("action");
            JsonObject jsonData = arrayData.getObject(0);
            long timestamp = getTimestamp(jsonData.getString("ts"));
            if ("snapshot".equalsIgnoreCase(action)) {
                l2Processor.onSnapshotPackageStarted(TimeConstants.TIMESTAMP_UNKNOWN, timestamp);
                processSnapshotSide(l2Processor, jsonData.getArray("bids"), false);
                processSnapshotSide(l2Processor, jsonData.getArray("asks"), true);
                l2Processor.onPackageFinished();
            } else if ("update".equalsIgnoreCase(action)) {
                l2Processor.onIncrementalPackageStarted(timestamp);
                processChanges(l2Processor, jsonData.getArray("bids"), false);
                processChanges(l2Processor, jsonData.getArray("asks"), true);
                l2Processor.onPackageFinished();
            }
        } else if ("trades".equalsIgnoreCase(channel)) {
            JsonArray jsonDataArray = object.getArrayRequired("data");
            for (int i = 0; i < jsonDataArray.size(); ++i) {
                JsonObject trade = jsonDataArray.getObjectRequired(i);
                long timestamp = getTimestamp(trade.getString("ts"));
                long price = Decimal64Utils.parse(trade.getStringRequired("px"));
                long size = Decimal64Utils.parse(trade.getStringRequired("sz"));

                tradeProducer.onTrade(timestamp, instrument, price, size);
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
                .withSource(OKEX_EXCHANGE_CODE)
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
            if (pair.size() < 2) {
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
            if (change.size() < 2) {
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

    private long getTimestamp(String tsString) {
        long timestamp = TimeConstants.TIMESTAMP_UNKNOWN;
        if (tsString != null) {
            timestamp = Long.parseLong(tsString);
        }

        return timestamp;
    }

}
