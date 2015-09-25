package org.foxteam.noisyfox.tianyidialassistant;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockFragment;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TyAssistantFragment extends SherlockFragment {
    static final String TAG = "TyAssistantFragment";
    TyMainActivity father = null;
    TextView t;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        father = (TyMainActivity) activity;
        father.registerFragment(this, TAG);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        father.unregisterFragment(TAG);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater
                .inflate(R.layout.fragment_tyassistant, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        View view = this.getView();

        t = (TextView) view.findViewById(R.id.textView_info);
        Button b = (Button) view.findViewById(R.id.button_getnow);

        updateMainText();

        b.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                father.mPSWOperator.requestNewPassword();
                t.setText("获取中，请耐心等待哦~");
            }

        });
        view.findViewById(R.id.btn_dial).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), TyDialActivity.class));
            }
        });

    }

    @Override
    public void onDestroy() {
        father.unregisterFragment(TAG);
        super.onDestroy();
    }

    public void updateMainText() {
        String psw = father.mPSWOperator.getLastPsw(false);

        if (psw.equals("")) {
            t.setText(R.string.psw_neverget);
        } else {
            Long time = father.mPSWOperator.getRecordTime();
            SimpleDateFormat formatter = new SimpleDateFormat(
                    "yyyy年MM月dd日\nHH:mm:ss", Locale.getDefault());
            Date curDate = new Date(time);
            String str = formatter.format(curDate);
            String w = "当前密码:\n" + psw + "\n获取时间:\n" + str;
            Long dTime_get = System.currentTimeMillis() - time;
            if (dTime_get > 5.5 * 60 * 60 * 1000) {
                w += "\n密码可能已经过期!";
            }

            t.setText(w);
        }
    }
}
