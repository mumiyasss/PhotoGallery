package com.mumiyasss.photogallery.activity;

import android.support.v4.app.Fragment;

import com.mumiyasss.crimeintent.SingleFragmentActivity;
import com.mumiyasss.photogallery.PhotoGalleryFragment;

import org.jetbrains.annotations.NotNull;

public class PhotoGalleryActivity extends SingleFragmentActivity {
    @NotNull
    @Override
    protected Fragment createFragment() {
        return PhotoGalleryFragment.newInstance();
    }
}
