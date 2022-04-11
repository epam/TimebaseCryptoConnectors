package com.epam.deltix.data.connectors.bitmex;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.data.connectors.commons.l2.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.ExchangeCodec;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.util.collections.generated.LongToLongHashMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BitmexFeed extends SingleWsFeed {
    private static final long BITMEX_EXCHANGE_CODE = ExchangeCodec.codeToLong("BITMEX");
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();
    private final Iso8601DateTimeParser dtParser = new Iso8601DateTimeParser();

    private final Map<String, L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent>>
        l2Processors = new HashMap<>();
    private final Map<String, LongToLongHashMap> priceLevels = new HashMap<>();

    private final DefaultEvent priceBookEvent = new DefaultEvent();
    private final TradeProducer tradeProducer;

    private final int depth;

    public BitmexFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final String... symbols)
    {
        super(uri, 5000, selected, output, errorListener, symbols);

        this.depth = depth;
        tradeProducer = new TradeProducer(BITMEX_EXCHANGE_CODE, output);
    }

    @Override
    protected void prepareSubscription(JsonWriter jsonWriter, String... symbols) {
        JsonValue subscriptionJson = JsonValue.newObject();
        JsonObject body = subscriptionJson.asObject();

        body.putString("op", "subscribe");

        JsonArray pairs = body.putArray("args");
        if (selected().level1() || selected().level2()) {
            Arrays.asList(symbols).forEach(s -> {
                pairs.addString("orderBookL2:" + s);
            });
        }

        if (selected().trades()) {
            Arrays.asList(symbols).forEach(s -> {
                pairs.addString("trade:" + s);
            });
        }

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
        if (object == null) {
            return;
        }

        // todo: process type information

        String type = object.getString("table");
        if ("orderBookL2".equalsIgnoreCase(type)) {
            String action = object.getString("action");
            JsonArray bookData = object.getArray("data");
            if (bookData != null && bookData.size() > 0) {
                JsonObject firstObject = bookData.getObject(0);
                String instrument = firstObject.getString("symbol");
                L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor
                    = getPriceBookProcessor(instrument);
                LongToLongHashMap idToPrice = getPriceLevels(instrument);
                if ("partial".equalsIgnoreCase(action)) {
                    l2Processor.onSnapshotPackageStarted(TimeConstants.TIMESTAMP_UNKNOWN, getTimestamp(bookData));
                    processSnapshot(instrument, l2Processor, idToPrice, bookData);
                    l2Processor.onPackageFinished();
                } else {
                    l2Processor.onIncrementalPackageStarted(getTimestamp(bookData));
                    processChanges(action, instrument, l2Processor, idToPrice, bookData);
                    l2Processor.onPackageFinished();
                }
            }
        } else if ("trade".equalsIgnoreCase(type)) {
            String action = object.getString("action");
            if ("insert".equalsIgnoreCase(action)) {
                JsonArray tradeData = object.getArray("data");
                if (tradeData != null) {
                    for (int i = 0; i < tradeData.size(); ++i) {
                        JsonObject trade = tradeData.getObject(i);
                        // todo: double types
                        long price = Decimal64Utils.fromDouble(trade.getDoubleRequired("price"));
                        long size = Decimal64Utils.fromDouble(trade.getDoubleRequired("size"));
                        String symbol = trade.getStringRequired("symbol");
                        long timestamp = dtParser.set(trade.getStringRequired("timestamp")).millis();

                        tradeProducer.onTrade(timestamp, symbol, price, size);
                    }
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
                .withSource(BITMEX_EXCHANGE_CODE)
                .withBookOutputSize(depth)
                .buildWithPriceBook(
                    builder.build()
                );
            l2Processors.put(instrument, result);
        }
        return result;
    }

    private LongToLongHashMap getPriceLevels(String instrument) {
        return priceLevels.computeIfAbsent(instrument, k -> new LongToLongHashMap());
    }

    private long getTimestamp(JsonArray quotePairs) {
        long timestamp = TimeConstants.TIMESTAMP_UNKNOWN;
        if (quotePairs == null) {
            return timestamp;
        }

        for (int i = 0; i < quotePairs.size(); i++) {
            JsonObject pair = quotePairs.getObjectRequired(i);
            String timeString = pair.getString("timestamp");
            if (timeString != null) {
                timestamp = Math.max(timestamp, dtParser.set(timeString).millis());
            }
        }

        return timestamp;
    }

    private void processSnapshot(
        String instrument,
        L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor,
        LongToLongHashMap idToPrice,
        JsonArray quotePairs)
    {
        if (quotePairs == null) {
            return;
        }

        idToPrice.clear();

        for (int i = 0; i < quotePairs.size(); i++) {
            JsonObject pair = quotePairs.getObjectRequired(i);
            String symbol = pair.getString("symbol");
            if (!instrument.equalsIgnoreCase(symbol)) {
                throw new RuntimeException("Invalid symbol in snapshot: " + symbol);
            }

            long id = pair.getLong("id");
            long size = Decimal64Utils.fromDouble(pair.getDoubleRequired("size"));
            long price = Decimal64Utils.fromDouble(pair.getDoubleRequired("price"));
            boolean isOffer = "sell".equalsIgnoreCase(pair.getStringRequired("side"));

            idToPrice.put(id, price);

            priceBookEvent.reset();
            priceBookEvent.set(isOffer, price, size);
            l2Processor.onEvent(priceBookEvent);
        }
    }

    private void processChanges(
        String action, String instrument,
        L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor,
        LongToLongHashMap idToPrice,
        JsonArray changes)
    {
        if (changes == null) {
            return;
        }

        for (int i = 0; i < changes.size(); i++) {
            JsonObject change = changes.getObjectRequired(i);

            String symbol = change.getString("symbol");
            if (!instrument.equalsIgnoreCase(symbol)) {
                throw new RuntimeException("Invalid symbol in snapshot: " + symbol);
            }

            long id = change.getLong("id");
            long size = TypeConstants.DECIMAL_NULL;
            long price = TypeConstants.DECIMAL_NULL;
            if ("insert".equalsIgnoreCase(action)) {
                price = Decimal64Utils.fromDouble(change.getDoubleRequired("price"));
                size = Decimal64Utils.fromDouble(change.getDoubleRequired("size"));
                idToPrice.put(id, price);
            } else if ("delete".equalsIgnoreCase(action)) {
                price = idToPrice.remove(id, Long.MIN_VALUE);
                if (price == Long.MIN_VALUE) {
                    throw new RuntimeException("Unknown price with id: " + id);
                }
            } else if ("update".equalsIgnoreCase(action)) {
                price = idToPrice.get(id, Long.MIN_VALUE);
                size = Decimal64Utils.fromDouble(change.getDoubleRequired("size"));
                if (price == Long.MIN_VALUE) {
                    throw new RuntimeException("Unknown price with id: " + id);
                }
            }
            boolean isOffer = "sell".equalsIgnoreCase(change.getStringRequired("side"));

            priceBookEvent.reset();
            priceBookEvent.set(isOffer, price, size);
            l2Processor.onEvent(priceBookEvent);
        }
    }
}
