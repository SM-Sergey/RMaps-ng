package com.sm.maps.applib.utils;

import android.content.SearchRecentSuggestionsProvider;

public class SearchSuggestionsProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "com.sm.maps.SuggestionProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;
    public SearchSuggestionsProvider() {
        super();
        setupSuggestions(AUTHORITY, MODE);
    }
}
