package com.is_great.pro.personalassistantbot;

import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import org.apache.commons.codec.language.Soundex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import Utils.Algorithms.LevenshteinDistance;
import Utils.DataStructure.MyApp;
import Utils.Libs.ASR.ASR;
import Utils.Libs.ConversationalLib.QueryUtils;
import Utils.Libs.TTS.TTS;

public class MainActivity extends ASR {

    private static final String LOGTAG ="PAB_MAIN_ACTIVITY";
    private static final String EXTRA_CALL_USER_COMMAND = "price";
    private static final String RECEIVER_NAME_EMAILHANDLER_QUERY = "email_sending_PAB";
    private static final String LOG_TAG = "PAB_LOADING_BOT_REPLY";
    private static final String EXTRA_RECEIVER_NAME ="sms_send" ;
    private static final String EXTRA_RECEIVER_SMS_REQUEST ="sms_request" ;
    private TTS myTts;

    /*
     * The similarity between each app name and the input of the user is measures from 0 (totally disimilar)
     * to 1 (identical). This threshold is used to determine which apps are considered (the ones which names
     * are similar with a value higher than the threshold).
     */

    //Enumeration with the 2 possibilities for calculating similarity
    private enum SimilarityAlgorithm{ORTHOGRAPHIC, PHONETIC};

    //Default values
    private static SimilarityAlgorithm DEFAULT_ALGORITHM = SimilarityAlgorithm.ORTHOGRAPHIC;
    private static float DEFAULT_THRESHOLD = 0; //From 0 to 1

    //Declaration of attributes
    private float similarityThreshold = DEFAULT_THRESHOLD;
    private SimilarityAlgorithm similarityCalculation = DEFAULT_ALGORITHM;

    // BOT
    private String Bot_reply;
    private static int Bot_ID=12;
    private static final String FORMAT = "json";
    private static final String BASE_URI_BOT="http://api.program-o.com/v2/chatbot/";

    //firebase
    public static final int RC_SIGN_IN = 1;
    private String mUsername;
    public static final String ANONYMOUS = "anonymous";
    private FirebaseAuth mFirebaseAuth;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth.AuthStateListener mAuthStateListener;


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
        setContentView(R.layout.activity_main);
        mUsername = ANONYMOUS;

        //Initialize Firebase Components
        mFirebaseAuth = FirebaseAuth.getInstance();

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    onSignedInInitialize(user.getDisplayName());
                } else {
                    // User is signed out
                    onSignedOutCleanup();

                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setProviders(Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                            new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };

        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
        }


        setSpeakButton();
        //Initialize the speech recognizer
        createRecognizer(getApplicationContext());

        //Initialize text to speech
        myTts = TTS.getInstance(this);
    }
    @Override
    public void onRestart()
    {
        super.onRestart();
        finish();
        startActivity(getIntent());
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            case R.id.settings_menu:
                intent = new Intent(MainActivity.this,Settings.class);
                startActivity(intent);
                return true;
            case R.id.about_menu:
                intent = new Intent(MainActivity.this,About.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onSignedInInitialize(String username) {
        mUsername = username;
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanup() {
        mUsername = ANONYMOUS;
        detachDatabaseReadListener();
    }

    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };

        }
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mChildEventListener = null;
        }
    }



    private void setSpeakButton() {
        Button speak = (Button) findViewById(R.id.btn_speech);
        speak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if("generic".equals(Build.BRAND.toLowerCase(Locale.US))){
                    Toast toast = Toast.makeText(getApplicationContext(),"ASR is not supported on virtual devices", Toast.LENGTH_SHORT);
                    toast.show();
                    Log.e(LOGTAG, "ASR attempt on virtual device");
                }
                else {
                    startListening();
                }
            }
        });
    }

    private void startListening() {
        if(deviceConnectedToInternet()){
            try {
                //Show a feedback to the user indicating that the app has started to listen
                indicateListening();

				/*Start listening, with the following default parameters:
					* Recognition model = Free form,
					* Number of results = 1 (we will use the best result to perform the search)
					*/
                listen(RecognizerIntent.LANGUAGE_MODEL_FREE_FORM, 1); //Start listening
            } catch (Exception e) {
                Toast toast = Toast.makeText(getApplicationContext(),"ASR could not be started: invalid params", Toast.LENGTH_SHORT);
                toast.show();
                Log.e(LOGTAG, e.getMessage());
            }
        } else {
            Toast toast = Toast.makeText(getApplicationContext(),"Please check your Internet connection", Toast.LENGTH_SHORT);
            toast.show();
            Log.e(LOGTAG, "Device not connected to Internet");
        }
    }

    /**
     * Provide visual feedback to the user
     */
    private void indicateListening() {

        Button button =(Button) findViewById(R.id.btn_speech);
        TextView response = (TextView) findViewById(R.id.tv_response);
        button.setBackgroundColor(ContextCompat.getColor(this, R.color.speechbtn_listening)
        );
        response.setText(R.string.started_listening);

        myTts.speak(getResources().getString(R.string.initial_prompt));
    }

    @Override
    public void processAsrReadyForSpeech() { }

    /**
     * Initiates a Google search intent with the results of the recognition
     */
    @Override
    public void processAsrResults(ArrayList<String> nBestList, float[] nBestConfidences) {

        Button talk = (Button) findViewById(R.id.btn_speech);
        TextView response = (TextView) findViewById(R.id.tv_response);
        response.setText(R.string.default_response);
        talk.setBackgroundColor(getResources().getColor(R.color.default_color));
        if(nBestList!=null){
            if(nBestList.size()>0){
                String bestResult = nBestList.get(0); //We will use the best result

                if(!containsAppLaunch(bestResult)){
                    if(containsWeatherDataRequest(bestResult)){

                        Intent intent = new Intent(MainActivity.this,WeatherActivity.class);
                        startActivity(intent);
                    }else if(containsEventReminder(bestResult)){
                        Intent intent =  new Intent(MainActivity.this,ReminderEvent.class);
                        startActivity(intent);

                    }else if(containsCallCommand(bestResult)){
                        Intent intent = new Intent(MainActivity.this,callActivity.class);
                        startActivity(intent);
                    }else if(containsGroceryPrice(bestResult)){
                        Intent intent = new Intent(MainActivity.this,GroceryLivePrice.class);
                        String[] data = bestResult.split("\\s+");

                        intent.putExtra(EXTRA_CALL_USER_COMMAND,data[data.length-1]);
                        startActivity(intent);

                    }else if(containsEmailComposeActivity(bestResult)){
                        String[] data =bestResult.split("\\s+");
                        Intent intent = new Intent(MainActivity.this,EmailHandler.class);
                        intent.putExtra(RECEIVER_NAME_EMAILHANDLER_QUERY,data[data.length-1]);
                        startActivity(intent);
                    }
                    else if(containsSearchQuery(bestResult)) {
                        indicateSearch(bestResult); //Provides feedback to the user that search is going to be started
                        googleText(bestResult);
                    }else if(containSMSRequest(bestResult)){
                        Intent intent = new Intent(MainActivity.this,callActivity.class);
                        intent.putExtra(EXTRA_RECEIVER_SMS_REQUEST,"YES");
                        startActivity(intent);
                    }else if(containsNewsLaunch(bestResult)){
                        Intent intent =new Intent(MainActivity.this,NewsActivity.class);
                        startActivity(intent);
                    }
                    else{
                        talkWithBOT(bestResult);
                    }
                }
                else{
                    //Obtains the apps installed in the device sorted from most to least similar name regarding the user input
                    //String[] = [0] = name, [1] = package, [2] = similarity
                    ArrayList<MyApp> sortedApps = getSimilarAppsSorted(bestResult);

                    //Shows the matching apps and their similarity values in a list
                    showMatchingNames(sortedApps);

                    //Launches the best matching app (if there is any)
                    if(sortedApps.size()<=0)
                    {
                        Toast toast = Toast.makeText(getApplicationContext(),"No app found with sufficiently similar name", Toast.LENGTH_SHORT);
                        toast.show();
                        Log.e(LOGTAG, "No app has a name with similarity > "+similarityThreshold);
                    }
                    else
                        launchApp(sortedApps.get(0));

                }
            }
        }
        setSpeakButton();
    }




    /**
     * Here is a list of pattern recognition function that matches pattern and according return boolean value
     * @param bestResult
     * @return Boolean
     */

    private boolean containsSearchQuery(String bestResult) {
        bestResult = bestResult.toLowerCase();
        if(bestResult.contains("search for") || bestResult.contains("google") ||
                bestResult.contains("find")){
            return true;
        }else{
            return false;
        }
    }
    private boolean containsNewsLaunch(String bestResult) {
        bestResult = bestResult.toLowerCase();
        if(bestResult.contains("show") && bestResult.contains("news") ||
                bestResult.contains("today")&&bestResult.contains("news")){
            return true;
        }else{
            return false;
        }
    }

    private boolean containSMSRequest(String bestResult) {
        bestResult = bestResult.toLowerCase();
        if(bestResult.contains("send sms") || bestResult.contains("write message") ||
                bestResult.contains("send message")){
            return true;
        }else{
            return false;
        }
    }

    private boolean containsEmailComposeActivity(String bestResult) {
        bestResult = bestResult.toLowerCase();
        if(bestResult.contains("compose") && bestResult.contains("email to") ||
                bestResult.contains("send") && bestResult.contains("email to")){
            return true;
        }else{
            return false;
        }
    }

    private boolean containsGroceryPrice(String bestResult) {
        bestResult = bestResult.toLowerCase();
        if(bestResult.contains("tell me") && bestResult.contains("price of") ||
                bestResult.contains("show") && bestResult.contains("price of")){
            return true;
        }else{
            return false;
        }
    }

    private boolean containsCallCommand(String bestResult) {
        bestResult = bestResult.toLowerCase();
        if(bestResult.contains("call to") || bestResult.contains("make a call") ||
                bestResult.contains("dial to") || bestResult.contains("connect to")){
            return true;
        }else{
            return false;
        }
    }

    private boolean containsEventReminder(String bestResult) {
        bestResult = bestResult.toLowerCase();
        if(bestResult.contains("set") && bestResult.contains("reminder") ||
                bestResult.contains("set") && bestResult.contains("appointment")){
            return true;
        }else{
            return false;
        }

    }

    private boolean containsWeatherDataRequest(String bestResult) {
        bestResult=bestResult.toLowerCase();
        if(bestResult.contains("show") && bestResult.contains("weather")){
            return true;
        }else{
            return false;
        }
    }

    private void showMatchingNames(ArrayList<MyApp> sortedApps) {
        for(MyApp app: sortedApps){
            Log.i(LOGTAG,app.getName()+" (Similarity: "+String.format("%.2f", app.getSimilarity())+")");
        }

    }

    private boolean containsAppLaunch(String bestResult) {
        bestResult=bestResult.toLowerCase();
        if(bestResult.contains("open") || bestResult.contains("launch")){
            return true;
        }else{
            return false;
        }
    }


    /***********************************************************************************************
     * BOT Voice code Implementation here
     * It does background data processing in another thread
     * reply of bot is stored in Bot_reply variable
     **********************************************************************************************
     * @param bestResult*/
    private void talkWithBOT(String bestResult) {

        // check for network connectivity
        ConnectivityManager cm =(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if(isConnected){
            AsyncBackgroundRunner requestReply = new AsyncBackgroundRunner();
            Uri baseUri = Uri.parse(BASE_URI_BOT);
            Uri.Builder uriBuilder = baseUri.buildUpon();
            uriBuilder.appendQueryParameter("bot_id",Bot_ID+"");
            uriBuilder.appendQueryParameter("say",bestResult);
            uriBuilder.appendQueryParameter("convo_id",mUsername);
            uriBuilder.appendQueryParameter("format",FORMAT);
            requestReply.execute(uriBuilder.toString());
        }else{
            Bot_reply=getResources().getString(R.string.NETWORK_ISSUE);
            myTts.speak(Bot_reply);
        }
    }
    private class AsyncBackgroundRunner extends AsyncTask<String,Void,String> {


        private String mUrl;
        @Override
        protected String doInBackground(String... url) {
            mUrl=url[0];
            if(mUrl == null){
                return null;
            }
            String result = QueryUtils.fetchBotReplyData(mUrl);
            Log.i(LOG_TAG,"Bot say"+result);
            return result;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(LOG_TAG,"In onCreateLoader method");
            Bot_reply="Thinking ...";
            myTts.speak(Bot_reply);
        }

        @Override
        protected void onPostExecute(String BotReply) {
            super.onPostExecute(BotReply);
            if(!TextUtils.isEmpty(BotReply) || BotReply != null){
                Bot_reply=BotReply;
            }else{
                Bot_reply="I didn't get it";
            }
            myTts.speak(Bot_reply);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            Bot_reply=getResources().getString(R.string.dummy_text);
        }
    }

    /*************************************************************************************************************************************
     * Text-processing methods to compare name of apps
     *************************************************************************************************************************************/

    /**
     * Obtains a collection with information about the apps which name is similar to what was recognized from the user. The collection is sorted
     * from most to least similar.
     * @param recogizedName Name of the app recognized from the user input
     * @return A collection of instances of MyApp. MyApp is an auxiliary class that we have created (see at the bottom of this file), to store
     * information about the apps retreived, concretely: name, package name and similarity to recognized name. If no apps are found, it returns
     * an empty list.
     */
    private ArrayList<MyApp> getSimilarAppsSorted(String recogizedName)
    {
        MyApp app;
        Double similarity=0.0;

        ArrayList<MyApp> similarApps = new ArrayList<MyApp>();

        PackageManager packageManager = getPackageManager();
        List<PackageInfo> apps = getPackageManager().getInstalledPackages(0);

        //For all apps installed in the device...
        for (int i=0; i<apps.size(); i++) {
            PackageInfo packInfo = apps.get(i);

            //Gets application name
            String name = packInfo.applicationInfo.loadLabel(packageManager).toString();

            //Gets package name
            String packageName = packInfo.packageName;

            //Measures similarity of the app's name with the user input
            switch(similarityCalculation){
                case ORTHOGRAPHIC:
                    similarity = compareOrthographic(normalize(recogizedName), normalize(name));
                    break;

                case PHONETIC:
                    similarity = comparePhonetic(normalize(recogizedName), normalize(name));
                    break;

                default:
                    similarity = compareOrthographic(normalize(recogizedName), normalize(name));
                    break;
            }

            //Adds the app to the collection if the similarity is higher than the threshold
            if(similarity > similarityThreshold) {
                app = new MyApp(name, packageName, similarity);
                similarApps.add(app);
            }
        }

        //Sorts apps from least to most similar, in order to do this, we use our own comparator,
        //using the "AppComparator" class, which is defined as a private class at the end of the file
        Collections.sort(similarApps, new AppComparator());

        for(MyApp aux: similarApps)
            Log.i(LOGTAG, "Similarity: "+aux.getSimilarity()+", Name: "+aux.getName()+", Package: "+aux.getPackageName());

        return similarApps;
    }


    /**
     * Normalizes a text
     * @param text
     * @return the input text without spaces and in lower case
     */
    private String normalize(String text){
        return text.trim().toLowerCase(Locale.US);
    }


    /**
     * Compares the names using the Levenshtein distance, which is the minimal number of characters you have to replace,
     * insert or delete to transform string a into string b.
     * We have used a computation of this distance provided by Wikipedia.
     * @return similarity from 0 (minimum) to 1 (maximum)
     */
    private double compareOrthographic(String a, String b){
        return LevenshteinDistance.computeLevenshteinDistance(a, b);
    }

    /**
     * Compares the names using their phonetic similarity, using the soundex algorithm.
     * We have used an implementation of this algorithm provided by Apache.
     * Attention: it only works for English
     */
    private double comparePhonetic(String recognizedApp, String nameApp){
        Soundex soundex = new Soundex();

        //Returns the number of characters in the two encoded Strings that are the same.
        //This return value ranges from 0 to the length of the shortest encoded String: 0 indicates little or no similarity,
        //and 4 out of 4 (for example) indicates strong similarity or identical values.
        double sim=0;
        try {
            sim = soundex.difference(recognizedApp, nameApp);
        } catch (Exception e) {
            Log.e(LOGTAG, "Error during soundex encoding. Similarity forced to 0");
            sim = 0;
        }
        return sim/4;
    }

    /**
     * Comparator for apps considering the similarity of their names to the recognized input.
     */
    private class AppComparator implements Comparator<MyApp> {

        @Override
        public int compare(MyApp app1, MyApp app2) {
            return (- Double.compare(app1.getSimilarity(), app2.getSimilarity()));
            // Multiply by -1 to get reverse ordering (from most to least similar)
        }
    }


    /**
     * Launches the app indicated.
     * @param app see the MyApp class defined at the end of this file
     */
    private void launchApp(MyApp app) {
        Intent launchApp = this.getPackageManager().getLaunchIntentForPackage(app.getPackageName()); //Launch by package name
        if (null != launchApp) {
            try {
                indicateLaunch(app.getName());
                Log.i(LOGTAG, "Launching "+app.getName());
                startActivity(launchApp);
                //VoiceLaunch.this.finish();
            } catch (Exception e) {
                Toast.makeText(getBaseContext(), app.getName()+" could not be launched", Toast.LENGTH_LONG).show(); //Show user-friendly name
                Log.e(LOGTAG, app.getName()+" could not be launched");
            }
        }
    }
    /**
     * Provides feedback to the user to show that the app is performing a search:
     * 		* It changes the color and the message of the speech button
     *      * It synthesizes a voice message
     */
    private void indicateLaunch(String appName) {
        changeButtonAppearanceToDefault();
        myTts.speak("Launching "+appName);
        Toast.makeText(getBaseContext(), "Launching "+appName, Toast.LENGTH_LONG).show(); //Show user-friendly name
    }

    /**
     * Provides feedback to the user (by means of a Toast and a synthesized message) when the ASR encounters an error
     */
    @Override
    public void processAsrError(int errorCode) {

        changeButtonAppearanceToDefault();

        String errorMessage;
        switch (errorCode)
        {
            case SpeechRecognizer.ERROR_AUDIO:
                errorMessage = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                errorMessage = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                errorMessage = "Insufficient permissions" ;
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                errorMessage = "Network related error" ;
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                errorMessage = "Network operation timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                errorMessage = "No recognition result matched" ;
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                errorMessage = "RecognitionServiceBusy" ;
                break;
            case SpeechRecognizer.ERROR_SERVER:
                errorMessage = "Server sends error status";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                errorMessage = "No speech input" ;
                break;
            default:
                errorMessage = "ASR error";
                break;
        }

        Log.e(LOGTAG, "Error when attempting to listen: "+ errorMessage);
        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();

        try {
            myTts.speak(errorMessage,"EN");
        } catch (Exception e) {
            Log.e(LOGTAG, "English not available for TTS, default language used instead");
        }

    }

    /**
     * Thi method does the call the intent for google search
     * @param criteria
     */
    private void googleText(String criteria) {
        if(deviceConnectedToInternet()){
            Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
            intent.putExtra(SearchManager.QUERY,criteria);
            startActivity(intent);

        }else{
            Toast.makeText(getApplicationContext(),"Please check your Internet connection", Toast.LENGTH_LONG).show(); //Not possible to carry out the intent
            Log.e(LOGTAG, "Device not connected to Internet");
        }
    }

    /**
     * This method is just mean't for giving feedback to the user
     * @param criteria
     */

    private void indicateSearch(String criteria) {
        changeButtonAppearanceToDefault();
        myTts.speak(getResources().getString(R.string.searching_prompt)+criteria);

    }

    /**
     * Providing feedback to the user to show that the App is idle
     * It changes the button color and respinse text
     */
    private void changeButtonAppearanceToDefault() {
        Button button =(Button) findViewById(R.id.btn_speech);
        TextView response = (TextView) findViewById(R.id.tv_response);
        button.setBackgroundColor(ContextCompat.getColor(this, R.color.default_color)
        );
        button.setText(getResources().getString(R.string.btn_label));
        response.setText(getResources().getString(R.string.default_response));
    }

    /**
     * Checks whether device is connected to Internet (returns true ) or not (returns false)
     * @return
     */
    private boolean deviceConnectedToInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
    }

    /**
     * Shuts down the TTS engine when finished
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        myTts.shutdown();
    }
}
