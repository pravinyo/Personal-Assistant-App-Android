package com.is_great.pro.personalassistantbot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import Utils.DataStructure.Form;
import Utils.Libs.TTS.TTS;
import Utils.Libs.formFillLib.DialogInterpreter;
import Utils.Libs.formFillLib.FormFillLibException;
import Utils.Libs.formFillLib.VXMLParser;
import Utils.Libs.xmlLib.RetrieveXMLTask;
import Utils.Libs.xmlLib.XMLAsyncResponse;

public class callActivity extends DialogInterpreter implements XMLAsyncResponse {
    private static final int PERMISSIONS_REQUEST_CALL_CONTACTS = 102;
    private static final String LOGTAG = "Call";
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;
    private static final int PERMISSIONS_REQUEST_SMS_CONTACTS = 104;
    // The ListView
    private ListView lstNames;
    private String number,name,message;
    private String query="";

    //speak
    private TTS myTts;

    //URL with the vxml file that contains the structure of the dialog
    private static final String URL_CALL_VXML = "https://drive.google.com/uc?export=download&id=0B7Mioou5gaILeFZxTVU3SU53NjA";
    private static final String DEFAULT_URL_CALL_VXML = "https://drive.google.com/uc?export=download&id=0B7Mioou5gaILeFZxTVU3SU53NjA";
    private static final String URL_SMS_VXML="https://drive.google.com/uc?export=download&id=0B7Mioou5gaILR1h1cE42NEhaMXM";
    private static final String DEFAULT_URL_SMS_VXML="https://drive.google.com/uc?export=download&id=0B7Mioou5gaILR1h1cE42NEhaMXM";
    private static final String EXTRA_RECEIVER_SMS_REQUEST ="sms_request" ;
    private static String SMS_REQUEST = "NO";

    HashMap<String,String> DATA_RECEIVED;

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
        setContentView(R.layout.activity_call);

        this.lstNames = (ListView) findViewById(R.id.lsName);
        //Start the interpretation of the VXML file
        Intent smsIntent =getIntent();
        if(smsIntent.getStringExtra(EXTRA_RECEIVER_SMS_REQUEST) != null){
            SMS_REQUEST = smsIntent.getStringExtra(EXTRA_RECEIVER_SMS_REQUEST);
        }

        startDialog();


    }
    /**
     * Initializes the ASR and TTS engines and retrieves the vxml file from the web.
     * When the file has been successfully retrieved, the <code>processXMLContents</code> method is invoked,
     * which parses and interprets the dialog
     */
    @SuppressLint("NewApi")
    void startDialog(){
        try{
            initializeAsrTts();
            if(SMS_REQUEST.equals("YES")){
                retrieveXML(URL_SMS_VXML, DEFAULT_URL_SMS_VXML);
            }else{
                retrieveXML(URL_CALL_VXML, DEFAULT_URL_CALL_VXML);
            }
        } catch (Exception e) {
            Log.e(LOGTAG, "Internet connection error"+e.getMessage());
            createAlert("Connection error", "Please check your Internet connection").show();
        }
    }
    /**
     * Retrieves the contents of an XML file in the specified url. If the first url (url) is not accessible,
     * the second url (url_default) is used.
     * When the file has been successfully retrieved, the <code>processXMLContents</code> method is invoked,
     * which parses and interprets the dialog
     */
    public void retrieveXML(String url, String url_default){
        RetrieveXMLTask retrieveXML = new RetrieveXMLTask();	//AsyncTask to retrieve the contents of the XML file from the URL
        retrieveXML.delegate = this;	//It is crucial in order to retrieve the data from the asyncrhonous task (see the AsyncResponse and RetrieveXMLTask classes)


		/*
		 * The string corresponding to the XML file is retrieved with an asynchronous task, and thus
		 * is executed in the background. When this process is finished, the "processXMLContents" method
		 * is invoked (see below).
		 */
        retrieveXML.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url, url_default); //An Executor that can be used to execute tasks in parallel.


        //See reference here: http://developer.android.com/reference/android/os/AsyncTask.Status.html
        if(retrieveXML.getStatus() == (AsyncTask.Status.PENDING)) {
            //Indicates that the task has not been executed yet
            Log.i(LOGTAG, "VXML reading: Pending");
        } else if(retrieveXML.getStatus() == (AsyncTask.Status.RUNNING)) {
            //Indicates that the task is running
            Log.i(LOGTAG, "VXML reading: Running");
        } else if(retrieveXML.getStatus() == (AsyncTask.Status.FINISHED)) {
            //Indicates that AsyncTask.onPostExecute has finished
            Log.i(LOGTAG, "VXML reading: Finished");
        }
    }
    /**
     * It is invoked when a VXML file has been read from a URL.
     * It parses and interprets the VXML code received in the input string.
     *
     * @param xmlContent VXML code containing the structure of the form-filling dialog
     */
    @Override
    public void processXMLContents(String xmlContent) {

        if(!xmlContent.contains("musicbrainz")){
            Form form;


            try {
                form = VXMLParser.parseVXML(xmlContent);
                startInterpreting(form);
            } catch (XmlPullParserException ex) {
                Log.e(LOGTAG, "Error parsing the VXML file: "+ex.getMessage());
                createAlert("Parsing error", "Please check your Internet connection").show();
            } catch (FormFillLibException ex) {
                Log.e(LOGTAG, ex.getMessage());
                createAlert("Parsing error", ex.getReason()).show();
            }
        }
        else{
            Toast.makeText(this,"Parsing done response need to be generated",Toast.LENGTH_SHORT).show();
            //parseMusicResults(xmlContent);
        }
    }

    /**
     * Once the VXML file has been interpreted, the results obtained are saved in "EmailData",
     * and the web service of MusicBrainZ is queried
     */
    @Override
    public void processDialogResults(HashMap<String, String> result) {
        Log.i(LOGTAG, "Dialogue end. The results are: "+result);
        DATA_RECEIVED = result;
        Log.i(LOGTAG,result.toString());
        if(SMS_REQUEST.equals("YES")){
            query = DATA_RECEIVED.get("cName");
            message = DATA_RECEIVED.get("cMessage");
        }else{
            query = DATA_RECEIVED.get("cName");
        }

        showContacts();

    }
    /**
     * Show the contacts in the ListView.
     */
    private void showContacts() {
        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.
            List<String> contacts = getContactNames();
            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, contacts);
            lstNames.setAdapter(adapter);
            lstNames.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Object data = adapter.getItem(position);
                    if(SMS_REQUEST.equals("YES")){
                        sendSMS((String)data);
                    }else{
                        callContact((String)data);
                    }
                    Toast.makeText(callActivity.this,(String)data,Toast.LENGTH_SHORT).show();
                }
            });
            if(adapter.getCount()==1){
                if(SMS_REQUEST.equals("YES")){
                    sendSMS(adapter.getItem(0));
                }else{
                    callContact(adapter.getItem(0));
                }
            }else if(adapter.getCount()>1){

                //initialize text to speech
                myTts=TTS.getInstance(this);

                myTts.speak("Please Select one of them");
            }

        }
    }
    private void sendSMS(String numberAndName) {

        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, PERMISSIONS_REQUEST_SMS_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.
            String[] data = numberAndName.split("=>");
            Toast.makeText(getBaseContext(),data[1],Toast.LENGTH_SHORT).show();
            startSMSSending(message,data[1]);
        }


    }

    private void startSMSSending(String message,String number) {
        Log.i(LOGTAG,"sms Sending Process started");
        Intent smsIntent = new Intent(Intent.ACTION_VIEW);
        smsIntent.setData(Uri.parse("smsto:"));
        smsIntent.setType("vnd.android-dir/mms-sms");
        smsIntent.putExtra("address"  , number);
        smsIntent.putExtra("sms_body"  , message);
        try {
            startActivity(smsIntent);
            finish();
            Log.i("Finished sending SMS...", "");
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(callActivity.this,
                    "SMS failed, please try again later.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                showContacts();
            } else {
                Toast.makeText(this, "Until you grant the permission, we canot display the names", Toast.LENGTH_SHORT).show();
            }
        }else if (requestCode == PERMISSIONS_REQUEST_CALL_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                showContacts();
            } else {
                Toast.makeText(this, "Until you grant the permission, we cannot initiate the call", Toast.LENGTH_SHORT).show();
            }
        }else if(requestCode == PERMISSIONS_REQUEST_SMS_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showContacts();
            } else {
                Toast.makeText(this, "Until you grant the permission, we cannot initiate the SMS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Read the name of all the contacts.
     *
     * @return a list of names.
     */
    private List<String> getContactNames() {
        List<String> contacts = new ArrayList<>();
        // Get the ContentResolver
        ContentResolver cr = getContentResolver();

        Cursor cursor =cr.query(android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[] { ContactsContract.CommonDataKinds.Phone.PHOTO_ID,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME ,
                        ContactsContract.CommonDataKinds.Phone.NUMBER},
                ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?",
                new String[]{"%"+query+"%"}, null);
        Toast.makeText(callActivity.this,query,Toast.LENGTH_SHORT).show();
        // Move the cursor to first. Also check whether the cursor is empty or not.
        if (cursor.moveToFirst()) {
            // Iterate through the cursor
            do {
                // Get the contacts name
                name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                number =cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                contacts.add(name+"=>"+number);
                Log.i(LOGTAG,name+"=>"+number);
            } while (cursor.moveToNext());
        }
        // Close the curosor
        cursor.close();

        return contacts;
    }

    private void callContact(String numberAndName) {

        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, PERMISSIONS_REQUEST_CALL_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.
            String[] data = numberAndName.split("=>");
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + data[1]));
            startActivity(intent);
            finish();
        }

    }
    /**
     * Creates an alert dialog in which the user must click OK to continue.
     * It is used in the class mainly to provide feedback about errors. The errors during parsing
     * are mainly due to problems with the Internet connection, so if the users click "ok" they
     * are aware of this fact and will try to solve it
     *
     * More about dialogs here: http://developer.android.com/guide/topics/ui/dialogs.html
     *
     * @param title Title of the dialog window
     * @param message Message of the dialog
     * @return alert dialog to be shown
     */
    private AlertDialog createAlert(String title, String message){

        //Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //Chain together various setter methods to set the dialog characteristics
        builder.setMessage(message);
        builder.setTitle(title);

        //Add the button
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Intent intent = new Intent(callActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });
        // Create the AlertDialog
        return builder.create();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
