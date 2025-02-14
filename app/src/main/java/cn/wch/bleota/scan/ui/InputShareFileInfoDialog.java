package cn.wch.bleota.scan.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.wch.bleota.R;
import cn.wch.bleota.databinding.DialogInputFileInfoBinding;
import cn.wch.otalib.entry.ImageType;

public class InputShareFileInfoDialog extends BaseDialogFragment{

    DialogInputFileInfoBinding binding;
    OnShareFileResult result;
    String filename;

    public static InputShareFileInfoDialog newInstance(String filename) {

        Bundle args = new Bundle();

        InputShareFileInfoDialog fragment = new InputShareFileInfoDialog(filename);
        fragment.setArguments(args);
        return fragment;
    }

    public InputShareFileInfoDialog(String filename) {
        this.filename=filename.replaceAll("/","");
    }

    @Override
    protected View onCreateRealView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        binding=DialogInputFileInfoBinding.inflate(inflater,container,false);
        init();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding=null;
    }

    private void init(){
        binding.tvFileName.setText(filename);
        binding.cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        binding.confirm.setOnClickListener(new View.OnClickListener() {
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
            result.onResult(binding.rbImageA.isChecked()?ImageType.A:ImageType.B,s);
        }
        dismiss();
    }

    public void setListener(OnShareFileResult result){
        this.result=result;
    }

    public static interface OnShareFileResult{
        void onResult(ImageType imageType,String filename);
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
