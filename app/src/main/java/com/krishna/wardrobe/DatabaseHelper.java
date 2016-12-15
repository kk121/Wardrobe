package com.krishna.wardrobe;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.krishna.wardrobe.models.FavouriteItems;
import com.krishna.wardrobe.models.WardrobeItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by krishna on 13/12/16.
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "wardrobe.db";
    private static final int DATABASE_VERSION = 1;

    private static DatabaseHelper databaseHelper;

    private static final String TABLE_SHIRT = "table_shirt";
    public static final String COLUMN_SHIRT_PATH = "shirt_path";
    private static final String COLUMN_ID = "_id";
    private static final String TABLE_TROUSER = "table_trouser";
    public static final String COLUMN_TROUSER_PATH = "trouser_path";
    private static final String TABLE_FAVOURITE = "table_favourite";

    private static final String CREATE_TABLE_SHIRT = "CREATE TABLE " + TABLE_SHIRT + "(" +
            COLUMN_ID + " integer primary key autoincrement," +
            COLUMN_SHIRT_PATH + " text);";
    private static final String CREATE_TABLE_TROUSER = "CREATE TABLE " + TABLE_TROUSER + "(" +
            COLUMN_ID + " integer primary key autoincrement," +
            COLUMN_TROUSER_PATH + " text);";
    private static final String CREATE_TABLE_FAVOURITE = "CREATE TABLE " + TABLE_FAVOURITE + "(" +
            COLUMN_ID + " integer primary key autoincrement," +
            COLUMN_SHIRT_PATH + " text," +
            COLUMN_TROUSER_PATH + " text);";

    private DatabaseHelper(Context context) {
        this(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public static DatabaseHelper getInstance(Context context) {
        if (databaseHelper == null) {
            synchronized (DATABASE_NAME) {
                databaseHelper = new DatabaseHelper(context);
            }
        }
        return databaseHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(CREATE_TABLE_SHIRT);
        sqLiteDatabase.execSQL(CREATE_TABLE_TROUSER);
        sqLiteDatabase.execSQL(CREATE_TABLE_FAVOURITE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        String dropTable = "DROP TABLE IF EXISTS ";
        sqLiteDatabase.execSQL(dropTable + TABLE_SHIRT);
        sqLiteDatabase.execSQL(dropTable + TABLE_TROUSER);
        sqLiteDatabase.execSQL(dropTable + TABLE_FAVOURITE);
        onCreate(sqLiteDatabase);
    }

    public long addShirtEntry(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return -1;
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SHIRT_PATH, imagePath);
        long id = database.insert(TABLE_SHIRT, null, values);
        database.close();
        return id;
    }

    public List<WardrobeItem> queryForAllShirts() {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_SHIRT, new String[]{COLUMN_ID, COLUMN_SHIRT_PATH}, null, null, null, null, null);
        return createWardrobeItemList(database, cursor);
    }

    public long addTrouserEntry(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) return -1;
        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TROUSER_PATH, imagePath);
        long id = database.insert(TABLE_TROUSER, null, values);
        database.close();
        return id;
    }

    public List<WardrobeItem> queryForAllTrousers() {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_TROUSER, new String[]{COLUMN_ID, COLUMN_TROUSER_PATH}, null, null, null, null, null);
        return createWardrobeItemList(database, cursor);
    }

    public boolean addFavouriteEntry(String shirtImagePath, String trouserImagePath) {
        if (isFavCombination(shirtImagePath, trouserImagePath)) return false;

        SQLiteDatabase database = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SHIRT_PATH, shirtImagePath);
        values.put(COLUMN_TROUSER_PATH, trouserImagePath);
        database.insert(TABLE_FAVOURITE, null, values);
        database.close();
        return true;
    }

    public int removeFavourite(String shirtImagePath, String trouserImagePath) {
        SQLiteDatabase database = getWritableDatabase();
        int n = database.delete(TABLE_FAVOURITE, COLUMN_SHIRT_PATH + "=? AND " + COLUMN_TROUSER_PATH + "=?", new String[]{shirtImagePath, trouserImagePath});
        database.close();
        return n;
    }

    public FavouriteItems randomQueryForFavourite() {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_FAVOURITE, new String[]{COLUMN_ID, COLUMN_SHIRT_PATH, COLUMN_TROUSER_PATH}, null, null, null, null, "RANDOM()");
        FavouriteItems favItem = new FavouriteItems();
        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
            favItem.id = cursor.getInt(0);
            favItem.shirt = cursor.getString(1);
            favItem.trouser = cursor.getString(2);
            cursor.close();
        }
        database.close();
        return favItem;
    }

    public FavouriteItems randomQueryForCombination() {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursorShirt = database.query(TABLE_SHIRT, new String[]{COLUMN_ID, COLUMN_SHIRT_PATH}, null, null, null, null, "RANDOM() LIMIT 1");
        Cursor cursorTrouser = database.query(TABLE_TROUSER, new String[]{COLUMN_ID, COLUMN_TROUSER_PATH}, null, null, null, null, "RANDOM() LIMIT 1");
        FavouriteItems favItem = new FavouriteItems();
        if (cursorShirt != null && cursorShirt.getCount() > 0 && cursorShirt.moveToFirst()) {
            favItem.shirt = cursorShirt.getString(1);
            cursorShirt.close();
        }
        if (cursorTrouser != null && cursorTrouser.getCount() > 0 && cursorTrouser.moveToFirst()) {
            favItem.trouser = cursorTrouser.getString(1);
            cursorTrouser.close();
        }
        database.close();
        return favItem;
    }

    private List<WardrobeItem> createWardrobeItemList(SQLiteDatabase database, Cursor cursor) {
        List<WardrobeItem> itemList = new ArrayList<>();
        if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
            do {
                WardrobeItem item = new WardrobeItem();
                item.id = cursor.getInt(0);
                item.imagePath = cursor.getString(1);
                itemList.add(item);
            } while (cursor.moveToNext());
            cursor.close();
            database.close();
        }
        return itemList;
    }

    public boolean isFavCombination(String shirt, String trouser) {
        SQLiteDatabase database = getReadableDatabase();
        Cursor cursor = database.query(TABLE_FAVOURITE, new String[]{COLUMN_ID}, COLUMN_SHIRT_PATH + "=? AND " + COLUMN_TROUSER_PATH + "=?", new String[]{shirt, trouser}, null, null, null);
        boolean isFav = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();
        database.close();
        return isFav;
    }
}
