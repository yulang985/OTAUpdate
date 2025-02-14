package cn.wch.bleota.scan.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.Locale;

import cn.wch.bleota.databinding.DialogDownloadBinding;
import cn.wch.netlib.NetClient;
import cn.wch.netlib.ResponseBean;
import cn.wch.otalib.entry.ImageType;
import cn.wch.otaupdate.updatefile.UpdateFileResolver;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class DownloadFileDialog extends BaseDialogFragment{

    DialogDownloadBinding binding;

    public static DownloadFileDialog newInstance() {

        Bundle args = new Bundle();

        DownloadFileDialog fragment = new DownloadFileDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected View onCreateRealView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogDownloadBinding.inflate(inflater);
        init();
        return binding.getRoot();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding=null;
    }

    private void init() {
        binding.tvStartDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s = binding.etDownload.getText().toString();
                if(s.equals("")){
                    showToast("下载地址为空");
                    return;
                }
                //prepareDownload(s);
                testDownload();
            }
        });
        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    private void prepareDownload(String url){
        ResponseBean responseBean = null;
        try {
            responseBean = NetClient.getInstance().parseUserUrl(url);
            String filename = responseBean.getFilename();
            ImageType imageType = findMatchType(responseBean.getImageType());
            if(imageType==null){
                showToast("response contains error image type");
                return;
            }
            File targetImageFile = UpdateFileResolver.getTargetImageFile(getActivity(), imageType, filename);
            startDownload(responseBean.getUrl(),targetImageFile);
        } catch (Exception e) {
            e.printStackTrace();
            showToast(e.getMessage());
        }
    }

    private void testDownload(){
        startDownload("https://adl.netease.com/d/g/uu/c/ydgw",UpdateFileResolver.getTargetImageFile(getActivity(),ImageType.A,"1.apk"));
    }


    private void startDownload(String dstUrl, File file){
        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                try {
                    NetClient.getInstance().downloadToFile(dstUrl, file, new NetClient.IDownloadProgress() {
                        @Override
                        public void onProgress(long progress, long total) {
                            //emitter.onNext((int) (total / progress));
                        }
                    });
                    emitter.onComplete();
                } catch (Exception e) {
                    emitter.onError(new Throwable(e.getMessage()));
                    return;
                }
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        showToast("开始下载");
                        binding.cancel.setVisibility(View.INVISIBLE);
                        binding.tvStartDownload.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onNext(Integer s) {
                        binding.pbProgress.setProgress(s);
                        binding.tvProgress.setText(String.format(Locale.getDefault(),"%d%",s));
                    }

                    @Override
                    public void onError(Throwable e) {
                        showToast("下载失败:"+e.getMessage());
                        binding.cancel.setEnabled(true);
                        binding.tvStartDownload.setEnabled(true);
                    }

                    @Override
                    public void onComplete() {
                        showToast("下载成功");
                        binding.cancel.setVisibility(View.VISIBLE);
                        binding.tvStartDownload.setVisibility(View.VISIBLE);
                    }
                });
    }

    private ImageType findMatchType(String imageType){
        ImageType[] values = ImageType.values();
        for (ImageType value : values) {
            if(value.toString().equals(imageType)){
                return value;
            }
        }
        return null;
    }

    private void showToast(String message){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(),message,Toast.LENGTH_SHORT).show();
            }
        });
    }


}
