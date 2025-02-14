package cn.wch.otaupdate.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.wch.otalib.entry.ImageType;
import cn.wch.otaupdate.databinding.DialogInputImageInfoBinding;

public class InputImageInfoDialog extends BaseDialogFragment2{
    DialogInputImageInfoBinding binding;
    String filename;
    OnImportFileResult result;

    public static InputImageInfoDialog newInstance(String filename) {

        Bundle args = new Bundle();

        InputImageInfoDialog fragment = new InputImageInfoDialog(filename);
        fragment.setArguments(args);
        return fragment;
    }

    public InputImageInfoDialog(String filename) {
        this.filename=filename.replaceAll("/","");

    }

    @Override
    protected View onCreateRealView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogInputImageInfoBinding.inflate(inflater);
        init();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding=null;
    }

    void init(){
        binding.tvFileName.setText(filename);
        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        binding.confirm.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                check();
            }
        });
    }

    private void check(){
        String s = binding.tvFileName.getText().toString();
        if("".equals(s)){
            showToast("文件名不能为空");
            return;
        }
        if(result!=null){
            result.onResult(binding.rbImageA.isChecked()? ImageType.A:ImageType.B,s);
        }
        dismiss();
    }

    public void setListener(OnImportFileResult result){
        this.result=result;
    }

    public static interface OnImportFileResult{
        void onResult(ImageType imageType,String filename);
    }

    private void showToast(final String message){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(),message,Toast.LENGTH_SHORT).show();
            }
        });
    }
}
