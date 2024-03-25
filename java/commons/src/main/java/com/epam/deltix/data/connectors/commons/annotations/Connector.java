package com.epam.deltix.data.connectors.commons.annotations;

import java.lang.annotation.*;

/**
 * Indicates that an annotated class is a "Data Connector" implementation.
 * The implementation can be automatically discovered, instantiated and configured by Data Connectors Runner.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Connector {

    /**
     * Data Connector name.
     *
     * @return name of the Data Connector
     */
    String value() default "";

}
