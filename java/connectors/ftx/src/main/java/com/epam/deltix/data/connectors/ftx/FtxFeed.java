package com.epam.deltix.data.connectors.ftx;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.data.connectors.commons.l2.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.ExchangeCodec;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.timebase.messages.TypeConstants;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FtxFeed extends SingleWsFeed {
    private static final long FTX_EXCHANGE_CODE = ExchangeCodec.codeToLong("FTX");
    private static final BigDecimal TIME_MILLIS_SCALE = new BigDecimal(1000);
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();
    private final Map<String, L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent>>
            l2Processors = new HashMap<>();
    private final DefaultEvent priceBookEvent = new DefaultEvent();
    private final TradeProducer tradeProducer;

    private final int depth;

    public FtxFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final String... symbols)
    {
        super(uri, 5000, selected, output, errorListener, symbols);

        this.depth = depth;
        tradeProducer = new TradeProducer(FTX_EXCHANGE_CODE, output);
    }

    @Override
    protected void prepareSubscription(JsonWriter jsonWriter, String... symbols) {
        if (selected().level1() || selected().level2()) {
            Arrays.asList(symbols).forEach(s -> {
                JsonValue subscriptionJson = JsonValue.newObject();
                JsonObject body = subscriptionJson.asObject();

                body.putString("op", "subscribe");
                body.putString("channel", "orderbook");
                body.putString("market", s);

                subscriptionJson.toJsonAndEoj(jsonWriter);
            });
        }

        if (selected().trades()) {
            Arrays.asList(symbols).forEach(s -> {
                JsonValue subscriptionJson = JsonValue.newObject();
                JsonObject body = subscriptionJson.asObject();

                body.putString("op", "subscribe");
                body.putString("channel", "trades");
                body.putString("market", s);

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

        String channel = object.getString("channel");
        if ("orderbook".equalsIgnoreCase(channel)) {
            String instrument = object.getString("market");
            L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor
                = getPriceBookProcessor(instrument);

            String type = object.getString("type");
            JsonObject jsonData = object.getObject("data");
            if ("partial".equalsIgnoreCase(type)) {
                long timestamp = getTimestamp(jsonData.getDecimal("time"));
                l2Processor.onSnapshotPackageStarted(TimeConstants.TIMESTAMP_UNKNOWN, timestamp);
                processSnapshotSide(l2Processor, jsonData.getArrayRequired("bids"), false);
                processSnapshotSide(l2Processor, jsonData.getArrayRequired("asks"), true);
                l2Processor.onPackageFinished();
            } else if ("update".equalsIgnoreCase(type)) {
                JsonArray bids = jsonData.getArray("bids");
                JsonArray asks = jsonData.getArray("asks");
                if ((bids != null && bids.size() > 0) || (asks != null && asks.size() > 0)) {
                    long timestamp = getTimestamp(jsonData.getDecimal("time"));
                    l2Processor.onIncrementalPackageStarted(timestamp);
                    processChanges(l2Processor, bids, false);
                    processChanges(l2Processor, asks, true);
                    l2Processor.onPackageFinished();
                }
            }
        } else if ("trades".equalsIgnoreCase(channel)) {
            String instrument = object.getString("market");
            JsonArray trades = object.getArray("data");
            if (trades != null) {
                for (int i = 0; i < trades.size(); ++i) {
                    JsonObject trade = trades.getObject(i);
                    long price = Decimal64Utils.fromBigDecimal(trade.getDecimalRequired("price"));
                    long size = Decimal64Utils.fromBigDecimal(trade.getDecimalRequired("size"));
                    long timestamp = OffsetDateTime.parse(trade.getString("time")).toInstant().toEpochMilli();
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
                .withSource(FTX_EXCHANGE_CODE)
                .withBookOutputSize(depth)
                .buildWithPriceBook(
                    builder.build()
                );
            l2Processors.put(instrument, result);
        }
        return result;
    }

    private void processSnapshotSide(
            final L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor,
            final JsonArray quotePairs,
            final boolean ask)
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
                    Decimal64Utils.fromBigDecimal(pair.getDecimalRequired(0)),
                    Decimal64Utils.fromBigDecimal(pair.getDecimalRequired(1))
            );
            l2Processor.onEvent(priceBookEvent);
        }
    }

    private void processChanges(
            final L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor,
            final JsonArray changes,
            final boolean ask)
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

            long size = Decimal64Utils.fromBigDecimal(change.getDecimalRequired(1));
            if (Decimal64Utils.isZero(size)) {
                size = TypeConstants.DECIMAL_NULL; // means delete the price
            }

            priceBookEvent.set(
                ask,
                Decimal64Utils.fromBigDecimal(change.getDecimalRequired(0)),
                size
            );
            l2Processor.onEvent(priceBookEvent);
        }
    }

    private long getTimestamp(BigDecimal time) {
        return time.multiply(TIME_MILLIS_SCALE).longValue();
    }
}
