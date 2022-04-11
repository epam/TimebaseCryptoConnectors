package com.epam.deltix.data.connectors.commons;

import java.io.Closeable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Retrier<C extends Closeable> implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(Retrier.class.getName());

    private final RetriableFactory<C> factory;

    private final Thread retrier;

    private boolean started; // guarded by this
    private volatile boolean closed;

    public Retrier(final RetriableFactory<C> factory, final int retryPauseMillis) {
        this.factory = factory;

        retrier = new Thread(Retrier.class.getSimpleName()) {

            @Override
            public void run() {
                while (true) {
                    C retriable = null;

                    try {
                        final AtomicReference<Throwable> errorRef = new AtomicReference<>();
                        final CountDownLatch retryLatch = new CountDownLatch(1);

                        synchronized (Retrier.this) {
                            if (closed) {
                                break;
                            }

                            retriable = factory.create(error -> {
                                errorRef.set(error);
                                retryLatch.countDown();
                            });
                        }

                        retryLatch.await();

                        final Throwable error = errorRef.get();
                        if (error != null) {
                            throw error;
                        }
                    } catch (final Throwable t) {
                        if (closed) {
                            break;
                        }
                        LOG.log(Level.WARNING, "An error happened: " + t.getLocalizedMessage(), t);
                    } finally {
                        Util.closeQuiet(retriable);
                    }

                    try {
                        sleep(retryPauseMillis);
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
            }
        };
    }

    public void start() {
        synchronized (this) {
            if (closed) {
                throw new IllegalStateException("Closed");
            }
            if (started) {
                throw new IllegalStateException("Already started");
            }
            started = true;
        }

        retrier.start();
    }

    @Override
    public void close() {
        Thread toInterrupt = null;

        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;

            if (started) {
                toInterrupt = retrier;
            }
        }

        if (toInterrupt == null) {
            return;
        }

        toInterrupt.interrupt();

        if (Thread.currentThread() != toInterrupt) {
            try {
                toInterrupt.join();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
