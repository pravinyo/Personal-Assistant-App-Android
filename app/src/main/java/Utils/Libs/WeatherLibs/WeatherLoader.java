package Utils.Libs.WeatherLibs;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import java.util.List;

import Utils.DataStructure.Forecast;

/**
 * Created by Pravinyo on 3/11/2017.
 */

public class WeatherLoader extends AsyncTaskLoader<List<Forecast>> {
    private final static String LOG_TAG=WeatherLoader.class.getName();
    private String mURL;
    public WeatherLoader(Context context,String url) {
        super(context);
        mURL=url;
    }

    @Override
    protected void onStartLoading() {
        Log.i(LOG_TAG,"In onstartLoading method");
        forceLoad();
    }

    @Override
    public List<Forecast> loadInBackground() {
        Log.i(LOG_TAG,"In loadInBackground method");
        if(mURL == null){
            return null;
        }
        List<Forecast> result= QueryUtils.fetchForecastData(mURL);
        return result;
    }
}
