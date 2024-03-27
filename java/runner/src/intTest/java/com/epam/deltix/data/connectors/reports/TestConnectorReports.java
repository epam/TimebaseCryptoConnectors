package com.epam.deltix.data.connectors.reports;

import com.epam.deltix.data.connectors.commons.json.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestConnectorReports {
    private final String connector;
    private final String stream;
    private final boolean disabled;

    private final Map<String, TestReport> reports = new LinkedHashMap<>();

    public TestConnectorReports(final JsonObject fromJsom) {
        this(fromJsom.getStringRequired("name"),
            fromJsom.getStringRequired("stream"),
            fromJsom.getBoolean("disabled"));
    }

    public TestConnectorReports(final String connector, final String stream, final boolean disabled) {
        this.connector = connector;
        this.stream = stream;
        this.disabled = disabled;
    }

    public void runTest(String name, Runnable r) {
        try {
            r.run();
            addTestOk(name);
        } catch (Throwable t) {
            addTestError(name, t);
            throw t;
        }
    }

    public void addTestOk(String name) {
        reports.put(name, new TestReport(name, TestStatus.OK, "Test " + name + " ok"));
    }

    public void addTestMessage(String name, String message) {
        reports.put(name, new TestReport(name, TestStatus.INFO, message));
    }

    public void addTestError(String name, Throwable error) {
        reports.put(name, new TestReport(name, "Test " + name + " failed", error));
    }

    public String connector() {
        return connector;
    }

    public String stream() {
        return stream;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public Map<String, TestReport> reports() {
        return reports;
    }

    @Override
    public String toString() {
        return "connector='" + connector + "', stream='" + stream + '\'';
    }
}
