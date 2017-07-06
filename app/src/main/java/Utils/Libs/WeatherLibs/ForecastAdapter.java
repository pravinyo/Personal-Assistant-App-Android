package Utils.Libs.WeatherLibs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.is_great.pro.personalassistantbot.R;

import java.util.ArrayList;

import Utils.DataStructure.Forecast;

/**
 * Created by Pravinyo on 3/10/2017.
 */

public class ForecastAdapter extends ArrayAdapter<Forecast> {

    public ForecastAdapter(Context context, ArrayList<Forecast> weathers){
        super(context,0,weathers);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View listItemView = convertView;
        if(listItemView ==  null){
            listItemView= LayoutInflater.from(getContext()).inflate(
                    R.layout.forecast_list_item,parent,false);
        }
        Forecast currentForecast = getItem(position);
        long date =currentForecast.getDt();
        String weatherDescription = currentForecast.getWeather_DESC();
        double high_temp=currentForecast.getMain_TEMP_MAX()-273.15;
        double low_temp = currentForecast.getMain_TEMP_MIN()-273.15;

        int weatherId = currentForecast.getWeatherID();
        int weatherImageId = PABWeatherUtils
                .getLargeArtResourceIdForWeatherCondition(weatherId);


        ImageView weatherIcon = (ImageView) listItemView.findViewById(R.id.weather_icon);
        weatherIcon.setImageResource(weatherImageId);

        TextView tv_date =(TextView) listItemView.findViewById(R.id.date);
        tv_date.setText(PABDateUtils.getDateCurrentTimeZone(date));

        TextView Weather_Description =(TextView) listItemView.findViewById(R.id.weather_description);
        Weather_Description.setText(weatherDescription);

        TextView High_Temp = (TextView) listItemView.findViewById(R.id.high_temperature);
        High_Temp.setText(PABWeatherUtils.formatTemperature(getContext(),high_temp));

        TextView Low_Temp = (TextView) listItemView.findViewById(R.id.low_temperature);
        Low_Temp.setText(PABWeatherUtils.formatTemperature(getContext(),low_temp));
        return listItemView;
    }
}
