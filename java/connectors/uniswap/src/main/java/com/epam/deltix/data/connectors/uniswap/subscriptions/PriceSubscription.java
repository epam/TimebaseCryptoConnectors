package com.epam.deltix.data.connectors.uniswap.subscriptions;

import com.epam.deltix.data.connectors.commons.HttpPoller;
import com.epam.deltix.data.connectors.commons.Logger;
import com.epam.deltix.data.connectors.commons.MdModel;
import com.epam.deltix.data.connectors.commons.MessageOutput;
import com.epam.deltix.data.connectors.uniswap.UniswapPricePoller;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class PriceSubscription extends Subscription {
    private MdModel.Options selected;
    private int amount;
    private int depth;
    private Logger logger;

    public PriceSubscription(
            final String uri,
            final MessageOutput messageOutput,
            final Logger logger,
            final MdModel.Options selected,
            final int amount,
            final int depth,
            final IdentifiedUniswapSymbol... symbols) {
        this(uri, messageOutput, logger, symbols);
        this.selected = selected;
        this.amount = amount;
        this.depth = depth;
        this.logger = logger;
    }

    public PriceSubscription(
            final String uri,
            final MessageOutput messageOutput,
            final Logger logger,
            final IdentifiedUniswapSymbol... symbols) {
        super(uri, messageOutput, logger, symbols);
    }

    @Override
    public Collection<HttpPoller> get() {
        return Arrays.stream(symbols).map(symbol -> {
            String name = symbol.token0() + "/" + symbol.token1();
            return new UniswapPricePoller(
                    uri,
                    null,
                    messageOutput,
                    selected,
                    amount,
                    depth,
                    symbol.token0Id(),
                    symbol.token1Id(),
                    logger,
                    name);
        }).collect(Collectors.toList());
    }
}
