package com.krishna.wardrobe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.krishna.wardrobe.models.WardrobeItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * Created by krishna on 13/12/16.
 */

public class ViewPagerAdapter extends PagerAdapter {
    private static final String TAG = "ViewPagerAdapter";
    private List<WardrobeItem> wardrobeItemList;
    private LayoutInflater inflater;
    private View emptyState;

    public ViewPagerAdapter(Context context, List<WardrobeItem> wardrobeItems, View emptyState) {
        wardrobeItemList = wardrobeItems;
        inflater = LayoutInflater.from(context);
        this.emptyState = emptyState;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View layout = inflater.inflate(R.layout.item_view_pager, container, false);
        ImageView imageView = (ImageView) layout.findViewById(R.id.image);
        retrieveImageAndSetToView(wardrobeItemList.get(position).imagePath, imageView);
        container.addView(layout);
        return layout;
    }

    private void retrieveImageAndSetToView(final String path, final ImageView imageView) {
        final File imageFile = new File(path);
        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... voids) {
                Bitmap image = null;
                try {
                    image = BitmapFactory.decodeStream(new FileInputStream(imageFile));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                return image;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                super.onPostExecute(bitmap);
                if (imageView != null)
                    imageView.setImageBitmap(bitmap);
            }
        }.execute();
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public int getCount() {
        if (wardrobeItemList != null) {
            if (wardrobeItemList.size() > 0) emptyState.setVisibility(View.GONE);
            return wardrobeItemList.size();
        }
        return 0;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }
}
