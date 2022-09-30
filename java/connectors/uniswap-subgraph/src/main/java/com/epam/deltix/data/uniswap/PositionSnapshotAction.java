package com.epam.deltix.data.uniswap;

import com.epam.deltix.timebase.messages.MarketMessage;
import com.epam.deltix.timebase.messages.SchemaElement;

public class PositionSnapshotAction extends MarketMessage implements UpdatableAction<PositionSnapshot> {
    private int index;
    private boolean isLast;
    private PositionSnapshot entity;
    private Action action;

    public PositionSnapshotAction() {
    }

    @SchemaElement()
    public int getIndex() {
        return index;
    }

    @Override
    public void setIndex(int index) {
        this.index = index;
    }

    @SchemaElement()
    public boolean isLast() {
        return isLast;
    }

    @Override
    public void setLast(boolean last) {
        this.isLast = last;
    }

    @SchemaElement()
    public PositionSnapshot getEntity() {
        return entity;
    }

    public void setEntity(PositionSnapshot entity) {
        this.entity = entity;
    }

    @SchemaElement()
    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    @Override
    public void setUpdatableAction(final PositionSnapshot entity, final Action action) {
        setSymbol(entity.getTbSymbol());
        this.entity = entity;
        this.action = action;
    }

    @Override
    public String toString() {
        return "PositionSnapshotAction{" +
                "symbol=" + getSymbol() +
                ", index=" + index +
                ", isLast=" + isLast +
                ", entity=" + entity +
                ", action=" + action +
                '}';
    }
}
