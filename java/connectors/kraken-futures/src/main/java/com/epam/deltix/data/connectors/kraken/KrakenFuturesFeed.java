package com.epam.deltix.data.connectors.kraken;

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

public class KrakenFuturesFeed extends SingleWsFeed {
    private static final long KRAKEN_EXCHANGE_CODE = ExchangeCodec.codeToLong("KRAKEN");
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();
    private final Iso8601DateTimeParser dtParser = new Iso8601DateTimeParser();
    private final Map<String, L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent>>
            l2Processors = new HashMap<>();
    private final DefaultEvent priceBookEvent = new DefaultEvent();
    private final TradeProducer tradeProducer;

    private final int depth;

    public KrakenFuturesFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final String... symbols)
    {
        super(uri, 5000, selected, output, errorListener, symbols);

        this.depth = depth;
        tradeProducer = new TradeProducer(KRAKEN_EXCHANGE_CODE, output);
    }

    @Override
    protected void prepareSubscription(JsonWriter jsonWriter, String... symbols) {
        if (selected().level1() || selected().level2()) {
            JsonValue subscriptionJson = JsonValue.newObject();
            JsonObject body = subscriptionJson.asObject();

            body.putString("event", "subscribe");
            body.putString("feed", "book");
            JsonArray pairs = body.putArray("product_ids");
            Arrays.asList(symbols).forEach(pairs::addString);

            subscriptionJson.toJsonAndEoj(jsonWriter);
        }

        if (selected().trades()) {
            JsonValue subscriptionJson = JsonValue.newObject();
            JsonObject body = subscriptionJson.asObject();

            body.putString("event", "subscribe");
            body.putString("feed", "trade");
            JsonArray pairs = body.putArray("product_ids");
            Arrays.asList(symbols).forEach(pairs::addString);

            subscriptionJson.toJsonAndEoj(jsonWriter);
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

        String instrument = object.getString("product_id");
        if (instrument == null) {
            return;
        }

        String feed = object.getString("feed");
        if ("book_snapshot".equalsIgnoreCase(feed)) {
            long timestamp = object.getLong("timestamp");
            L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor
                = getPriceBookProcessor(instrument);
            l2Processor.onSnapshotPackageStarted(TimeConstants.TIMESTAMP_UNKNOWN, timestamp);
            processSnapshotSide(l2Processor, object.getArray("bids"), false);
            processSnapshotSide(l2Processor, object.getArray("asks"), true);
            l2Processor.onPackageFinished();
        } else if ("book".equalsIgnoreCase(feed)) {
            long timestamp = object.getLong("timestamp");
            L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor
                = getPriceBookProcessor(instrument);

            l2Processor.onIncrementalPackageStarted(timestamp);
            priceBookEvent.reset();
            long size = Decimal64Utils.fromBigDecimal(object.getDecimalRequired("qty"));
            if (Decimal64Utils.isZero(size)) {
                size = TypeConstants.DECIMAL_NULL; // means delete the price
            }
            priceBookEvent.set(
                "sell".equalsIgnoreCase(object.getStringRequired("side")),
                Decimal64Utils.fromBigDecimal(object.getDecimalRequired("price")),
                size
            );
            l2Processor.onEvent(priceBookEvent);
            l2Processor.onPackageFinished();
        } else if ("trade".equalsIgnoreCase(feed)) {
            long price = Decimal64Utils.fromBigDecimal(object.getDecimalRequired("price"));
            long size = Decimal64Utils.fromBigDecimal(object.getDecimalRequired("qty"));
            long timestamp = object.getLong("time");

            tradeProducer.onTrade(timestamp, instrument, price, size);
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
                .withSource(KRAKEN_EXCHANGE_CODE)
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
            JsonObject pair = quotePairs.getObjectRequired(i);
            priceBookEvent.reset();
            priceBookEvent.set(
                    ask,
                    Decimal64Utils.fromBigDecimal(pair.getDecimalRequired("price")),
                    Decimal64Utils.fromBigDecimal(pair.getDecimalRequired("qty"))
            );
            l2Processor.onEvent(priceBookEvent);
        }
    }
}
