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

public class KrakenSpotFeed extends SingleWsFeed {
    private static final long KRAKEN_EXCHANGE_CODE = ExchangeCodec.codeToLong("KRAKEN");
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();
    private final Map<String, L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent>>
        l2Processors = new HashMap<>();
    private final DefaultEvent priceBookEvent = new DefaultEvent();
    private final TradeProducer tradeProducer;

    private final int depth;

    public KrakenSpotFeed(
        final String uri,
        final int depth,
        final MdModel.Options selected,
        final CloseableMessageOutput output,
        final ErrorListener errorListener,
        final String... symbols) {

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
            JsonArray pairs = body.putArray("pair");
            Arrays.asList(symbols).forEach(pairs::addString);
            JsonObject subscription = body.putObject("subscription");
            subscription.putString("name", "book");
            subscription.putInteger("depth", getKrakenAvailableDepth());
            subscriptionJson.toJsonAndEoj(jsonWriter);
        }

        if (selected().trades()) {
            JsonValue subscriptionJson = JsonValue.newObject();
            JsonObject body = subscriptionJson.asObject();
            body.putString("event", "subscribe");
            JsonArray pairs = body.putArray("pair");
            Arrays.asList(symbols).forEach(pairs::addString);
            JsonObject subscription = body.putObject("subscription");
            subscription.putString("name", "trade");
            subscriptionJson.toJsonAndEoj(jsonWriter);
        }
    }

    private int getKrakenAvailableDepth() {
        if (depth <= 10) {
            return 10;
        } else if (depth <= 25) {
            return 25;
        } else if (depth <= 100) {
            return 100;
        } else if (depth <= 500) {
            return 500;
        } else {
            return 1000;
        }
    }

    @Override
    protected void onJson(final CharSequence data, final boolean last, final JsonWriter jsonWriter) {
        jsonParser.parse(data);

        if (!last) {
            return;
        }

        JsonValue jsonValue = jsonParser.eoj();

        JsonArray array = jsonValue.asArray();
        if (array == null) {
            return;
        }

        String type = array.getString(2);
        if (type == null) {
            return;
        }

        if (type.startsWith("book-")) {
            L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor
                = getPriceBookProcessor(array.getString(3));
            JsonObject values = array.getObject(1);
            JsonArray bs = values.getArray("bs");
            JsonArray as = values.getArray("as");
            if (bs != null || as != null) {
                l2Processor.onSnapshotPackageStarted(
                    TimeConstants.TIMESTAMP_UNKNOWN,
                    computeTimestamp(
                        bs, computeTimestamp(as, TimeConstants.TIMESTAMP_UNKNOWN)
                    )
                );
                processSnapshotSide(l2Processor, bs, false);
                processSnapshotSide(l2Processor, as, true);
                l2Processor.onPackageFinished();
            } else {
                JsonArray b = values.getArray("b");
                JsonArray a = values.getArray("a");
                if (b != null || a != null) {
                    l2Processor.onIncrementalPackageStarted(
                        computeTimestamp(
                            a, computeTimestamp(b, TimeConstants.TIMESTAMP_UNKNOWN)
                        )
                    );
                    processChanges(l2Processor, b, false);
                    processChanges(l2Processor, a, true);
                    l2Processor.onPackageFinished();
                }
            }
        } else if (type.startsWith("trade")) {
            String instrument = array.getString(3);
            JsonArray trades = array.getArray(1);
            if (trades != null) {
                for (int i = 0; i < trades.size(); ++i) {
                    JsonArray trade = trades.getArray(i);
                    long price = Decimal64Utils.parse(trade.getString(0));
                    long size = Decimal64Utils.parse(trade.getString(1));
                    long timestamp = Util.parseTime(trade.getString(2));

                    tradeProducer.onTrade(timestamp, instrument, price, size);
                }
            }
        }
    }

    private L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent>
        getPriceBookProcessor(String instrument) {

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

    private long computeTimestamp(JsonArray quotes, long startTimestamp) {
        long timestamp = startTimestamp;
        if (quotes == null) {
            return timestamp;
        }

        for (int i = 0; i < quotes.size(); i++) {
            JsonArray quote = quotes.getArray(i);
            if (quote != null) {
                if (quote.size() >= 3) {
                    String timeString = quote.getString(2);
                    if (timeString != null) {
                        long time = Util.parseTime(timeString);
                        timestamp = Math.max(timestamp, time);
                    }
                }
            }
        }

        return timestamp;
    }

    private void processSnapshotSide(
        final L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor,
        final JsonArray quotePairs,
        final boolean ask) {

        if (quotePairs == null) {
            return;
        }

        for (int i = 0; i < quotePairs.size(); i++) {
            final JsonArray pair = quotePairs.getArrayRequired(i);
            if (pair.size() != 3) {
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
        final L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor,
        final JsonArray changes,
        final boolean ask) {

        if (changes == null) {
            return;
        }

        for (int i = 0; i < changes.size(); i++) {
            final JsonArray change = changes.getArrayRequired(i);
            // todo: process republish?
            if (change.size() != 3 && change.size() != 4) {
                throw new IllegalArgumentException("Unexpected size of a change :" + change.size());
            }
            priceBookEvent.reset();

            long size = Decimal64Utils.parse(change.getStringRequired(1));
            if (Decimal64Utils.isZero(size)) {
                size = TypeConstants.DECIMAL_NULL; // means delete the price
            }

            priceBookEvent.set(
                ask,
                Decimal64Utils.parse(change.getStringRequired(0)),
                size
            );
            l2Processor.onEvent(priceBookEvent);
        }
    }
}
