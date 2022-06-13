package com.epam.deltix.data.connectors.uniswap;

import com.epam.deltix.data.connectors.commons.CloseableMessageOutput;
import com.epam.deltix.data.connectors.commons.ErrorListener;
import com.epam.deltix.data.connectors.commons.HttpFeed;
import com.epam.deltix.data.connectors.commons.HttpPoller;
import com.epam.deltix.data.connectors.commons.Logger;
import com.epam.deltix.data.connectors.commons.MdModel;
import com.epam.deltix.data.connectors.uniswap.subscriptions.BundleSubscription;
import com.epam.deltix.data.connectors.uniswap.subscriptions.FactorySubscription;
import com.epam.deltix.data.connectors.uniswap.subscriptions.IdentifiedUniswapSymbol;
import com.epam.deltix.data.connectors.uniswap.subscriptions.PoolSubscription;
import com.epam.deltix.data.connectors.uniswap.subscriptions.Subscription;
import com.epam.deltix.data.connectors.uniswap.subscriptions.TokenIdentifier;
import com.epam.deltix.data.connectors.uniswap.subscriptions.TokenSubscription;
import com.epam.deltix.data.connectors.uniswap.subscriptions.UniswapSymbol;
import com.epam.deltix.data.uniswap.BundleAction;
import com.epam.deltix.data.uniswap.FactoryAction;
import com.epam.deltix.data.uniswap.PoolAction;
import com.epam.deltix.data.uniswap.TokenAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UniswapFeed extends HttpFeed {
    private final String uri;
    private final int pollTimeoutMillis;
    private final String[] symbols;

    public UniswapFeed(
            final String uri,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final int pollTimeoutMillis,
            final String... symbols) {
        super(selected, output, errorListener, logger);

        this.uri = uri;
        this.pollTimeoutMillis = pollTimeoutMillis;
        this.symbols = symbols != null ? symbols : new String[]{};
    }

    @Override
    public void start() {
        super.start();

        // 1. resolve uniswap token ids
        final IdentifiedUniswapSymbol[] identifiedUniswapSymbols;
        if (symbols == null || symbols.length == 0) {
            identifiedUniswapSymbols = new IdentifiedUniswapSymbol[]{};
        } else {
            try {
                identifiedUniswapSymbols = getIdentifiedUniswapSymbols();
            } catch (final Throwable t) {
                onError(t);
                return; // cannot continue without ids.
                // The error event sent to be resolved (process to be re-scheduled)
            }
        }

        // 2. prepare and run pollers
        final MdModel.Options selected = selected();

        final List<Subscription> subscriptions = new ArrayList<>();

        if (selected.custom(FactoryAction.class)) {
            subscriptions.add(new FactorySubscription(
                    uri,
                    this,
                    logger(),
                    identifiedUniswapSymbols
            ));
        }
        if (selected.custom(BundleAction.class)) {
            subscriptions.add(new BundleSubscription(
                    uri,
                    this,
                    logger(),
                    identifiedUniswapSymbols
            ));
        }
        if (selected.custom(PoolAction.class)) {
            subscriptions.add(new PoolSubscription(
                    uri,
                    this,
                    logger(),
                    identifiedUniswapSymbols
            ));
        }
        if (selected.custom(TokenAction.class)) {
            subscriptions.add(new TokenSubscription(
                    uri,
                    this,
                    logger(),
                    identifiedUniswapSymbols
            ));
        }

        for (final Subscription subscription : subscriptions) {
            for (final HttpPoller poller : subscription.get()) {
                schedule(poller, pollTimeoutMillis);
            }
        }
    }

    private IdentifiedUniswapSymbol[] getIdentifiedUniswapSymbols() throws Exception {
        final UniswapSymbol[] uniswapSymbols = Arrays.stream(symbols)
                .map(UniswapSymbol::new).toArray(UniswapSymbol[]::new);

        final String[] tokenSymbols =
                UniswapSymbol.collectSymbols(uniswapSymbols);

        final Map<String, TokenIdentifier.Info> tokenInfos =
                TokenIdentifier.identify(uri, tokenSymbols);

        return IdentifiedUniswapSymbol.identify(tokenInfos, uniswapSymbols);
    }
}
