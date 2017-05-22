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
 * Query the catalog based on a keyword passed in from {@link SearchActivity}. Display the list of
 * results, supporting pagination via an "endless" scroll listener.
 */
public class SearchResultsActivity extends Activity implements SearchTool.TitleSearchListener {

    public static final String EXTRA_KEYWORD = "keyword";

    private static final String TAG = "SearchResultsActivity";
    private static final String KEY_PAGE = "page";
    private static final String KEY_IS_INITIAL_SEARCH_DONE = "isInitialSearchDone";
    private static final String KEY_KEYWORD = "keyword";
    private static final int PAGE_SIZE = 10;

    private ProgressBar progressBar;
    private TextView searchResultsTitle;
    private RecyclerView recyclerView;
    private TextView noResultsText;
    private String keyword;
    private int page = 1;
    private boolean isInitialSearchDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        searchResultsTitle = (TextView) findViewById(R.id.searchResultsTitle);
        recyclerView = (RecyclerView) findViewById(R.id.resultsListView);
        noResultsText = (TextView) findViewById(R.id.noResultsText);
        progressBar = (ProgressBar) findViewById(R.id.resultsProgressBar);

        if (savedInstanceState != null) {
            page = savedInstanceState.getInt(KEY_PAGE, 1);
            isInitialSearchDone = savedInstanceState.getBoolean(KEY_IS_INITIAL_SEARCH_DONE);
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation()));
        EndlessScrollListener scrollListener = new EndlessScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page) {
                loadMore(page);
            }
        };
        scrollListener.setVisibleThreshold(PAGE_SIZE * 2);
        scrollListener.setCurrentPage(page);
        recyclerView.addOnScrollListener(scrollListener);
        ResultsAdapter resultsAdapter = new ResultsAdapter(this, new AdapterItemClickListener() {
            @Override
            public void onClick(int position) {
                SearchTool queue = SearchTool.instance(SearchResultsActivity.this);
                final List<SearchItem> list = queue.getSearchItems();
                if (position < list.size()) {
                    final SearchItem searchItem = list.get(position);
                    Intent intent = new Intent(SearchResultsActivity.this, DetailsActivity.class);
                    intent.putExtra(DetailsActivity.EXTRA_SEARCH_ITEM_ID, searchItem.imdbID);
                    startActivity(intent);
                }
            }
        });
        recyclerView.setAdapter(resultsAdapter);

        if (!isInitialSearchDone) {
            isInitialSearchDone = true;
            page = 1;
            keyword = getIntent().getExtras().getString(EXTRA_KEYWORD);
            if (!TextUtils.isEmpty(keyword)) {
                SearchTool.instance(this).queueSearchByTitle(this, keyword, page, this);
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
        outState.putInt(KEY_PAGE, page);
        outState.putBoolean(KEY_IS_INITIAL_SEARCH_DONE, isInitialSearchDone);
        outState.putString(KEY_KEYWORD, keyword);
    }

    private void showNoResults() {
        final SearchTool searchTool = SearchTool.instance(this);
        final List<SearchItem> searchItems = searchTool.getSearchItems();
        if (searchItems == null || searchItems.isEmpty()) {
            progressBar.setVisibility(View.INVISIBLE);
            noResultsText.setVisibility(View.VISIBLE);
        }
    }

    private void showResults() {
        final SearchTool searchTool = SearchTool.instance(this);
        final int totalItems = searchTool.getTotalItems();
        if (totalItems > 0) {
            Log.d(TAG, "showResults: totalItems=" + totalItems);
            searchResultsTitle.setText(getString(R.string.searchResultsTitle, keyword, totalItems));
            progressBar.setVisibility(View.INVISIBLE);
            recyclerView.setVisibility(View.VISIBLE);
            noResultsText.setVisibility(View.INVISIBLE);
        } else {
            showNoResults();
        }
    }

    private void loadMore(final int page) {
        Log.d(TAG, "loadMore: page=" + page);
        this.page = page;
        final SearchTool searchTool = SearchTool.instance(this);
        final int totalItems = searchTool.getTotalItems();
        if (!TextUtils.isEmpty(keyword) && (totalItems - (page * PAGE_SIZE) >= -PAGE_SIZE)) {
            searchTool.queueSearchByTitle(SearchResultsActivity.this, keyword, page, this);
        }
    }

    @Override
    public void onError(VolleyError error) {
        Log.d(TAG, "volley error: " + error);
        showNoResults();
    }

    @Override
    public void onSuccess() {
        Log.d(TAG, "volley success");
        progressBar.setVisibility(View.INVISIBLE);
        showResults();
    }

    private interface AdapterItemClickListener {
        void onClick(int position);
    }

    private static class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.ViewHolder> {

        private final WeakReference<Context> context;
        private final ViewHolder.ViewHolderClickListener vhcListener;

        ResultsAdapter(Context context, final AdapterItemClickListener aicListener) {
            this.context = new WeakReference<>(context);
            vhcListener = new ViewHolder.ViewHolderClickListener() {
                @Override
                public void onClick(int position) {
                    if (aicListener != null) {
                        aicListener.onClick(position);
                    }
                }
            };
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.summary_list_item, parent, false);
            return new ViewHolder(view, vhcListener);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final SearchTool requestQueue = SearchTool.instance(context.get());
            final List<SearchItem> items = requestQueue.getSearchItems();
            SearchItem item = items.get(position);
            if (item != null) {
                holder.title.setText(item.Title);
                holder.position.setText(String.valueOf(position + 1));
                holder.year.setText(item.Year);
                holder.type.setText(item.Type);
                holder.image.setImageUrl(item.Poster, requestQueue.getImageLoader());
            }
        }

        @Override
        public int getItemCount() {
            return SearchTool.instance(context.get()).getSearchItems().size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            final NetworkImageView image;
            final TextView position;
            final TextView title;
            final TextView year;
            final TextView type;
            final ViewHolderClickListener listener;

            ViewHolder(View itemView, ViewHolderClickListener listener) {
                super(itemView);
                this.listener = listener;
                itemView.setOnClickListener(this);
                image = (NetworkImageView) itemView.findViewById(R.id.moviePhoto);
                image.setDefaultImageResId(R.mipmap.noimageavailable);
                position = (TextView) itemView.findViewById(R.id.position);
                title = (TextView) itemView.findViewById(R.id.title);
                year = (TextView) itemView.findViewById(R.id.year);
                type = (TextView) itemView.findViewById(R.id.type);
            }

            @Override
            public void onClick(View view) {
                Log.v(TAG, "ViewHolder.onClick: position = " + getAdapterPosition());
                if (listener != null) {
                    listener.onClick(getAdapterPosition());
                }
            }

            interface ViewHolderClickListener {
                void onClick(int position);
            }
        }
    }
}
