package com.epam.deltix.data.connectors.runner.model;

public class DataConnectorDto {

    private String name;
    private String status;

    public DataConnectorDto() {
    }

    public DataConnectorDto(String name, String status) {
        this.name = name;
        this.status = status;
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
