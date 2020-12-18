package com.jarvan.fluwx.utils;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


import com.jarvan.fluwx.constant.WeChatPluginImageSchema;
import com.jarvan.fluwx.constant.WechatPluginKeys;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import io.flutter.plugin.common.PluginRegistry;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import top.zibin.luban.Luban;

public class WeChatThumbnailUtil {
    public static final  int SHARE_MINI_PROGRAM_IMAGE_THUMB_LENGTH = 120;
    public static final int MINI_PROGRAM_SCALED_WIDTH = 480;
    public static final int SHARE_IMAGE_THUMB_LENGTH = 32;
    private static final int COMMON_THUMB_WIDTH = 150;

    private WeChatThumbnailUtil() {
    }

    public static byte[] thumbnailForMiniProgram(String thumbnail, PluginRegistry.Registrar registrar) {
        File file;
        if (thumbnail.startsWith(WeChatPluginImageSchema.SCHEMA_ASSETS)) {
            file = getAssetFile(thumbnail, registrar);
        } else if (thumbnail.startsWith(WeChatPluginImageSchema.SCHEMA_FILE)) {
            file = new File(thumbnail);
        } else {
            file = downloadImage(thumbnail);
        }
        return compress(file, registrar,SHARE_MINI_PROGRAM_IMAGE_THUMB_LENGTH,MINI_PROGRAM_SCALED_WIDTH);
    }


    private static byte[] fromAssetForMiniProgram(String thumbnail, PluginRegistry.Registrar registrar) {
        File file;
        if (thumbnail.startsWith(WeChatPluginImageSchema.SCHEMA_ASSETS)) {
            file = getAssetFile(thumbnail, registrar);
        } else if (thumbnail.startsWith(WeChatPluginImageSchema.SCHEMA_FILE)) {
            file = new File(thumbnail);
        } else {
            file = downloadImage(thumbnail);
        }
        return compress(file, registrar,SHARE_MINI_PROGRAM_IMAGE_THUMB_LENGTH,MINI_PROGRAM_SCALED_WIDTH);
    }

    public static byte[] thumbnailForCommon(String thumbnail, PluginRegistry.Registrar registrar) {
        File file;
        if (thumbnail.startsWith(WeChatPluginImageSchema.SCHEMA_ASSETS)) {
            file = getAssetFile(thumbnail, registrar);
        } else if (thumbnail.startsWith(WeChatPluginImageSchema.SCHEMA_FILE)) {
            file = new File(thumbnail);
        } else {
            file = downloadImage(thumbnail);
        }
        return compress(file, registrar,SHARE_IMAGE_THUMB_LENGTH,COMMON_THUMB_WIDTH);
    }

    private static byte[] compress(File file, PluginRegistry.Registrar registrar,int resultMaxLength,int scaledWidth) {
        if (file == null) {
            return new byte[]{};
        }


        try {
            File compressedFile = Luban
                    .with(registrar.context())
                    .ignoreBy(resultMaxLength)
                    .setTargetDir(registrar.context().getCacheDir().getAbsolutePath())
                    .get(file.getAbsolutePath());
            if (compressedFile.length() < resultMaxLength * 1024) {
                Source source = Okio.source(compressedFile);
                BufferedSource bufferedSource = Okio.buffer(source);
                byte[] bytes = bufferedSource.readByteArray();
                source.close();
                bufferedSource.close();
                return bytes;
            }
            byte[] result = createScaledBitmapWithRatio(compressedFile,scaledWidth);
            if (result.length < resultMaxLength * 1024) {
                return result;
            }

            return createScaledBitmap(compressedFile, resultMaxLength,scaledWidth);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[]{};
    }

    private static byte[] createScaledBitmapWithRatio(File file,int scaledWidth) {

        Bitmap originBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        Bitmap result = ThumbnailCompressUtil.createScaledBitmapWithRatio(originBitmap, scaledWidth, true);

        String path = file.getAbsolutePath();
        String suffix = path.substring(path.lastIndexOf("."), path.length());
        return bmpToByteArray(result, suffix, true);


    }

    private static byte[] createScaledBitmap(File file,int resultMaxLength,int scaledWidth) {
        Bitmap originBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

        Bitmap result = null;

        int width =scaledWidth;
        while (width > 10){
            result = ThumbnailCompressUtil.createScaledBitmap(originBitmap, width, false);
            if (result.getByteCount() < resultMaxLength* 1024){
                break;
            }else {
                width = width -10;
            }
        }

        originBitmap.recycle();

        return bmpToByteArray(result, ".png", true);
    }

    private static byte[] bmpToByteArray(Bitmap bitmap, String suffix, boolean recycle) {
//        int bytes = bitmap.getByteCount();
//        ByteBuffer buf = ByteBuffer.allocate(bytes);
//        bitmap.copyPixelsToBuffer(buf);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Bitmap.CompressFormat format = Bitmap.CompressFormat.PNG;
        if (suffix.toLowerCase().equals(".jpg") || suffix.toLowerCase().equals(".jpeg")) {
            format = Bitmap.CompressFormat.JPEG;
        }

        bitmap.compress(format, 100, byteArrayOutputStream);
        InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        byte[] result = null;

        if (recycle) {
            bitmap.recycle();
        }
        Source source = Okio.source(inputStream);
        BufferedSource bufferedSource = Okio.buffer(source);
        try {
            result = bufferedSource.readByteArray();
            source.close();
            bufferedSource.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static File getAssetFile(String thumbnail, PluginRegistry.Registrar registrar) {
        File result = null;
        String key = thumbnail.substring(WeChatPluginImageSchema.SCHEMA_ASSETS.length(), thumbnail.length());
        AssetFileDescriptor fileDescriptor = AssetManagerUtil.openAsset(registrar, key, getPackage(key));

        if (fileDescriptor != null) {
            try {
                result = File.createTempFile(UUID.randomUUID().toString(), getSuffix(key));
                OutputStream outputStream = new FileOutputStream(result);
                BufferedSink sink = Okio.buffer(Okio.sink(outputStream));
                Source source = Okio.source(fileDescriptor.createInputStream());
                sink.writeAll(source);
                source.close();
                sink.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private static String getPackage(String assetsName) {
        String packageStr = null;
        if (assetsName.contains(WechatPluginKeys.PACKAGE)) {
            int index = assetsName.indexOf(WechatPluginKeys.PACKAGE);
            packageStr = assetsName.substring(index + WechatPluginKeys.PACKAGE.length(), assetsName.length());
        }
        return packageStr;
    }
    private static File downloadImage(String url) {
        File result = null;
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(url).get().build();
        try {
            Response response = okHttpClient.newCall(request).execute();
            ResponseBody responseBody = response.body();
            if (response.isSuccessful() && responseBody != null) {
                result = File.createTempFile(UUID.randomUUID().toString(), getSuffix(url));
                OutputStream outputStream = new FileOutputStream(result);
                BufferedSink sink = Okio.buffer(Okio.sink(outputStream));
                sink.writeAll(responseBody.source());
                sink.flush();
                sink.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }


    private static File inputStreamToFile(InputStream inputStream,String suffix) {
        File result = null;
        try {
            result = File.createTempFile(UUID.randomUUID().toString(), suffix);
            OutputStream outputStream = new FileOutputStream(result);
            BufferedSink sink = Okio.buffer(Okio.sink(outputStream));
            Source source = Okio.source(inputStream);
            sink.writeAll(source);
            sink.flush();
            sink.close();
            source.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private static String getSuffix(String path) {
        String suffix = ".jpg";
        int index = path.lastIndexOf(".");
        if (index > 0) {
            suffix = path.substring(index, path.length());
        }
        return suffix;
    }
}
