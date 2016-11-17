package example.fussen.image;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.List;

/**
 * Created by Bodyplus on 2016/4/7.
 */
public class PhotoAdapter extends BaseAdapter {


    private Context context;

    private List<Bitmap> mData;

    public PhotoAdapter(Context context, List<Bitmap> data) {
        this.context = context;

        this.mData = data;

    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = View.inflate(context, R.layout.item_my_photo, null);

        ImageView image = (ImageView) view.findViewById(R.id.iv_image);
        image.setImageBitmap(mData.get(position));
        return view;
    }

}

