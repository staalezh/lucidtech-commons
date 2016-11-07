package io.lucidtech.commons.cache;

import android.util.Log;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DiskCache {
    private static String TAG = DiskCache.class.getSimpleName();

	private static final int VALUE_IDX = 0;
	private static final int VALUE_COUNT = 1;
	private int appVersion;
    private DiskLruCache diskLruCache;

    private DiskCache(int diskSize, File dir, int appVersion) throws IOException {
		this.appVersion = appVersion;
        diskLruCache = DiskLruCache.open(dir, appVersion, VALUE_COUNT, diskSize);
    }

    public static synchronized DiskCache open(int diskSize, File dir, int appVersion)
			throws IOException {
		return new DiskCache(diskSize, dir, appVersion);
	}

    public boolean exists(String key) {
        DiskLruCache.Snapshot snapshot = null;
        try {
            snapshot = diskLruCache.get(toInternalKey(key));
            if (snapshot == null) {
                return false;
            } else {
                return true;
            }

        } catch (IllegalStateException ise) {
            if (ise.getMessage().equals("cache is closed")) {
                return false;
            } else {
                throw new RuntimeException(ise);
            }
        }
        catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            if (snapshot != null)
                snapshot.close();
        }
    }

    public boolean remove(String key) {
        try {
            return diskLruCache.remove(toInternalKey(key));
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public void clear() throws IOException {
        File dir = diskLruCache.getDirectory();
        long maxSize = diskLruCache.getMaxSize();
        diskLruCache.delete();

        diskLruCache = DiskLruCache.open(dir, appVersion, VALUE_COUNT, maxSize);
    }

	public OutputStream openStream(String key) throws IOException {
        if (diskLruCache.isClosed()) {
            open();
        }

        if (!diskLruCache.isClosed()) {
            DiskLruCache.Editor editor = diskLruCache.edit(toInternalKey(key));
            if (editor != null) {
                try {
                    return new CacheOutputStream(editor.newOutputStream(VALUE_IDX), editor);
                } catch (IOException e) {
                    editor.abort();
                    return null;
                }
            }
        }

        return null;
	}

	private String toInternalKey(String key) {
		return md5(key);
	}

	private String md5(String s) {
		try {
			MessageDigest m = MessageDigest.getInstance("MD5");
			m.update(s.getBytes("UTF-8"));
			byte[] digest = m.digest();
			BigInteger bigInt = new BigInteger(1, digest);
			return bigInt.toString(16);
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError();
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError();
		}
	}

    private boolean open() {
        try {
            diskLruCache.open(diskLruCache.getDirectory(), appVersion, VALUE_COUNT, diskLruCache.getMaxSize());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public InputStreamEntry getInputStream(String key) throws IOException {
        if (diskLruCache.isClosed()) {
            open();
        }

        try {
            DiskLruCache.Snapshot snapshot = diskLruCache.get(toInternalKey(key));
            if (snapshot == null) return null;
            return new InputStreamEntry(snapshot);

        } catch (IllegalStateException ise) {
            Log.d(TAG, "Cache is closed: " + ise.getMessage());
            return null;
        }
    }

	public static class InputStreamEntry {
		private final DiskLruCache.Snapshot snapshot;

		public InputStreamEntry(DiskLruCache.Snapshot snapshot) {
			this.snapshot = snapshot;
		}

		public InputStream getInputStream() {
			return snapshot.getInputStream(VALUE_IDX);
		}
	}

	public class CacheOutputStream extends OutputStream {

		private final DiskLruCache.Editor editor;
		private boolean failed = false;
        private OutputStream os;

		private CacheOutputStream(OutputStream os, DiskLruCache.Editor editor) {
			this.editor = editor;
            this.os = os;
		}

		@Override
		public void close() {
			try {
				os.close();
			} catch (IOException e) {
                Log.d("DiskCache", e.getMessage());
			} finally {
                try {
                    if (failed) {
                        editor.abort();
                    } else {
                        editor.commit();
                    }
                } catch (IOException e) {
                    Log.d("DiskCache", e.getMessage());
                }
            }
		}

		@Override
		public void flush() throws IOException {
			try {
				os.flush();
			} catch (IOException e) {
				failed = true;
				throw e;
			}
		}


		@Override
		public void write(int oneByte) throws IOException {
			try {
                os.write(oneByte);
			} catch (IOException e) {
				failed = true;
				throw e;
			}
		}

		@Override
		public void write(byte[] buffer) throws IOException {
			try {
				os.write(buffer);
			} catch (IOException e) {
				failed = true;
				throw e;
			}
		}

		@Override
		public void write(byte[] buffer, int offset, int length) throws IOException {
			try {
				os.write(buffer, offset, length);
			} catch (IOException e) {
				failed = true;
				throw e;
			}
		}
	}
}
