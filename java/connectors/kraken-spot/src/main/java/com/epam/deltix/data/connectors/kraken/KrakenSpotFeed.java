package com.epam.deltix.data.connectors.kraken;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import com.epam.deltix.timebase.messages.TypeConstants;

import java.util.Arrays;

public class KrakenSpotFeed extends MdSingleWsFeed {
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();

    private final int depth;

    public KrakenSpotFeed(
        final String uri,
        final int depth,
        final MdModel.Options selected,
        final CloseableMessageOutput output,
        final ErrorListener errorListener,
        final Logger logger,
        final String... symbols) {

        super("KRAKEN",
                uri,
                depth,
                getKrakenBookSize(depth),
                5000,
                selected,
                output,
                errorListener,
                logger,
                null,
                false,
                symbols);

        this.depth = depth;
    }

    private static int getKrakenBookSize(int depth) {
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
    protected void subscribe(JsonWriter jsonWriter, String... symbols) {
        if (selected().level1() || selected().level2()) {
            JsonValue subscriptionJson = JsonValue.newObject();
            JsonObject body = subscriptionJson.asObject();
            body.putString("event", "subscribe");
            JsonArray pairs = body.putArray("pair");
            Arrays.asList(symbols).forEach(pairs::addString);
            JsonObject subscription = body.putObject("subscription");
            subscription.putString("name", "book");
            subscription.putInteger("depth", getKrakenBookSize(depth));
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

        String type = array.getString(array.size() - 2);
        if (type == null) {
            return;
        }

        String instrument = array.getString(array.size() - 1);
        if (instrument == null) {
            return;
        }

        if (type.startsWith("book-")) {
            JsonObject values = array.getObject(1);
            JsonArray bs = values.getArray("bs");
            JsonArray as = values.getArray("as");
            if (bs != null || as != null) {
                QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(instrument,
                    computeTimestamp(bs, computeTimestamp(as, TimeConstants.TIMESTAMP_UNKNOWN))
                );
                processSnapshotSide(quotesListener, bs, false);
                processSnapshotSide(quotesListener, as, true);
                quotesListener.onFinish();
            } else {
                JsonArray b = values.getArray("b");
                JsonArray a = values.getArray("a");
                JsonObject values2 = array.getObject(2);
                if (values2 != null) {
                    if (b == null) {
                        b = values2.getArray("b");
                    }
                    if (a == null) {
                        a = values2.getArray("a");
                    }
                }

                if (b != null || a != null) {
                    QuoteSequenceProcessor quotesListener = processor().onBookUpdate(instrument,
                        computeTimestamp(a, computeTimestamp(b, TimeConstants.TIMESTAMP_UNKNOWN))
                    );
                    processChanges(quotesListener, b, false);
                    processChanges(quotesListener, a, true);
                    quotesListener.onFinish();
                }
            }
        } else if (type.startsWith("trade")) {
            JsonArray trades = array.getArray(1);
            if (trades != null) {
                for (int i = 0; i < trades.size(); ++i) {
                    JsonArray trade = trades.getArray(i);
                    long price = trade.getDecimal64Required(0);
                    long size = trade.getDecimal64Required(1);
                    long timestamp = Util.parseTime(trade.getString(2));

                    processor().onTrade(instrument, timestamp, price, size);
                }
            }
        }
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
            final QuoteSequenceProcessor quotesListener, final JsonArray quotePairs, final boolean ask) {

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

            quotesListener.onQuote(
                pair.getDecimal64Required(0),
                pair.getDecimal64Required(1),
                ask
            );
        }
    }

    private void processChanges(
            final QuoteSequenceProcessor quotesListener, final JsonArray changes, final boolean ask) {

        if (changes == null) {
            return;
        }

        for (int i = 0; i < changes.size(); i++) {
            final JsonArray change = changes.getArrayRequired(i);
            if (change.size() != 3 && change.size() != 4) {
                throw new IllegalArgumentException("Unexpected size of a change :" + change.size());
            }

            long size = change.getDecimal64Required(1);
            if (Decimal64Utils.isZero(size)) {
                size = TypeConstants.DECIMAL_NULL; // means delete the price
            }

            try {
                quotesListener.onQuote(
                    change.getDecimal64Required(0),
                    size,
                    ask
                );
            } catch (Throwable t) {
                int k = 3;
            }
        }
    }
}
