package cn.wch.bleota.scan.ui;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import cn.wch.bleota.R;

public class MyTextView extends AppCompatTextView {
    public MyTextView(@NonNull Context context) {
        super(context,null,0);
    }

    public MyTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs,0);
    }

    public MyTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setEnabled(boolean enabled) {
        setTextColor(enabled? Color.parseColor("#3a89d1"):Color.parseColor("#8C8C8C"));
        super.setEnabled(enabled);
    }
}
