package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.timebase.messages.InstrumentMessage;

public interface MessageOutput {

    void send(InstrumentMessage message);

}
