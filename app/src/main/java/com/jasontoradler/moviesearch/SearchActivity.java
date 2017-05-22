package com.jasontoradler.moviesearch;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.jasontoradler.moviesearch.network.SearchTool;

/**
 * Launcher activity which allows user to enter a keyword to search the catalog and display any
 * results via {@link SearchResultsActivity}.
 */
public class SearchActivity extends Activity {

    private EditText searchBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchBox = (EditText) findViewById(R.id.searchBox);
        searchBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    search();
                    handled = true;
                }
                return handled;
            }
        });
    }

    private void search() {
        final String keyword = searchBox.getText().toString();
        if (!keyword.isEmpty()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);

            SearchTool.instance(this).clearResults();

            Intent intent = new Intent(this, SearchResultsActivity.class);
            intent.putExtra(SearchResultsActivity.EXTRA_KEYWORD, keyword);
            startActivity(intent);
        }
    }

    public void onClearButtonClick(View view) {
        searchBox.setText("");
        SearchTool.instance(this).clearResults();
    }

    public void onSearchButtonClick(View view) {
        search();
    }
}
