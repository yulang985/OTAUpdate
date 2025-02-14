package cn.wch.otalib.utils;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.RequiresApi;

import cn.wch.blelib.utils.Location;

public class LocationUtil {

    @RequiresApi(api = Build.VERSION_CODES.P)
    public static boolean isLocationEnable(Context context){

        return Location.isLocationEnable(context);
    }

    public static void requestLocationService(Context context){
        Location.requestLocationService(context);
    }
}
