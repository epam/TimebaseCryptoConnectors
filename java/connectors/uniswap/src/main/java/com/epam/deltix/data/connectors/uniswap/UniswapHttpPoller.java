package com.epam.deltix.data.connectors.uniswap;

import com.epam.deltix.data.connectors.commons.ErrorListener;
import com.epam.deltix.data.connectors.commons.HttpPoller;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public abstract class UniswapHttpPoller implements HttpPoller {
    private final URI graphQlUri;

    public UniswapHttpPoller(final String graphQlUri) throws URISyntaxException {
        this.graphQlUri = new URI(graphQlUri);
    }

    @Override
    public void poll(
            final HttpClient client,
            final Runnable continuator,
            final ErrorListener errorListener) {
        client.executor().get().execute(new Request(client, continuator, errorListener));
    }

    private class Request implements Runnable {
        private final HttpClient client;
        private final Runnable continuator;
        private final ErrorListener errorListener;

        private volatile String requestBody;

        public Request(
                final HttpClient client,
                final Runnable continuator,
                final ErrorListener errorListener) {
            this.client = client;
            this.continuator = continuator;
            this.errorListener = errorListener;
            this.requestBody = firstRequestBody();
        }

        @Override
        public void run() {
            try {
                final HttpRequest request = HttpRequest.newBuilder()
                        .uri(graphQlUri)
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(10))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                client
                        .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                        .thenApply(response -> response.body())
                        .whenComplete((body, error) -> {
                            if (error != null) {
                                errorListener.onError(error);
                            } else {
                                requestBody = nextRequestBody(body);
                                if (requestBody == null) {
                                    continuator.run();
                                    return;
                                }
                                client.executor().get().execute(this);
                            }
                        });
            } catch (final Throwable t) {
                errorListener.onError(t);
            }
        }
    }

    protected abstract String firstRequestBody();

    protected String nextRequestBody(final String response) {
        return null;
    }
}