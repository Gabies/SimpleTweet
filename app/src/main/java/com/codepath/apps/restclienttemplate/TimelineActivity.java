package com.codepath.apps.restclienttemplate;

import android.os.Bundle;
import android.util.Log;

import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import okhttp3.Headers;

public class TimelineActivity extends AppCompatActivity {

    public static final String TAG = "TimelineActivity";

    TwitterClient client;
    RecyclerView rvTweets;
    List<Tweet> tweets;
    TweetsAdapter adapter;
    SwipeRefreshLayout swipeContainer;
    EndlessRecyclerViewScrollListener scrollListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        client = TwitterApp.getRestClient(this);

        swipeContainer = findViewById(R.id.swipeContainer);
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,    //config. the colors
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i(TAG, "fetching new data");
                populateHomeTimeline();
            }
        });


        rvTweets = findViewById(R.id.rvTweets);    //find the RV
        tweets = new ArrayList<>();
        adapter = new TweetsAdapter(this, tweets);   // init the list of tweets and adapter
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvTweets.setLayoutManager(layoutManager);   //setup layout manager
        rvTweets.setAdapter(adapter);

        Log.d(TAG, "check for print before");
        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                Log.i(TAG, "onLoadMore" + page);
                loadMoreData();   //gets the next set of tweet if it reaches the bottom
            }
        };

        Log.d(TAG, "check for print after");
        rvTweets.addOnScrollListener(scrollListener);
        populateHomeTimeline();
    }

    //other API call to get the next page of tweets and add the objects to our current list of tweets
    public void loadMoreData() {
        // 1. Send an API request to retrieve appropriate paginated data
        client.getNextPageOfTweets(new JsonHttpResponseHandler() {
                @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
//                Log.i(TAG, "onSuccessLoad more" + json.toString());
                // 2. Deserialize and construct new model objects from the API response
                JSONArray jsonArray = json.jsonArray;
                try {
                    List<Tweet> tweets = Tweet.fromJsonArray(jsonArray);
                    // 3. Append the new data objects to the existing set of items inside the array of items
                    // 4. Notify the adapter of the new items made with `notifyItemRangeInserted()
                    adapter.addAll(tweets);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailure for loadMoreData!", throwable);
            }
        }, tweets.get(tweets.size() -1 ).id);
    }

    private void populateHomeTimeline() {
        client.getHomeTimeline(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                JSONArray jsonArray = json.jsonArray;
                try {
                    adapter.clear();
                    adapter.addAll(Tweet.fromJsonArray(jsonArray));
                    swipeContainer.setRefreshing(false);   //indicate refreshing has finished
                } catch (JSONException e) {
                    Log.e(TAG, "Json excption", e);
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailure", throwable);
            }
        });
    }

}
