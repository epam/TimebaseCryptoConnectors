package com.epam.deltix.data.connectors.uniswapsubgraph;

import com.epam.deltix.data.connectors.commons.Factory;
import com.epam.deltix.data.connectors.commons.JsonObjectsListener;
import com.epam.deltix.data.connectors.commons.MessageOutput;
import com.epam.deltix.data.connectors.commons.json.JsonObject;
import com.epam.deltix.data.uniswap.Action;
import com.epam.deltix.data.uniswap.Updatable;
import com.epam.deltix.data.uniswap.UpdatableAction;
import com.epam.deltix.timebase.messages.MarketMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstantListener<U extends Updatable, A extends UpdatableAction<U>>
        implements JsonObjectsListener {
    private final Map<String, U> state = new HashMap<>();
    private final List<A> actions = new ArrayList<>();
    private Factory<U> entityFactory;
    private Factory<A> entityActionFactory;
    private MessageOutput messageOutput;

    ConstantListener(final Factory<U> entityFactory,
                     final Factory<A> entityActionFactory,
                     final MessageOutput messageOutput) {
        this.entityFactory = entityFactory;
        this.entityActionFactory = entityActionFactory;
        this.messageOutput = messageOutput;
    }

    @Override
    public void onObjectsStarted() {
        actions.clear();
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
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onObjectsFinished() {
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
}
