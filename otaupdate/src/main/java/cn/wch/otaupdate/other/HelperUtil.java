package cn.wch.otaupdate.other;

import androidx.annotation.NonNull;

import java.io.File;

public class HelperUtil {

    public static boolean isHexFile(@NonNull File file){
        if(file!=null && (file.exists()) && (file.getName().endsWith("hex") || file.getName().endsWith("HEX"))){
            return true;
        }
        return false;
    }

    public static boolean isBinFile(@NonNull File file){
        if(file!=null && (file.exists()) && (file.getName().endsWith("bin") || file.getName().endsWith("BIN"))){
            return true;
        }
        return false;
    }
}
