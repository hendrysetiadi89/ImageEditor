package com.example.nakama.imageeditor.imageeditor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.example.nakama.imageeditor.R;

import java.util.ArrayList;

/**
 * Created by Hendry on 9/25/2017.
 */

public class ImageEditorActivity extends AppCompatActivity implements ImageEditorFragment.OnImageEditorFragmentListener {

    public static final int REQUEST_CODE = 520;
    public static final String EXTRA_IMAGE_URLS = "IMG_URLS";
    public static final String EXTRA_DELETE_CACHE_WHEN_EXIT = "DEL_CACHE";

    public static final String SAVED_IMAGE_INDEX = "IMG_IDX";
    public static final String SAVED_IMAGE_URLS = "SAVED_IMG_URLS";
    public static final String SAVED_ALL_CROPPED_PATHS = "SAVED_CROPPED_PATHS";
    public static final String RESULT_IMAGE_PATH = "RES_PATH";

    private ArrayList<String> imageUrls;
    private ArrayList<String> resultImageUrls;

    // store the cropped paths (and the original), so the unused files can be deleted later
    private ArrayList<String> savedCroppedPaths;

    private int imageIndex;

    private TkpdProgressDialog progressDialog;

    public static void start(Context context, Fragment fragment, ArrayList<String> imageUrls, boolean delCacheWhenExit) {
        Intent intent = createInstance(context, imageUrls, delCacheWhenExit);
        fragment.startActivityForResult(intent, REQUEST_CODE);
    }

    public static void start(Activity activity, ArrayList<String> imageUrls, boolean delCacheWhenExit) {
        Intent intent = createInstance(activity, imageUrls, delCacheWhenExit);
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    public static Intent createInstance(Context context, ArrayList<String> imageUrls, boolean delCacheWhenExit) {
        Intent intent = new Intent(context, ImageEditorActivity.class);
        intent.putExtra(EXTRA_IMAGE_URLS, imageUrls);
        intent.putExtra(EXTRA_DELETE_CACHE_WHEN_EXIT, delCacheWhenExit);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        if (savedInstanceState == null) {
            if (getIntent().hasExtra(EXTRA_IMAGE_URLS)) {
                imageUrls = getIntent().getStringArrayListExtra(EXTRA_IMAGE_URLS);
            } else {
                finish();
                return;
            }
            imageIndex = 0;
            resultImageUrls = new ArrayList<>();
            savedCroppedPaths = new ArrayList<>();
        } else {
            imageIndex = savedInstanceState.getInt(SAVED_IMAGE_INDEX, 0);
            imageUrls = savedInstanceState.getStringArrayList(SAVED_IMAGE_URLS);
            resultImageUrls = savedInstanceState.getStringArrayList(RESULT_IMAGE_PATH);
            savedCroppedPaths = savedInstanceState.getStringArrayList(SAVED_ALL_CROPPED_PATHS);
        }

        if (resultImageUrls == null || resultImageUrls.size() == 0) {
            boolean isNetworkImage = false;
            for (int i = 0, sizei = imageUrls.size(); i < sizei; i++) {
                if (imageUrls.get(i).startsWith("http")) {
                    isNetworkImage = true;
                    break;
                }
            }
            if (isNetworkImage) {
                showProgressDialog();
                ImageDownloadHelper imageDownloadHelper = new ImageDownloadHelper(this);
                imageDownloadHelper.convertHttpPathToLocalPath(imageUrls, true,
                        new ImageDownloadHelper.OnImageDownloadListener() {
                            @Override
                            public void onError(Throwable e) {
                                hideProgressDialog();
                            }

                            @Override
                            public void onSuccess(ArrayList<String> resultLocalPaths) {
                                hideProgressDialog();
                                imageUrls = resultLocalPaths;
                                copyOriginalUrlsToResult();
                                startEditLocalImages();
                            }
                        });
            } else {
                copyOriginalUrlsToResult();
                startEditLocalImages();
            }
        } else {
            copyOriginalUrlsToResult();
            startEditLocalImages();
        }

    }


    private void startEditLocalImages() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.findFragmentByTag(ImageEditorFragment.TAG) == null) {
            replaceEditorFragment(fragmentManager);
        }
        setUpToolbarTitle();
    }

    private void replaceEditorFragment(FragmentManager fragmentManager){
        fragmentManager.beginTransaction()
                .replace(R.id.container, getNewEditorFragment(), ImageEditorFragment.TAG)
                .commit();
    }

    protected ImageEditorFragment getNewEditorFragment(){
        return ImageEditorFragment.newInstance( getImageUrl());
    }

    protected String getImageUrl(){
        return imageUrls.get(imageIndex);
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new TkpdProgressDialog(this, TkpdProgressDialog.NORMAL_PROGRESS);
            progressDialog.setCancelable(false);
        }
        if (!progressDialog.isProgress()) {
            progressDialog.showDialog();
        }
    }

    private void hideProgressDialog() {
        if (!isFinishing() && progressDialog != null && progressDialog.isProgress()) {
            progressDialog.dismiss();
        }
    }

    private void copyOriginalUrlsToResult() {
        resultImageUrls = new ArrayList<>();
        savedCroppedPaths = new ArrayList<>();
        for (int i = 0, sizei = imageUrls.size(); i < sizei; i++) {
            resultImageUrls.add(imageUrls.get(i));
            savedCroppedPaths.add(imageUrls.get(i));
        }
    }

    @Override
    public void onSuccessCrop(String path){
        // save the new path
        if (resultImageUrls == null) {
            return;
        }
        if (imageIndex >= resultImageUrls.size()) {
            imageIndex = resultImageUrls.size() - 1;
        }
        resultImageUrls.set(imageIndex, path);
        addCroppedPath(path);
        imageIndex++;
        if (imageIndex == imageUrls.size()) {
            finishEditing(true);
        } else {
            // continue to next image index
            FragmentManager fragmentManager = getSupportFragmentManager();
            replaceEditorFragment(fragmentManager);
            setUpToolbarTitle();
        }
    }

    public void addCroppedPath(String path){
        savedCroppedPaths.add(path);
    }

    private void finishEditing(boolean isResultOK) {
        Intent intent = new Intent();
        if (isResultOK) {
            setResult(Activity.RESULT_OK, intent);
            intent.putExtra(RESULT_IMAGE_PATH, resultImageUrls);
            if (getIntent().getBooleanExtra(EXTRA_DELETE_CACHE_WHEN_EXIT, true)) {
                deleteAllTkpdFilesNotInResult(savedCroppedPaths, resultImageUrls);
            }
        } else {
            setResult(Activity.RESULT_CANCELED, intent);
            intent.putExtra(RESULT_IMAGE_PATH, getIntent().getStringArrayListExtra(EXTRA_IMAGE_URLS));
            if (getIntent().getBooleanExtra(EXTRA_DELETE_CACHE_WHEN_EXIT, true)) {
                deleteAllTkpdFilesNotInResult(savedCroppedPaths, getIntent().getStringArrayListExtra(EXTRA_IMAGE_URLS));
            }
        }
        finish();
    }

    private void deleteAllTkpdFilesNotInResult(ArrayList<String> savedCroppedPaths, ArrayList<String> resultImageUrls){
        ArrayList<String> toBeDeletedFiles = new ArrayList<>();
        for (int i=0, sizei = savedCroppedPaths.size(); i<sizei; i++) {
            String savedCroppedPath = savedCroppedPaths.get(i);
            boolean croppedFilesIsInResult = false;
            for (int j = 0, sizej = resultImageUrls.size(); j<sizej; j++) {
                if (savedCroppedPath.equals(resultImageUrls.get(j))) {
                    croppedFilesIsInResult = true;
                    break;
                }
            }
            if (!croppedFilesIsInResult) {
                toBeDeletedFiles.add(savedCroppedPath);
            }
        }
        FileUtils.deleteAllCacheTkpdFiles(toBeDeletedFiles);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() != 0) {
            imageIndex--;
            setUpToolbarTitle();
            getSupportFragmentManager().popBackStack();
        } else {
            finishEditing(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }

    private void setUpToolbarTitle() {
        String title = getString(R.string.title_activity_image_edit);
        if (imageUrls.size() > 1) {
            title += "(" + (imageIndex + 1) + "/" + imageUrls.size() + ")";
        }
        getSupportActionBar().setTitle(title);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVED_IMAGE_INDEX, imageIndex);
        outState.putStringArrayList(RESULT_IMAGE_PATH, resultImageUrls);
        outState.putStringArrayList(SAVED_IMAGE_URLS, imageUrls);
        outState.putStringArrayList(SAVED_ALL_CROPPED_PATHS, savedCroppedPaths);
    }
}
