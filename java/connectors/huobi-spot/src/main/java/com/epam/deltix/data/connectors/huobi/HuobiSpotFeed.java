package com.epam.deltix.data.connectors.huobi;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.data.connectors.commons.l2.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.ExchangeCodec;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class HuobiSpotFeed extends SingleWsFeed {
    private static final long HUOBI_EXCHANGE_CODE = ExchangeCodec.codeToLong("HUOBI");
    private static final AtomicLong ID_GENERATOR = new AtomicLong();
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();
    private final Map<String, L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent>>
            l2Processors = new HashMap<>();
    private final DefaultEvent priceBookEvent = new DefaultEvent();
    private final TradeProducer tradeProducer;

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
        tradeProducer = new TradeProducer(HUOBI_EXCHANGE_CODE, output);
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
            L2Processor<PriceBook<DefaultItem<DefaultEvent>, DefaultEvent>, DefaultItem<DefaultEvent>, DefaultEvent> l2Processor
                = getPriceBookProcessor(instrument);

            JsonObject tick = object.getObject("tick");
            if (tick != null) {
                l2Processor.onSnapshotPackageStarted(TimeConstants.TIMESTAMP_UNKNOWN, timestamp);
                processSnapshotSide(l2Processor, tick.getArray("bids"), false);
                processSnapshotSide(l2Processor, tick.getArray("asks"), true);
                l2Processor.onPackageFinished();
            }
        } else if ("trade".equalsIgnoreCase(topicElements[2]) && "detail".equalsIgnoreCase(topicElements[3])) {
            JsonObject tick = object.getObject("tick");
            if (tick != null) {
                JsonArray dataJson = tick.getArray("data");
                if (dataJson != null) {
                    for (int i = 0; i < dataJson.size(); ++i) {
                        JsonObject trade = dataJson.getObject(i);
                        long price = Decimal64Utils.fromBigDecimal(trade.getDecimalRequired("price"));
                        long size = Decimal64Utils.fromBigDecimal(trade.getDecimalRequired("amount"));

                        tradeProducer.onTrade(timestamp, instrument, price, size);
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
                .withSource(HUOBI_EXCHANGE_CODE)
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

}
