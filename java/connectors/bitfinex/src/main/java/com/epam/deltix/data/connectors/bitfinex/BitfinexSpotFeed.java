package com.epam.deltix.data.connectors.bitfinex;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.*;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.timebase.messages.TypeConstants;
import com.epam.deltix.util.collections.generated.LongToObjectHashMap;

import java.util.Arrays;

public class BitfinexSpotFeed extends MdSingleWsFeed {
    // all fields are used by one single thread of WsFeed's ExecutorService
    private final JsonValueParser jsonParser = new JsonValueParser();

    private final int depth;

    private final LongToObjectHashMap<Channel> channels = new LongToObjectHashMap<>();

    private enum ChannelType {
        BOOK,
        TRADES
    }

    private static class Channel {
        private final ChannelType type;
        private final String symbol;
        private boolean snapshotReceived;

        public Channel(ChannelType type, String symbol) {
            this.type = type;
            this.symbol = symbol;
        }

        public ChannelType type() {
            return type;
        }

        public String symbol() {
            return symbol;
        }

        public boolean snapshotReceived() {
            if (snapshotReceived) {
                return true;
            }

            snapshotReceived = true;
            return false;
        }
    }

    public BitfinexSpotFeed(
            final String uri,
            final int depth,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final String... symbols) {

        super("BITFINEX", uri, depth, 10000, selected, output, errorListener, symbols);
        this.depth = depth;
    }

    @Override
    protected void subscribe(JsonWriter jsonWriter, String... symbols) {
        channels.clear();

        if (selected().level1() || selected().level2()) {
            Arrays.asList(symbols).forEach(s -> {
                JsonValue subscriptionJson = JsonValue.newObject();
                JsonObject body = subscriptionJson.asObject();

                body.putString("event", "subscribe");
                body.putString("channel", "book");
                body.putString("symbol", s);
                body.putString("prec", "P0");
                body.putString("freq", "F0");
                body.putString("len", String.valueOf(getBitfinexAvailableDepth()));

                subscriptionJson.toJsonAndEoj(jsonWriter);
            });
        }

        if (selected().trades()) {
            Arrays.asList(symbols).forEach(s -> {
                JsonValue subscriptionJson = JsonValue.newObject();
                JsonObject body = subscriptionJson.asObject();

                body.putString("event", "subscribe");
                body.putString("channel", "trades");
                body.putString("symbol", s);

                subscriptionJson.toJsonAndEoj(jsonWriter);
            });
        }
    }

    private int getBitfinexAvailableDepth() {
        if (depth <= 1) {
            return 1;
        } else if (depth <= 25) {
            return 25;
        } else if (depth <= 100) {
            return 100;
        } else {
            return 250;
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
        JsonArray array = jsonValue.asArray();
        if (object != null) {
            if ("subscribed".equalsIgnoreCase(object.getString("event"))) {
                String channel = object.getString("channel");
                String symbol = object.getString("symbol");
                long channelId = object.getLong("chanId");
                ChannelType type;
                if ("book".equalsIgnoreCase(channel)) {
                    type = ChannelType.BOOK;
                } else if ("trades".equalsIgnoreCase(channel)) {
                    type = ChannelType.TRADES;
                } else {
                    return;
                }

                if (channels.get(channelId, null) == null) {
                    channels.put(channelId, new Channel(type, symbol));
                }
            }
        } else if (array != null) {
            if (array.size() < 2) {
                return;
            }

            long channelId = array.getLong(0);
            Channel channel = channels.get(channelId, null);
            if (channel != null) {
                if (channel.type() == ChannelType.BOOK) {
                    if (array.getArray(1) == null) {
                        return;
                    }

                    if (!channel.snapshotReceived()) {
                        QuoteSequenceProcessor quotesListener = processor().onBookSnapshot(channel.symbol);
                        processQuotes(quotesListener, array.getArray(1), true);
                        quotesListener.onFinish();
                    } else {
                        QuoteSequenceProcessor quotesListener = processor().onBookUpdate(channel.symbol);
                        processQuotes(quotesListener, array.getArray(1), false);
                        quotesListener.onFinish();
                    }
                } else if (channel.type() == ChannelType.TRADES) {
                    if (channel.snapshotReceived()) {
                        if (array.size() < 3) {
                            return;
                        }

                        if ("tu".equalsIgnoreCase(array.getString(1))) {
                            JsonArray trade = array.getArray(2);
                            if (trade != null) {
                                if (trade.size() < 4) {
                                    throw new RuntimeException("Invalid trade size: " + trade.size());
                                }

                                processor().onTrade(
                                    channel.symbol(),
                                    trade.getLong(1),
                                    Decimal64Utils.abs(trade.getDecimal64Required(2)),
                                    trade.getDecimal64Required(3)
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    private void processQuotes(QuoteSequenceProcessor quotesListener, JsonArray quotes, boolean snapshot) {
        if (quotes.getArray(0) != null) {
            for (int i = 0; i < quotes.size(); ++i) {
                processQuote(quotesListener, quotes.getArray(i), snapshot);
            }
        } else {
            processQuote(quotesListener, quotes, snapshot);
        }
    }

    private void processQuote(QuoteSequenceProcessor quotesListener, JsonArray quote, boolean snapshot) {
        if (quote.size() < 3) {
            throw new RuntimeException("Invalid quote size: " + quote.size());
        }

        long price = quote.getDecimal64(0);
        long size = quote.getDecimal64(2);
        boolean isAsk = Decimal64Utils.isLess(size, Decimal64Utils.ZERO);
        size = Decimal64Utils.abs(size);
        if (!snapshot && Decimal64Utils.isZero(quote.getDecimal64Required(1))) {
            size = TypeConstants.DECIMAL_NULL; // means delete the price
        }

        quotesListener.onQuote(price, size, isAsk);
    }

}
