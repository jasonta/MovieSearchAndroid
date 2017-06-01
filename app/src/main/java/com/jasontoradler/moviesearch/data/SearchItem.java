package com.jasontoradler.moviesearch.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jasontoradler.moviesearch.SearchResultsActivity;

/**
 * Data used in the each of the {@link SearchResultsActivity} list items.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchItem {
    public String Title;
    public String Year;
    public String imdbID;
    public String Type;
    public String Poster;

    public boolean isFavorite;
}
