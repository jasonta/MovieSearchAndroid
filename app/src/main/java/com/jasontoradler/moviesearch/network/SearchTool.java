package com.jasontoradler.moviesearch.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasontoradler.moviesearch.R;
import com.jasontoradler.moviesearch.data.MovieDetails;
import com.jasontoradler.moviesearch.data.SearchResults;
import com.jasontoradler.moviesearch.data.SearchItem;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Provides methods to queue asynchronous search requests of the catalog for keyword matches or
 * specific courses/specializations, and to parse the results or deliver an error message.
 * Also includes an ImageLoader with a built-in cache to aid in loading images in the background.
 */
public final class SearchTool {

    private static final String TAG = "SearchTool";
    private static SearchTool instance;

    private final RequestQueue requestQueue;
    private final ImageLoader imageLoader;
    private final List<SearchItem> searchItems = new ArrayList<>();
    private int totalItems;
    private int prevPage;
    private String prevKeyword;

    private SearchTool(final Context context) {
        requestQueue = Volley.newRequestQueue(context);

        imageLoader = new ImageLoader(requestQueue,
                new ImageLoader.ImageCache() {
                    private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(
                            (int) (Runtime.getRuntime().maxMemory() / 4)) {
                        @Override
                        protected int sizeOf(String key, Bitmap bitmap) {
                            return bitmap.getByteCount();
                        }
                    };

                    @Override
                    public Bitmap getBitmap(String url) {
                        return cache.get(url);
                    }

                    @Override
                    public void putBitmap(String url, Bitmap bitmap) {
                        cache.put(url, bitmap);
                    }
                });
    }

    public static SearchTool instance(final Context context) {
        if (instance == null) {
            instance = new SearchTool(context);
        }
        return instance;
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    public void clearResults() {
        searchItems.clear();
        totalItems = 0;
        prevKeyword = null;
        prevPage = 0;
    }

    public List<SearchItem> getSearchItems() {
        return searchItems;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void queueSearchByTitle(
            final Context context,
            final String keyword,
            int page,
            final TitleSearchListener titleSearchListener) {
        // only make a request for new data
        if (prevKeyword == null || (page != prevPage && TextUtils.equals(keyword, prevKeyword))) {
            prevPage = page;
            prevKeyword = keyword;
            String encodedKeyword = null;
            try {
                encodedKeyword = URLEncoder.encode(keyword, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "error encoding keyword: '" + keyword + "', " + e.getLocalizedMessage());
            }

            String url = context.getString(R.string.title_search_url, encodedKeyword, page);
            Log.v(TAG, "queueSearchByTitle: " + url);
            StringRequest request = new StringRequest(
                    Request.Method.GET,
                    url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, "queueSearchByTitle onResponse");
                            parseTitleSearchResults(response);
                            if (titleSearchListener != null) {
                                titleSearchListener.onSuccess();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, "queueSearchByTitle onErrorResponse: " + error);
                            if (titleSearchListener != null) {
                                titleSearchListener.onError(error);
                            }
                        }
                    });
            requestQueue.add(request);
        }
    }

    private void parseTitleSearchResults(final String response) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            SearchResults searchResults = mapper.readValue(response, SearchResults.class);
            if (searchResults != null) {
                if (searchResults.Response.equalsIgnoreCase("true")) {
                    Log.d(TAG, "total items: " + searchResults.totalResults);
                    totalItems = searchResults.totalResults;
                    Log.d(TAG, "adding " + searchResults.Search.size() + " items");
                    searchItems.addAll(searchResults.Search);
                    Log.d(TAG, "current total: " + searchItems.size());
                } else {
                    Log.d(TAG, "response was false: error=" + searchResults.Error);
                    searchItems.clear();
                    totalItems = 0;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "error parsing Title search Response: " + e);
        }
    }

    public void queueSearchById(
            final Context context,
            final String imdbId,
            final IdSearchListener idSearchListener) {
        String encodedKeyword = null;
        try {
            encodedKeyword = URLEncoder.encode(imdbId, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "error encoding imdbId: '" + imdbId + "', " + e.getLocalizedMessage());
        }

        String url = context.getString(R.string.id_search_url, encodedKeyword);
        Log.v(TAG, "queueSearchById: " + url);
        StringRequest request = new StringRequest(
                Request.Method.GET,
                url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "queueSearchById: onResponse");
                        MovieDetails movieDetails = parseIdSearchResults(response, imdbId);
                        if (idSearchListener != null) {
                            idSearchListener.onSuccess(movieDetails);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "queueSearchById: onErrorResponse " + error);
                        if (idSearchListener != null) {
                            idSearchListener.onError(error);
                        }
                    }
                });
        requestQueue.add(request);
    }

    private MovieDetails parseIdSearchResults(final String response, final String imdbId) {
        MovieDetails movieDetails = new MovieDetails();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode tree = mapper.readTree(response);
            final Iterator<Map.Entry<String, JsonNode>> fields = tree.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> entry = fields.next();
                if (entry.getValue().isTextual()) {
                    final String value = entry.getValue().asText();
                    if (entry.getKey().equalsIgnoreCase("response")) {
                        movieDetails.response = value;
                    } else if (entry.getKey().equalsIgnoreCase("poster")) {
                        movieDetails.poster = value;
                    } else  if (entry.getKey().equalsIgnoreCase("title")) {
                        movieDetails.title = value;
                    } else {
                        movieDetails.data.add(new Pair<>(entry.getKey(), value));
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "could not read IdSearchResults: " + e);
        }
        return movieDetails;
    }

    public interface TitleSearchListener {
        void onError(VolleyError error);

        void onSuccess();
    }

    public interface IdSearchListener {
        void onError(VolleyError error);

        void onSuccess(MovieDetails movieDetails);
    }
}
