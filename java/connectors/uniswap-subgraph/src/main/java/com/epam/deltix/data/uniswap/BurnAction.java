package com.epam.deltix.data.uniswap;

import com.epam.deltix.timebase.messages.MarketMessage;
import com.epam.deltix.timebase.messages.SchemaElement;

public class BurnAction extends MarketMessage implements UpdatableAction<Burn> {
    private int index;
    private boolean isLast;
    private Burn entity;
    private Action action;

    public BurnAction() {
    }

    @SchemaElement()
    public int getIndex() {
        return index;
    }

    @Override
    public void setIndex(final int index) {
        this.index = index;
    }

    @SchemaElement()
    public boolean isLast() {
        return isLast;
    }

    @Override
    public void setLast(final boolean last) {
        isLast = last;
    }

    @SchemaElement()
    public Burn getEntity() {
        return entity;
    }

    public void setEntity(Burn entity) {
        this.entity = entity;
    }

    @SchemaElement()
    public Action getAction() {
        return action;
    }

    public void setAction(final Action action) {
        this.action = action;
    }

    @Override
    public void setUpdatableAction(final Burn entity, final Action action) {
        setSymbol(entity.getTbSymbol());
        this.entity = entity;
        this.action = action;
    }

    @Override
    public String toString() {
        return "BurnAction{" +
                "symbol=" + getSymbol() +
                ", index=" + index +
                ", isLast=" + isLast +
                ", entity=" + entity +
                ", action=" + action +
                '}';
    }
}
