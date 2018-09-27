package com.mumiyasss.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.mumiyasss.photogallery.net.FlickrFetchr;

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
    private Handler mResponseHandler; // Handler c ссылкой на Looper главного потока

    // Передадим ответственность за обработку загруженного изображения другому классу
    // (в данном случае PhotoGalleryFragment)
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    /**
     * Интерфейс, который должен реализовать класс-обработчик изображения
     *
     * @param <T> Идентификатор в виде PhotoHolder
     */
    public interface ThumbnailDownloadListener<T> {
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        mThumbnailDownloadListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
    }

    // Todo: динамическое определение свободного места под кэш
    private PhotoCache photoCache = new PhotoCache(100);

    /**
     * Этот метод вызвается в run().
     */
    @Override
    protected void onLooperPrepared() {
        // Handler всегда ассоцируется с Looper текущего потока.
        // P.s. Затем этот экземпляр Handler можно передать другому потоку.
        // Переданный экземпляр Handler сохраняет связь с Looper потока-создателя.
        // Все сообщения, за которые отвечает Handler, будут обрабатываться
        // в очереди Looper связанного потока.
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
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

            final Bitmap bitmapToShow;
            final Bitmap cachedBitmap = photoCache.getBitmapFromMemory(url);
            if (cachedBitmap == null) {
                byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
                bitmapToShow = BitmapFactory
                        .decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
                photoCache.setBitmapToMemory(url, bitmapToShow);
                Log.i(TAG, "Bitmap created");
            } else {
                Log.d("TEST_CACHE", "Достаем из кэша");
                bitmapToShow = cachedBitmap;
            }

            // Runnable будет выполнен в том потоке,
            // к которому привязан данный Handler.
            mResponseHandler.post(new Runnable() {
                public void run() {
                    if (mRequestMap.get(target) != url ||
                            mHasQuit) {
                        return;
                    }
                    mRequestMap.remove(target);
                    mThumbnailDownloadListener.onThumbnailDownloaded(target,
                            bitmapToShow);
                }
            });
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }

    public void clearQueue() {
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
        mRequestMap.clear();
    }

    @Override
    public boolean quit() {
        mHasQuit = true;
        return super.quit();
    }

    /**
     * Выполняется в MAIN потоке.
     *
     * @param target PhotoHolder
     * @param url    URL картинки, которая будет вставлена в PhotoHolder
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
            // Handler всегда ассоцируется с Looper текущего потока.
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
