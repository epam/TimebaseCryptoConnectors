package com.epam.deltix.data.connectors.runner;

import com.epam.deltix.data.connectors.Application;
import com.epam.deltix.data.connectors.commons.DataConnectorSettings;
import com.epam.deltix.data.connectors.commons.DataConnector;
import com.epam.deltix.data.connectors.commons.MdModel;
import com.epam.deltix.data.connectors.commons.MdModelEnum;
import com.epam.deltix.data.connectors.commons.Util;
import com.epam.deltix.data.connectors.commons.annotations.Connector;
import com.epam.deltix.data.connectors.commons.annotations.ConnectorSettings;
import com.epam.deltix.data.connectors.runner.settings.ConnectorsSettingsProvider;
import com.epam.deltix.data.connectors.runner.settings.RunnerSettings;
import com.epam.deltix.data.connectors.runner.settings.TimebaseSettings;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.reflections.Reflections;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.function.Consumer;

@Service
public class ConnectorsRunner {

    private static final Log LOGGER = LogFactory.getLog(ConnectorsRunner.class);

    private final RunnerSettings runnerSettings;
    private final TimebaseSettings timebaseSettings;
    private final ConnectorsSettingsProvider connectorsSettings;
    private final ObjectMapper objectMapper;

    private final Map<String, DataConnector<?>> connectors = new HashMap<>();

    public ConnectorsRunner(RunnerSettings runnerSettings,
                            TimebaseSettings timebaseSettings,
                            ConnectorsSettingsProvider connectorsSettings,
                            ObjectMapper objectMapper
    ) {
        this.runnerSettings = runnerSettings;
        this.timebaseSettings = timebaseSettings;
        this.connectorsSettings = connectorsSettings;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void discoverAndInstantiateDataConnectors() {
        Map<String, ConnectorImplementation<?, ?>> implementations = discoverImplementations(
            discoverSettings()
        );
        implementations.forEach((name, implementation) -> {
            LOGGER.info().append("Discovered Data Connector implementation: ").append(implementation).commit();
        });

        connectorsSettings.getConnectors().forEach((name, settings) -> {
            String connectorType = connectorsSettings.extractType(name);
            if (connectorType == null) {
                connectorType = name;
            }

            ConnectorImplementation<?, ?> implementation = implementations.get(connectorType.toLowerCase());
            if (implementation != null) {
                DataConnector<?> connector = instantiateDataConnector(implementation, settings);
                if (connectors.get(name) != null) {
                    throw new RuntimeException("Duplicate connector '" + name + "'");
                }

                connectors.put(name, connector);
                LOGGER.info().append("Connector '").append(name).append("' instantiated").commit();
            } else {
                LOGGER.error().append("Can't find implementation of connector '").append(name)
                    .append("' with type '").append(connectorType)
                    .append("'.").commit();
            }
        });
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startConnectors() {
        connectors.forEach((name, connector) -> {
            MdModel.Options model = buildModel(connector.model().select(), name);
            LOGGER.info().append("Connector '").append(name)
                .append("' subscribe model: ").append(model).commit();

            String[] instruments = buildInstruments(name);
            LOGGER.info().append("Connector '").append(name)
                .append("' subscribe instruments: ").append(Arrays.asList(instruments)).commit();

            connector.subscribe(model, instruments);
            LOGGER.info().append("Connector '").append(name).append("' started").commit();
        });
    }

    @PreDestroy
    public void stopConnectors() {
        connectors.forEach((name, connector) -> {
            try {
                connector.close();
            } catch (Throwable t) {
                LOGGER.error().append("Failed to close data connector '").append(name).append("'").commit();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, ConnectorImplementation<?, ?>> discoverImplementations(
        Map<String, Class<? extends DataConnectorSettings>> settingsImplementations
    ) {
        Map<String, ConnectorImplementation<?, ?>> implementations = new HashMap<>();
        discoverPackages().forEach(p -> discoverPackageSubtypes(p, DataConnector.class, cls -> {
            if (cls.isAnnotationPresent(Connector.class)) {
                Connector annotation = cls.getAnnotation(Connector.class);
                String name = annotation.value();
                if (name == null || name.isEmpty()) {
                    String className = cls.getSimpleName();
                    int index = className.lastIndexOf(DataConnector.class.getSimpleName());
                    if (index > 0) {
                        name = className.substring(0, index);
                    }
                }

                if (name == null || name.isEmpty()) {
                    LOGGER.warn().append("Found data connector implementation ").append(cls.getName())
                        .append(" with unknown data connector name. Please specify @Connector annotation value.")
                        .commit();
                } else {
                    Class<?> settingsClass = settingsImplementations.get(name.toLowerCase());
                    if (settingsClass == null) {
                        LOGGER.info().append("Can't find settings class for data connector ").append(cls.getName())
                            .append(". DataConnectorSettings class will be injected for the connector.").commit();
                        settingsClass = DataConnectorSettings.class;
                    }

                    implementations.put(
                        name.toLowerCase(), new ConnectorImplementation(cls, settingsClass)
                    );
                }
            }
        }));

        return implementations;
    }

    private Map<String, Class<? extends DataConnectorSettings>> discoverSettings() {
        Map<String, Class<? extends DataConnectorSettings>> settings = new HashMap<>();
        discoverPackages().forEach(p -> discoverPackageSubtypes(p, DataConnectorSettings.class, (cls) -> {
            if (cls.isAnnotationPresent(ConnectorSettings.class)) {
                ConnectorSettings annotation = cls.getAnnotation(ConnectorSettings.class);
                String name = annotation.value();
                if (name == null || name.isEmpty()) {
                    name = getNameFromClass(cls, DataConnectorSettings.class.getSimpleName());
                }

                if (name == null || name.isEmpty()) {
                    LOGGER.warn().append("Found settings implementation ").append(cls.getName())
                        .append(" with unknown data connector name. Please specify @ConnectorSettings annotation value.")
                        .commit();
                } else {
                    if (settings.get(name.toLowerCase()) != null) {
                        LOGGER.warn().append("Connector settings'").append(name)
                            .append("' discovered more then one times").commit();
                    }

                    settings.put(name.toLowerCase(), cls);

                    LOGGER.debug().append("Discovered settings class ").append(cls.getName())
                        .append(" for connector ").append(name).commit();
                }
            }
        }));

        return settings;
    }

    private List<String> discoverPackages() {
        List<String> packages = new ArrayList<>();
        packages.add(Application.class.getPackageName());
        if (runnerSettings.getDiscoverPackages() != null) {
            packages.addAll(runnerSettings.getDiscoverPackages());
        }

        return packages;
    }

    private <T> void discoverPackageSubtypes(String pack, Class<T> subType, Consumer<Class<? extends T>> processor) {
        new Reflections(pack).getSubTypesOf(subType).forEach(processor);
    }

    private String getNameFromClass(Class<?> cls, String suffix) {
        String className = cls.getSimpleName();
        int index = className.lastIndexOf(suffix);
        if (index > 0) {
            return className.substring(0, index);
        }

        return null;
    }

    private DataConnector<?> instantiateDataConnector(
        ConnectorImplementation<?, ?> implementation, Object settingsMap
    ) {
        try {
            return implementation.getConnectorClass()
                .getConstructor(implementation.getSettingsClass())
                .newInstance(
                    instantiateDataConnectorSettings(implementation, settingsMap)
                );
        } catch (Throwable t) {
            LOGGER.error().append("Failed to instantiate data connector ").append(implementation)
                .append(t).commit();
            throw new RuntimeException(t);
        }
    }

    private DataConnectorSettings instantiateDataConnectorSettings(
        ConnectorImplementation<?, ?> implementation, Object settingsMap
    ) {
        DataConnectorSettings connectorSettings = objectMapper.convertValue(
            settingsMap, implementation.getSettingsClass()
        );

        connectorSettings.setTbUrl(timebaseSettings.getUrl());
        connectorSettings.setTbUser(timebaseSettings.getUser());
        connectorSettings.setTbPassword(timebaseSettings.getPassword());

        return connectorSettings;
    }


    private MdModel.Options buildModel(MdModel.Selection modelSelection, String connector) {
        List<String> modelList = connectorsSettings.extractModel(connector);
        if (modelList != null) {
            for (String modelValue : modelList) {
                try {
                    MdModelEnum.with(modelSelection, modelValue);
                } catch (Throwable t) {
                    LOGGER.error().append("Invalid model value: ").append(modelValue)
                        .append(". Valid model values are: ").append(Arrays.asList(MdModelEnum.values()))
                        .commit();
                }
            }
        }

        return modelSelection;
    }

    private String[] buildInstruments(String connector) {
        String instruments = connectorsSettings.extractInstruments(connector);
        if (instruments != null) {
            return Util.splitInstruments(instruments);
        } else {
            return new String[0];
        }
    }
}
