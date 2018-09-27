package com.mumiyasss.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.LruCache;

public class PhotoCache extends LruCache<String, Bitmap> {
    public PhotoCache(int maxSize) {
        super(maxSize);
    }

    public Bitmap getBitmapFromMemory(String key) {
        return this.get(key);
    }

    public void setBitmapToMemory(String key, Bitmap bitmap) {
        if (getBitmapFromMemory(key) == null) {
            this.put(key, bitmap);
            Log.d("TEST_CACHE", key + " добавлен в кэш");
        }
    }
}
