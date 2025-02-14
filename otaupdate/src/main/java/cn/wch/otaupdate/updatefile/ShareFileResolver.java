package cn.wch.otaupdate.updatefile;

import static cn.wch.otaupdate.updatefile.UpdateFileResolver.OTA_FOLDER;
import static cn.wch.otaupdate.updatefile.UpdateFileResolver.OTA_FOLDER_IMAGE_A;
import static cn.wch.otaupdate.updatefile.UpdateFileResolver.OTA_FOLDER_IMAGE_B;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cn.wch.otalib.entry.ImageType;
import cn.wch.otalib.utils.LogTool;

public class ShareFileResolver {

    public static void uriToFile(Context context, Uri uri, ImageType imageType,String newName)throws Exception {
        LogTool.d("#1");
        File target = UpdateFileResolver.getTargetImageFile(context, imageType, newName);
        if (target==null){
            throw new Exception("invalid image type");
        }
        LogTool.d("#2"+target.getAbsolutePath());
        if(!target.exists()){
            if(!target.createNewFile()){
                throw new Exception("create file fail");
            }
        }
        LogTool.d("#3");
        ContentResolver contentResolver = context.getContentResolver();
        InputStream is = contentResolver.openInputStream(uri);
        OutputStream os = new FileOutputStream(target);
        write(is,os);
    }


    //将输入流的数据拷贝到输出流
    public static void write(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[1024 * 1024];
        while (true) {
            int len = is.read(buffer);
            if (len < 0) break;
            os.write(buffer, 0, len);
        }
        os.flush();
        is.close();
        os.close();
    }
}
