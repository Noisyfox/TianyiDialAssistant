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
							"感谢您的支持~咱会再接再厉做出更好的app!\n(重启程序生效)",
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(mainActivity,
							"555~不要嘛。。咱的广告不会乱弹的啦\n(重启程序生效)", Toast.LENGTH_SHORT)
							.show();
				}
			}

		});

		// 当Activity第一次创建时调用,此方法需继承接口UpdateScordNotifier
		YjfSDK.getInstance(this, this).initInstance("", "", "", "");
		// Banner广告---------------------------------------------------------------------------------------------------------------------------------
		if (show_add) {
			// 显示推广条
			final bannerView bannerView = BannerSDK.getInstance(this)
					.getBanner();
			linearLayout.addView(bannerView);
			// 推广条轮转
			BannerSDK.getInstance(MainActivity.this).showBanner(bannerView);
		}
		// Banner广告---------------------------------------------------------------------------------------------------------------------------------

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
				t.setText("获取中，请重新进入程序以刷新此处的密码显示");
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
