package com.is_great.pro.personalassistantbot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import Utils.DataStructure.News;
import Utils.Libs.NewsLibs.NewsAdapter;
import Utils.Libs.NewsLibs.QueryUtils;

/**
 * Created by Pravinyo on 3/28/2017.
 */

public class NewsActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";
    private EditText Search_Query;
    private Button Search_Button;
    private ProgressBar mLoader;
    private NewsAdapter adapter;
    private ListView newsesListView;
    private String search_term;

    private static final String DATA_NEWSAPI_BASE_URL =
            "https://newsapi.org/v1/articles";
    private static String resource_Id="";
    private static final String Api_key="apiKey";
    private static final String key ="your key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean isNightMode = preference.getBoolean(getString(R.string.settings_preference_key),false);
        if(isNightMode){
            setTheme(R.style.PAB);
        }else{
            setTheme(R.style.MainTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);
        newsesListView = (ListView) findViewById(R.id.News_LV);
        Search_Query=(EditText) findViewById(R.id.search_news);
        Search_Button=(Button) findViewById(R.id.search_button);
        mLoader = (ProgressBar) findViewById(R.id.progressbar);
        mLoader.showContextMenu();
        mLoader.setVisibility(View.GONE);
        adapter = new NewsAdapter(getApplicationContext(),new ArrayList<News>());
        newsesListView.setAdapter(adapter);
        newsesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String url = ((TextView)view.findViewById(R.id.url)).getText().toString();
                Toast.makeText(getApplicationContext(),url,Toast.LENGTH_SHORT).show();
                Intent startBrowser = new Intent(Intent.ACTION_VIEW);
                startBrowser.setData(Uri.parse(url));
                startActivity(startBrowser);
            }
        });
        Search_Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // do fetching of query
                search_term=Search_Query.getText().toString();
                updateUI();
            }
        });
    }

    private void updateUI() {

        // check for network connectivity
        Log.i(LOG_TAG,"In updateUI method");
        ConnectivityManager cm =(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if(isConnected){
            Log.i(LOG_TAG,"Connection is available");
            Uri baseUri = Uri.parse(DATA_NEWSAPI_BASE_URL);
            Uri.Builder uriBuilder = baseUri.buildUpon();
            if(TextUtils.isEmpty(search_term) || search_term == null){
                resource_Id="google-news";
            }else {
                resource_Id=search_term;
            }
            uriBuilder.appendQueryParameter("source",resource_Id);
            uriBuilder.appendQueryParameter(Api_key,key);
            Log.i(LOG_TAG,"fetching data from server");
            new FetchData().execute(uriBuilder.toString());
        }else{
            Log.i(LOG_TAG,"No connection");
            mLoader.setVisibility(View.GONE);
        }
        Toast.makeText(this,search_term,Toast.LENGTH_SHORT).show();
    }

    private class FetchData extends AsyncTask<String ,Void,List<News>> {

        @Override
        protected List<News> doInBackground(String... params) {
            String mURL = params[0];
            Log.i(LOG_TAG,"In doInBackground method");
            if(mURL == null){
                return null;
            }
            return QueryUtils.fetchNewsData(mURL);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(LOG_TAG,"In onPreExecute method");
            mLoader.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(List<News> newses) {
            super.onPostExecute(newses);
            Log.i(LOG_TAG,"In onPostExecute Method");
            mLoader.setVisibility(View.GONE);

            adapter.clear();

            if(newses != null && ! newses.isEmpty()){
                Log.i(LOG_TAG,"data fetch completed data decoded and adding to ui");
                adapter.addAll(newses);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Log.i(LOG_TAG,"in onCancelled Method");
            adapter.clear();
        }
    }
}
