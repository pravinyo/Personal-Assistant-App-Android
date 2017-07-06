package com.is_great.pro.personalassistantbot;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import Utils.DataStructure.Grocery;
import Utils.Libs.LiveGroceryDataLib.GroceryAdapter;
import Utils.Libs.LiveGroceryDataLib.GroceryLoader;

/**
 * Created by Pravinyo on 3/16/2017.
 */

public class GroceryLivePrice extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<Grocery>> {
    private static final int GROCERY_LOADER_ID=2;
    private static final String LOG_TAG = "MainActivity";

    private ProgressBar mLoader;
    private GroceryAdapter adapter;
    private ListView groceryListView;
    private String search_term;

    private static final String DATA_GOV_REQUEST_URL=
            "https://data.gov.in/api/datastore/resource.json";
    private static final String resource_Id="9ef84268-d588-465a-a308-a864a43d0070";
    private static final String Api_key="api-key";
    private static final String key ="a6e173bd2fce2defb8a41acb54a4a76a";
    private static final String filter_type_commodity="filters[commodity]";
    private static final String EXTRA_CALL_USER_COMMAND = "price";

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
        setContentView(R.layout.activity_grocery);
        groceryListView = (ListView) findViewById(R.id.grocery_LV);

        mLoader = (ProgressBar) findViewById(R.id.progressbar);
        mLoader.showContextMenu();
        mLoader.setVisibility(View.GONE);

        Intent intent = getIntent();
        search_term=intent.getStringExtra(EXTRA_CALL_USER_COMMAND);
        if(search_term!=null){
            updateUI();
        }
    }

    private void updateUI() {
        mLoader.setVisibility(View.VISIBLE);
        adapter = new GroceryAdapter(this,new ArrayList<Grocery>());
        groceryListView.setAdapter(adapter);
        // check for network connectivity
        ConnectivityManager cm =(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if(isConnected){


            // Get a reference to the LoaderManager, in order to interact with loaders.
            LoaderManager loaderManager = getLoaderManager();

            // Initialize the loader. Pass in the int ID constant defined above and pass in null for
            // the bundle. Pass in this activity for the LoaderCallbacks parameter (which is valid
            // because this activity implements the LoaderCallbacks interface).
            Log.i(LOG_TAG,"initLoader is called");
            loaderManager.initLoader(GROCERY_LOADER_ID, null, this);
        }else{
            mLoader.setVisibility(View.GONE);
        }
        Toast.makeText(this,search_term,Toast.LENGTH_SHORT).show();
    }

    @Override
    public Loader<List<Grocery>> onCreateLoader(int id, Bundle args) {
        Log.i(LOG_TAG,"In onCreateLoader method");

        Uri baseUri = Uri.parse(DATA_GOV_REQUEST_URL);
        Uri.Builder uriBuilder = baseUri.buildUpon();
        uriBuilder.appendQueryParameter("resource_id",resource_Id);
        uriBuilder.appendQueryParameter(Api_key,key);
        uriBuilder.appendQueryParameter(filter_type_commodity,search_term);

        return new GroceryLoader(this,uriBuilder.toString());
    }

    @Override
    public void onLoadFinished(Loader<List<Grocery>> loader, List<Grocery> groceries) {

        mLoader.setVisibility(View.GONE);
        Log.i(LOG_TAG,"In LoadFinished Method");
        adapter.clear();

        if(groceries != null && ! groceries.isEmpty()){
            adapter.addAll(groceries);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Grocery>> loader) {

        Log.i(LOG_TAG,"in onLoaderReset Method");
        adapter.clear();
    }
}
