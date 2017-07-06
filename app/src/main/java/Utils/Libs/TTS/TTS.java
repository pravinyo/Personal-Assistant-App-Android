package Utils.Libs.TTS;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * Created by Pravinyo on 3/7/2017.
 */

public class TTS implements TextToSpeech.OnInitListener {

    private TextToSpeech myTTS;
    private static TTS singleton;

    private static final String LOGTAG = "TTS";

    /**
     * Creates the single <code>TTS</code> instance and initializes the text to speech
     * engine. It is private so that it cannot be invoked from outside the
     * class and thus it is not possible to create new <code>TTS</code> objects.
     *
     * @param ctx context of the interaction
     */
    private TTS(Context ctx) {
        myTTS = new TextToSpeech(ctx,(TextToSpeech.OnInitListener) this);
    }

    /**
     * Returns the single <code>TTS</code> instance. If it did not exist, it creates it beforehand.
     *
     * @param ctx context of the interaction
     * @return reference to the single <code>TTS</code> instance
     */
    public static TTS getInstance(Context ctx){
        if(singleton==null){
            singleton = new TTS(ctx);
        }

        return singleton;
    }

    /**
     * Sets the locale for speech synthesis taking into account the language and country codes
     * If the <code>countryCode</code> is null, it just sets the language, if the
     * <code>languageCode</code> is null, it uses the default language of the device
     * If any of the codes are not valid, it uses the default language
     *
     * @param languageCode a String representing the language code, e.g. EN
     * @param countryCode a String representing the country code for the language used, e.g. US.
     * @throws Exception when the codes supplied cannot be used and the default locale is selected
     */
    public void setLocale(String languageCode, String countryCode) throws Exception{
        if(languageCode==null)
        {
            setLocale();
            throw new Exception("Language code was not provided, using default locale");
        }
        else{
            if(countryCode==null)
                setLocale(languageCode);
            else {
                Locale lang = new Locale(languageCode, countryCode);
                if (myTTS.isLanguageAvailable(lang) == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE )
                    myTTS.setLanguage(lang);
                {
                    setLocale();
                    throw new Exception("Language or country code not supported, using default locale");
                }
            }
        }
    }

    /**
     * Sets the locale for speech synthesis taking into account the language code
     * If the code is null or not valid, it uses the default language of the device
     *
     * @param languageCode a String representing the language code, e.g. EN
     * @throws Exception when the code supplied cannot be used and the default locale is selected
     */
    public void setLocale(String languageCode) throws Exception{
        if(languageCode==null)
        {
            setLocale();
            throw new Exception("Language code was not provided, using default locale");
        }
        else {
            Locale lang = new Locale(languageCode);
            if (myTTS.isLanguageAvailable(lang) != TextToSpeech.LANG_MISSING_DATA && myTTS.isLanguageAvailable(lang) != TextToSpeech.LANG_NOT_SUPPORTED)
                myTTS.setLanguage(lang);
            else
            {
                setLocale();
                throw new Exception("Language code not supported, using default locale");
            }
        }
    }

    /**
     * Sets the default language of the device as locale for speech synthesis
     */
    public void setLocale(){
        myTTS.setLanguage(Locale.getDefault());
    }

    /**
     * Synthesizes a text in the language indicated (or in the default language of the device
     * it it is not available)
     *
     * @param languageCode language for the TTS, e.g. EN
     * @param countryCode country for the TTS, e.g. US
     * @param text string to be synthesized
     * @throws Exception when the codes supplied cannot be used and the default locale is selected
     */
    public void speak(String text, String languageCode, String countryCode) throws Exception{
        setLocale(languageCode, countryCode);
        myTTS.speak(text, TextToSpeech.QUEUE_ADD, null);
    }

    /**
     * Synthesizes a text in the language indicated (or in the default language of the device
     * if it is not available)
     *
     * @param languageCode language for the TTS, e.g. EN
     * @param text string to be synthesized
     * @throws Exception when the code supplied cannot be used and the default locale is selected
     */
    public void speak(String text, String languageCode) throws Exception{
        setLocale(languageCode);
        myTTS.speak(text, TextToSpeech.QUEUE_ADD, null);
    }

    /**
     * Synthesizes a text using the default language of the device
     *
     * @param text string to be synthesized
     */
    public void speak(String text){
        setLocale();
        myTTS.speak(text, TextToSpeech.QUEUE_ADD, null);
    }

    /**
     * Stops the synthesizer if it is speaking
     */
    public void stop(){
        if(myTTS.isSpeaking())
            myTTS.stop();
    }

    /**
     * Stops the speech synthesis engine. It is important to call it, as
     * it releases the native resources used.
     */
    public void shutdown(){
        myTTS.stop();
        myTTS.shutdown();
        singleton=null;		/*
		 						This is necessary in order to force the creation of a new TTS instance after shutdown.
		 						It is useful for handling runtime changes such as a change in the orientation of the device,
		 						as it is necessary to create a new instance with the new context.
		 						See here: http://developer.android.com/guide/topics/resources/runtime-changes.html
							*/
    }

    /*
     * A <code>TextToSpeech</code> instance can only be used to synthesize text once
     * it has completed its initialization.
     * (non-Javadoc)
     * @see android.speech.tts.TextToSpeech.OnInitListener#onInit(int)
     */
    @Override
    public void onInit(int status) {
        if(status != TextToSpeech.ERROR){
            setLocale();
        }
        else
        {
            Log.e(LOGTAG, "Error creating the TTS");
        }

    }

}
