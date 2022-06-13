package com.epam.deltix.data.uniswap;

import com.epam.deltix.timebase.messages.MarketMessage;
import com.epam.deltix.timebase.messages.SchemaElement;

public class TokenAction extends MarketMessage implements UpdatableAction<Token> {
    private int index;
    private boolean isLast;
    private Token entity;
    private Action action;

    public TokenAction() {
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
    public Token getEntity() {
        return entity;
    }

    public void setEntity(Token entity) {
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
    public void setUpdatableAction(final Token entity, final Action action) {
        setSymbol(entity.getTbSymbol());
        this.entity = entity;
        this.action = action;
    }

    @Override
    public String toString() {
        return "TokenAction{" +
                "symbol=" + getSymbol() +
                ", index=" + index +
                ", isLast=" + isLast +
                ", entity=" + entity +
                ", action=" + action +
                '}';
    }
}
