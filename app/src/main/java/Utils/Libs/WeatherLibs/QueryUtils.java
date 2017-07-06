package Utils.Libs.WeatherLibs;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import Utils.DataStructure.Forecast;

/**
 * Created by Pravinyo on 3/11/2017.
 */

public final class QueryUtils {
    /**
     * Sample JSON response for a USGS query
     */
    private static final String LOG_TAG = "QueryUtils";

    /**
     * Create a private constructor because no one should ever create a {@link QueryUtils} object.
     * This class is only meant to hold static variables and methods, which can be accessed
     * directly from the class name QueryUtils (and an object instance of QueryUtils is not needed).
     */
    private QueryUtils() {
    }

    /**
     * Return a list of {@link Forecast} objects that has been built up from
     * parsing the given JSON response.
     */
    private static List<Forecast> extractFeatureFromJson(String weatherJSON) {
        // If the JSON string is empty or null, then return early.
        if (TextUtils.isEmpty(weatherJSON)) {
            return null;
        }

        // Create an empty ArrayList that we can start adding earthquakes to
        List<Forecast> forecasts = new ArrayList<>();

        // Try to parse the JSON response string. If there's a problem with the way the JSON
        // is formatted, a JSONException exception object will be thrown.
        // Catch the exception so the app doesn't crash, and print the error message to the logs.
        try {

            // Create a JSONObject from the JSON response string
            JSONObject baseJsonResponse = new JSONObject(weatherJSON);

            // Extract the JSONArray associated with the key called "list",
            // which represents a list of forecasts.
            JSONArray forecastArray = baseJsonResponse.getJSONArray("list");

            // For each earthquake in the earthquakeArray, create an {@link Earthquake} object
            for (int i = 0; i < forecastArray.length(); i++) {

                // Get a single earthquake at position i within the list of earthquakes
                JSONObject currentForecast = forecastArray.getJSONObject(i);

                //get the date time for the current forecast
                long dateTime = currentForecast.getLong("dt");


                JSONObject main = currentForecast.getJSONObject("main");
                // Extract the value for the key called "temp"
                double temp = main.getDouble("temp");
                double temp_min = main.getDouble("temp_min");
                double temp_max = main.getDouble("temp_max");
                double pressure = main.getDouble("pressure");
                int humidity = main.getInt("humidity");


                JSONObject weather = currentForecast.getJSONArray("weather").getJSONObject(0);
                int weather_id = weather.getInt("id");
                String weather_type = weather.getString("main");
                String weather_description = weather.getString("description");
                String weather_icon = weather.getString("icon");

                JSONObject wind = currentForecast.getJSONObject("wind");
                double speed = wind.getDouble("speed");
                double degree = wind.getDouble("deg");

                // Create a new {@link Earthquake} object with the magnitude, location, time,
                // and url from the JSON response.
                //Earthquake earthquake = new Earthquake(magnitude, location, time, url);

                Forecast forecast = new Forecast(weather_id, weather_type, weather_description, weather_icon, temp, temp_min, temp_max, pressure, humidity, speed, degree, dateTime);
                // Add the new {@link Earthquake} to the list of earthquakes.
                Log.i(LOG_TAG, "Humidity" + forecast.getMain_HUMIDITY() + "\t Temp:" + forecast.getMain_TEMP());
                forecasts.add(forecast);
            }
        } catch (JSONException e) {
            // If an error is thrown when executing any of the above statements in the "try" block,
            // catch the exception here, so the app doesn't crash. Print a log message
            // with the message from the exception.
            Log.e("QueryUtils", "Problem parsing the forecast JSON results", e);
        }

        // Return the list of earthquakes
        return forecasts;
    }

    /**
     * Returns new URL object from the given string URL.
     */
    private static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Problem building the URL ", e);
        }
        return url;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     */
    private static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode() + "\nURL: " + url);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem retrieving the forecast JSON results.", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                // Closing the input stream could throw an IOException, which is why
                // the makeHttpRequest(URL url) method signature specifies than an IOException
                // could be thrown.
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Convert the {@link InputStream} into a String which contains the
     * whole JSON response from the server.
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    /**
     * Query the openWeatherMap dataset and return a list of {@link Forecast} objects.
     */
    public static List<Forecast> fetchForecastData(String requestUrl) {
        Log.i(LOG_TAG, "In fetch Forecast data method");

        // Create URL object
        URL url = createUrl(requestUrl);

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem making the HTTP request.", e);
        }

        // Extract relevant fields from the JSON response and create a list of {@link Earthquake}s
        List<Forecast> forecasts = extractFeatureFromJson(jsonResponse);

        // Return the list of {@link Earthquake}s
        return forecasts;
    }

    public static Forecast fetchLiveForecastData(String requestUrl) {
        Log.i(LOG_TAG, "In fetch Forecast data method");

        // Create URL object
        URL url = createUrl(requestUrl);

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem making the HTTP request.", e);
        }

        // Extract relevant fields from the JSON response and create a list of {@link Earthquake}s
        Forecast forecast = extractLiveFeatureFromJson(jsonResponse);

        // Return the list of {@link Earthquake}s
        return forecast;
    }

    private static Forecast extractLiveFeatureFromJson(String weatherJSON) {
        // Create a JSONObject from the JSON response string
        JSONObject baseJsonResponse = null;
        Forecast forecast=null;

        try {
            baseJsonResponse = new JSONObject(weatherJSON);
            JSONArray weather = baseJsonResponse.getJSONArray("weather");
            int weather_id = weather.getJSONObject(0).getInt("id");
            String weather_type = weather.getJSONObject(0).getString("main");
            String weather_description = weather.getJSONObject(0).getString("description");
            String weather_icon = weather.getJSONObject(0).getString("icon");

            JSONObject main = baseJsonResponse.getJSONObject("main");
            // Extract the value for the key called "temp"
            double temp = main.getDouble("temp");
            double temp_min = main.getDouble("temp_min");
            double temp_max = main.getDouble("temp_max");
            double pressure = main.getDouble("pressure");
            int humidity = main.getInt("humidity");

            JSONObject wind = baseJsonResponse.getJSONObject("wind");
            double speed = wind.getDouble("speed");
            double degree = wind.getDouble("deg");

            long dateTime = baseJsonResponse.getLong("dt");

            JSONObject sys = baseJsonResponse.getJSONObject("sys");
            long sunrise = sys.getLong("sunrise");
            long sunset = sys.getLong("sunset");

            forecast = new Forecast(weather_id, weather_type, weather_description, weather_icon, temp, temp_min, temp_max, pressure, humidity, speed, degree, dateTime,
                    sunrise, sunset);
            // Add the new {@link Earthquake} to the list of earthquakes.
            Log.i(LOG_TAG, "Humidity" + forecast.getMain_HUMIDITY() + "\t Temp:" + forecast.getMain_TEMP());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return forecast;
    }
}
