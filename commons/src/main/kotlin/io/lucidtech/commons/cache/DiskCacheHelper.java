package io.lucidtech.commons.cache;

import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;

public class DiskCacheHelper {
    public static <T> void put(DiskCache cache, String key, T item, Type type) {
        Gson gson = new Gson();
        String jsonList = gson.toJson(item, type);
        try {
            OutputStream os = cache.openStream(key);
            if (os == null) return;
            os.write(jsonList.getBytes());
            os.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static <T> T get(DiskCache cache, String key, Type type) {
        try {
            DiskCache.InputStreamEntry entry = cache.getInputStream(key);
            if (entry == null) {
                return null;
            }

            Gson gson = new Gson();
            InputStream is = entry.getInputStream();

            String data = "";

            byte[] buffer = new byte[512];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) > 0) {
                data += new String(buffer, 0, bytesRead);
            }

            T ret = gson.fromJson(data, type);
            is.close();
            return ret;
        } catch (IOException ioe) {
            Log.d("DiskCacheHelper", ioe.getMessage());
            return null;
        } catch (NullPointerException npe) {
            npe.printStackTrace();
            return null;
        }
    }
}
