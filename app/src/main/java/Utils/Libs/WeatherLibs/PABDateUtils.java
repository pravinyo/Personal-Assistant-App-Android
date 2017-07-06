package Utils.Libs.WeatherLibs;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by Pravinyo on 3/28/2017.
 */

public class PABDateUtils {
    private static final String[] DAYS_ARRAY={"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
    public  static String getDateCurrentTimeZone(long timestamp) {
        try{
            Calendar calendar = Calendar.getInstance();
            TimeZone tz = TimeZone.getDefault();
            calendar.setTimeInMillis(timestamp * 1000);
            calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));

            String day = DAYS_ARRAY[calendar.get(Calendar.DAY_OF_WEEK)-1];
            String time = getNormalized(calendar.get(Calendar.HOUR))+":"+getNormalized(calendar.get(Calendar.MINUTE));
            String marker = (calendar.get(Calendar.HOUR_OF_DAY)<12)? " AM":((calendar.get(Calendar.HOUR_OF_DAY)> 12)?" PM":" Noon");

            return day +" "+time+marker;
        }catch (Exception e) {
        }
        return "";
    }

    private static String getNormalized(int Value) {
        if(Value<10){
            return "0"+Value;
        }else{
            return Value+"";
        }
    }
}
