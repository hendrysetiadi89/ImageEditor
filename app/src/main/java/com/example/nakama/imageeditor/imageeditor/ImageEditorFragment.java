package com.example.nakama.imageeditor.imageeditor;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.nakama.imageeditor.R;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;

/**
 * Created by User on 9/25/2017.
 */

public class ImageEditorFragment extends Fragment implements CropImageView.OnSetImageUriCompleteListener, CropImageView.OnCropImageCompleteListener {
    public static final String TAG = ImageEditorFragment.class.getSimpleName();
    protected static final String ARG_LOCAL_PATH = "loc_pth";
    private static final String SAVED_PATH = "svd_path";
    private static final int CROP_COMPRESSION = 100;

    protected CropImageView mCropImageView;
    private String localPath;

    OnImageEditorFragmentListener onImageEditorFragmentListener;
    private String croppedPath;

    public interface OnImageEditorFragmentListener {
        void onSuccessCrop(String localPath);

        void addCroppedPath(String croppedPath);
    }

    public static ImageEditorFragment newInstance(String localPath) {
        Bundle args = new Bundle();
        args.putString(ARG_LOCAL_PATH, localPath);
        ImageEditorFragment fragment = new ImageEditorFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        localPath = getArguments().getString(ARG_LOCAL_PATH);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            croppedPath = savedInstanceState.getString(SAVED_PATH);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image_editor, container, false);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mCropImageView = (CropImageView) view.findViewById(R.id.cropImageView);
        mCropImageView.setOnSetImageUriCompleteListener(this);
        mCropImageView.setOnCropImageCompleteListener(this);

        final File imgFile = new File(localPath);
        if (imgFile.exists()) {
            mCropImageView.post(new Runnable() {
                @Override
                public void run() {
                    mCropImageView.setImageUriAsync(Uri.fromFile(imgFile));
                }
            });
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_image_editor, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.main_action_crop) {
            // no need to crop if the rect is same and in local tkpd already
            if (checkIfSameWithPrevImage()) {
//                UnifyTracking.eventClickSaveEditImageProduct(AppEventTracking.ImageEditor.NO_ACTION);
                onImageEditorFragmentListener.onSuccessCrop(localPath);
            } else {
                boolean isRotate = mCropImageView.getRotatedDegrees() != 0;
                boolean isCrop = !mCropImageView.getCropRect().equals(mCropImageView.getWholeImageRect());
//                if (isRotate) {
//                    UnifyTracking.eventClickSaveEditImageProduct(AppEventTracking.ImageEditor.ROTATE);
//                }
//                if (isCrop) {
//                    UnifyTracking.eventClickSaveEditImageProduct(AppEventTracking.ImageEditor.CROP);
//                }

//                if (this instanceof ImageEditorWatermarkFragment) {
//                    if (((ImageEditorWatermarkFragment)this).isUseWatermark()) {
//                        UnifyTracking.eventClickSaveEditImageProduct(AppEventTracking.ImageEditor.WATERMARK);
//                    }
//                }

                File file = FileUtils.getTkpdImageCacheFile(FileUtils.generateUniqueFileName());
                croppedPath = file.getAbsolutePath();
                mCropImageView.startCropWorkerTask(0, 0, CropImageView.RequestSizeOptions.NONE,
                        Uri.fromFile(file), Bitmap.CompressFormat.PNG, CROP_COMPRESSION);
            }
            return true;
        } else if (item.getItemId() == R.id.main_action_rotate) {
            mCropImageView.rotateImage(90);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected boolean checkIfSameWithPrevImage() {
        return mCropImageView.getRotatedDegrees() == 0 &&
                (mCropImageView.getCropRect() == null ||
                        mCropImageView.getCropRect().equals(mCropImageView.getWholeImageRect())) &&
                FileUtils.isInTkpdCache(new File(localPath));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mCropImageView != null) {
            mCropImageView.setOnSetImageUriCompleteListener(null);
            mCropImageView.setOnCropImageCompleteListener(null);
        }
    }

    @Override
    public void onSetImageUriComplete(CropImageView view, Uri uri, Exception error) {
        // no op
    }

    @Override
    public void onCropImageComplete(CropImageView view, CropImageView.CropResult result) {
        handleCropResult(result);
    }

    private void handleCropResult(CropImageView.CropResult result) {
        if (result.getError() == null) {
            Uri uri = result.getUri();
            if (uri == null) {
                Bitmap bitmap = result.getBitmap();
                if (bitmap != null) {
                    bitmap = processBitmap(bitmap);
                    File file = FileUtils.writeImageToTkpdPath(bitmap, FileUtils.generateUniqueFileName());
                    if (file != null && file.exists()) {
                        String path = file.getAbsolutePath();
                        onImageEditorFragmentListener.onSuccessCrop(path);
                    }
                }
            } else {
                if (!TextUtils.isEmpty(croppedPath)) {
                    croppedPath = processCroppedPath(croppedPath);
                    onImageEditorFragmentListener.onSuccessCrop(croppedPath);
                }
            }
        } else {
            Log.e("AIC", "Failed to crop image", result.getError());
            Toast.makeText(getActivity(), "Image crop failed: " + result.getError().getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // will be override on child
    protected Bitmap processBitmap(Bitmap bitmap) {
        return bitmap;
    }

    // will be override on child
    protected String processCroppedPath(String croppedPath) {
        return croppedPath;
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void onAttach(Activity activity) {
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onAttachListener(activity);
        }
    }

    @TargetApi(23)
    @Override
    public final void onAttach(Context context) {
        super.onAttach(context);
        onAttachListener(context);
    }

    protected void onAttachListener(Context context) {
        onImageEditorFragmentListener = (OnImageEditorFragmentListener) context;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_PATH, croppedPath);
    }
}
