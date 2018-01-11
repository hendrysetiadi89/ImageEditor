package com.example.nakama.imageeditor.imageeditor;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by m.normansyah on 12/8/15.
 */
public class FileUtils {
    public static final String CACHE_TOKOPEDIA = "/cache/tokopedia/";
    public static final String PNG = ".png";
    public static final String PACKAGE_NAME = "tokopedia";

    /**
     * example of result : /storage/emulated/0/Android/data/com.tokopedia.tkpd/1451274244/
     *
     * @param root
     * @return
     */
    public static String getFolderPathForUpload(String root) {
        return root + "/Android/data/" + PACKAGE_NAME + "/" + (System.currentTimeMillis() / 1000L) + new Random().nextInt(1000) + "/";
    }

    public static String getFolderPathForUploadNoRand(String root) {
        return root + "/Android/data/" + PACKAGE_NAME + "/";
    }

    public static String getFileNameWithoutExt(String path) {
        String fileName = path.substring(path.lastIndexOf("/") + 1);
        int pos = fileName.lastIndexOf(".");
        if (pos > 0) {
            fileName = fileName.substring(0, pos);
        }
        return fileName;
    }

    public static String generateUniqueFileName() {
        return String.valueOf(System.currentTimeMillis() / 1000L) + new Random().nextInt(1000);
    }

    public static String generateUniqueFileName(String path) {
        return String.valueOf(path.hashCode()).replaceAll("-", "");
    }

    /**
     * will wrte the buffer to Tkpdpath with the filename supply. Extension will be .jpg
     * example of result : /storage/emulated/0/Android/data/com.tokopedia.tkpd/cache/tokopedia/IMG_451274244.jpg
     *
     * @param buffer   result of compressed image in jpeg
     * @param fileName name of file to write to Tkpd Path
     * @return
     */
    public static File writeImageToTkpdPath(byte[] buffer, String fileName) {
        if (buffer != null) {
            File photo = getTkpdImageCacheFile(fileName);
            if (photo.exists()) {
                // photo already exist in cache
                if (photo.length() == buffer.length) {
                    return photo;
                } else { // the length is different, delete it and write the new one
                    photo.delete();
                    if (writeBufferToFile(buffer, photo.getPath())) {
                        return photo;
                    }
                }
            } else {
                if (writeBufferToFile(buffer, photo.getPath())) {
                    return photo;
                }
            }
        }
        return null;
    }

    public static File writeImageToTkpdPath(Bitmap bitmap, String fileName) {
        if (bitmap != null) {
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            byte[] bytes;
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bao);
            bytes = bao.toByteArray();
            return writeImageToTkpdPath(bytes, fileName);
        } else {
            return null;
        }
    }

    public static File writeImageToTkpdPath(InputStream source, String fileName) {
        File photo = getTkpdImageCacheFile(fileName);

        if (photo.exists()) {
            photo.delete();
        }
        if (writeStreamToFile(source, photo)) {
            return photo;
        }

        return null;
    }

    public static boolean isInTkpdCache(File file) {
        File tkpdCacheDirectory = getTkpdCacheDirectory();
        String tkpdcacheDirPath = tkpdCacheDirectory.getAbsolutePath();
        if (file.exists() && file.getAbsolutePath().contains(tkpdcacheDirPath)) {
            return true;
        }
        return false;
    }

    public static void deleteAllCacheTkpdFiles(ArrayList<String> filesToDelete) {
        if (filesToDelete == null || filesToDelete.size() == 0) {
            return;
        }
        for (int i = 0, sizei = filesToDelete.size(); i < sizei; i++) {
            String filePathToDelete = filesToDelete.get(i);
            deleteAllCacheTkpdFile(filePathToDelete);
        }
    }

    public static void deleteAllCacheTkpdFile(String fileToDeletePath) {
        if (TextUtils.isEmpty(fileToDeletePath)) {
            return;
        }
        File fileToDelete = new File(fileToDeletePath);
        if (isInTkpdCache(fileToDelete)) {
            fileToDelete.delete();
        }
    }

    @NonNull
    private static File getTkpdCacheDirectory() {
        String externalDirPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String tkpdFolderPath = FileUtils.getFolderPathForUploadNoRand(externalDirPath);

        File tkpdRootdirectory = new File(tkpdFolderPath);
        if (!tkpdRootdirectory.exists()) {
            tkpdRootdirectory.mkdirs();
        }
        File tkpdCachedirectory = new File(tkpdRootdirectory.getAbsolutePath() + CACHE_TOKOPEDIA);
        if (!tkpdCachedirectory.exists()) {
            tkpdCachedirectory.mkdirs();
        }
        return tkpdCachedirectory;
    }

    @NonNull
    public static File getTkpdImageCacheFile(String fileName) {
        File tkpdCachedirectory = getTkpdCacheDirectory();
        return new File(tkpdCachedirectory.getAbsolutePath() + "/" + fileName + PNG);
    }

    // URI starts with "content://gmail-ls/"
    public static String getPathFromGmail(Context context, Uri contentUri) {
        File attach;
        try {
            InputStream attachment = context.getContentResolver().openInputStream(contentUri);
            String fileName = FileUtils.generateUniqueFileName();
            attach = FileUtils.writeImageToTkpdPath(attachment, fileName);
            if (attach == null) {
                return null;
            }
            return attach.getAbsolutePath();
        } catch (Exception e) {
            return null;
        }
    }

    public static String getTkpdPathFromURI(Context context, Uri uri) {
        InputStream is = null;
        if (uri.getAuthority() != null) {
            try {
                is = context.getContentResolver().openInputStream(uri);
                String path = getPathFromMediaUri(context, uri);
                Bitmap bmp = BitmapFactory.decodeStream(is);
                if (!TextUtils.isEmpty(path)) {
                    bmp = ImageHandler.RotatedBitmap(bmp, path);
                }
                String fileName = FileUtils.generateUniqueFileName();
                File file = writeImageToTkpdPath(bmp, fileName);
                if (file != null) {
                    return file.getAbsolutePath();
                } else {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static String getPathFromMediaUri(Context context, Uri contentUri) {

        String res = "";
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    res = cursor.getString(column_index);
                    return res;
                }
            } catch (Exception e) {
                return null;
            }
            finally {
                cursor.close();
            }
        } else {
            return contentUri.getPath();
        }
        return res;
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     * @author paulburke
     */

    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && isDocumentURI(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = getDocumentID(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = getDocumentID(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = getDocumentID(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    @TargetApi(19)
    private static boolean isDocumentURI(Context context, Uri uri) {
        return DocumentsContract.isDocumentUri(context, uri);
    }

    @TargetApi(19)
    private static String getDocumentID(Uri uri) {
        return DocumentsContract.getDocumentId(uri);
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static boolean writeBufferToFile(byte[] buffer, String path) {
        try {
            FileOutputStream fos = new FileOutputStream(path);

            fos.write(buffer);
            fos.close();
            return true;
        } catch (IOException e) {
            return false;
        }

    }

    private static boolean writeStreamToFile(InputStream source, File file) {
        OutputStream outStream;
        try {
            outStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];

            int length;
            //copy the file content in bytes
            while ((length = source.read(buffer)) > 0) {
                outStream.write(buffer, 0, length);
            }
            source.close();
            outStream.close();
            return true;
        } catch (IOException e) {
            return false;
        }

    }

    public static byte[] compressImage(String imagePathToCompress, int maxWidth, int maxHeight, int compressionQuality) {
        Bitmap tempPicToUpload = compressImageToBitmap(imagePathToCompress, maxWidth, maxHeight, compressionQuality);
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        if (tempPicToUpload != null) {
            tempPicToUpload.compress(Bitmap.CompressFormat.PNG, compressionQuality, bao);
            return bao.toByteArray();
        }
        return null;
    }


    public static Bitmap compressImageToBitmap(String imagePathToCompress, int maxWidth, int maxHeight, int compressionQuality) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        BitmapFactory.Options checksize = new BitmapFactory.Options();
        checksize.inPreferredConfig = Bitmap.Config.ARGB_8888;
        checksize.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePathToCompress, checksize);
        options.inSampleSize = ImageHandler.calculateInSampleSize(checksize);
        Bitmap tempPic = BitmapFactory.decodeFile(imagePathToCompress, options);
        Bitmap tempPicToUpload;
        if (tempPic != null) {
            try {
                tempPic = ImageHandler.RotatedBitmap(tempPic, imagePathToCompress);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            if (tempPic.getWidth() > maxWidth || tempPic.getHeight() > maxHeight) {
                tempPicToUpload = ImageHandler.ResizeBitmap(tempPic, compressionQuality);
            } else {
                tempPicToUpload = tempPic;
            }
            return tempPicToUpload;
        }
        return null;
    }

    /**
     * example of result : /storage/emulated/0/Android/data/com.tokopedia.tkpd/1451274244/image.jpg
     *
     * @param root
     * @param output
     * @param extension
     * @return
     */
    public static String getPathForUpload(String root, String output, String extension) {
        return root + "/Android/data/" + PACKAGE_NAME + "/" + (System.currentTimeMillis() / 1000L) + "/" + output + "." + extension;
    }

    public static void writeStringAsFileExt(Context context, final String fileContents, String fileName) {
        try {
            File root = Environment.getExternalStorageDirectory();
            FileWriter out = new FileWriter(new File(root.getAbsolutePath()) + "/" + fileName);//new File(context.getExternalFilesDir(null)
            out.write(fileContents);
            out.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
}