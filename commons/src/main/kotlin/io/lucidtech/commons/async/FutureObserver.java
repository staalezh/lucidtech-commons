package io.lucidtech.commons.async;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Looper;

public abstract class FutureObserver extends ContentObserver {
    private final Uri uri;

    public FutureObserver(final Uri uri) {
        super(new android.os.Handler(Looper.getMainLooper()));
        this.uri = uri;
    }

    @Override
    final public void onChange(boolean selfChange) {
        onChange(selfChange, null);
    }

    @Override
    final public void onChange(boolean selfChange, final Uri changedUri) {
        if (uri.getPath().equals(changedUri.getPath())) {
            onChange(uri);
        }
    }

    abstract protected void onChange(final Uri uri);
}
