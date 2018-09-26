package com.mumiyasss.photogallery;

import android.net.Uri;
import android.util.Log;

import com.mumiyasss.photogallery.model.GalleryItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {
    private static final String TAG = "FlickrFetchr";

    private static final String API_KEY = "db76b367786f49e9f12764f8f74f0947";
    /**
     * Создает новый объект URL по входному параметру строки.
     * Затем openConnection() создает объект подключения к
     * заданному URL адресу.
     *
     * @param urlSpec Url для создания подключения
     * @return ответ в виде массива байтов
     * @throws IOException если код ответа НЕ 200 OK
     */
    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);

        // openConnection() возвращает UrlConnection, однако так как
        // подключение происходит по HTTP, то мы можем преобразовать в
        // HttpUrlConnection.
        //
        // Это открывает доступ к HTTP-интерфейсам
        // для работы с методами запросов, кодами ответов,
        // методами потоковой передачи и т. д.
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

        // Объект HttpUrlConnection представляет подключение, но
        // связь с конечной точкой будет установлена только после
        // вызова getInputStream() (или getOutputStream() для POST-запросов)

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                        ": with " + urlSpec);
            }

            // После создания объекта URL и открытия подключения программа
            // многократно вызывает read(), пока в подключении не кончатся данные.
            // Объект InputStream предоставляет байты по мере их доступности.
            // Когда чтение будет завершено, программа закрывает его и выдает
            // массив байтов из ByteArrayOutputStream.
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchItems() {
        List<GalleryItem> items = new ArrayList<>();

        try {
            String url = Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    // Значение url_s приказывает Flickr включить
                    // URL-адрес для уменьшенной версии изображения, если оно доступно.
                    .appendQueryParameter("extras", "url_s")
                    .build().toString();
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items, jsonBody);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        } catch (JSONException je){
            Log.e(TAG, "Failed to parse JSON", je);
        }
        return items;
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody)
            throws IOException, JSONException {
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");
        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);
            GalleryItem item = new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));
            if (!photoJsonObject.has("url_s")) {
                continue;
            }
            item.setUrl(photoJsonObject.getString("url_s"));
            items.add(item);
        }
    }
}

