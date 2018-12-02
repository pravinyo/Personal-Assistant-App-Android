package com.is_great.pro.personalassistantbot;

import android.Manifest;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.is_great.pro.personalassistantbot.databinding.ActivityWeatherBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import Utils.DataStructure.Forecast;
import Utils.Libs.WeatherLibs.ForecastAdapter;
import Utils.Libs.WeatherLibs.PABDateUtils;
import Utils.Libs.WeatherLibs.PABWeatherUtils;
import Utils.Libs.WeatherLibs.QueryUtils;
import Utils.Libs.WeatherLibs.WeatherLoader;

public class WeatherActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks,
        LocationListener, LoaderManager.LoaderCallbacks<List<Forecast>>{


    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 4455;
    private static final int WEATHER_LOADER_ID = 14;
    private static final String AppId = "your_id";
    private static final int FORECAST_LOADER_ID = 7;
    protected GoogleApiClient mGoogleApiClient;

    protected TextView City;
    protected Location mLastLocation;
    protected LocationRequest mLocationRequest;
    private TextView mEmptyStateTextView;

    private ProgressBar mLoader;
    private ForecastAdapter adapter;


    private static final String OPEN_WEATHER_MAP_URL_FORECAST =
            "http://api.openweathermap.org/data/2.5/forecast";
    private static final String OPEN_WEATHER_MAP_URL_WEATHER =
            "http://api.openweathermap.org/data/2.5/activity_weather";

    public static final String LOG_TAG = WeatherActivity.class.getName();
    private String mlat = "lat_value", mlon = "lon_value";//use gps coordinate
    Boolean dataIsNotLoaded=true;
    SharedPreferences sharedPreferences;
    private String mCity = "";

    private ActivityWeatherBinding mWeatherBinding;

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
        DataBindingUtil.setContentView(this,R.layout.activity_weather);

        City = (TextView) findViewById(R.id.city_label);

        if (ContextCompat.checkSelfPermission(WeatherActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(WeatherActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

        }
        sharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        buildGoogleApiClient();
        // wait intill all the data is collected
        floodBannerUI();
        floodUI();

    }

    private void floodBannerUI() {
        Uri baseUri;
        Uri.Builder uriBuilder;
        baseUri = Uri.parse(OPEN_WEATHER_MAP_URL_WEATHER);
        uriBuilder = baseUri.buildUpon();

        uriBuilder.appendQueryParameter("lat",mlat);
        uriBuilder.appendQueryParameter("lon",mlon);
        uriBuilder.appendQueryParameter("appid",AppId);

        new LoadFreshData().execute(uriBuilder.toString());
    }

    private synchronized void floodUI() {
        ListView weatherListView = (ListView) findViewById(R.id.list);
        mEmptyStateTextView = (TextView) findViewById(R.id.empty_view);
        weatherListView.setEmptyView(mEmptyStateTextView);
        mLoader = (ProgressBar) findViewById(R.id.loading_spinner);
        mLoader.showContextMenu();

        //create a new ArrayAdapter of Forecast
        adapter = new ForecastAdapter(this, new ArrayList<Forecast>());
        weatherListView.setAdapter(adapter);


        //check for network connectivity
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (isConnected) {
                LoaderManager loaderManager = getLoaderManager();
                Log.i(LOG_TAG, "initLoader is called");
                loaderManager.initLoader(FORECAST_LOADER_ID,null,this);

        } else {
            mLoader.setVisibility(View.GONE);
            mEmptyStateTextView.setText(R.string.NO_CONNECTION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the task you need to do.
                    Toast.makeText(this, "I got the location access ", Toast.LENGTH_SHORT).show();

                } else {

                    // permission denied, boo! Disable the functionality that depends on this permission.
                    Toast.makeText(this, "Sorry! I need Location Permission ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        floodBannerUI();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(5*60*1000);
        mLocationRequest.setFastestInterval(60*1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
          
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLastLocation != null){
            dataIsNotLoaded=false;
            //mLongitude.setText(String.valueOf(mLastLocation.getLongitude()));
            //mLatitude.setText(String.valueOf(mLastLocation.getLatitude()));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

        Log.v(LOG_TAG,"Suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Log.v(LOG_TAG,"Error  : "+connectionResult.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {


        Log.v(LOG_TAG,location.toString());
//        mLongitude.setText(String.valueOf(location.getLongitude()));
//        mLatitude.setText(String.valueOf(location.getLatitude()));

        // added code
        Toast.makeText(
                getBaseContext(),
                "Location changed: Lat: " + location.getLatitude() + " Lng: "
                        + location.getLongitude(), Toast.LENGTH_SHORT).show();

        /*------- To get city name from coordinates -------- */
        String cityName = null;
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = gcd.getFromLocation(location.getLatitude(),
                    location.getLongitude(), 1);
            if (addresses.size() > 0) {
                Log.i("probro1234",addresses+"");
                cityName = addresses.get(0).getLocality();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }


        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("long",location.getLongitude()+"");
        editor.putString("lat",location.getLatitude()+"");
        editor.putString("city",cityName);
        editor.commit();

        updateParameters();
    }

    private void updateParameters() {
        mlat = sharedPreferences.getString("long",""+mlat);
        mlon = sharedPreferences.getString("lat",mlon);
        mCity = sharedPreferences.getString("city",null);
    }


    @Override
    public Loader<List<Forecast>> onCreateLoader(int id, Bundle args) {
        Log.i(LOG_TAG,"n onCreateLoader method");
        Uri baseUri;
        Uri.Builder uriBuilder;
            baseUri = Uri.parse(OPEN_WEATHER_MAP_URL_FORECAST);
            uriBuilder = baseUri.buildUpon();

            uriBuilder.appendQueryParameter("lat",mlat);
            uriBuilder.appendQueryParameter("lon",mlon);
            uriBuilder.appendQueryParameter("appid",AppId);
            return new WeatherLoader(this,uriBuilder.toString());
    }

    @Override
    public void onLoadFinished(Loader<List<Forecast>> loader, List<Forecast> forecasts) {
        mLoader.setVisibility(View.GONE);
        Log.i(LOG_TAG,"In onLoadFinished method");
       // mWeatherBinding.primaryInfo.emptyView;
        mEmptyStateTextView.setText(R.string.NO_RESULT);
        //clear adapter for previous forecast data
        adapter.clear();

        if(forecasts != null && !forecasts.isEmpty()){
            City.setText("Current City : "+mCity);
            if(loader.getId() == FORECAST_LOADER_ID){
                adapter.addAll(forecasts);
            }
        }

    }

    private void updateTodayBannerUI(Forecast forecast) {

        Log.i(LOG_TAG,forecast.getWeather_DESC());
        TextView date = (TextView) findViewById(R.id.date);
        ImageView image_icon =(ImageView) findViewById(R.id.weather_icon);
        TextView desc =(TextView) findViewById(R.id.weather_description);
        TextView high_temp =(TextView) findViewById(R.id.high_temperature);
        TextView low_temp =(TextView) findViewById(R.id.low_temperature);

        int weatherId = forecast.getWeatherID();
        int weatherImageId = PABWeatherUtils
                .getLargeArtResourceIdForWeatherCondition(weatherId);
        image_icon.setImageResource(weatherImageId);
        date.setText(PABDateUtils.getDateCurrentTimeZone(forecast.getDt()));
        desc.setText(forecast.getWeather_DESC());
        high_temp.setText(PABWeatherUtils.formatTemperature(getBaseContext(),forecast.getMain_TEMP_MAX()));
        low_temp.setText(PABWeatherUtils.formatTemperature(getBaseContext(),forecast.getMain_TEMP_MIN()));
    }


    @Override
    public void onLoaderReset(Loader<List<Forecast>> loader) {
        Log.i(LOG_TAG,"In onLoadReset Method");
        adapter.clear();
    }

    private class LoadFreshData extends AsyncTask<String,Void,Forecast>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Forecast forecast) {
            super.onPostExecute(forecast);
            if(forecast != null) {
                updateTodayBannerUI(forecast);
            }else{
                Toast.makeText(getBaseContext(),"Week Network",Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected Forecast doInBackground(String... params) {
            String mURL = params[0];
            Log.i(LOG_TAG,"In loadInBackground method");
            if(mURL == null){
                return null;
            }
            return QueryUtils.fetchLiveForecastData(mURL);
        }
    }
}
