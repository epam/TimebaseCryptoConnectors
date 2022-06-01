package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.data.connectors.commons.json.JsonObject;

public interface JsonObjectsListener {

    void onObjectsStarted();

    void onObject(JsonObject object);

    void onObjectsFinished();

}
