package org.foxteam.noisyfox.tianyidialassistant;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;

/**
 * Created by Noisyfox on 2015/11/18.
 */
public class PSWSharingFragment extends Fragment implements View.OnClickListener {

    private CheckBox mCheckBox_enable;
    private RadioButton mRadioButton_share, mRadioButton_get;
    private EditText mEditText_share_share_port, mEditText_share_share_psw, mEditText_share_get_ip, mEditText_share_get_port, mEditText_share_get_psw;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_psw_sharing, container, false);

        mCheckBox_enable = (CheckBox) v.findViewById(R.id.checkbox_enable_share);
        mRadioButton_share = (RadioButton) v.findViewById(R.id.radiobtn_sharing);
        mRadioButton_get = (RadioButton) v.findViewById(R.id.radiobtn_get);
        mEditText_share_share_port = (EditText) v.findViewById(R.id.editText_share_share_port);
        mEditText_share_share_psw = (EditText) v.findViewById(R.id.editText_share_share_psw);
        mEditText_share_get_ip = (EditText) v.findViewById(R.id.editText_share_get_ip);
        mEditText_share_get_port = (EditText) v.findViewById(R.id.editText_share_get_port);
        mEditText_share_get_psw = (EditText) v.findViewById(R.id.editText_share_get_psw);

        v.findViewById(R.id.btn_save).setOnClickListener(this);

        final View content = v.findViewById(R.id.content);
        final View content_share = v.findViewById(R.id.content_share);
        final View content_get = v.findViewById(R.id.content_get);

        mCheckBox_enable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                content.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
            }
        });

        mRadioButton_share.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                content_share.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                content_get.setVisibility(isChecked ? View.GONE : View.VISIBLE);
            }
        });

        loadData();

        return v;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_save:
                saveData();
                break;
        }
    }

    private void loadData() {

    }

    private void saveData() {

    }

}