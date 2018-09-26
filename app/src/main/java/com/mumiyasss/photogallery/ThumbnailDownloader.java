package com.mumiyasss.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @param <T> Идентификатор загрузки
 */
public class ThumbnailDownloader<T> extends HandlerThread {
    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private boolean mHasQuit = false;

    private Handler mRequestHandler;
    private ConcurrentMap<T, String> mRequestMap = new ConcurrentHashMap<>();

    public ThumbnailDownloader() {
        super(TAG);
    }

    /**
     * Этот метод вызвается в run().
     */
    @Override
    protected void onLooperPrepared() {
        // В отдельном потоке
        mRequestHandler = new  Handler() {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == MESSAGE_DOWNLOAD) {
                    T target = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                    handleDownloadThumbnailRequest(target);
                }
            }
        };
    }

    /**
     * Выполняется в потоке ThumbnailDownloader, т.к.
     * Looper вызывает у Handler handleMessage(), откуда и вызывается
     * handleDownloadThumbnailRequest()
     *
     * @param target PhotoHolder
     */
    private void handleDownloadThumbnailRequest(final T target) {
        try {
            final String url = mRequestMap.get(target);
            if (url == null) {
                return;
            }
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory
                    .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        } }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    /**
     * Выполняется в MAIN потоке.
     * @param target PhotoHolder
     * @param url URL картинки, которая будет вставлена в PhotoHolder
     */
    public void queueThumbnail(T target, String url) {
        Log.i(TAG, "Got a URL: " + url);

        if (url == null) {
            mRequestMap.remove(target);
        } else {
            mRequestMap.put(target, url);
            // URL не передается в obtainMessage(), т.к.
            // URL сохраняется в mRequestMap, и является постоянно
            // актуальным, т.к. target это PhotoHolder,
            // который часто переиспользуется
            // ---------------------------------------------------
            // mRequestHandler далее передает сообщение в очередь
            // сообщений Лупперу, который выполняется в отдельном потоке.
            // Таким образом вызов
            // mRequestHandler.handleMessage() -> handleDownloadThumbnailRequest()
            // буде выполняться в отедельном потоке.
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD, target)
                    .sendToTarget();
        }
    }
}
