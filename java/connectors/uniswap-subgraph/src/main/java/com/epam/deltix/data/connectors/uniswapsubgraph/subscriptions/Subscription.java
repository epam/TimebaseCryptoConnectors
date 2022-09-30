package com.epam.deltix.data.connectors.uniswapsubgraph.subscriptions;

import com.epam.deltix.data.connectors.commons.HttpPoller;
import com.epam.deltix.data.connectors.commons.Logger;
import com.epam.deltix.data.connectors.commons.MessageOutput;

import java.util.Collection;
import java.util.function.Supplier;

public abstract class Subscription implements Supplier<Collection<HttpPoller>> {
    protected final String uri;
    protected final MessageOutput messageOutput;
    protected final Logger logger;
    protected final IdentifiedUniswapSymbol[] symbols;

    protected Subscription(
            final String uri,
            final MessageOutput messageOutput,
            final Logger logger,
            final IdentifiedUniswapSymbol... symbols) {

        this.uri = uri;
        this.messageOutput = messageOutput;
        this.logger = logger;
        this.symbols = symbols;
    }
}
