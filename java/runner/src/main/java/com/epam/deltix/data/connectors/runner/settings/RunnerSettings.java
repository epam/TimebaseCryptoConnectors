package com.epam.deltix.data.connectors.runner.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties("runner")
public class RunnerSettings {

    private List<String> discoverPackages;

    public List<String> getDiscoverPackages() {
        return discoverPackages;
    }

    public void setDiscoverPackages(List<String> discoverPackages) {
        this.discoverPackages = discoverPackages;
    }
}
