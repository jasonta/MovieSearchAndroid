package com.jasontoradler.moviesearch.ui;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

/**
 * Allow dynamically loading more data into a {@link RecyclerView} upon scrolling.
 */
public abstract class EndlessScrollListener extends RecyclerView.OnScrollListener {

    private int mVisibleThreshold = 10;
    private int mCurrentPage = 0;
    private int mPreviousTotalItemCount = 0;
    private boolean mLoading = true;
    private final RecyclerView.LayoutManager mLayoutManager;

    public EndlessScrollListener(LinearLayoutManager layoutManager) {
        mLayoutManager = layoutManager;
    }

    @Override
    public void onScrolled(RecyclerView view, int dx, int dy) {
        int totalItemCount = mLayoutManager.getItemCount();
        if (mLoading && (totalItemCount > mPreviousTotalItemCount)) {
            mLoading = false;
            mPreviousTotalItemCount = totalItemCount;
        }

        // if loading is finished and view is scrolled past threshold, increment the page and
        // kick off a new load
        int lastVisibleItemPosition = ((LinearLayoutManager) mLayoutManager).findLastVisibleItemPosition();
        if (!mLoading && (lastVisibleItemPosition + mVisibleThreshold) > totalItemCount) {
            mCurrentPage++;
            onLoadMore(mCurrentPage);
            mLoading = true;
        }
    }

    public void setCurrentPage(int page) {
        this.mCurrentPage = page;
    }

    /**
     * Set the minimum number of items to have below last visible item before triggering
     * {@link #onLoadMore(int)} to load more items.
     *
     * @param visibleThreshold number of items below last visible item before loading more
     */
    public void setVisibleThreshold(int visibleThreshold) {
        mVisibleThreshold = visibleThreshold;
    }

    protected void resetState() {
        mCurrentPage = 0;
        mPreviousTotalItemCount = 0;
        mLoading = true;
    }

    /**
     * Called when new data must be loaded into the {@link RecyclerView}.
     *
     * @param page page of data that needs to be loaded
     */
    protected abstract void onLoadMore(int page);
}
