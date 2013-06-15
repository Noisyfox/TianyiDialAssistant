package org.foxteam.noisyfox.tianyidialassistant;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.emar.escore.banner.BannerSDK;
import com.emar.escore.sdk.YjfSDK;
import com.emar.escore.sdk.view.bannerView;
import com.emar.escore.sdk.widget.UpdateScordNotifier;

public class MainActivity extends Activity implements UpdateScordNotifier {
	MainActivity mainActivity;
	TextView t;
	LinearLayout linearLayout;
	CheckBox allowAdd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mainActivity = this;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		linearLayout = (LinearLayout) findViewById(R.id.banner_linear);
		allowAdd = (CheckBox) findViewById(R.id.checkBox1);

		SharedPreferences sp = this.getApplicationContext()
				.getSharedPreferences("Ad", Context.MODE_PRIVATE);
		boolean show_add = sp.getBoolean("showAd", true);
		allowAdd.setChecked(show_add);

		allowAdd.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				SharedPreferences sp = mainActivity.getApplicationContext()
						.getSharedPreferences("Ad", Context.MODE_PRIVATE);
				Editor e = sp.edit();
				e.putBoolean("showAd", arg1);
				e.commit();
				if (arg1) {
					Toast.makeText(mainActivity,
							"��л����֧��~�ۻ��ٽ������������õ�app!\n(����������Ч)",
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(mainActivity,
							"555~��Ҫ����۵Ĺ�治���ҵ�����\n(����������Ч)", Toast.LENGTH_SHORT)
							.show();
				}
			}

		});

		// ��Activity��һ�δ���ʱ����,�˷�����̳нӿ�UpdateScordNotifier
		YjfSDK.getInstance(this, this).initInstance("", "", "", "");
		// Banner���---------------------------------------------------------------------------------------------------------------------------------
		if (show_add) {
			// ��ʾ�ƹ���
			final bannerView bannerView = BannerSDK.getInstance(this)
					.getBanner();
			linearLayout.addView(bannerView);
			// �ƹ�����ת
			BannerSDK.getInstance(MainActivity.this).showBanner(bannerView);
		}
		// Banner���---------------------------------------------------------------------------------------------------------------------------------

		Util.registerReceiver(this);
		t = (TextView) this.findViewById(R.id.textView_info);
		Button b = (Button) this.findViewById(R.id.button_getnow);

		String w = Util.updateMainText(this);
		if (w.equals("")) {
			t.setText(R.string.psw_neverget);
		} else {
			t.setText(w);
		}

		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Util.sendSMS(mainActivity, "10001", "xykdmm");
				t.setText("��ȡ�У������½��������ˢ�´˴���������ʾ");
			}

		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		String w = Util.updateMainText(this);
		if (w.equals("")) {
			t.setText(R.string.psw_neverget);
		} else {
			t.setText(w);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		Util.unregisterReceiver(this);
		YjfSDK.getInstance(this, this).recordAppClose();
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

}
