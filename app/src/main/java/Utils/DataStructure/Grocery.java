package Utils.DataStructure;

/**
 * Created by Pravinyo on 3/16/2017.
 */

public class Grocery {
    private String mState;
    private String mMarket;
    private String mDistrict;
    private String mCommodity;
    private long mDateTime;
    private float max_price;
    private float min_price;
    private float modal_price;

    public Grocery(String state,String market,String district,
                   String commodity,long datetime,float max,float min,
                   float modal){
        mState=state;
        mMarket=market;
        mDateTime=datetime;
        mDistrict=district;
        mCommodity=commodity;
        max_price=max;
        min_price=min;
        modal_price=modal;

    }

    public String getState() {
        return mState;
    }

    public String getMarket() {
        return mMarket;
    }

    public String getDistrict() {
        return mDistrict;
    }

    public String getCommodity() {
        return mCommodity;
    }

    public long getDateTime() {
        return mDateTime;
    }

    public float getMax_price() {
        return max_price;
    }

    public float getMin_price() {
        return min_price;
    }

    public float getModal_price() {
        return modal_price;
    }
}

