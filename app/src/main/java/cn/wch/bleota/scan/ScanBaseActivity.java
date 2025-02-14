package cn.wch.bleota.scan;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.touchmcu.ui.DialogUtil;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import cn.wch.bleota.scan.ui.InputShareFileInfoDialog;
import cn.wch.otalib.entry.ImageType;
import cn.wch.otalib.utils.LogTool;
import cn.wch.otaupdate.updatefile.ShareFileResolver;
import cn.wch.otaupdate.updatefile.UpdateFileResolver;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


public abstract class ScanBaseActivity extends AppCompatActivity {
    private int BT_CODE=111;
    private Context context;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=this;
        initWidget();
        initBle();
        checkShareFile();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkShareFile();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == BT_CODE && resultCode == RESULT_OK) {
            autoRun();
        } else if (requestCode == BT_CODE) {
            showToast("请允许打开蓝牙");
        }else {

        }
    }

    private void initBle(){
        if(!isSupportBle(context)){
            showToast("不支持BLE");
            return;
        }
        RxPermissions permissions=new RxPermissions(this);
        permissions.requestEachCombined(Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Permission>() {
                    @Override
                    public void accept(Permission permission) throws Exception {
                        if (permission.granted) {//全部同意后调用
                            if(!UpdateFileResolver.createPrivateFolder(ScanBaseActivity.this)){
                                showToast("创建Image文件夹失败");
                            }
                            checkAdapter();
                        } else if (permission.shouldShowRequestPermissionRationale) {//只要有一个选择：禁止，但没有选择“以后不再询问”，以后申请权限，会继续弹出提示
                            showToast("请允许权限,否则APP不能正常运行");
                        } else {//只要有一个选择：禁止，但选择“以后不再询问”，以后申请权限，不会继续弹出提示
                            showToast("请到设置中打开权限");
                        }
                    }
                });
    }

    private void checkAdapter(){
        if(BluetoothAdapter.getDefaultAdapter()==null){
            return;
        }
        if(!BluetoothAdapter.getDefaultAdapter().isEnabled()){
            Intent i=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(i,BT_CODE);
        }else {
            autoRun();
        }
    }

    protected boolean isBluetoothAdapterOpened(){
        return BluetoothAdapter.getDefaultAdapter()!=null && BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    public static boolean isSupportBle(Context context){
        PackageManager packageManager=context.getPackageManager();
        return BluetoothAdapter.getDefaultAdapter()!=null && packageManager!=null && packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    protected void showToast(final String message){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
            }
        });
    }


    protected void autoRun(){

    }

    abstract void initWidget();

    //接受分享文件
    private void checkShareFile(){
        //获得资源URI
        Intent intent = getIntent();
        if(intent==null){
            return;
        }
        Uri uri = intent.getData();
        if(uri==null){
            return;
        }
        LogTool.d("we receive share file: "+uri.toString());
        String lastPathSegment = uri.getLastPathSegment();
        if(lastPathSegment!=null && !lastPathSegment.endsWith(".bin") && !lastPathSegment.endsWith(".BIN")
                && !lastPathSegment.endsWith(".hex") && !lastPathSegment.endsWith(".HEX")){
            showToast("只支持分享bin或者hex格式的文件");
            return;
        }
        showShareFileDialog(uri.getLastPathSegment(),uri);
    }

    private void showShareFileDialog(String filename,final Uri uri){
        InputShareFileInfoDialog dialog=InputShareFileInfoDialog.newInstance(filename);
        dialog.setCancelable(false);
        dialog.setListener(new InputShareFileInfoDialog.OnShareFileResult() {
            @Override
            public void onResult(ImageType imageType, String filename) {
                saveShareFileToPrivateFolder(ScanBaseActivity.this,uri,imageType,filename);
            }
        });
        dialog.show(getSupportFragmentManager(),InputShareFileInfoDialog.class.getName());
    }

    private void saveShareFileToPrivateFolder(Context context, Uri uri, ImageType imageType,String newName){
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
                        DialogUtil.getInstance().showLoadingDialog(ScanBaseActivity.this,"正在保存文件");
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
                        showToast("已成功保存分享文件");
                    }
                });
    }


}
