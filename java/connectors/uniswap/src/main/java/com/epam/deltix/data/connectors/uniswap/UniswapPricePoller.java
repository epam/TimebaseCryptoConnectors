package com.epam.deltix.data.connectors.uniswap;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.uniswap.quoter.aggregator.QuoteAggregator;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.tickdb.pub.TimeConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.net.http.HttpClient;

public class UniswapPricePoller implements HttpPoller {
    private final MdProcessor processor;
    private final int amount;
    private final int depth;
    private final Logger logger;
    private final String name;
    private final QuoteAggregator aggregator;

    public UniswapPricePoller(
            final MessageOutput messageOutput,
            final MdModel.Options selected,
            final int amount,
            final int depth,
            final Logger logger,
            final String name,
            final QuoteAggregator aggregator) {
        processor = MdProcessor.create("UNISWAP", messageOutput, selected, depth);
        this.amount = amount;
        this.logger = logger;
        this.name = name;
        this.depth = depth;
        this.aggregator = aggregator;
    }

    @Override
    public void poll(
            final HttpClient client,
            final Runnable continuator,
            final ErrorListener errorListener) {
        client.executor().get().execute(new PriceRequest(client, continuator, errorListener));
    }

    private class PriceRequest implements Runnable {
        private final HttpClient client;
        private final Runnable continuator;
        private final ErrorListener errorListener;

        public PriceRequest(
                final HttpClient client,
                final Runnable continuator,
                final ErrorListener errorListener) {
            this.client = client;
            this.continuator = continuator;
            this.errorListener = errorListener;
        }

        @Override
        public void run() {
            JSONObject orderBook = aggregator.buildLevel2(new BigDecimal(amount), depth);
            processQuotes(orderBook);
            continuator.run();
        }

        private void processQuotes(JSONObject quotes) {
            QuoteSequenceProcessor quotesListener = processor.onBookSnapshot(name, TimeConstants.USE_CURRENT_TIME);
            processSnapshotSide(quotesListener, quotes.getJSONArray("asks"), true);
            processSnapshotSide(quotesListener, quotes.getJSONArray("bids"), false);
            quotesListener.onFinish();
        }

        protected void processSnapshotSide(QuoteSequenceProcessor quotesListener, JSONArray quotePairs, boolean ask) {
            if (quotePairs == null) {
                return;
            }

            for (int i = 0; i < quotePairs.length(); i++) {
                final JSONArray pair = quotePairs.getJSONArray(i);
                if (pair.length() < 2) {
                    throw new IllegalArgumentException("Unexpected size of "
                            + (ask ? "an ask" : "a bid")
                            + " quote: "
                            + pair.length());
                }

                quotesListener.onQuote(
                        Decimal64Utils.parse(pair.getString(0)),
                        Decimal64Utils.parse(pair.getString(1)),
                        ask
                );
            }
        }
    }
}
