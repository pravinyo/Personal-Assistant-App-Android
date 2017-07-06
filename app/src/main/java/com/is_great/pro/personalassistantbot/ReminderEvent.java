package com.is_great.pro.personalassistantbot;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;


import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;


import Utils.DataStructure.Form;
import Utils.Libs.TTS.TTS;
import Utils.Libs.formFillLib.DialogInterpreter;
import Utils.Libs.formFillLib.FormFillLibException;
import Utils.Libs.formFillLib.VXMLParser;
import Utils.Libs.xmlLib.RetrieveXMLTask;
import Utils.Libs.xmlLib.XMLAsyncResponse;

/**
 * Created by Pravinyo on 3/15/2017.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class ReminderEvent extends DialogInterpreter implements XMLAsyncResponse{

    private static final String LOGTAG = "REMINDER";
    private TTS myTTS;

    //URL with the vxml file that contains the structure of the dialog
    private static final String URL_VXML = "https://drive.google.com/uc?export=download&id=0B7Mioou5gaILTDZnM0xGemJaUW8";
    private static final String DEFAULT_URL_VXML = "https://drive.google.com/uc?export=download&id=0B7Mioou5gaILTDZnM0xGemJaUW8l";

    HashMap<String,String> reminderData;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean isNightMode = preference.getBoolean(getString(R.string.settings_preference_key),false);
        if(isNightMode){
            setTheme(R.style.PAB);
        }else{
            setTheme(R.style.MainTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_event);
        //Start the interpretation of the VXML file
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
        reminderData = result;
        Log.i(LOGTAG,result.toString());
        addEventToCalender(reminderData.get("rTitle")
                ,reminderData.get("rDescription")
                ,0
                ,2
                ,5
                ,true
                ,1);
        myTTS=TTS.getInstance(ReminderEvent.this);
        myTTS.speak(getResources().getString(R.string.reminder_set_success_message));
    }
    void addEventToCalender(String title, String Description, int all_day, int stateTimeInMinutes, int endTimeInMinutes, Boolean setReminder, int hasAlarm){


        Calendar cal = Calendar.getInstance();
        Uri EVENTS_URI = Uri.parse(getCalendarUriBase(true) + "events");
        ContentResolver cr = getContentResolver();
        TimeZone timeZone = TimeZone.getDefault();

        /** Inserting an event in calendar. */
        ContentValues values = new ContentValues();
        values.put(CalendarContract.Events.CALENDAR_ID, 1);
        values.put(CalendarContract.Events.TITLE, title);
        values.put(CalendarContract.Events.DESCRIPTION, Description);
        values.put(CalendarContract.Events.ALL_DAY, all_day);//default uou can keep zero

        values.put(CalendarContract.Events.DTSTART, cal.getTimeInMillis() + stateTimeInMinutes * 60 * 1000);
        values.put(CalendarContract.Events.DTEND, cal.getTimeInMillis() + endTimeInMinutes * 60 * 1000);
        values.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID());
        values.put(CalendarContract.Events.HAS_ALARM, hasAlarm);// keep 1 for yes
        Uri event = cr.insert(EVENTS_URI, values);

        // Display event id.
        Toast.makeText(getApplicationContext(), "Event added :: ID :: " + event.getLastPathSegment(), Toast.LENGTH_SHORT).show();

        if(setReminder == true){
            /** Adding reminder for event added. */
            Uri REMINDERS_URI = Uri.parse(getCalendarUriBase(true) + "reminders");
            values = new ContentValues();
            values.put(CalendarContract.Reminders.EVENT_ID, Long.parseLong(event.getLastPathSegment()));
            values.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);
            values.put(CalendarContract.Reminders.MINUTES, stateTimeInMinutes);
            cr.insert(REMINDERS_URI, values);
        }

    }
    /** Returns Calendar Base URI, supports both new and old OS. */
    private String getCalendarUriBase(boolean eventUri) {
        Uri calendarURI = null;
        try {
            if (android.os.Build.VERSION.SDK_INT <= 7) {
                calendarURI = (eventUri) ? Uri.parse("content://calendar/") : Uri.parse("content://calendar/calendars");
            } else {
                calendarURI = (eventUri) ? Uri.parse("content://com.android.calendar/") : Uri
                        .parse("content://com.android.calendar/calendars");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return calendarURI.toString();
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
