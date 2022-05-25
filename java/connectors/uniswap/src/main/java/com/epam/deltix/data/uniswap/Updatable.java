package com.epam.deltix.data.uniswap;

import com.epam.deltix.data.connectors.commons.json.JsonObject;

public interface Updatable {

    String getTbSymbol();

    boolean update(JsonObject from);

}
