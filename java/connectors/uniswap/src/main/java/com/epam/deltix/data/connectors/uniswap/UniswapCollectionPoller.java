package com.epam.deltix.data.connectors.uniswap;

import com.epam.deltix.data.connectors.commons.Factory;
import com.epam.deltix.data.connectors.commons.GraphQlQuery;
import com.epam.deltix.data.connectors.commons.JsonObjectsListener;
import com.epam.deltix.data.connectors.commons.MessageOutput;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.uniswap.Action;
import com.epam.deltix.data.uniswap.UpdatableAction;
import com.epam.deltix.data.uniswap.Updatable;
import com.epam.deltix.timebase.messages.MarketMessage;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UniswapCollectionPoller<U extends Updatable, A extends UpdatableAction<U>>
        extends PaginatingUniswapHttpPoller {

    public UniswapCollectionPoller(
            final String graphQlUri,
            final GraphQlQuery.Query query,
            final Factory<U> entityFactory,
            final Factory<A> entityActionFactory,
            final MessageOutput messageOutput) throws URISyntaxException {

        super(
                graphQlUri,
                query,
                1_000,
                new JsonObjectsListener() {
                    private final Map<String, U> state = new HashMap<>();
                    private final Map<String, U> toDelete = new HashMap<>();

                    private final List<A> actions = new ArrayList<>();

                    @Override
                    public void onObjectsStarted() {
                        actions.clear();
                        toDelete.clear();
                        toDelete.putAll(state);
                    }

                    @Override
                    public void onObject(final JsonObject object) {
                        final String id = object.getStringRequired("id");

                        U entity = state.get(id);
                        if (entity == null) {
                            entity = entityFactory.create();
                            entity.update(object);
                            state.put(id, entity);

                            final A action = entityActionFactory.create();
                            action.setUpdatableAction(entity, Action.NEW);

                            actions.add(action);
                        } else {
                            if (entity.update(object)) {
                                final A action = entityActionFactory.create();
                                action.setUpdatableAction(entity, Action.UPDATE);

                                actions.add(action);
                            }
                            toDelete.remove(id);
                        }
                    }

                    @Override
                    @SuppressWarnings("unchecked")
                    public void onObjectsFinished() {
                        if (!toDelete.isEmpty()) {
                            toDelete.values().stream().forEach(entity -> {
                                        final A action = entityActionFactory.create();
                                        action.setUpdatableAction(entity, Action.DELETE);

                                        actions.add(action);
                                    }
                            );
                        }

                        if (actions.isEmpty()) {
                            return;
                        }

                        for (int i = 0; i < actions.size(); i++) {
                            final A action = actions.get(i);
                            action.setIndex(i);
                            action.setLast(i == (actions.size() - 1));
                            messageOutput.send((MarketMessage) action); // unchecked
                        }
                    }
                });
    }
}
