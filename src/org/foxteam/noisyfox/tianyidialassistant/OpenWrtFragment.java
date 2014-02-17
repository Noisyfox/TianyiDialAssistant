package org.foxteam.noisyfox.tianyidialassistant;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.actionbarsherlock.app.SherlockFragment;

public class OpenWrtFragment extends SherlockFragment {
	static final String TAG = "OpenWrtFragment";
	TyMainActivity father = null;

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
		return inflater.inflate(R.layout.activity_openwrt, container, false);
	}

	@Override
	public void onDestroy() {
		father.unregisterFragment(TAG);
		super.onDestroy();
	}
}
