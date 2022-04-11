package com.epam.deltix.data.connectors.coinbase;

import com.epam.deltix.data.connectors.commons.CloseableMessageOutput;
import com.epam.deltix.data.connectors.commons.ErrorListener;
import com.epam.deltix.data.connectors.commons.Iso8601DateTimeParser;
import com.epam.deltix.data.connectors.commons.MdModel;
import com.epam.deltix.data.connectors.commons.SingleWsFeed;
import com.epam.deltix.data.connectors.commons.TradeProducer;
import com.epam.deltix.data.connectors.commons.json.JsonArray;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.commons.json.JsonValue;
import com.epam.deltix.data.connectors.commons.json.JsonValueParser;
import com.epam.deltix.data.connectors.commons.json.JsonWriter;
import com.epam.deltix.data.connectors.commons.l2.BestBidOfferProducer;
import com.epam.deltix.data.connectors.commons.l2.ChainedL2Listener;
import com.epam.deltix.data.connectors.commons.l2.DefaultEvent;
import com.epam.deltix.data.connectors.commons.l2.DefaultItem;
import com.epam.deltix.data.connectors.commons.l2.L2Processor;
import com.epam.deltix.data.connectors.commons.l2.L2Producer;
import com.epam.deltix.data.connectors.commons.l2.PriceBook;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.ExchangeCodec;
import com.epam.deltix.timebase.messages.TypeConstants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CoinbaseFeed extends SingleWsFeed {
    private static final long COINBASE_EXCHANGE_CODE = ExchangeCodec.codeToLong("COINBASE");
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();
    private final Iso8601DateTimeParser dtParser = new Iso8601DateTimeParser();
    private final Map<String, L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent>>
            l2Processors = new HashMap<>();
    private final DefaultEvent priceBookEvent = new DefaultEvent();
    private final TradeProducer tradeProducer;

    public CoinbaseFeed(
            final String uri,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final String... symbols) {
        super(uri, 5000, selected, output, errorListener, symbols);

        tradeProducer = new TradeProducer(COINBASE_EXCHANGE_CODE, output);
    }

    @Override
    protected void prepareSubscription(
            final JsonWriter jsonWriter,
            final String... symbols) {

        final JsonValue subscriptionJson = JsonValue.newObject();

        final JsonObject body = subscriptionJson.asObject();
        body.putString("type", "subscribe");

        final JsonArray productIds = body.putArray("product_ids");
        Arrays.stream(symbols).forEach(productIds::addString);

        final JsonArray channels = body.putArray("channels");
        if (selected().trades()) {
            channels.addString("ticker");
        }
        if (selected().level1() || selected().level2()) {
            channels.addString("level2");
        }

        //channels.addString("heartbeat"); //?

        subscriptionJson.toJsonAndEoj(jsonWriter);
    }

    @Override
    protected void onJson(final CharSequence data, final boolean last, final JsonWriter jsonWriter) {
        jsonParser.parse(data);

        if (!last) {
            return;
        }

        final JsonValue jsonValue = jsonParser.eoj();

        final JsonObject object = jsonValue.asObjectRequired();
        final String type = object.getString("type");

        if (type == null) {
            return;
        }

        switch (type) {
            case "ticker": {
                final String productId = object.getStringRequired("product_id");
                tradeProducer.onTrade(
                        productId,
                        Decimal64Utils.parse(object.getStringRequired("price")),
                        Decimal64Utils.parse(object.getStringRequired("last_size")));
                break;
            }

            case "snapshot": {
                final L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor
                        = getPriceBookProcessor(object);
                l2Processor.onSnapshotPackageStarted();
                processSnapshotSide(l2Processor, object.getArray("bids"), false);
                processSnapshotSide(l2Processor, object.getArray("asks"), true);
                l2Processor.onPackageFinished();
                break;
            }

            case "l2update": {
                final L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor
                        = getPriceBookProcessor(object);
                dtParser.set(object.getStringRequired("time"));
                l2Processor.onIncrementalPackageStarted(dtParser.millis());
                processChanges(l2Processor, object.getArrayRequired("changes"));
                l2Processor.onPackageFinished();
                break;
            }

            default:
                break;
        }
    }

    private L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> getPriceBookProcessor(final JsonObject object) {
        final String productId = object.getStringRequired("product_id");

        L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent>
                result = l2Processors.get(productId);

        if (result == null) {

            final ChainedL2Listener.Builder<DefaultItem<DefaultEvent>, DefaultEvent> builder =
                    ChainedL2Listener.builder();

            if (selected().level1()) {
                builder.with(new BestBidOfferProducer<>(this));
            }
            if (selected().level2()) {
                builder.with(new L2Producer<>(this));
            }

            result = L2Processor.builder()
                    .withInstrument(productId)
                    .withSource(COINBASE_EXCHANGE_CODE)
                    .withBookOutputSize(10)
                    .buildWithPriceBook(
                            builder.build()
                    );
            l2Processors.put(productId, result);
        }

        return result;
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
            final L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor,
            final JsonArray changes) {

        if (changes == null) {
            return;
        }

        for (int i = 0; i < changes.size(); i++) {
            final JsonArray change = changes.getArrayRequired(i);
            if (change.size() != 3) {
                throw new IllegalArgumentException("Unexpected size of a change :" + change.size());
            }
            priceBookEvent.reset();

            long size = Decimal64Utils.parse(change.getStringRequired(2));
            if (Decimal64Utils.isZero(size)) {
                size = TypeConstants.DECIMAL_NULL; // means delete the price
            }

            priceBookEvent.set(
                    "sell".equals(change.getStringRequired(0)),
                    Decimal64Utils.parse(change.getStringRequired(1)),
                    size
            );

            l2Processor.onEvent(priceBookEvent);
        }
    }
}