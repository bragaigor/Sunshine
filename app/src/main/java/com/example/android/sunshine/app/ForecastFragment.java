package com.example.android.sunshine.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Braga on 5/19/2016.
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment{

    private ArrayAdapter<String> mforecastAdapter;

    public ForecastFragment() {}

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //inflater.inflate(R.menu.main, menu);
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = prefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        String metrics = prefs.getString(getString(R.string.pref_scale_key), getString(R.string.pref_scale_default));
        weatherTask.execute(location, metrics);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mforecastAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview);

        ListView forecastView = (ListView)rootView.findViewById(R.id.listView_forecast);
        forecastView.setAdapter(mforecastAdapter);

        forecastView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast = mforecastAdapter.getItem(position);

                Context context = getActivity();
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, forecast, duration);
                toast.setGravity(Gravity.CENTER | Gravity.BOTTOM, 5, 20);
                toast.show();

                Intent intent = new Intent(context, DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(intent);
            }
        });

        return rootView;
    }
    public class FetchWeatherTask extends AsyncTask<String, Void, ArrayList<String>> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        private ArrayList<String> forecastArr;

        @Override
        protected ArrayList<String> doInBackground(String... params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;
            String format = "json";
            String units = "metric";
            String appId = "70190a422cf8c259d4ab0b76eb644874";
            int numDays = 7;
            ArrayList<String> forecastList = new ArrayList<>();

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                //URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=Norfolk,VA&mode=json&units=metric&cnt=7&appid=70190a422cf8c259d4ab0b76eb644874");
                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";

                final String QUERY_PARAM = "q";
                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAY_PARAM = "cnt";
                final String APP_ID = "appid";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_PARAM, params[0])
                        .appendQueryParameter(FORMAT_PARAM, format)
                        .appendQueryParameter(UNITS_PARAM, units)
                        .appendQueryParameter(DAY_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(APP_ID, appId)
                        .build();

                String urlRequest = builtUri.toString();
                //Log.v(LOG_TAG, "Built URI " + urlRequest);

                URL url = new URL(urlRequest);

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();

                forecastList = getListofForecast(forecastJsonStr, params[1]);

                //Log.v(LOG_TAG, "Forecast JSON String " + forecastJsonStr);

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                forecastJsonStr = null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();


                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return forecastList;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            if(result != null) {
                mforecastAdapter.clear();
                mforecastAdapter.addAll(result);
            }
        }

        public ArrayList<String> getListofForecast(String JSONString, String scale) {

            ArrayList<String> weekForecast = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            Date date = cal.getTime();
            SimpleDateFormat sdf1 = new SimpleDateFormat("E, LLL dd");

            double maxTemp = 0.0, minTemp = 0.0;

            try {
                JSONObject obj = new JSONObject(JSONString);

                JSONArray jsonAr = obj.getJSONArray("list");

                for(int i = 0; i < jsonAr.length(); i++) {
                    String dt = jsonAr.getJSONObject(i).get("dt").toString();
                    //String maxTemp = jsonAr.getJSONObject(i).getJSONObject("temp").get("max").toString();
                    // Values are in celsius
                    maxTemp = jsonAr.getJSONObject(i).getJSONObject("temp").getDouble("max");
                    minTemp = jsonAr.getJSONObject(i).getJSONObject("temp").getDouble("min");

                    // TODO: IMPLEMENT CONVERSTION TO FAREINGHEIT IF SCALE IS imperial
                    if (scale.matches("imperial")) {
                        //System.out.println("********** CURRENT SCALE IS IMPERIAL ***********");

                    }

                    String dayDescri = jsonAr.getJSONObject(i).getJSONArray("weather").getJSONObject(0).getString("description").toString();

                    //System.out.println("Description: " + dayDescri);
                    //System.out.println("Max temp: " + maxTemp);
                    String dayInfo = sdf1.format(date) + " - " + dayDescri + " - " + Math.round(maxTemp) + "/" + Math.round(minTemp);
                    //Log.v(LOG_TAG, dayInfo);
                    weekForecast.add(dayInfo);
                    cal.add(Calendar.DAY_OF_WEEK, 1);
                    date = cal.getTime();
                }

            } catch(Exception e) {
                e.printStackTrace();
            }
            return weekForecast;
        }
    }
}
