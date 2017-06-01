package com.jasontoradler.moviesearch;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.NetworkImageView;
import com.jasontoradler.moviesearch.data.MovieDetails;
import com.jasontoradler.moviesearch.network.SearchTool;

import java.util.List;

/**
 * Display the Title, image, and description of a specific course or specialization.
 */
public class DetailsActivity extends Activity implements SearchTool.IdSearchListener {

    public static final String EXTRA_SEARCH_ITEM_ID = "searchItemId";

    private static final String TAG = "DetailsActivity";
    private RecyclerView detailsList;
    private NetworkImageView networkImageView;
    private TextView title;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        detailsList = (RecyclerView) findViewById(R.id.detailsList);
        networkImageView = (NetworkImageView) findViewById(R.id.detailsImage);
        title = (TextView) findViewById(R.id.title);
        progressBar = (ProgressBar) findViewById(R.id.progress);

        final String id = getIntent().getStringExtra(EXTRA_SEARCH_ITEM_ID);
        Log.d(TAG, "imdbId: " + id);
        if (!TextUtils.isEmpty(id)) {
            final SearchTool searchTool = SearchTool.instance(this);
            searchTool.queueSearchById(this, id, this);
        } else {
            Log.e(TAG, "missing extra: search id");
        }
    }

    @Override
    public void onError(VolleyError error) {
        Log.e(TAG, "onError: " + error);
    }

    @Override
    public void onSuccess(MovieDetails movieDetails) {
        if (movieDetails != null) {
            Log.d(TAG, "onSuccess: " + movieDetails);

            final SearchTool searchTool = SearchTool.instance(DetailsActivity.this);
            networkImageView.setImageUrl(movieDetails.poster, searchTool.getImageLoader());
            networkImageView.setDefaultImageResId(R.mipmap.noimageavailable);
            networkImageView.setVisibility(View.VISIBLE);
            title.setText(movieDetails.title);
            title.setVisibility(View.VISIBLE);

            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            detailsList.setLayoutManager(layoutManager);
            detailsList.addItemDecoration(new DividerItemDecoration(detailsList.getContext(),
                    layoutManager.getOrientation()));
            detailsList.setAdapter(new DetailsAdapter(movieDetails.data));
            detailsList.setVisibility(View.VISIBLE);

            progressBar.setVisibility(View.GONE);
        } else {
            Log.e(TAG, "onSuccess error: MovieDetails is null");
        }
    }

    /**
     * MovieDetails are bound and displayed in simple key-value (String-String) pair of TextViews.
     * Each list item alternates between a dark and light background. Also, URL values are made
     * clickable.
     */
    private static class DetailsAdapter extends RecyclerView.Adapter<DetailsAdapter.ViewHolder> {

        final List<Pair<String, String>> data;

        DetailsAdapter(List<Pair<String, String>> data) {
            this.data = data;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(
                    android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final Pair<String, String> entry = data.get(position);
            final Resources resources = holder.itemView.getResources();
            // alternate background color on each list item
            if (position % 2 == 0) {
                holder.itemView.setBackgroundColor(resources.getColor(R.color.darkItem));
            } else {
                holder.itemView.setBackgroundColor(resources.getColor(R.color.lightItem));
            }
            holder.key.setText(entry.first);
            holder.value.setText(entry.second);
        }

        @Override
        public int getItemCount() {
            return data != null ? data.size() : 0;
        }

        /**
         * Simple view holder to contain the key and value text fields.
         */
        static class ViewHolder extends RecyclerView.ViewHolder {

            final TextView key;
            final TextView value;

            ViewHolder(View itemView) {
                super(itemView);
                key = (TextView) itemView.findViewById(android.R.id.text1);
                key.setBackgroundResource(android.R.color.transparent);
                value = (TextView) itemView.findViewById(android.R.id.text2);
                value.setBackgroundResource(android.R.color.transparent);
                value.setAutoLinkMask(Linkify.WEB_URLS);
            }
        }
    }
}
