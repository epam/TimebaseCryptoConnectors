package com.epam.deltix.data.connectors.runner.controllers;

import com.epam.deltix.data.connectors.runner.ConnectorsRunner;
import com.epam.deltix.data.connectors.runner.model.DataConnectorDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Controller
@RequestMapping("/api/v0")
public class DataConnectorsController {

    private final ConnectorsRunner runner;

    @Autowired
    public DataConnectorsController(ConnectorsRunner runner) {
        this.runner = runner;
    }

    @ResponseBody
    @RequestMapping(value = "/connectors", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<DataConnectorDto> getConnectors() {
        List<DataConnectorDto> connectors = new ArrayList<>();
        runner.forEachConnector((name, connector) -> {
            connectors.add(
                new DataConnectorDto(name, "UNKNOWN") // todo: get status
            );
        });

        return connectors;
    }
}
