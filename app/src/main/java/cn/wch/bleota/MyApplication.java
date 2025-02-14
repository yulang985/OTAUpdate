package cn.wch.bleota;

import android.app.Application;
import cn.wch.otalib.WCHOTAManager;
import cn.wch.otalib.utils.FileParseTest;
import cn.wch.otalib.utils.LogTool;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            WCHOTAManager.getInstance().init(this);
            //FileParseTest.init(this);
        } catch (Exception e) {
            LogTool.d(e.getMessage());
            e.printStackTrace();
        }

    }
}
