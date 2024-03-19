package com.epam.deltix.data.connectors.reports;

public class TestReport {
    private final String name;
    private final TestStatus status;
    private final String message;
    private final Throwable error;

    public TestReport(String name, TestStatus status, String message) {
        this(name, status, message, null);
    }

    public TestReport(String name, String message, Throwable error) {
        this(name, TestStatus.FAILED, message, error);
    }

    public TestReport(String name, TestStatus status, String message, Throwable error) {
        this.name = name;
        this.status = status;
        this.message = message;
        this.error = error;
    }

    public String name() {
        return name;
    }

    public TestStatus status() {
        return status;
    }

    public String message() {
        return message;
    }

    public Throwable error() {
        return error;
    }
}
