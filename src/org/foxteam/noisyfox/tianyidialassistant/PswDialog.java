package org.foxteam.noisyfox.tianyidialassistant;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.app.Activity;
import android.content.Intent;

public class PswDialog extends Activity {
	PswDialog ins = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ins = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_psw_dialog);
		Intent i = this.getIntent();
		String s = i.getStringExtra("psw");
		Button b = (Button) this.findViewById(R.id.button1);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				ins.finish();
			}
		});

		if (s != null) {
			TextView t = (TextView) this.findViewById(R.id.pswView);
			t.setText(s);
		}
	}

}
