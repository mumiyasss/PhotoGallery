package com.mumiyasss.photogallery;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Класс для сохранения поисковых запросов
 * в SharedPreferences
 */
public class QueryPreferences {
    // Можно было бы использовать Context.getSharedPreferences(String,int)
    // Однако на практике часто важен не конкретный экземпляр,
    // а его совместное использование в пределах всего приложения.
    // В таких ситуациях лучше использовать метод PreferenceManager.
    // getDefaultSharedPreferences(Context), который возвращает экземпляр
    // с именем по умолчанию и закрытыми (private) разрешениями
    private static final String PREF_SEARCH_QUERY = "searchQuery";

    public static String getStoredQuery(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_SEARCH_QUERY, null);
    }

    public static void setStoredQuery(Context context, String query) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_SEARCH_QUERY, query)
                .apply();
    }
}