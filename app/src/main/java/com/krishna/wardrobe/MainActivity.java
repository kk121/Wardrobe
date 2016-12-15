package com.krishna.wardrobe;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.krishna.wardrobe.models.FavouriteItems;
import com.krishna.wardrobe.models.WardrobeItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final int REQ_PERMISSION = 555;
    private static final int REQUEST_CAMERA_SHIRT = 515;
    private static final int REQUEST_CAMERA_TROUSER = 516;
    private static final int REQUEST_GALLERY_SHIRT = 517;
    private static final int REQUEST_GALLERY_TROUSER = 518;
    private static final String KEY_CURRENT_SHIRT = "cur_shirt";
    private static final String KEY_CURRENT_TROUSER = "cur_trouser";
    private static final int TYPE_SHIRT = 1;
    private static final int TYPE_TROUSER = 2;
    private int curShirt = 0;
    private int curTrouser = 0;

    private ViewPager viewPagerShirt;
    private ViewPager viewPagerTrouser;
    private ImageButton btnFav;
    private ProgressBar progressBar;
    private View btnShuffle;
    private String mCurrentPhotoPath;
    private ArrayList<WardrobeItem> shirtList = new ArrayList<>();
    private ArrayList<WardrobeItem> trouserList = new ArrayList<>();
    private boolean isFavCombination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            curShirt = savedInstanceState.getInt(KEY_CURRENT_SHIRT, 0);
            curTrouser = savedInstanceState.getInt(KEY_CURRENT_TROUSER, 0);
            Log.d(TAG, "onCreate: ");
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_add_shirt).setOnClickListener(this);
        findViewById(R.id.btn_add_trouser).setOnClickListener(this);
        btnShuffle = findViewById(R.id.btn_shuffle);
        btnShuffle.setOnClickListener(this);
        btnFav = (ImageButton) findViewById(R.id.btn_fav);
        btnFav.setOnClickListener(this);
        View emptyState = findViewById(R.id.tv_empty_state);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        viewPagerShirt = (ViewPager) findViewById(R.id.pager_shirt);
        viewPagerTrouser = (ViewPager) findViewById(R.id.pager_trouser);
        viewPagerShirt.setAdapter(new ViewPagerAdapter(MainActivity.this, shirtList, emptyState));
        viewPagerTrouser.setAdapter(new ViewPagerAdapter(MainActivity.this, trouserList, emptyState));
        setupOnPageChangeListener();

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(AlarmReceiver.FROM_NOTIFICATION))
            showRandomCombination();

        fetchAllShirts();
        fetchAllTrousers();
        scheduleAlarmForDailyNotification();
    }

    private void scheduleAlarmForDailyNotification() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        Intent receiverIntent = new Intent(getApplicationContext(), AlarmReceiver.class);
        calendar.set(Calendar.HOUR_OF_DAY, 6);
        Long calTime = calendar.getTimeInMillis();
        if (calTime < System.currentTimeMillis())
            calTime += 1000 * 60 * 60 * 24L;
        PendingIntent alarmIntent1 = PendingIntent.getBroadcast(getApplicationContext(), 1, receiverIntent, 0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calTime,
                AlarmManager.INTERVAL_DAY, alarmIntent1);
    }

    private void fetchAllShirts() {
        new AsyncTask<Void, Void, List<WardrobeItem>>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected List<WardrobeItem> doInBackground(Void... voids) {
                return DatabaseHelper.getInstance(MainActivity.this).queryForAllShirts();
            }

            @Override
            protected void onPostExecute(List<WardrobeItem> itemList) {
                shirtList.clear();
                shirtList.addAll(itemList);
                viewPagerShirt.getAdapter().notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                makeActionButtonsVisible();
            }
        }.execute();
    }

    private void fetchAllTrousers() {
        new AsyncTask<Void, Void, List<WardrobeItem>>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected List<WardrobeItem> doInBackground(Void... voids) {
                return DatabaseHelper.getInstance(MainActivity.this).queryForAllTrousers();
            }

            @Override
            protected void onPostExecute(List<WardrobeItem> itemList) {
                trouserList.clear();
                trouserList.addAll(itemList);
                viewPagerTrouser.getAdapter().notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                makeActionButtonsVisible();
                viewPagerShirt.setCurrentItem(curShirt);
                viewPagerTrouser.setCurrentItem(curTrouser);
                changeFavButtonColor();
            }
        }.execute();
    }

    private void makeActionButtonsVisible() {
        if (trouserList.size() > 0 && shirtList.size() > 0) {
            btnFav.setVisibility(View.VISIBLE);
            btnShuffle.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_CURRENT_SHIRT, viewPagerShirt.getCurrentItem());
        outState.putInt(KEY_CURRENT_TROUSER, viewPagerTrouser.getCurrentItem());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        curShirt = savedInstanceState.getInt(KEY_CURRENT_SHIRT, 0);
        curTrouser = savedInstanceState.getInt(KEY_CURRENT_TROUSER, 0);
        super.onRestoreInstanceState(savedInstanceState);
    }

    private void setupOnPageChangeListener() {
        ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                changeFavButtonColor();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        };
        viewPagerShirt.addOnPageChangeListener(pageChangeListener);
        viewPagerTrouser.addOnPageChangeListener(pageChangeListener);
    }

    private void changeFavButtonColor() {
        if (shirtList.size() == 0 || trouserList.size() == 0) return;
        isFavCombination = DatabaseHelper.getInstance(MainActivity.this).isFavCombination(shirtList.get(viewPagerShirt.getCurrentItem()).imagePath, trouserList.get(viewPagerTrouser.getCurrentItem()).imagePath);
        if (isFavCombination)
            btnFav.setImageResource(R.drawable.ic_fav_filled);
        else
            btnFav.setImageResource(R.drawable.ic_fav);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_add_shirt:
                showFilterPopup(view, REQUEST_GALLERY_SHIRT, REQUEST_CAMERA_SHIRT);
                break;
            case R.id.btn_add_trouser:
                showFilterPopup(view, REQUEST_GALLERY_TROUSER, REQUEST_CAMERA_TROUSER);
                break;
            case R.id.btn_fav:
                removeOrAddFavourite((ImageButton) view);
                break;
            case R.id.btn_shuffle:
                showRandomCombination();
                break;
        }
    }

    // Display anchored popup menu based on view selected
    private void showFilterPopup(View v, final int galleryReqCode, final int cameraReqCode) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.popup_filters, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_gallery:
                        requestImageFromGallery(galleryReqCode);
                        return true;
                    case R.id.menu_camera:
                        requestImageFromCamera(cameraReqCode);
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.show();
    }

    private void removeOrAddFavourite(ImageButton view) {
        String shirtImage = shirtList.get(viewPagerShirt.getCurrentItem()).imagePath;
        String trouserImage = trouserList.get(viewPagerTrouser.getCurrentItem()).imagePath;
        if (isFavCombination) {
            int n = DatabaseHelper.getInstance(MainActivity.this).removeFavourite(shirtImage, trouserImage);
            if (n > 0) {
                isFavCombination = false;
                view.setImageResource(R.drawable.ic_fav);
                showToast(getString(R.string.txt_fav_mark_removed));
            }
        } else {
            boolean inserted = DatabaseHelper.getInstance(this).addFavouriteEntry(shirtImage, trouserImage);
            if (inserted) {
                isFavCombination = true;
                view.setImageResource(R.drawable.ic_fav_filled);
                showToast(getString(R.string.txt_fav_marked));
            }
        }
    }

    private void showToast(String message) {
        Toast toast = Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT);
        toast.getView().findViewById(android.R.id.message).setBackgroundColor(ContextCompat.getColor(this, R.color.transparent));
        toast.show();
    }

    private void showRandomCombination() {
        Random random = new Random();
        FavouriteItems item;
        // if next random is 0 then fetch random item from favourite items
        // else fetch from general shirt and trouser table
        if (random.nextInt() == 0)
            item = DatabaseHelper.getInstance(this).randomQueryForFavourite();
        else
            item = DatabaseHelper.getInstance(this).randomQueryForCombination();
        shirtList.add(new WardrobeItem(item.id, item.shirt));
        trouserList.add(new WardrobeItem(item.id, item.trouser));
        viewPagerShirt.getAdapter().notifyDataSetChanged();
        viewPagerTrouser.getAdapter().notifyDataSetChanged();
        viewPagerShirt.setCurrentItem(viewPagerShirt.getAdapter().getCount() - 1);
        viewPagerTrouser.setCurrentItem(viewPagerTrouser.getAdapter().getCount() - 1);
    }

    private void requestImageFromGallery(int requestCode) {
        if (!checkForPermission()) return;
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, requestCode);
    }

    private boolean checkForPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;
        else if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_PERMISSION);
        }
        return false;
    }

    private void requestImageFromCamera(int requestCode) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            Log.d(TAG, "requestImageFromCamera: ");
            File photoFile = null;
            try {
                photoFile = Utils.createTempImageFile(this);
                mCurrentPhotoPath = photoFile.getAbsolutePath();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                Log.d(TAG, "requestImageFromCamera: " + photoFile);
                grantUriPermission(takePictureIntent, photoURI);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, requestCode);
            }
        }
    }

    /* FileProvider bug in pre-lollipop devices. Grant uri permission for all packages that might need it */
    private void grantUriPermission(Intent intent, Uri uri) {
        List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String path;
            long id;
            switch (requestCode) {
                case REQUEST_CAMERA_SHIRT:
                    path = mCurrentPhotoPath;
                    saveImageToDB(path, TYPE_SHIRT);
                    break;
                case REQUEST_CAMERA_TROUSER:
                    path = mCurrentPhotoPath;
                    saveImageToDB(path, TYPE_TROUSER);
                    break;
                case REQUEST_GALLERY_SHIRT:
                    path = getAbsolutePath(data.getData());
                    saveImageToDB(path, TYPE_SHIRT);
                    break;
                case REQUEST_GALLERY_TROUSER:
                    path = getAbsolutePath(data.getData());
                    saveImageToDB(path, TYPE_TROUSER);
                    break;
            }
        }
    }

    public String getAbsolutePath(Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DATA}; //MediaStore.MediaColumns.DATA
        String path = null;
        Log.d(TAG, "getAbsolutePath: ");
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(projection[0]);
            cursor.moveToFirst();
            path = cursor.getString(column_index);
            Log.d(TAG, "getAbsolutePath: " + path);
            cursor.close();
        }
        if (path == null) {
            path = uri.getPath();
        }
        Log.d(TAG, "getAbsolutePath: " + path);
        return path;
    }

    /* Downscale the image if it's big before saving to internal directory and then save its path to database */
    private void saveImageToDB(final String absolutePath, final int itemType) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                Bitmap imageBitmap = Utils.getDecodedBitmap(MainActivity.this, absolutePath);
                if (imageBitmap == null) {
                    return null;
                }
                FileOutputStream outputStream = null;
                File file = null;
                try {
                    file = Utils.createImageFile(MainActivity.this);
                    outputStream = new FileOutputStream(file);
                    imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    return file.getAbsolutePath();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    if (outputStream != null) try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(String path) {
                super.onPostExecute(path);
                if (path == null) {
                    showToast("Error! Unable to read image");
                    return;
                } else if (itemType == TYPE_TROUSER) {
                    long id = DatabaseHelper.getInstance(MainActivity.this).addTrouserEntry(path);
                    if (id > -1) {
                        trouserList.add(new WardrobeItem((int) id, path));
                        viewPagerTrouser.getAdapter().notifyDataSetChanged();
                        viewPagerTrouser.setCurrentItem(viewPagerTrouser.getAdapter().getCount() - 1);
                    }
                } else {
                    long id = DatabaseHelper.getInstance(MainActivity.this).addShirtEntry(path);
                    if (id > -1) {
                        shirtList.add(new WardrobeItem((int) id, path));
                        viewPagerShirt.getAdapter().notifyDataSetChanged();
                        viewPagerShirt.setCurrentItem(viewPagerShirt.getAdapter().getCount() - 1);
                    }
                }
                makeActionButtonsVisible();
            }
        }.execute();
    }
}
