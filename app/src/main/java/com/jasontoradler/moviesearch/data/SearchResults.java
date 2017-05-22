package com.jasontoradler.moviesearch.data;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * Corresponds to the JSON schema of the Title search results.
 */
public final class SearchResults {

    public String Response;
    public int totalResults;
    @JsonIgnore
    public String Error;
    public List<SearchItem> Search;
}
