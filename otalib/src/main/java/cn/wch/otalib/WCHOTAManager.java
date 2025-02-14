package cn.wch.otalib;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;

import cn.wch.blelib.exception.BLELibException;
import cn.wch.blelib.host.WCHBluetoothManager;
import cn.wch.blelib.utils.LogUtil;
import cn.wch.otalib.callback.IProgress;
import cn.wch.otalib.entry.ChipType;
import cn.wch.otalib.command.CommandUtil;
import cn.wch.otalib.constant.OTAConstant;
import cn.wch.otalib.entry.CurrentImageInfo;
import cn.wch.otalib.entry.ImageType;
import cn.wch.otalib.utils.FileParseTest;
import cn.wch.otalib.utils.FileParseUtil;
import cn.wch.otalib.utils.ParseUtil;

public class WCHOTAManager extends AbstractOTAManager{

    private static WCHOTAManager wchotaManager;

    protected BluetoothGattCharacteristic mOTA;

    protected Handler handler=new Handler(Looper.getMainLooper());

    private boolean stopFlag=false;
    private IProgress progress;

    public static WCHOTAManager getInstance() {
        if(wchotaManager==null){
            synchronized (WCHOTAManager.class){
                wchotaManager=new WCHOTAManager();
            }
        }
        return wchotaManager;
    }

    public void init(Application application)throws Exception{
        WCHBluetoothManager.getInstance().init(application);
    }

    @Override
    boolean checkCharacteristics(List<BluetoothGattService> list) {
        mOTA=null;
        for (BluetoothGattService service : list) {
            if (service.getUuid().toString().equalsIgnoreCase(OTAConstant.OTA_ServiceUUID)) {
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    String s = characteristic.getUuid().toString();
                    if (s.equalsIgnoreCase(OTAConstant.OTA_CharacterUUID)) {
                        mOTA=characteristic;
                        mOTA.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    }
                }
            }
        }
        return mOTA!=null;
    }

    @Override
    void preSet() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                setMTU();
            }
        });
    }

    /**
     * 获取当前硬件信息
     * @return CurrentImageInfo 硬件信息
     * @throws Exception
     */
    public CurrentImageInfo getCurrentImageInfo() throws Exception {
        LogUtil.d("try-->getCurrentImageInfo");
        byte[] imageInfoCommand = CommandUtil.getImageInfoCommand();
        byte[] response=writeAndRead(mOTA,mOTA,imageInfoCommand,imageInfoCommand.length);
        return ParseUtil.parseImageFromResponse(response);
    }

    /**
     * 取消OTA升级
     */
    public void cancelOTAUpdate(){
        stopFlag=true;
    }

    /**
     * 开始OTA升级
     * @param eraseAddr 擦除地址
     * @param file 升级文件
     * @param currentImageInfo 硬件信息
     * @param progress 升级进度回调
     * @throws Exception
     */
    @RequiresPermission(anyOf = {Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE})
    public void startOTAUpdate(int eraseAddr, @NonNull File file, @NonNull CurrentImageInfo currentImageInfo, IProgress progress) throws Exception {
        stopFlag=false;
        this.progress=progress;

        if(currentImageInfo.getChipType()== ChipType.UNKNOWN){
            throw new IOException("unknown chip type");
        }
        LogUtil.d("current chip type:"+currentImageInfo.getChipType().toString());
        //image信息
        LogUtil.d("start update use file: "+file.getAbsolutePath());
        int startAddr=0;
        if(currentImageInfo.getChipType()==ChipType.CH573
                || currentImageInfo.getChipType()==ChipType.CH583
                || currentImageInfo.getChipType()==ChipType.CH32V208
                || currentImageInfo.getChipType()==ChipType.CH32F208){
            //CH583/CH573
            //BIN文件擦除地址有用户自己输入
            //HEX文件需从调用getHexFileEraseAddr(File)方法获取擦除地址

            if(FileParseUtil.isBinFile(file)){
                startAddr=eraseAddr;
            }else if(FileParseUtil.isHexFile(file)){
                startAddr=getHexFileEraseAddr(file);
            }else {
                throw new Exception("invalid file type");
            }
            if(currentImageInfo.getType()== ImageType.A){

            }else if(currentImageInfo.getType()== ImageType.B){

            }else {
                throw new Exception("CurrentImageInfo illegal");
            }
        }else if(currentImageInfo.getChipType()==ChipType.CH579){
            if(currentImageInfo.getType()== ImageType.A){
                startAddr=currentImageInfo.getOffset();
            }else if(currentImageInfo.getType()== ImageType.B){
                startAddr=0;
            }else {
                throw new Exception("CurrentImageInfo illegal");
            }
        }

        //读取文件
        LogUtil.d("读取文件");
        ByteBuffer byteBuffer=null;
        if(file.getName().endsWith(".bin") || file.getName().endsWith(".BIN")){
            if(currentImageInfo.getChipType()==ChipType.CH573
                    || currentImageInfo.getChipType()==ChipType.CH583
                    || currentImageInfo.getChipType()==ChipType.CH32V208
                    || currentImageInfo.getChipType()==ChipType.CH32F208){
                byteBuffer= FileParseUtil.parseBinFile2(file,startAddr);
            }else {
                byteBuffer= FileParseUtil.parseBinFile(file);
            }
        }else if(file.getName().endsWith(".hex") || file.getName().endsWith(".HEX")){
            byteBuffer=FileParseUtil.parseHexFile(file);
        }else {
            throw new Exception("only support hex and bin image file");
        }
        if(byteBuffer==null){
            throw new Exception("parse file fail");
        }
        LogUtil.d("byteBuffer  capacity: "+byteBuffer.capacity());
        int total=  byteBuffer.capacity();
        LogUtil.d("total size: "+total);
        //读取文件的offset
        LogUtil.d("解析文件");

        if(currentImageInfo.getChipType()==ChipType.CH573
                || currentImageInfo.getChipType()==ChipType.CH583
                || currentImageInfo.getChipType()==ChipType.CH32V208
                || currentImageInfo.getChipType()==ChipType.CH32F208){
            if(!checkImageIllegal(currentImageInfo,byteBuffer)){
                throw new Exception("image file is illegal!");
            }
        }else if(currentImageInfo.getChipType()==ChipType.CH579){
            //目前不需要检查Image合法性
        }
        int blockSize = currentImageInfo.getBlockSize();
        LogUtil.d("blockSize: "+blockSize);
        //v1.2--修改擦除块的计算方式
        int nBlocks = ((total+(currentImageInfo.getBlockSize()-1))/currentImageInfo.getBlockSize());

        LogUtil.d("erase nBlocks: "+(nBlocks & 0xffff));

        /*int nBlocks = (((total)+ OTA_BLOCK_SIZE - 1) / OTA_BLOCK_SIZE);
        LogUtil.d("image nBlocks: "+(nBlocks & 0xffff));*/
        if(progress!=null){
            progress.onInformation(String.format(Locale.getDefault(),"erase address 0x%x", startAddr));
            progress.onEraseStart();
        }
        //开始擦除
        LogUtil.d("start erase... ");
        LogUtil.d("startAddr: "+startAddr);
        LogUtil.d("nBlocks: "+nBlocks);
        //DebugUtil.getInstance().write("start erase... "+" startAddr: "+startAddr+" "+"nBlocks: "+nBlocks);
        byte[] eraseCommand = CommandUtil.getEraseCommand(startAddr, nBlocks);
        byte[] bytes = writeAndRead(mOTA,mOTA,eraseCommand,eraseCommand.length,1000);
        if(!ParseUtil.parseEraseResponse(bytes)){
            LogUtil.d("erase fail!");
            if(progress!=null){
                progress.onError("erase fail!");
            }
            return;
        }else {
            LogUtil.d("erase success!");
            if(progress!=null){
                progress.onEraseFinish();
            }
        }
        if(progress!=null){
            progress.onProgramStart();
        }
        byte[] realBuffer = byteBuffer.array();
        //开始编程
        //FileParseTest.saveAsBinFile(realBuffer);
        try {
            CommandUtil.updateAddressBase(currentImageInfo.getChipType());
            CommandUtil.updateMTU(getCurrentMtu());
        } catch (BLELibException e) {
            e.printStackTrace();
        }
        LogUtil.d("start program... ");
        //DebugUtil.getInstance().write("start program... ");
        int offset=0;
        while (offset<realBuffer.length){
            if(checkStopFlag()){
                return;
            }
            //有效数据的长度
            int programmeLength = CommandUtil.getProgrammeLength2(realBuffer, offset);
            byte[] programmeCommand = CommandUtil.getProgrammeCommand2(offset + startAddr, realBuffer, offset);

            if(!write(mOTA,programmeCommand,programmeCommand.length)){
                if(progress!=null){
                    progress.onError("program fail!");
                }
                return;
            }
            offset+=programmeLength;
            LogUtil.d("progress: "+offset+"/"+realBuffer.length);
            if(progress!=null){
                progress.onProgramProgress(offset,realBuffer.length);
            }

        }
        LogUtil.d("program complete! ");
        if(progress!=null){
            progress.onProgramFinish();
        }
        //开始校验
        if(progress!=null){
            progress.onVerifyStart();
        }
        LogUtil.d("start verify... ");
        //DebugUtil.getInstance().write("start verify... ");
        int vIndex=0;
        while (vIndex<realBuffer.length){
            if(checkStopFlag()){
                return;
            }
            int verifyLength = CommandUtil.getVerifyLength2(realBuffer, vIndex);
            byte[] verifyCommand = CommandUtil.getVerifyCommand2(vIndex + startAddr, realBuffer, vIndex);
            if(!write(mOTA,verifyCommand,verifyCommand.length)){
                if(progress!=null){
                    progress.onError("verify fail!");
                }
                return;
            }
            vIndex+=verifyLength;
            LogUtil.d("progress: "+vIndex+"/"+realBuffer.length);
            if(progress!=null){
                progress.onVerifyProgress(vIndex,realBuffer.length);
            }
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        byte[] bytes1 = read(mOTA);
        if(!ParseUtil.parseVerifyResponse(bytes1)){
            if(progress!=null){
                LogUtil.d("---->verify fail!");
                progress.onError("verify fail!");
            }
            return;
        }
        LogUtil.d("verify complete! ");
        if(progress!=null){
            progress.onVerifyFinish();
        }
        //结束
        LogUtil.d("start ending... ");
        byte[] endCommand = CommandUtil.getEndCommand();

        if(!write(mOTA,endCommand, endCommand.length)){
            if(progress!=null){
                progress.onError("ending fail!");
            }
            return;
        }else {
            LogUtil.d("ending success!");
            if(progress!=null){
                progress.onEnd();
            }
        }
    }

    private boolean checkStopFlag(){
        if(stopFlag){
            if(progress!=null){
                progress.onCancel();
            }
            return true;
        }
        return false;
    }


}
