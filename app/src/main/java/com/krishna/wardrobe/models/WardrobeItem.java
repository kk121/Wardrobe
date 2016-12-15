package com.krishna.wardrobe.models;

/**
 * Created by krishna on 13/12/16.
 */

public class WardrobeItem {
    public int id;
    public String imagePath;

    public WardrobeItem() {
    }

    public WardrobeItem(int id, String imagePath) {
        this.id = id;
        this.imagePath = imagePath;
    }
}
