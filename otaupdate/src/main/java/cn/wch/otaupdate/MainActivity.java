package cn.wch.otaupdate;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.touchmcu.ui.DialogUtil;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import cn.wch.otalib.WCHOTAManager;
import cn.wch.otalib.callback.ConCallback;
import cn.wch.otalib.callback.IProgress;
import cn.wch.otalib.entry.ChipType;
import cn.wch.otalib.entry.CurrentImageInfo;
import cn.wch.otalib.entry.ImageType;
import cn.wch.otalib.utils.LogTool;
import cn.wch.otaupdate.other.Constant;
import cn.wch.otaupdate.other.HelperUtil;
import cn.wch.otaupdate.other.ImageFile;
import cn.wch.otaupdate.other.TimeUtil;
import cn.wch.otaupdate.ui.ChipTypeDialog;
import cn.wch.otaupdate.ui.EraseAddrDialog;
import cn.wch.otaupdate.ui.FileListDialog;
import cn.wch.otaupdate.ui.InputImageInfoDialog;
import cn.wch.otaupdate.updatefile.ShareFileResolver;
import cn.wch.otaupdate.updatefile.UpdateFileResolver;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.View;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private String address;


    private TextView tv_target;
    private TextView tv_chip;
    private TextView tv_version;
    private TextView tv_offset;
    private TextView tv_new;
    private Button getInfo;
    private Button selectA;
    private Button selectB;
    private TextView monitor;
    private ProgressBar progressBar;
    private TextView tvLog;
    private Button start;

    private CurrentImageInfo currentImageInfo;
    private File targetFile;

    private Handler handler=new Handler(Looper.getMainLooper());


    private ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            Intent data = result.getData();
            if(data==null){
                return;
            }
            Uri uri = result.getData().getData();
            handleUriImported(uri);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("OTA升级");
        ;
        setSupportActionBar(toolbar);
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setHomeButtonEnabled(true);
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }
        initWidget();
        init();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        menu.findItem(R.id.connect).setVisible(!WCHOTAManager.getInstance().isConnected(address));
        menu.findItem(R.id.disconnect).setVisible(WCHOTAManager.getInstance().isConnected(address));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if(itemId==android.R.id.home){
            onBackPressed();
        }else if(itemId==R.id.connect){
            connect();
        }else if(itemId==R.id.disconnect){
            try {
                WCHOTAManager.getInstance().cancelOTAUpdate();
                WCHOTAManager.getInstance().disconnect(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else if(itemId==R.id.importFile){
            importOtherFile();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if(WCHOTAManager.getInstance().isConnected(address)){
            DialogUtil.getInstance().showSimpleDialog(MainActivity.this, "蓝牙已经连接，确定断开？", "断开", "算了", new DialogUtil.IResult() {
                @Override
                public void onContinue() {
                    WCHOTAManager.getInstance().cancelOTAUpdate();
                    try {
                        WCHOTAManager.getInstance().disconnect(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onCancel() {

                }
            });
            return;
        }
        stopMonitor();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            WCHOTAManager.getInstance().disconnect(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initWidget() {
        tv_chip= findViewById(R.id.tw_chip_type);
        tv_target = findViewById(R.id.tw_target);
        tv_version = findViewById(R.id.tw_version);
        tv_offset = findViewById(R.id.tw_offset);
        tv_new = findViewById(R.id.tw_file);
        getInfo = findViewById(R.id.btn_getinfo);
        selectA = findViewById(R.id.btn_load_a);
        selectB = findViewById(R.id.btn_load_b);
        progressBar = findViewById(R.id.pb_progress);
        tvLog = findViewById(R.id.tw_log);
        start = findViewById(R.id.btn_start);
        monitor = findViewById(R.id.monitor);

        tvLog.setMovementMethod(ScrollingMovementMethod.getInstance());

        getInfo.setEnabled(false);
        selectA.setEnabled(false);
        selectB.setEnabled(false);
        start.setEnabled(false);

        getInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getTargetImageInfo();
            }
        });
        selectA.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFile(ImageFile.A);
            }
        });
        selectB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFile(ImageFile.B);
            }
        });
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(start.getText().toString().equalsIgnoreCase(Constant.START)){
                    beforeStartUpgrade();
                }else if(start.getText().toString().equalsIgnoreCase(Constant.CANCEL)){
                    cancel();
                }
            }
        });
    }

    private void init() {
        if (getIntent() != null) {
            address = getIntent().getStringExtra(Constant.ADDRESS);
        }

        if (address == null) {
            LogTool.d("mac address is null");
            return;
        }
        connect();
    }

    private void connect(){
        //开始连接
        WCHOTAManager.getInstance().connect(address, new ConCallback() {
            @Override
            public void OnError(String mac, Throwable t) {
                showToast(t.getMessage());
            }

            @Override
            public void OnConnecting(String mac) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        DialogUtil.getInstance().showLoadingDialog(MainActivity.this, "正在连接");
                    }
                });
            }

            @Override
            public void OnConnectSuccess(String mac) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getInfo.setEnabled(true);
                        progressBar.setProgress(0);
                        DialogUtil.getInstance().hideLoadingDialog();
                        invalidateOptionsMenu();
                    }
                });
                clearLog();
                updateLog(mac+" is connected"+"\r\n");
                startMonitor();
            }

            @Override
            public void OnInvalidDevice(String mac) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        DialogUtil.getInstance().hideLoadingDialog();
                        showToast("该设备不是目标设备");
                        try {
                            WCHOTAManager.getInstance().disconnect(false);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }

            @Override
            public void OnConnectTimeout(String mac) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        DialogUtil.getInstance().hideLoadingDialog();
                        invalidateOptionsMenu();
                    }
                });
                showToast("连接超时");
            }

            @Override
            public void OnDisconnect(String mac, BluetoothDevice bluetoothDevice, int status) {
                showToast("蓝牙已断开连接");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getInfo.setEnabled(false);
                        selectA.setEnabled(false);
                        selectB.setEnabled(false);
                        start.setEnabled(false);
                        invalidateOptionsMenu();
                    }
                });
                updateLog(mac+" is disconnected"+"\r\n");
                stopMonitor();
            }
        });
    }

    private void getTargetImageInfo() {
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                currentImageInfo = WCHOTAManager.getInstance().getCurrentImageInfo();
                if (currentImageInfo == null) {
                    emitter.onError(new Throwable("获取Image信息失败"));
                    return;
                }
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        DialogUtil.getInstance().showLoadingDialog(MainActivity.this, "获取Image信息...");
                    }

                    @Override
                    public void onNext(String s) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        DialogUtil.getInstance().hideLoadingDialog();
                        LogTool.d(e.getMessage());
                        showToast(e.getMessage());
                        selectA.setEnabled(false);
                        selectB.setEnabled(false);
                        start.setEnabled(false);
                        tv_chip.setText("null");
                        tv_target.setText("null");
                        tv_version.setText("null");
                        tv_offset.setText("null");
                        tv_new.setText("null");
                    }

                    @Override
                    public void onComplete() {
                        DialogUtil.getInstance().hideLoadingDialog();
                        if(currentImageInfo.getChipType()!=null){
                            tv_chip.setText(currentImageInfo.getChipType().getDescription());
                        }

                        tv_target.setText(currentImageInfo.getType().toString());
                        tv_version.setText(currentImageInfo.getVersion());
                        tv_offset.setText(String.format(Locale.US, "%d", currentImageInfo.getOffset()));
                        start.setEnabled(true);
                        selectA.setEnabled(currentImageInfo.getType()== ImageType.B);
                        selectB.setEnabled(currentImageInfo.getType()== ImageType.A);
                    }
                });
    }

    private void loadFile(ImageFile imageFile) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 111);
            return;
        }
//        if (!UpdateFileResolver.createPrivateFolder(this)) {
//            showToast("创建文件夹失败");
//            return;
//        }
        File otaDir = getExternalFilesDir(UpdateFileResolver.OTA_FOLDER);
        File image = new File(otaDir, imageFile == ImageFile.A ? UpdateFileResolver.OTA_FOLDER_IMAGE_A : UpdateFileResolver.OTA_FOLDER_IMAGE_B);
        if (image == null || !image.exists()) {
            showToast("image文件夹不存在");
            return;
        }
        final File[] files = image.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {

                return name.endsWith(".bin") || name.endsWith(".BIN") || name.endsWith(".hex") || name.endsWith(".HEX");
            }
        });
        if (files == null || files.length == 0) {
            showToast("没有找到升级文件");
            return;
        }
        FileListDialog dialog = FileListDialog.newInstance(new ArrayList<File>(Arrays.asList(files)));
        dialog.setCancelable(false);
        dialog.show(getSupportFragmentManager(), FileListDialog.class.getSimpleName());
        dialog.setOnChooseListener(new FileListDialog.OnChooseFileListener() {
            @Override
            public void onChoose(File file) {
                targetFile = file;
                tv_new.setText(String.format(Locale.US, "size: %d", file.length()));
            }
        });
    }

    public void beforeStartUpgrade(){
        if(targetFile==null){
            showToast("升级文件为空");
            return;
        }
        //检查芯片类型
        if(currentImageInfo.getChipType()== ChipType.UNKNOWN){
            ChipTypeDialog dialog=ChipTypeDialog.newInstance();
            dialog.setCancelable(false);
            dialog.show(getSupportFragmentManager(),ChipTypeDialog.class.getName());
            dialog.setSelectListener(new ChipTypeDialog.OnSelectListener() {
                @Override
                public void onSelect(ChipType chipType) {
                    currentImageInfo.setChipType(chipType);
                    updateLog("select chip:"+chipType.getDescription()+"\r\n");
                    startUpgrade();
                }
            });
        }else {
            startUpgrade();
        }
    }

    public void startUpgrade(){

        //使用hex文件升级时，擦除地址自动解析，不需要输入,填0即可
        //使用bin文件升级时，CH579自动填充，其他需要手动输入
        if(HelperUtil.isHexFile(targetFile)){
            //使用hex升级
            startOTAUpgrade(0);
        }else if(HelperUtil.isBinFile(targetFile)){
            if(currentImageInfo.getChipType()==ChipType.CH579){
                startOTAUpgrade(0);
            }else {
                EraseAddrDialog dialog=EraseAddrDialog.newInstance();
                dialog.setCancelable(false);
                dialog.show(getSupportFragmentManager(),EraseAddrDialog.class.getName());
                dialog.setOnClickListener(new EraseAddrDialog.OnClickListener() {
                    @Override
                    public void onStartUpgrade(int addr) {
                        startOTAUpgrade(addr);
                    }
                });
            }


        }
        LogTool.d("other type file");
    }

    /**
     * 使用hex文件升级，擦除起始地址自动解析
     */
    private void startOTAUpgrade(final int eraseAddress) {
        LogTool.d("startHexFileUpgrade");
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
                if (currentImageInfo == null) {
                    emitter.onError(new Throwable("image 信息为空"));
                    return;
                }
                if (targetFile == null) {
                    emitter.onError(new Throwable("未选择升级固件"));
                    return;
                }
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    emitter.onError(new Throwable("没有读写文件的权限"));
                    return;
                }
                WCHOTAManager.getInstance().startOTAUpdate(eraseAddress,targetFile, currentImageInfo, new IProgress() {
                    @Override
                    public void onEraseStart() {
                        emitter.onNext("erase start!"+"\r\n");
                    }

                    @Override
                    public void onEraseFinish() {
                        emitter.onNext("erase finish!"+"\r\n");
                    }

                    @Override
                    public void onProgramStart() {
                        resetCount();
                        emitter.onNext("program start!"+"\r\n");
                    }

                    @Override
                    public void onProgramProgress(int current, int total) {
                        updateCount(current);
                        update(current, total);
                    }

                    @Override
                    public void onProgramFinish() {
                        emitter.onNext("program finish!"+"\r\n");
                    }

                    @Override
                    public void onVerifyStart() {
                        resetCount();
                        emitter.onNext("verify start!"+"\r\n");
                    }

                    @Override
                    public void onVerifyProgress(int current, int total) {
                        updateCount(current);
                        update(current, total);
                    }

                    @Override
                    public void onVerifyFinish() {
                        emitter.onNext("verify finish!"+"\r\n");
                    }

                    @Override
                    public void onEnd() {
                        emitter.onNext("end!"+"\r\n");
                        emitter.onComplete();
                    }

                    @Override
                    public void onCancel() {
                        emitter.onNext("cancel!"+"\r\n");
                        emitter.onError(new Throwable("取消升级"));
                    }

                    @Override
                    public void onError(String message) {
                        LogTool.d("onError :"+message);
                        emitter.onNext(message+"\r\n");
                        emitter.onError(new Throwable(message));
                    }

                    @Override
                    public void onInformation(String message) {
                        emitter.onNext(message+"\r\n");
                    }
                });

            }
        }).subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        //开始升级
                        getInfo.setEnabled(false);
                        selectA.setEnabled(false);
                        selectB.setEnabled(false);
                        start.setEnabled(true);
                        start.setText(Constant.CANCEL);
                        progressBar.setProgress(0);
                    }

                    @Override
                    public void onNext(String s) {
                        updateLog(s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        resetCount();
                        getInfo.setEnabled(true);
                        selectA.setEnabled(false);
                        selectB.setEnabled(false);
                        start.setEnabled(false);
                        start.setText(Constant.START);
                        tv_chip.setText("null");
                        tv_target.setText("null");
                        tv_version.setText("null");
                        tv_offset.setText("null");
                        tv_new.setText("null");
                        currentImageInfo=null;
                        LogTool.d(e.getMessage());
                        showToast(e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        resetCount();
                        getInfo.setEnabled(true);
                        selectA.setEnabled(false);
                        selectB.setEnabled(false);
                        start.setEnabled(false);
                        start.setText(Constant.START);
                        updateLog("update success!"+"\r\n");
                        LogTool.d("Complete!");
                    }
                });
    }

    private void cancel(){
        WCHOTAManager.getInstance().cancelOTAUpdate();
    }

    private void update(final int current, final int total){
        handler.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(current*100/total);
            }
        });
    }

    private void updateLog(final String message){
        handler.post(new Runnable() {
            @Override
            public void run() {
                tvLog.append(TimeUtil.getCurrentTime()+">> "+message);
                int offset = tvLog.getLineCount() * tvLog.getLineHeight();
                //int maxHeight = usbReadValue.getMaxHeight();
                int height = tvLog.getHeight();
                //USBLog.d("offset: "+offset+"  maxHeight: "+maxHeight+" height: "+height);
                if (offset > height) {
                    //USBLog.d("scroll: "+(offset - usbReadValue.getHeight() + usbReadValue.getLineHeight()));
                    tvLog.scrollTo(0, offset - tvLog.getHeight() + tvLog.getLineHeight());
                }
            }
        });
    }

    private void clearLog(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                tvLog.setText("");
                tvLog.scrollTo(0, 0);
            }
        });
    }

    private void showToast(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this,message,Toast.LENGTH_SHORT).show();
            }
        });
    }

    /////////////////////////////////////speed/////////////////////////////////////////////
    int oldTemp=0;
    int newTemp=0;
    private ScheduledExecutorService scheduledExecutorService;
    private Runnable speedRunnable=new Runnable() {
        @Override
        public void run() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    int count=newTemp-oldTemp;
                    oldTemp=newTemp;
                    if(count>=0){
                        monitor.setText(String.format(Locale.US,"速度：%d字节/秒",count));
                    }
                }
            });
        }
    };
    void startMonitor(){
        if(speedRunnable==null){
            LogTool.d("speed monitor is null");
            return;
        }
        stopMonitor();
        LogTool.d("开始定时器");
        resetCount();
        scheduledExecutorService= Executors.newScheduledThreadPool(2);
        scheduledExecutorService.scheduleWithFixedDelay(speedRunnable,100,1000, TimeUnit.MILLISECONDS);
    }

    void stopMonitor(){

        if(scheduledExecutorService!=null){
            LogTool.d("取消定时器");
            scheduledExecutorService.shutdown();
            scheduledExecutorService=null;
        }
        resetCount();
    }

    void resetCount(){
        oldTemp=0;
        newTemp=0;
    }

    void updateCount(int newCount){
        newTemp=newCount;
    }

    //从外部导入升级文件
    void importOtherFile(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        launcher.launch(intent);
    }

    void handleUriImported(final Uri uri){
        if(uri==null){
            return;
        }
        LogTool.d("import other file "+uri.toString());
        InputImageInfoDialog dialog=InputImageInfoDialog.newInstance(uri.getLastPathSegment());
        dialog.setCancelable(false);
        dialog.setListener(new InputImageInfoDialog.OnImportFileResult() {
            @Override
            public void onResult(ImageType imageType, String filename) {
                saveFileToPrivateFolder(MainActivity.this,uri,imageType,filename);
            }
        });
        dialog.show(getSupportFragmentManager(),InputImageInfoDialog.class.getName());
    }

    private void saveFileToPrivateFolder(final Context context, final Uri uri, final ImageType imageType,final String newName){
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) throws Exception {
                try {
                    ShareFileResolver.uriToFile(context,uri,imageType,newName);
                    emitter.onComplete();
                } catch (Exception e) {
                    emitter.onError(new Throwable(e.getMessage()));
                    return;
                }
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        DialogUtil.getInstance().showLoadingDialog(MainActivity.this,"正在导入文件");
                    }

                    @Override
                    public void onNext(String s) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        DialogUtil.getInstance().hideLoadingDialog();
                        showToast(e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        DialogUtil.getInstance().hideLoadingDialog();
                        showToast("已成功导入文件");
                    }
                });
    }
}
