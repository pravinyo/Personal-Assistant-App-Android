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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
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

/**
 * Created by Pravinyo on 3/16/2017.
 */

public class EmailHandler extends DialogInterpreter implements XMLAsyncResponse {
    private static final String LOG_TAG = "call_Email";
    private static final String LOGTAG = "EMAIL HANDLER";
    private static final String RECEIVER_NAME_EMAILHANDLER_QUERY = "email_sending_PAB";
    // The ListView
    private ListView lstNames;
    private String Email,name;
    private String query="Anshika";

    // Request code for READ_CONTACTS. It can be any Email > 0.
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 104;

    //speak
    private TTS myTts;

    //URL with the vxml file that contains the structure of the dialog
    private static final String URL_VXML = "https://drive.google.com/uc?export=download&id=0B7Mioou5gaILUzRwUzhOZldYQVU";
    private static final String DEFAULT_URL_VXML = "https://drive.google.com/uc?export=download&id=0B7Mioou5gaILUzRwUzhOZldYQVU";

    HashMap<String,String> EmailData;

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

        // Find the list view
        this.lstNames = (ListView) findViewById(R.id.lsName);

        Intent intent = getIntent();
        query=intent.getStringExtra(RECEIVER_NAME_EMAILHANDLER_QUERY);
        // Read and show the contacts
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
            retrieveXML(URL_VXML, DEFAULT_URL_VXML);
        } catch (Exception e) {
            Log.e(LOGTAG, "Internet connection error");
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
        }
    }

    /**
     * Once the VXML file has been interpreted, the results obtained are saved in "EmailData",
     * and the web service of MusicBrainZ is queried
     */
    @Override
    public void processDialogResults(HashMap<String, String> result) {
        Log.i(LOGTAG, "Dialogue end. The results are: "+result);
        EmailData = result;
        Log.i(LOGTAG,result.toString());
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
            List<String> contacts = getNameEmailDetails();
            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, contacts);
            lstNames.setAdapter(adapter);
            lstNames.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String item = adapter.getItem(position);
                    //callContact((String)data);
                    startEmailProcess(item);
                    Toast.makeText(EmailHandler.this,item,Toast.LENGTH_SHORT).show();
                }
            });
            if(adapter.getCount()==1){
                //callContact(adapter.getItem(0));
                startEmailProcess(adapter.getItem(0));
                Toast.makeText(EmailHandler.this,"adapter size is one",Toast.LENGTH_SHORT).show();
            }else if(adapter.getCount()>1){

                //initialize text to speech
                myTts=TTS.getInstance(this);

                Toast.makeText(EmailHandler.this,"more than one data in adapter",Toast.LENGTH_SHORT).show();
                myTts.speak("Please Select one of them");
            }
        }
    }

    private void startEmailProcess(String item) {
        String[] data = item.split("=>");
        sendEmail(new String[]{data[1]},null,EmailData.get("eTitle"),EmailData.get("eBody"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                showContacts();
            } else {
                Toast.makeText(this, "Until you grant the permission, we cannot display the names", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private String SELECTION = ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?";
    String[] argSelection = {"%"+query+"%"};

    public List<String> getNameEmailDetails(){
        List<String> names = new ArrayList<>();
        ContentResolver cr = getContentResolver();

        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,null, SELECTION, argSelection, null);
        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                Cursor cur1 = cr.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                        new String[]{id}, null);
                while (cur1.moveToNext()) {
                    //to get the contact names
                    String name=cur1.getString(cur1.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    Log.e("Name :", name);
                    String email = cur1.getString(cur1.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                    Log.e("Email", email);
                    if(email!=null && !name.contains("@")){
                        names.add(name+"=>"+email);
                    }
                }
                cur1.close();
            }
        }
        return names;
    }

    protected void sendEmail(String[] TO,String[] CC,String Subject,String message) {
        Log.i("Send email", "");
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_CC, CC);
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,TO);
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, Subject);
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);
        startActivity(emailIntent);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            finish();
            Log.i(LOG_TAG,"Finished sending email...");
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(EmailHandler.this, "There is no email client installed.", Toast.LENGTH_SHORT).show();
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
                // User clicked OK button
            }
        });
        // Create the AlertDialog
        return builder.create();
    }
}
