package org.foxteam.noisyfox.tianyidialassistant;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.qiang.escore.banner.BannerSDK;
import com.qiang.escore.sdk.YjfSDK;
import com.qiang.escore.sdk.view.BannerView;
import com.qiang.escore.sdk.widget.UpdateScordNotifier;

public class TyAssistantFragment extends SherlockFragment implements
		UpdateScordNotifier {
	static final String TAG = "TyAssistantFragment";
	TyMainActivity father = null;
	TextView t;
	LinearLayout linearLayout;

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

		linearLayout = (LinearLayout) view.findViewById(R.id.banner_linear);

		boolean show_add = PreferenceManager
				.getDefaultSharedPreferences(father).getBoolean(
						"checkbox_advertisement", true);

		// 当Activity第一次创建时调用,此方法需继承接口UpdateScordNotifier
		YjfSDK.getInstance(father, this).initInstance("", "", "", "");
		// Banner广告---------------------------------------------------------------------------------------------------------------------------------
		if (show_add) {
			// 显示推广条
			final BannerView bannerView = BannerSDK.getInstance(father)
					.getBanner();
			linearLayout.addView(bannerView);
			// 推广条轮转
			BannerSDK.getInstance(father).showBanner(bannerView);
		}
		// Banner广告---------------------------------------------------------------------------------------------------------------------------------

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

	}

	@Override
	public void onDestroy() {
		YjfSDK.getInstance(father, this).recordAppClose();
		father.unregisterFragment(TAG);
		super.onDestroy();
	}

	@Override
	public void updateScoreFailed(int arg0, int arg1, String arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateScoreSuccess(int arg0, int arg1, int arg2, String arg3) {
		// TODO Auto-generated method stub

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
