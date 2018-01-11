package com.example.nakama.imageeditor.imageeditor;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.drawable.AnimationDrawable;
import android.view.View;
import android.widget.ImageView;

import com.example.nakama.imageeditor.R;

public class TkpdProgressDialog {

    public static int NORMAL_PROGRESS = 1;
    public static int MAIN_PROGRESS = 2;
    private Context context;
    private int state;
    private int substate = 0;
    private String msgLoading;
    private ProgressDialog progress;
    private ImageView LoadingIcon;
    private AnimationDrawable loadingAnimation;
    private View view;
    private Boolean isShow = false;
    private int loadingViewId;
    private Boolean isCancelable = false;

    public TkpdProgressDialog(Context context, int state, String msg) {
        this.context = context;
        this.state = state;
        msgLoading = msg;
        substate = 0;
    }

    public TkpdProgressDialog(Context context, int state) {
        this.context = context;
        this.state = state;
        msgLoading = "Loading...";
        substate = 0;
    }

    public TkpdProgressDialog(Context context, int state, View view) {
        this.context = context;
        this.state = 3;
        this.view = view;
        this.state = state;
        substate = 1;
    }

    public void setLoadingViewId(int loadingViewId) {
        this.loadingViewId = loadingViewId;
    }

    public void showDialog() {
        try {
            if (state == NORMAL_PROGRESS) {
                if (!isShow) {
                    progress = new ProgressDialog(context);
                    progress.setMessage(msgLoading);
                    progress.setTitle("");
                    progress.setCancelable(isCancelable);
                    isShow = true;
                    progress.show();
                    isShow = true;
                }
            } else if (state == MAIN_PROGRESS && substate == 0) {
                progress = new ProgressDialog(context, R.style.CoolDialog);
                progress.show();
                progress.setContentView(R.layout.loader);
                progress.setCancelable(isCancelable);
                progress.setOnCancelListener(new OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        ((Activity) context).finish();
                    }
                });
                isShow = true;
            } else {
                view.findViewById(loadingViewId).setVisibility(View.VISIBLE);
                isShow = true;
            }
        } catch (Exception e) {
        }
    }

    public Boolean getCancelable() {
        return isCancelable;
    }

    public void dismiss() {
        if (state == MAIN_PROGRESS && substate == 1) {
            view.findViewById(loadingViewId).setVisibility(View.GONE);
        } else {
            if (progress != null) {
                isShow = false;
                if (progress.isShowing()) progress.dismiss();
            }
        }
    }

    public Boolean isProgress() {
        return isShow;
    }

    public void setCancelable(boolean isCancelable) {
        this.isCancelable = isCancelable;
    }

    public ProgressDialog getProgress() {
        return progress;
    }
}