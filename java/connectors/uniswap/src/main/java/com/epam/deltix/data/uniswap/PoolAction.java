package com.epam.deltix.data.uniswap;

import com.epam.deltix.timebase.messages.MarketMessage;

public class PoolAction extends MarketMessage implements UpdatableAction<Pool> {
    private int index;
    private boolean isLast;
    private Pool entity;
    private Action action;

    public PoolAction() {
    }

    public int getIndex() {
        return index;
    }

    @Override
    public void setIndex(final int index) {
        this.index = index;
    }

    public boolean isLast() {
        return isLast;
    }

    @Override
    public void setLast(final boolean last) {
        isLast = last;
    }

    public Pool getEntity() {
        return entity;
    }

    public Action getAction() {
        return action;
    }

    @Override
    public void setUpdatableAction(final Pool entity, final Action action) {
        setSymbol(entity.getTbSymbol());
        this.entity = entity;
        this.action = action;
    }

    @Override
    public String toString() {
        return "PoolAction{" +
                "symbol=" + getSymbol() +
                ", index=" + index +
                ", isLast=" + isLast +
                ", entity=" + entity +
                ", action=" + action +
                '}';
    }
}
