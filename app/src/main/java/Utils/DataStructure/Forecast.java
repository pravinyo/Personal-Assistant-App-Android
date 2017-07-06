package Utils.DataStructure;

/**
 * Created by Pravinyo on 3/10/2017.
 */

public class Forecast {

    private int mWeather_ID;
    private String mWeather_MAIN;
    private String mWeather_DESC;
    private String mWeather_ICON;
    private double mMain_TEMP,mMain_PRESSURE,mMain_TEMP_MIN,mMain_TEMP_MAX;
    private int mMain_HUMIDITY;
    private double mWind_SPEED;
    private double mWind_DEG;
    private long mDt;
    private long SUNRISE=0,SUNSET=0;

    public Forecast(int id,String main,String desc,String icon,double temp,double temp_min,double temp_max,double pressure,
                    int humidity,double speed,double deg,long dt,long sunrise,long sunset){
        mWeather_ID=id;
        mWeather_MAIN=main;
        mWeather_DESC=desc;
        mWeather_ICON=icon;
        mMain_HUMIDITY=humidity;
        mMain_PRESSURE=pressure;
        mMain_TEMP=temp;
        mMain_TEMP_MIN=temp_min;
        mMain_TEMP_MAX=temp_max;
        mWind_SPEED=speed;
        mWind_DEG=deg;
        mDt=dt;
        SUNRISE=sunrise;
        SUNSET=sunset;

    }
    public Forecast(int id,String main,String desc,String icon,double temp,double temp_min,double temp_max,double pressure,
                    int humidity,double speed,double deg,long dt){
        mWeather_ID=id;
        mWeather_MAIN=main;
        mWeather_DESC=desc;
        mWeather_ICON=icon;
        mMain_HUMIDITY=humidity;
        mMain_PRESSURE=pressure;
        mMain_TEMP=temp;
        mMain_TEMP_MIN=temp_min;
        mMain_TEMP_MAX=temp_max;
        mWind_SPEED=speed;
        mWind_DEG=deg;
        mDt=dt;

    }

    public int getWeatherID(){
        return mWeather_ID;
    }

    public double getmMain_PRESSURE() {
        return mMain_PRESSURE;
    }

    public double getMain_TEMP() {
        return mMain_TEMP;
    }

    public double getMain_TEMP_MAX() {
        return mMain_TEMP_MAX;
    }

    public double getMain_TEMP_MIN() {
        return mMain_TEMP_MIN;
    }

    public double getWind_DEG() {
        return mWind_DEG;
    }

    public double getWind_SPEED() {
        return mWind_SPEED;
    }

    public int getMain_HUMIDITY() {
        return mMain_HUMIDITY;
    }

    public long getDt() {
        return mDt;
    }

    public long getSUNRISE() {
        return SUNRISE;
    }

    public long getSUNSET() {
        return SUNSET;
    }

    public String getWeather_DESC() {
        return mWeather_DESC;
    }

    public String getWeather_MAIN() {
        return mWeather_MAIN;
    }
}
