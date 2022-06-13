package com.epam.deltix.data.connectors.commons;

import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.spi.conn.DisconnectEventListener;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.schema.*;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import java.io.IOException;
import java.io.UncheckedIOException;

public class TbMessageOutputFactory implements CloseableMessageOutputFactory {
    private static final int GET_STREAM_RETRIES = 5;

    private final String tbUrl;
    private final String streamKey;
    private final RecordClassDescriptor[] types;
    private final Logger logger;

    public TbMessageOutputFactory(
            final String tbUrl,
            final String streamKey,
            final RecordClassDescriptor[] types,
            final Logger logger) {
        this.tbUrl = tbUrl;
        this.streamKey = streamKey;
        this.types = types;
        this.logger = logger;
    }

    public TbMessageOutputFactory(
            final String tbUrl,
            final String streamKey,
            final Class[] types,
            final Logger logger) {
        this.tbUrl = tbUrl;
        this.streamKey = streamKey;
        this.logger = logger;

        this.types = new RecordClassDescriptor[types.length];

        final Introspector introspector = Introspector.createEmptyMessageIntrospector();
        for (int i = 0; i < types.length; i++) {
            try {
                this.types[i] = introspector.introspectRecordClass(types[i]);
            } catch (Introspector.IntrospectionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public CloseableMessageOutput create() {
        TickDBClient tb = null;
        TickLoader<InstrumentMessage> loader = null;
        try {
            tb = (TickDBClient) TickDBFactory.createFromUrl(tbUrl); // unchecked
            tb.setTimeout(10_000);
            tb.addDisconnectEventListener(new DisconnectEventListener() {
                @Override
                public void onDisconnected() {
                    throw new RuntimeException("No more reconnections for this connection");
                }

                @Override
                public void onReconnected() {
                }
            });
            tb.open(false);

            final DXTickStream stream = getOrCreateStreamWithRetries(tb, GET_STREAM_RETRIES);

            final LoadingOptions options = new LoadingOptions(false);

            loader = stream.createLoader(options); // unchecked

            final TickDBClient finalTb = tb;
            final TickLoader<InstrumentMessage> finalLoader = loader;

            return new CloseableMessageOutput() {
                @Override
                public void send(final InstrumentMessage message) {
                    finalLoader.send(message);
                }

                @Override
                public void close() {
                    try {
                        finalLoader.flush();
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    } finally {
                        Util.closeQuiet(finalLoader);
                        Util.closeQuiet(finalTb);
                    }
                }
            };
        } catch (final Exception e) {
            Util.closeQuiet(loader);
            Util.closeQuiet(tb);
            throw e;
        }
    }

    private DXTickStream getOrCreateStreamWithRetries(final DXTickDB db, final int retriesCount) {
        for (int i = 0; i < retriesCount; ++i) {
            try {
                return getOrCreateStream(db);
            } catch (Throwable t) {
                logger.warning(() -> "Failed to get or create stream '" + streamKey + "'. Retrying...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        throw new RuntimeException("Failed to get or create stream'" + streamKey + "'. Number of retries exceeded.");
    }

    private DXTickStream getOrCreateStream(final DXTickDB tb) {
        final DXTickStream stream = tb.getStream(streamKey);
        if (stream != null) {
            checkAndUpdateSchema(stream);
            return stream;
        }

        logger.info(() -> "Cannot find the stream '" + streamKey + "'. Preparing a new one...");

        final StreamOptions options = new StreamOptions();
        options.name = streamKey;
        options.description = "Stream " + streamKey;
        options.setPolymorphic(types);

        return tb.createStream(streamKey, options);
    }

    private void checkAndUpdateSchema(DXTickStream stream) {
        RecordClassSet streamSchema = stream.getStreamOptions().getMetaData();
        RecordClassSet connectorSchema = addMissingTypes(types, streamSchema.getContentClasses());
        StreamMetaDataChange change = new SchemaAnalyzer(new SchemaMapping()).getChanges(
            streamSchema,
            MetaDataChange.ContentType.Polymorphic,
            connectorSchema,
            MetaDataChange.ContentType.Polymorphic
        );

        SchemaChange.Impact impact = getChangeImpact(change);
        if (impact == SchemaChange.Impact.DataConvert) {
            logger.info(() -> "Applying schema changes to stream " + stream.getKey());
            stream.execute(new SchemaChangeTask(change));
            BackgroundProcessInfo process;
            while ((process = stream.getBackgroundProcess()) != null && !process.isFinished()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
            logger.info(() -> "Schema changes to stream " + stream.getKey() + " successfully applied");
        } else if (impact == SchemaChange.Impact.DataLoss) {
            logger.warning(() -> "Stream " + stream.getKey() + " schema can not be converted without data loss. " +
                "In case of any problems, please, change stream schema manually or choose another stream for connector.");
        }
    }

    public SchemaChange.Impact getChangeImpact(StreamMetaDataChange schemaChange) {
        SchemaChange.Impact result = SchemaChange.Impact.None;
        for (ClassDescriptorChange change : schemaChange.changes) {
            SchemaChange.Impact imp = getChangeImpact(change, schemaChange.mapping);
            if (imp == SchemaChange.Impact.DataLoss) {
                return SchemaChange.Impact.DataLoss;
            } else if (imp == SchemaChange.Impact.DataConvert) {
                result = SchemaChange.Impact.DataConvert;
            }
        }

        return result;
    }

    public SchemaChange.Impact getChangeImpact(ClassDescriptorChange descriptorChange, SchemaMapping mapping) {
        if (descriptorChange.getSource() != null && descriptorChange.getTarget() != null) {
            // modify
            SchemaChange.Impact result = SchemaChange.Impact.None;
            for (AbstractFieldChange change : descriptorChange.getChanges()) {
                SchemaChange.Impact imp;
                if (change instanceof EnumFieldTypeChange) {
                    imp = ((EnumFieldTypeChange) change).getChangeImpact(mapping);
                } else {
                    imp = change.getChangeImpact();
                }

                if (imp == SchemaChange.Impact.DataLoss) {
                    return SchemaChange.Impact.DataLoss;
                } else if (imp == SchemaChange.Impact.DataConvert) {
                    result = SchemaChange.Impact.DataConvert;
                }
            }

            return result;
        } else if (descriptorChange.getTarget() != null) {
            return SchemaChange.Impact.DataConvert; // create
        } else {
            return SchemaChange.Impact.None; // delete
        }
    }

    private static RecordClassSet addMissingTypes(RecordClassDescriptor[] types, RecordClassDescriptor[] streamTypes) {
        RecordClassSet newTypes = new RecordClassSet(types);
        for (RecordClassDescriptor streamType : streamTypes) {
            boolean found = false;
            for (RecordClassDescriptor type : types) {
                if (type.getName().equalsIgnoreCase(streamType.getName())) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                newTypes.addContentClasses(streamType);
            }
        }

        return newTypes;
    }
}
