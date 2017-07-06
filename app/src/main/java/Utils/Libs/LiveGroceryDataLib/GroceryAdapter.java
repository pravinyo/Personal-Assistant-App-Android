package Utils.Libs.LiveGroceryDataLib;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.is_great.pro.personalassistantbot.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import Utils.DataStructure.Grocery;

/**
 * Created by Pravinyo on 3/16/2017.
 */

public class GroceryAdapter extends ArrayAdapter<Grocery> {

    public GroceryAdapter(Context context, ArrayList<Grocery> groceries){
        super(context,0,groceries);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View listItemView=convertView;
        if(listItemView == null){
            listItemView= LayoutInflater.from(getContext()).inflate(
                    R.layout.grocery_list_item,parent,false);
        }

        Grocery currentGrocery= getItem(position);


        long dateStamp = currentGrocery.getDateTime();
        String state =currentGrocery.getState();
        String district = currentGrocery.getDistrict();
        String market = currentGrocery.getMarket();
        String commodity = currentGrocery.getCommodity();
        float max_price=currentGrocery.getMax_price();
        float min_price = currentGrocery.getMin_price();
        float modal_price = currentGrocery.getModal_price();

        TextView date = (TextView) listItemView.findViewById(R.id.timestamp);
        date.setText(dateNormalize(dateStamp));

        TextView state_TV = (TextView) listItemView.findViewById(R.id.state_text_view);
        state_TV.setText(state);

        TextView district_TV =(TextView) listItemView.findViewById(R.id.district_text_view);
        district_TV.setText(district);

        TextView market_TV =(TextView) listItemView.findViewById(R.id.market_text_view);
        market_TV.setText(market);

        TextView commodity_TV = (TextView) listItemView.findViewById(R.id.commodity_text_view);
        commodity_TV.setText(commodity);

        TextView max_price_TV = (TextView) listItemView.findViewById(R.id.Max_Price_text_view);
        max_price_TV.setText(max_price+"");

        TextView min_price_TV =(TextView) listItemView.findViewById(R.id.Min_Price_text_view);
        min_price_TV.setText(min_price+"");

        TextView modal_price_TV = (TextView) listItemView.findViewById(R.id.Modal_text_view);
        modal_price_TV.setText(modal_price+"");


        return listItemView;
    }
    private String dateNormalize(long unixSeconds){
        Date date = new Date(unixSeconds*1000L); // *1000 is to convert seconds to milliseconds
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm"); // the format of your date
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Calcutta")); // give a timezone reference for formating (see comment at the bottom
        String formattedDate = sdf.format(date);
        return formattedDate;
    }
}

