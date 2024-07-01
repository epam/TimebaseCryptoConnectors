package com.epam.deltix.data.connectors.runner.model;

public class DataConnectorDto {

    private String type;
    private String name;
    private String stream;
    private String status;

    private boolean disabled;

    public DataConnectorDto() {
    }

    public DataConnectorDto(String type, String name, String stream, String status, boolean disabled) {
        this.type = type;
        this.name = name;
        this.stream = stream;
        this.status = status;
        this.disabled = disabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(final String stream) {
        this.stream = stream;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
