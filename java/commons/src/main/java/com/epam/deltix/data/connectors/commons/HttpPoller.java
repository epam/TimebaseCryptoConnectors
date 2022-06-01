package com.epam.deltix.data.connectors.commons;

import java.net.http.HttpClient;

public interface HttpPoller {

    void poll(HttpClient client, Runnable continuator, ErrorListener errorListener);

}
