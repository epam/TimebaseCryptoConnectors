package com.epam.deltix.data.connectors.uniswap;

import com.epam.deltix.data.connectors.commons.*;
import com.epam.deltix.data.connectors.commons.json.JsonArray;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.connectors.commons.json.JsonValue;
import com.epam.deltix.data.connectors.commons.json.JsonValueParser;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class UniswapPricePoller implements HttpPoller {
    private final MdProcessor processor;
    private String token0Id;
    private String token1id;
    private String uri;
    private int amount;
    private int depth;
    private Logger logger;
    private String name;

    public UniswapPricePoller(
            final String uri,
            final GraphQlQuery.Query queryTemplate,
            final MessageOutput messageOutput,
            final MdModel.Options selected,
            final int amount,
            final int depth,
            final String token0Id,
            final String token1id,
            final Logger logger,
            final String name) {
        processor = MdProcessor.create("UNISWAP", messageOutput, selected, depth, depth);
        this.token0Id = token0Id;
        this.token1id = token1id;
        this.uri = uri;
        this.amount = amount;
        this.logger = logger;
        this.name = name;
        this.depth = depth;
    }

    @Override
    public void poll(
            final HttpClient client,
            final Runnable continuator,
            final ErrorListener errorListener) {
        client.executor().get().execute(new UniswapPricePoller.PriceRequest(client, continuator, errorListener));
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
            String fullUrl = uri + "price?token0=" + token0Id + "&token1=" + token1id
                    + "&amount=" + amount + "&depth=" + depth;
            HttpClient httpClient = HttpClient
                    .newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            try {
                try {
                    HttpResponse<String> response = httpClient.send(
                            HttpRequest.newBuilder(new URI(fullUrl)).GET().build(),
                            HttpResponse.BodyHandlers.ofString());
                    processQuotes(response.body());
                } catch (URISyntaxException e) {
                    logger.warning("Error: url: " + fullUrl + " is not valid", e);
                }
            } catch (final Throwable t) {
                errorListener.onError(t);
            }
            continuator.run();
        }

        private void processQuotes(String body) {
            JsonValueParser jsonParser = new JsonValueParser();
            jsonParser.parse(body);

            JsonValue jsonValue = jsonParser.eoj();
            JsonObject object = jsonValue.asObject();
            long time = object.getLong("timestamp");
            QuoteSequenceProcessor quotesListener = processor.onBookSnapshot(name, time);
            processSnapshotSide(quotesListener, object.getArray("asks"), true);
            processSnapshotSide(quotesListener, object.getArray("bids"), false);
            quotesListener.onFinish();
        }

        protected void processSnapshotSide(QuoteSequenceProcessor quotesListener, JsonArray quotePairs, boolean ask) {
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

                quotesListener.onQuote(
                        pair.getDecimal64Required(0),
                        pair.getDecimal64Required(1),
                        ask
                );
            }
        }
    }
}
