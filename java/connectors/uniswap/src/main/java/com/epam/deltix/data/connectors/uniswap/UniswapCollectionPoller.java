package com.epam.deltix.data.connectors.uniswap;

import com.epam.deltix.data.connectors.commons.Factory;
import com.epam.deltix.data.connectors.commons.GraphQlQuery;
import com.epam.deltix.data.connectors.commons.MessageOutput;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.uniswap.Updatable;
import com.epam.deltix.data.uniswap.UpdatableAction;

import java.util.function.Predicate;

public class UniswapCollectionPoller<U extends Updatable, A extends UpdatableAction<U>>
        extends PaginatingUniswapHttpPoller {

    public UniswapCollectionPoller(
            final String graphQlUri,
            final GraphQlQuery.Query queryTemplate,
            final Factory<U> entityFactory,
            final Factory<A> entityActionFactory,
            final MessageOutput messageOutput) {
        this(
                graphQlUri,
                queryTemplate,
                object -> true,
                entityFactory,
                entityActionFactory,
                messageOutput
        );
    }

    public UniswapCollectionPoller(
            final String graphQlUri,
            final GraphQlQuery.Query queryTemplate,
            final Predicate<JsonObject> objectFilter,
            final Factory<U> entityFactory,
            final Factory<A> entityActionFactory,
            final MessageOutput messageOutput) {

        this(
                graphQlUri,
                queryTemplate,
                object -> true,
                entityFactory,
                entityActionFactory,
                messageOutput,
                true);
    }

    public UniswapCollectionPoller(
            final String graphQlUri,
            final GraphQlQuery.Query queryTemplate,
            final Predicate<JsonObject> objectFilter,
            final Factory<U> entityFactory,
            final Factory<A> entityActionFactory,
            final MessageOutput messageOutput,
            final boolean isUpdatable) {
        super(
                graphQlUri,
                queryTemplate,
                objectFilter,
                1_000, // TODO: a system property with a default value?
                isUpdatable ?
                        new UpdatableListener<U, A>(
                                entityFactory,
                                entityActionFactory,
                                messageOutput) :
                        new ConstantListener<U, A>(
                                entityFactory,
                                entityActionFactory,
                                messageOutput)
        );
    }
}
