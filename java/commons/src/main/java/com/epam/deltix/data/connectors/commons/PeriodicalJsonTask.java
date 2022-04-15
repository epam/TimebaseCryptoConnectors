package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.data.connectors.commons.json.JsonWriter;

public interface PeriodicalJsonTask {

    long delayMillis();

    void execute(JsonWriter jsonWriter);

}
