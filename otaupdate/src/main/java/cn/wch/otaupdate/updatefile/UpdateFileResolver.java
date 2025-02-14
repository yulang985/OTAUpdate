package cn.wch.otaupdate.updatefile;

import android.content.Context;

import java.io.File;

import cn.wch.otalib.entry.ImageType;

/**
 * 升级文件入口：
 * 1.通过电脑将文件放到/Android/data/cn.wch.bleota/files/OTAFile/下
 * 2.使用安卓自带的文件浏览器，选择文件
 * 3.微信/QQ分享的文件
 */
public class UpdateFileResolver {

    //image 升级文件位置，放在getExternalFilesDir()下
    public static final String OTA_FOLDER="OTAFile";
    public static final String OTA_FOLDER_IMAGE_A="imageA";
    public static final String OTA_FOLDER_IMAGE_B="imageB";

    //在沙盒创建应用的私有目录
    public static boolean createPrivateFolder(Context context) {
        File otaDir = context.getExternalFilesDir(OTA_FOLDER);
        File imageA=new File(otaDir, OTA_FOLDER_IMAGE_A);
        File imageB=new File(otaDir, OTA_FOLDER_IMAGE_B);
        if(!imageA.exists()){
            imageA.mkdirs();
        }
        if(!imageB.exists()){
            imageB.mkdirs();
        }
        return imageA.exists() && imageB.exists();
    }

    public static File getTargetImageFile(Context context,ImageType imageType,String filename){
        File otaDir=context.getExternalFilesDir(OTA_FOLDER);
        File imageA=new File(otaDir, OTA_FOLDER_IMAGE_A);
        File imageB=new File(otaDir, OTA_FOLDER_IMAGE_B);
        File target=null;
        if(imageType== ImageType.A){
            imageA.mkdirs();
            target=new File(imageA,filename);
        }else if(imageType==ImageType.B){
            imageB.mkdirs();
            target=new File(imageB,filename);
        }
        return target;
    }





}
