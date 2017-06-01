package com.jasontoradler.moviesearch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;
import com.jasontoradler.moviesearch.data.SearchItem;
import com.jasontoradler.moviesearch.network.SearchTool;
import com.jasontoradler.moviesearch.ui.EndlessScrollListener;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Query the catalog based on a mKeyword passed in from {@link SearchActivity}. Display the list of
 * results, supporting pagination via an "endless" scroll listener.
 */
public class SearchResultsActivity extends Activity implements SearchTool.TitleSearchListener {

    public static final String EXTRA_KEYWORD = "mKeyword";

    private static final String TAG = "SearchResultsActivity";
    private static final String KEY_PAGE = "page";
    private static final String KEY_IS_INITIAL_SEARCH_DONE = "isInitialSearchDone";
    private static final String KEY_KEYWORD = "keyword";
    private static final int PAGE_SIZE = 10;
    private static final int VISIBLE_THRESHOLD = PAGE_SIZE * 3;

    private ProgressBar mProgressBar;
    private TextView mSearchResultsTitle;
    private RecyclerView mRecyclerView;
    private TextView mNoResultsText;
    private String mKeyword;
    private int mPage = 1;
    private boolean mIsInitialSearchDone;
    private ResultsAdapter mResultsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        mSearchResultsTitle = (TextView) findViewById(R.id.searchResultsTitle);
        mRecyclerView = (RecyclerView) findViewById(R.id.resultsListView);
        mNoResultsText = (TextView) findViewById(R.id.noResultsText);
        mProgressBar = (ProgressBar) findViewById(R.id.resultsProgressBar);

        if (savedInstanceState != null) {
            mPage = savedInstanceState.getInt(KEY_PAGE, 1);
            mIsInitialSearchDone = savedInstanceState.getBoolean(KEY_IS_INITIAL_SEARCH_DONE);
            mKeyword = savedInstanceState.getString(KEY_KEYWORD);
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this,
                layoutManager.getOrientation()));
        EndlessScrollListener scrollListener = new EndlessScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page) {
                loadMore(page);
            }
        };
        scrollListener.setVisibleThreshold(VISIBLE_THRESHOLD);
        scrollListener.setCurrentPage(mPage);
        mRecyclerView.addOnScrollListener(scrollListener);
        mResultsAdapter = new ResultsAdapter(this, new ResultsAdapterClickListener() {
            @Override
            public void onItemClick(int position) {
                SearchTool searchTool = SearchTool.instance(SearchResultsActivity.this);
                SearchItem searchItem = searchTool.getItem(position);
                if (searchItem != null) {
                    Intent intent = new Intent(SearchResultsActivity.this, DetailsActivity.class);
                    intent.putExtra(DetailsActivity.EXTRA_SEARCH_ITEM_ID, searchItem.imdbID);
                    startActivity(intent);
                }
            }

            @Override
            public void onHeartClick(int position) {
                Log.d(TAG, "onHeartClick: " + position);
                SearchTool searchTool = SearchTool.instance(SearchResultsActivity.this);
                SearchItem searchItem = searchTool.getItem(position);
                if (searchItem != null) {
                    searchItem.isFavorite ^= true;
                    Log.d(TAG, "set '" + searchItem.Title + "' to favorite: " + searchItem.isFavorite);
                    mResultsAdapter.notifyDataSetChanged();
                }
            }
        });
        mRecyclerView.setAdapter(mResultsAdapter);

        if (!mIsInitialSearchDone) {
            mIsInitialSearchDone = true;
            mPage = 1;
            mKeyword = getIntent().getExtras().getString(EXTRA_KEYWORD);
            if (!TextUtils.isEmpty(mKeyword)) {
                SearchTool.instance(this).queueSearchByTitle(this, mKeyword, mPage, this);
            } else {
                Log.e(TAG, "ERROR: missing keyword extra!");
                showNoResults();
            }
        } else {
            showResults();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_PAGE, mPage);
        outState.putBoolean(KEY_IS_INITIAL_SEARCH_DONE, mIsInitialSearchDone);
        outState.putString(KEY_KEYWORD, mKeyword);
    }

    private void showNoResults() {
        Log.d(TAG, "showNoResults");
        final SearchTool searchTool = SearchTool.instance(this);
        final List<SearchItem> searchItems = searchTool.getSearchItems();
        if (searchItems == null || searchItems.isEmpty()) {
            mProgressBar.setVisibility(View.INVISIBLE);
            mNoResultsText.setVisibility(View.VISIBLE);
        }
    }

    private void showResults() {
        final SearchTool searchTool = SearchTool.instance(this);
        final int totalItems = searchTool.getTotalItems();
        if (totalItems > 0) {
            Log.d(TAG, "showResults: totalItems=" + totalItems);
            mSearchResultsTitle.setText(getString(R.string.searchResultsTitle, mKeyword, totalItems));
            mProgressBar.setVisibility(View.INVISIBLE);
            mRecyclerView.setVisibility(View.VISIBLE);
            mNoResultsText.setVisibility(View.INVISIBLE);
            mResultsAdapter.notifyDataSetChanged();
        } else {
            showNoResults();
        }
    }

    private void loadMore(final int page) {
        Log.d(TAG, "loadMore: page=" + page);
        mPage = page;
        final SearchTool searchTool = SearchTool.instance(this);
        final int totalItems = searchTool.getTotalItems();
        if (!TextUtils.isEmpty(mKeyword) && (totalItems - (page * PAGE_SIZE) >= (1 - PAGE_SIZE))) {
            searchTool.queueSearchByTitle(SearchResultsActivity.this, mKeyword, page, this);
        }
    }

    @Override
    public void onError(VolleyError error) {
        Log.d(TAG, "volley error: " + error);
        showResults();
    }

    @Override
    public void onSuccess() {
        Log.d(TAG, "volley success");
        showResults();
    }

    /**
     * Listen for clicks on the list item and 'heart' (favorite)
     */
    private interface ResultsAdapterClickListener {
        void onItemClick(int position);

        void onHeartClick(int position);
    }

    private static class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.ViewHolder> {

        private final WeakReference<Context> mContext;
        private final ResultsAdapterClickListener mListener;

        ResultsAdapter(Context context, ResultsAdapterClickListener listener) {
            mContext = new WeakReference<>(context);
            mListener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.summary_list_item, parent, false);
            return new ViewHolder(view, mListener);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            SearchTool searchTool = SearchTool.instance(mContext.get());
            SearchItem item = searchTool.getItem(position);
            if (item != null) {
                holder.title.setText(item.Title);
                holder.position.setText(String.valueOf(position + 1));
                holder.year.setText(item.Year);
                holder.type.setText(item.Type);
                holder.image.setImageUrl(item.Poster, searchTool.getImageLoader());
                holder.heart.setImageResource(item.isFavorite ? R.mipmap.heart_full : R.mipmap.heart_empty);
            }
        }

        @Override
        public int getItemCount() {
            return SearchTool.instance(mContext.get()).getSearchItems().size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            final NetworkImageView image;
            final TextView position;
            final TextView title;
            final TextView year;
            final TextView type;
            final ImageView heart;
            final ResultsAdapterClickListener listener;

            ViewHolder(View itemView, ResultsAdapterClickListener listener) {
                super(itemView);
                this.listener = listener;
                itemView.setOnClickListener(this);
                image = (NetworkImageView) itemView.findViewById(R.id.moviePhoto);
                image.setDefaultImageResId(R.mipmap.noimageavailable);
                position = (TextView) itemView.findViewById(R.id.position);
                title = (TextView) itemView.findViewById(R.id.title);
                year = (TextView) itemView.findViewById(R.id.year);
                type = (TextView) itemView.findViewById(R.id.type);
                heart = (ImageView) itemView.findViewById(R.id.heartImage);
                heart.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                final int position = getAdapterPosition();
                if (listener != null) {
                    if (view == itemView) {
                        Log.v(TAG, "ViewHolder: clicked item " + position);
                        listener.onItemClick(position);
                    } else {
                        Log.v(TAG, "ViewHolder: clicked heart " + position);
                        listener.onHeartClick(position);
                    }
                }
            }
        }
    }
}
