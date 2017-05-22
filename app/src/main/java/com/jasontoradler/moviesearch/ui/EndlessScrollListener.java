package com.jasontoradler.moviesearch.ui;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * Allow dynamically loading more data into a {@link RecyclerView} upon scrolling.
 */
public abstract class EndlessScrollListener extends RecyclerView.OnScrollListener {

    private int visibleThreshold = 10;
    private int currentPage = 0;
    private int previousTotalItemCount = 0;
    private boolean loading = true;
    private final RecyclerView.LayoutManager layoutManager;

    public EndlessScrollListener(LinearLayoutManager layoutManager) {
        this.layoutManager = layoutManager;
    }

    @Override
    public void onScrolled(RecyclerView view, int dx, int dy) {
        int totalItemCount = layoutManager.getItemCount();
        if (loading && (totalItemCount > previousTotalItemCount)) {
            loading = false;
            previousTotalItemCount = totalItemCount;
        }

        // if loading is finished and view is scrolled past threshold, increment the page and
        // kick off a new load
        int lastVisibleItemPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
        if (!loading && (lastVisibleItemPosition + visibleThreshold) > totalItemCount) {
            currentPage++;
            onLoadMore(currentPage);
            loading = true;
        }
    }

    public void setCurrentPage(int page) {
        this.currentPage = page;
    }

    /**
     * Set the minimum number of items to have below last visible item before triggering
     * {@link #onLoadMore(int)} to load more items.
     *
     * @param visibleThreshold number of items below last visible item before loading more
     */
    public void setVisibleThreshold(int visibleThreshold) {
        this.visibleThreshold = visibleThreshold;
    }

    protected void resetState() {
        currentPage = 0;
        previousTotalItemCount = 0;
        loading = true;
    }

    /**
     * Called when new data must be loaded into the {@link RecyclerView}.
     *
     * @param page page of data that needs to be loaded
     */
    protected abstract void onLoadMore(int page);
}
