package Utils.Libs.NewsLibs;

/**
 * Created by Pravinyo on 3/28/2017.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

import Utils.DataStructure.News;
import com.is_great.pro.personalassistantbot.R;

/**
 * Created by Pravinyo on 3/26/2017.
 */

public class NewsAdapter extends ArrayAdapter {
    public NewsAdapter(Context context, ArrayList<News> newses){
        super(context,0,newses);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View listItemView=convertView;

        ViewHolder viewHolder = null;
        News currentNews= (News) getItem(position);

        if(listItemView == null){
            listItemView= LayoutInflater.from(getContext()).inflate(
                    R.layout.news_list_item,parent,false);
            viewHolder = new ViewHolder();
            viewHolder.imageView=(ImageView) listItemView.findViewById(R.id.imageLink);
            listItemView.setTag(viewHolder);
        }

        if(currentNews != null){

            String author =currentNews.getAuthor();
            TextView author_TV = (TextView) listItemView.findViewById(R.id.author);
            if(author == null){
                author_TV.setText("Unknown");
            }else{
                author_TV.setText(author);
            }

            TextView title_TV = (TextView) listItemView.findViewById(R.id.title);
            title_TV.setText(currentNews.getTitle());

            TextView description_TV =(TextView) listItemView.findViewById(R.id.description);
            description_TV.setText(currentNews.getDescription());

            TextView url_TV =(TextView) listItemView.findViewById(R.id.url);
            url_TV.setText(currentNews.getUrl());
            url_TV.setVisibility(View.GONE);

            TextView publishedAT = (TextView) listItemView.findViewById(R.id.publishedAt);
            try {
                publishedAT.setText(getNormalizedDate(currentNews.getPublishedAt()));
            } catch (ParseException e) {
                e.printStackTrace();
            }

        }


        viewHolder = (ViewHolder)listItemView.getTag();
        viewHolder.imageURL = currentNews.getImageUrl();
        new DownloadImageTask().execute(viewHolder);
        return listItemView;
    }


    private class DownloadImageTask extends AsyncTask<ViewHolder, Void, ViewHolder> {

        protected ViewHolder doInBackground(ViewHolder... urls) {
            ViewHolder viewHolder = urls[0];

            try {
                URL imageURL = new URL(viewHolder.imageURL);
                viewHolder.bitmap = BitmapFactory.decodeStream(imageURL.openStream());
            } catch (IOException e) {
                Log.e("error", "Downloading Image Failed");
                viewHolder.bitmap = null;
            }
            return viewHolder;
        }

        protected void onPostExecute(ViewHolder result) {
            if (result.bitmap == null) {
                result.imageView.setImageResource(R.drawable.no_image);
            } else {
                result.imageView.setImageBitmap(result.bitmap);
            }
        }
    }

    public static class ViewHolder{
        ImageView imageView;
        String imageURL;
        Bitmap bitmap;
    }
    private String getNormalizedDate(String UtcDate) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
//        format.setTimeZone(TimeZone.getTimeZone("UTC"));
//        Date date = null;
//        try {
//            date = format.parse(Udate);
//        } catch (ParseException e) {
//            Toast.makeText(getContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
//        }
//        if(date == null){
//            return "null";
//        }else{
//            return date.toString();
//        }
        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
        return formatter.format(format.parse(UtcDate));
    }
}
