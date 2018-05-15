package com.microsoft.codepush.react;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by solachen on 2018/5/9.
 */

public class FileCopyUtils {

    private static final String SEPARATOR = File.separator;//路径分隔符

    /**
     * 复制assets中的文件到指定目录
     *
     * @param context     上下文
     * @param assetsPath  assets资源路径
     * @param storagePath 目标文件夹的路径
     */
    public static void copyFilesFromAssets(Context context, String assetsPath, String storagePath) {
        String temp = "";

        if (TextUtils.isEmpty(storagePath)) {
            return;
        } else if (storagePath.endsWith(SEPARATOR)) {
            storagePath = storagePath.substring(0, storagePath.length() - 1);
        }

        if (TextUtils.isEmpty(assetsPath) || assetsPath.equals(SEPARATOR)) {
            assetsPath = "";
        } else if (assetsPath.endsWith(SEPARATOR)) {
            assetsPath = assetsPath.substring(0, assetsPath.length() - 1);
        }

        AssetManager assetManager = context.getAssets();
        try {
            File file = new File(storagePath);
            if (!file.exists()) {//如果文件夹不存在，则创建新的文件夹
                file.mkdirs();
            }

            // 获取assets目录下的所有文件及目录名
            String[] fileNames = assetManager.list(assetsPath);
            if (fileNames.length > 0) {//如果是目录 apk
                for (String fileName : fileNames) {
                    if (!TextUtils.isEmpty(assetsPath)) {
                        temp = assetsPath + SEPARATOR + fileName;//补全assets资源路径
                    }

                    String[] childFileNames = assetManager.list(temp);
                    if (!TextUtils.isEmpty(temp) && childFileNames.length > 0) {//判断是文件还是文件夹：如果是文件夹
                        copyFilesFromAssets(context, temp, storagePath + SEPARATOR + fileName);
                    } else {//如果是文件
                        InputStream inputStream = assetManager.open(temp);
                        readInputStream(storagePath + SEPARATOR + fileName, inputStream);
                    }
                }
            } else {//如果是文件 doc_test.txt或者apk/app_test.apk
                InputStream inputStream = assetManager.open(assetsPath);
                if (assetsPath.contains(SEPARATOR)) {//apk/app_test.apk
                    assetsPath = assetsPath.substring(assetsPath.lastIndexOf(SEPARATOR), assetsPath.length());
                }
                readInputStream(storagePath + SEPARATOR + assetsPath, inputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 读取输入流中的数据写入输出流
     *
     * @param storagePath 目标文件路径
     * @param inputStream 输入流
     */
    public static void readInputStream(String storagePath, InputStream inputStream) {
        File file = new File(storagePath);
        try {
            if (!file.exists()) {
                // 1.建立通道对象
                FileOutputStream fos = new FileOutputStream(file);
                // 2.定义存储空间
                byte[] buffer = new byte[inputStream.available()];
                // 3.开始读文件
                int lenght = 0;
                while ((lenght = inputStream.read(buffer)) != -1) {// 循环从输入流读取buffer字节
                    // 将Buffer中的数据写到outputStream对象中
                    fos.write(buffer, 0, lenght);
                }
                fos.flush();// 刷新缓冲区
                // 4.关闭流
                fos.close();
                inputStream.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 复制res/raw中的文件到指定目录
     * @param context 上下文
     * @param id 资源ID
     * @param fileName 文件名
     * @param storagePath 目标文件夹的路径
     */
    public static void copyFilesFromRaw(Context context, int id, String fileName,String storagePath){
        InputStream inputStream=context.getResources().openRawResource(id);
        File file = new File(storagePath);
        if (!file.exists()) {//如果文件夹不存在，则创建新的文件夹
            file.mkdirs();
        }
        readInputStream(storagePath + SEPARATOR + fileName, inputStream);
    }


    public static String createCodePushFile(Context context){
        String mBinaryContentsHash = CodePushUpdateUtils.getHashForBinaryContents(context, false);
        String deployKey = CodePush.getCodePushInstance().getDeploymentKey();
        Log.v("season deploymentKey",deployKey+"");
        Log.v("source mBinaryContentsHash",mBinaryContentsHash+"");

        //String mBinaryContentsHash = "e9c79effc5b6bb19d54a6af676c513b96d4c8cac876dc5b857d47ed790bb2ad7";

        String codePushHashPath = "";

        if(mBinaryContentsHash!=null&&mBinaryContentsHash!=""){

            String path = context.getFilesDir().getAbsolutePath();
            Log.v("season",path);
            String codePushPath = CodePushUtils.appendPathComponent(path, CodePushConstants.CODE_PUSH_FOLDER_PREFIX);

            File sourceDir = new File(codePushPath);

            if (!sourceDir.exists()) {
                boolean file_true = sourceDir.mkdirs();
                Log.v("season",file_true+"");
                Log.v("season",codePushPath);
            }

            String statusFilePath = CodePushUtils.appendPathComponent(codePushPath, CodePushConstants.STATUS_FILE);
            Log.v("season",statusFilePath);
            System.out.println(FileUtils.fileAtPathExists(statusFilePath));
            if (!FileUtils.fileAtPathExists(statusFilePath)) {
                try {
                    JSONObject mJSONObject =  new JSONObject();
                    mJSONObject.putOpt("currentPackage",mBinaryContentsHash);
                    createJSON(statusFilePath,mJSONObject.toString());
                }catch (Exception e){
                    Log.e("seasonerror",e.toString());
                }

            }

            //hash路径
            codePushHashPath = CodePushUtils.appendPathComponent(codePushPath, mBinaryContentsHash);
            File hashDir = new File(codePushHashPath);
            if (!hashDir.exists()) {
                hashDir.mkdirs();
            }

            String bundlePath = CodePushUtils.appendPathComponent(codePushHashPath, CodePushConstants.CODE_PUSH_FOLDER_PREFIX);

            File bundleDir = new File(bundlePath);
            if (!bundleDir.exists()) {
                bundleDir.mkdirs();
            }

            //bundle
            copyFilesFromAssets(context, "source/CodePush", bundlePath);

            /*copyFilesFromAssets(context, "index.android.bundle", bundlePath);
            copyFilesFromAssets(context, "index.android.bundle.meta", bundlePath);*/

            //app.json
            String packageFilePath = CodePushUtils.appendPathComponent(codePushHashPath, CodePushConstants.PACKAGE_FILE_NAME);
            if (!FileUtils.fileAtPathExists(packageFilePath)) {
                try {
                    JSONObject mJSONObject =  new JSONObject();
                    mJSONObject.putOpt("isPending",false);
                    mJSONObject.putOpt("downloadUrl","https://dl.pvp.xoyo.com/prod/static/lr911v3WNUgmyvP4mc74mgcVfjiR");
                    mJSONObject.putOpt("packageSize",42930352);
                    mJSONObject.putOpt("packageHash",mBinaryContentsHash);
                    mJSONObject.putOpt("label","v48");
                    mJSONObject.putOpt("description","");
                    mJSONObject.putOpt("failedInstall",false);
                    mJSONObject.putOpt("binaryModifiedTime","1524829297066");
                    mJSONObject.putOpt("bundlePath","/CodePush/index.android.bundle");
                    mJSONObject.putOpt("deploymentKey",deployKey);


                    createJSON(packageFilePath,mJSONObject.toString());

                }catch (Exception e){
                    Log.e("seasonerror",e.toString());
                }

            }
        }
        return codePushHashPath;
    }

    public static void createJSON(String FilePath,String data) {
        try {

            File writename = new File(FilePath);
            writename.createNewFile();
            BufferedWriter out = new BufferedWriter(new FileWriter(writename));
            out.write(data);
            out.flush();
            out.close();
        } catch (Exception e) {
            Log.e("seasonerror", e.toString());
        }
    }
}
