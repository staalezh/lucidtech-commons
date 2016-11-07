package io.lucidtech.commons.async;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class FutureWrapper<T> implements Future<T> {
    private final T result;
    private boolean isCancelled = false;

    public FutureWrapper(final T result) {
        this.result = result;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        isCancelled = true;
        return true;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public T get() {
        return result;
    }

    @Override
    public T get(long timeout, TimeUnit unit) {
        return result;
    }
}
