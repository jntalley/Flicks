package me.jntalley.flicks;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import me.jntalley.flicks.models.Config;
import me.jntalley.flicks.models.Movie;


public class MovieListActivity extends AppCompatActivity {

    //constants
    //base URL for the API
    public final static String API_BASE_URL = "https://api.themoviedb.org/3";
    //parameter name for the API key
    public final static String API_KEY_PARAM = "api_key";
    //the API key
    public String API_KEY;
    //tag for logging this activity for errors
    public final static String TAG = "MovieListActivity";


    //instance fields
    AsyncHttpClient client;
    //list of currently playing movies
    ArrayList<Movie> movies;
    //track the adapter and the recycler view
    RecyclerView rvMovies;
    MovieAdapter adapter;

    //image config
    Config config;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_list);
        //initialize the client on create
        client = new AsyncHttpClient();
        //inititalize the list
        movies = new ArrayList<>();
        API_KEY = getResources().getString(R.string.API_KEY);
        //get configuration on creation
        getConfiguration();
        //initialize the adapter --- movies cannot be reinitialized after this point
        adapter = new MovieAdapter(movies);
        //resolve the recycler view
        rvMovies = (RecyclerView) findViewById(R.id.rvMovies);
        rvMovies.setLayoutManager(new LinearLayoutManager(this));
        rvMovies.setAdapter(adapter);

    }

    //network call for currently playing from API
    private void getNowPlaying() {
        //create the url
        String url =API_BASE_URL + "/movie/now_playing";
        //set request params
        RequestParams params = new RequestParams();
        params.put(API_KEY_PARAM, API_KEY); //API KEY ALWAYS REQUIRED
        //execute a GET request for a JASON object response
        client.get(url ,params, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                //load the result into the movies list
                try {
                    JSONArray results = response.getJSONArray("results");
                    //iterate through result and create movie objects
                    for (int i =0; i<results.length(); i++) {
                        Movie movie = new Movie(results.getJSONObject(i));
                        movies.add(movie);
                        //notify adapter when a movie is added
                        adapter.notifyItemInserted(movies.size()-1);

                    }
                    Log.i(TAG, String.format("Loaded %s movies", results.length()));

                } catch (JSONException e) {
                    logError("Failed to parse now playing movies", e, true);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                logError("Failed to get data from now playing endpoint", throwable, true);

            }
        });
    }

    //outside of onCreate within Movie list
    //get the configuration from the API
    private void getConfiguration() {
        //create the url
        String url =API_BASE_URL + "/configuration";
        //set request params
        RequestParams params = new RequestParams();
        params.put(API_KEY_PARAM, API_KEY); //API KEY ALWAYS REQUIRED
        //execute a GET request for a JASON object response
        client.get(url ,params, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    config = new Config(response);
                    Log.i(TAG, String.format("Loaded configuration with imageBaseUrl %s and posterSize %s", config.getImageBaseUrl(), config.getPosterSize()));
                    //pass config to adapter
                    adapter.setConfig(config);
                    //get the now playing movies
                    getNowPlaying();

                } catch (JSONException e) {
                    logError("Failed while parsing configuration", e,true);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                logError("Failed getting configuration", throwable, true);
            }
        });
    }

    //new helper method for handling errors /silent failures
    private void logError(String message, Throwable error, boolean alertUser){
        //always log the error
        Log.e(TAG,message,error);
        //alert the user to silent errors
        if (alertUser) {
            //show a lon toast with the error message
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }
}
