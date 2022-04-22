package com.epam.deltix.data.connectors.runner.model;

public class DataConnectorDto {

    private String type;
    private String name;
    private String status;

    public DataConnectorDto() {
    }

    public DataConnectorDto(String type, String name, String status) {
        this.type = type;
        this.name = name;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
