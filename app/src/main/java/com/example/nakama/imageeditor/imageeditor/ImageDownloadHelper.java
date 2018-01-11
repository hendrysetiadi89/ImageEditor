package com.example.nakama.imageeditor.imageeditor;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Class to separate the logic download - from image urls to tokopedia local paths
 * Created by hendry on 8/8/2017.
 */

public class ImageDownloadHelper {

    private Context context;

    private static final int WIDTH_DOWNLOAD = 2048;
    private static final int DEF_WIDTH_CMPR = 2048;
    private static final int DEF_QLTY_COMPRESS = 95;

    private boolean needCompressTkpd = false;

    private OnImageDownloadListener onImageDownloadListener;
    public interface OnImageDownloadListener{
        void onError(Throwable e);
        void onSuccess(ArrayList<String> localPaths);
    }
    public ImageDownloadHelper(Context context){
        this.context = context;
    }

    public void convertHttpPathToLocalPath(List<String> urlsToDownload,
                                           boolean needCompressTkpd,
                                           OnImageDownloadListener onImageDownloadListener) {
        this.needCompressTkpd = needCompressTkpd;
        this.onImageDownloadListener= onImageDownloadListener;
        downloadImages(urlsToDownload)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .unsubscribeOn(Schedulers.io())
                .subscribe(
                        new Subscriber<List<File>>() {
                            @Override
                            public void onCompleted() {
                            }

                            @Override
                            public void onError(Throwable e) {
                                if (ImageDownloadHelper.this.onImageDownloadListener!= null) {
                                    ImageDownloadHelper.this.onImageDownloadListener.onError(e);
                                }
                            }

                            @Override
                            public void onNext(List<File> files) {
                                ArrayList<String> resultLocalPaths = new ArrayList<>();
                                if (files == null || files.size() == 0) {
                                    throw new NullPointerException();
                                }
                                for (int i = 0, sizei = files.size(); i < sizei; i++) {
                                    resultLocalPaths.add(files.get(i).getAbsolutePath());
                                }
                                if (ImageDownloadHelper.this.onImageDownloadListener != null) {
                                    ImageDownloadHelper.this.onImageDownloadListener.onSuccess(resultLocalPaths);
                                }
                            }
                        }
                );
    }

    private Observable<List<File>> downloadImages(final List<String> urls) {
        // use concat map to preserve the ordering
        return Observable.from(urls)
                .concatMap(new Func1<String, Observable<File>>() {
                    @Override
                    public Observable<File> call(String url) {
                        return downloadObservable(url);
                    }
                }).toList();
    }

    @NonNull
    private Observable<File> downloadObservable(String url) {
        return Observable.just(url)
                .map(new Func1<String, File>() {
                    @Override
                    public File call(String url) {
                        if (context == null ){
                            return null;
                        }
                        if (url.startsWith("http")) {
                            FutureTarget<File> future = Glide.with(context)
                                    .load(url)
                                    .downloadOnly(WIDTH_DOWNLOAD, WIDTH_DOWNLOAD);
                            try {
                                File cacheFile = future.get();
                                String cacheFilePath = cacheFile.getAbsolutePath();
                                File photo;
                                if (needCompressTkpd) {
                                    String fileNameToMove = FileUtils.generateUniqueFileName();
                                    photo = FileUtils.writeImageToTkpdPath(
                                            FileUtils.compressImage(
                                                    cacheFilePath, DEF_WIDTH_CMPR, DEF_WIDTH_CMPR, DEF_QLTY_COMPRESS),
                                            fileNameToMove);
                                } else {
                                    photo = writeImageToTkpdPath(cacheFile);
                                }
                                if (photo != null) {
                                    return photo;
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                                throw new RuntimeException(e.getMessage());
                            }
                            return null;
                        } else {
                            return new File(url);
                        }
                    }
                });
    }

    private File writeImageToTkpdPath(File source) {
        InputStream inStream;
        OutputStream outStream;
        File dest = null;
        try {

            File directory = new File(FileUtils.getFolderPathForUploadNoRand(Environment.getExternalStorageDirectory().getAbsolutePath()));
            if (!directory.exists()) {
                directory.mkdirs();
            }
            dest = new File(directory.getAbsolutePath() + FileUtils.generateUniqueFileName() );

            inStream = new FileInputStream(source);
            outStream = new FileOutputStream(dest);

            byte[] buffer = new byte[1024];

            int length;
            //copy the file content in bytes
            while ((length = inStream.read(buffer)) > 0) {

                outStream.write(buffer, 0, length);

            }

            inStream.close();
            outStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return dest;
    }
}
