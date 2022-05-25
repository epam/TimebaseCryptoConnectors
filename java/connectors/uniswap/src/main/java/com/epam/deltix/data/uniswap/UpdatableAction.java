package com.epam.deltix.data.uniswap;

public interface UpdatableAction<U extends Updatable> {
    void setIndex(final int index);

    void setLast(final boolean last);

    void setUpdatableAction(U entity, Action action);
}
