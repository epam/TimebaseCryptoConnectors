package com.epam.deltix.data.connectors.uniswap;

import com.epam.deltix.data.connectors.commons.CloseableMessageOutput;
import com.epam.deltix.data.connectors.commons.ErrorListener;
import com.epam.deltix.data.connectors.commons.GraphQlQuery;
import com.epam.deltix.data.connectors.commons.HttpFeed;
import com.epam.deltix.data.connectors.commons.Logger;
import com.epam.deltix.data.connectors.commons.MdModel;
import com.epam.deltix.data.uniswap.Pool;
import com.epam.deltix.data.uniswap.PoolAction;
import com.epam.deltix.data.uniswap.Token;
import com.epam.deltix.data.uniswap.TokenAction;

import java.net.URISyntaxException;

public class UniswapFeed extends HttpFeed {
    private static GraphQlQuery.Query poolsQuery() {
        final GraphQlQuery.Query result = GraphQlQuery.query("pools");
        result.arguments().withOrderBy("id");

        result.withScalar("id");
        result.withScalar("liquidity");
        result.withScalar("liquidityProviderCount");

        final GraphQlQuery.Object token0 = result.withObject("token0");
        token0.withScalar("id");
        token0.withScalar("symbol");

        final GraphQlQuery.Object token1 = result.withObject("token1");
        token1.withScalar("id");
        token1.withScalar("symbol");

        return result;
    }


    private static GraphQlQuery.Query tokensQuery() {
        final GraphQlQuery.Query result = GraphQlQuery.query("tokens");
        result.arguments().withOrderBy("id");

        result.withScalar("id");
        result.withScalar("symbol");
        result.withScalar("name");
        result.withScalar("decimals");
        result.withScalar("totalSupply");
        result.withScalar("volume");
        result.withScalar("volumeUSD");
        result.withScalar("untrackedVolumeUSD");
        result.withScalar("feesUSD");
        result.withScalar("txCount");
        result.withScalar("poolCount");
        result.withScalar("totalValueLocked");
        result.withScalar("totalValueLockedUSD");
        result.withScalar("totalValueLockedUSDUntracked");
        result.withScalar("derivedETH");
        return result;
    }

    private final String uri;
    private final int pollTimeoutMillis;

    public UniswapFeed(
            final String uri,
            final MdModel.Options selected,
            final CloseableMessageOutput output,
            final ErrorListener errorListener,
            final Logger logger,
            final int pollTimeoutMillis) {
        super(selected, output, errorListener, logger);

        this.uri = uri;
        this.pollTimeoutMillis = pollTimeoutMillis;
    }

    @Override
    public void start() {
        super.start();

        final MdModel.Options selected = selected();

        try {
            if (selected.custom(PoolAction.class)) {
                schedule(new UniswapCollectionPoller<Pool, PoolAction>(
                                uri,
                                poolsQuery(),
                                Pool::new,
                                PoolAction::new,
                                this),
                        pollTimeoutMillis);
            }

            if (selected.custom(TokenAction.class)) {
                schedule(new UniswapCollectionPoller<Token, TokenAction>(
                                uri,
                                tokensQuery(),
                                Token::new,
                                TokenAction::new,
                                this),
                        pollTimeoutMillis);
            }
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
