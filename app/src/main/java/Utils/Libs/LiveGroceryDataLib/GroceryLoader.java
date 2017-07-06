package Utils.Libs.LiveGroceryDataLib;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import java.util.List;

import Utils.DataStructure.Grocery;

/**
 * Created by Pravinyo on 3/16/2017.
 */

public class GroceryLoader extends AsyncTaskLoader<List<Grocery>> {
    private static final String LOG_TAG = "GroceryLoader";
    private String mURL;

    public GroceryLoader(Context context, String url) {
        super(context);
        mURL = url;
    }
    @Override
    protected void onStartLoading() {
        Log.i(LOG_TAG,"In onstartLoading method");
        forceLoad();
    }

    @Override
    public List<Grocery> loadInBackground() {
        Log.i(LOG_TAG,"In loadInBackground method");
        if(mURL == null){
            return null;
        }
        List<Grocery> result = QueryUtils.fetchGroceryData(mURL);
        return result;
    }
}

