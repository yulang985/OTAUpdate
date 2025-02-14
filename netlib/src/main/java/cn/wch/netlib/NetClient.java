package cn.wch.netlib;

import androidx.annotation.NonNull;

import com.alibaba.fastjson.JSON;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetClient {
    private static volatile NetClient netClient;
    private OkHttpClient client;
    private final static String TAG_PARSE="TAG_PARSE";
    private final static String TAG_DOWNLOAD="TAG_DOWNLOAD";

    public static NetClient getInstance() {
        if(netClient ==null) {
            synchronized (NetClient.class) {
                netClient =new NetClient();
            }
        }
        return netClient;
    }

    public NetClient() {
        client=new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20,TimeUnit.SECONDS)
                .build();
    }

    public ResponseBean parseUserUrl(String dstUrl)throws Exception{
        Response response = getResponse(dstUrl,TAG_PARSE);
        if(response==null){
            throw new Exception("parse url fail");
        }
        ResponseBean responseBean = parseResponse(response, ResponseBean.class);
        if(responseBean==null){
            throw new Exception("parse response fail");
        }
        return responseBean;
    }

    public void downloadToFile(String dstUrl, File file,@NonNull IDownloadProgress progress)throws Exception{

        byte[] buf = new byte[2048];
        int len = 0;
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            LogTool.d("start download#1");
            Response response = getResponse(dstUrl,TAG_DOWNLOAD);
            if(response==null || response.body()==null){
                throw new Exception("request download url fail");
            }
            LogTool.d(response.toString());
            LogTool.d("start download#2");
            LogTool.d(response.headers().toString()+"");
            LogTool.d(response.header("Content-Length")+"");

            is = response.body().byteStream();
            long total = response.body().contentLength();

            long offset=0;

            LogTool.d("total-->" + total); //$NON-NLS-1$

            fos = new FileOutputStream(file);

            progress.onProgress(offset,total);

            while ((len = is.read(buf)) != -1) {
                fos.write(buf, 0, len);
                //update progress
                offset+=len;
                progress.onProgress(offset,total);
            }

            fos.flush();

        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void cancelDownload(){
        Dispatcher dispatcher = client.dispatcher();
        for (Call runningCall : dispatcher.runningCalls()) {
            if(TAG_DOWNLOAD.equals(runningCall.request().tag())){
                runningCall.cancel();
            }
        }
    }

    private Response getResponse(String dstUrl,String TAG) throws Exception {

        final Request request = new Request.Builder().header("User-Agent","Mozilla/5.0").url(dstUrl).tag(TAG).get().build();
        LogTool.d(request.toString());
        Call call = client.newCall(request);
        Response response = call.execute();
        if (response != null && response.body() != null) {
            return response;
        } else {
            return null;
        }
    }

    public static interface IDownloadProgress {
        void onProgress(long progress,long total);
    }

    private static  <T> T parseResponse(Response response,Class<T> c) {
        if(response!=null && response.body()!=null && response.isSuccessful()) {
            String result;
            try {
                result = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            return JSON.parseObject(result, c);
        }else {
            return null;
        }
    }
}
