package org.foxteam.noisyfox.tianyidialassistant;

import org.foxteam.noisyfox.tianyidialassistant.PlanManager.OnPlanEditListener;
import org.foxteam.noisyfox.tianyidialassistant.PlanManager.Plan;
import org.foxteam.noisyfox.tianyidialassistant.PlanManager.PlanAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockFragment;

public class OpenWrtFragment extends SherlockFragment implements
		OnItemClickListener {
	static final String TAG = "OpenWrtFragment";

	TyMainActivity father = null;
	OpenWrtHelper opHelper = MyApplication.getOpenWrtHelper();// new
																// OpenWrtHelper();

	View head_view = null;
	View foot_view = null;
	ListView listView = null;

	ImageView iv = null;
	EditText editText_psw_override = null;
	CheckBox checkBox_psw_override = null;

	String progressText;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		father = (TyMainActivity) activity;
		father.registerFragment(this, TAG);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		progressText = getString(R.string.ssh_working);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		father.unregisterFragment(TAG);
		opHelper.flushAll();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_openwrt, container,
				false);

		listView = (ListView) rootView.findViewById(R.id.listView_main);

		head_view = LayoutInflater.from(getActivity()).inflate(
				R.layout.fragment_openwrt_head, null);
		foot_view = LayoutInflater.from(getActivity()).inflate(
				R.layout.fragment_openwrt_foot, null);

		listView.addHeaderView(head_view);
		listView.addFooterView(foot_view);
		listView.setAdapter(MyApplication.getPlanManager().getAdapter(father));
		listView.setOnItemClickListener(this);

		iv = (ImageView) head_view.findViewById(R.id.imageView_status);
		editText_psw_override = (EditText) head_view
				.findViewById(R.id.editText_psw_override);
		checkBox_psw_override = (CheckBox) head_view
				.findViewById(R.id.checkBox_psw_override);

		updateStatus(OpenWrtHelper.STATUS_UNKNOWN);

		head_view.findViewById(R.id.button_check_status).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						father.showProgress(progressText);
						opHelper.loadSSHSettings(father);
						new Thread() {
							@Override
							public void run() {
								boolean needWifiClose = opHelper
										.beforeAction(father);
								int status = opHelper.checkDialStatus();
								opHelper.afterAction(father, needWifiClose,
										true);

								father.mainHandler.sendMessage(father.mainHandler
										.obtainMessage(
												TyMainActivity.MSG_UPDATE_PPPOE_STATUS,
												status, 0));
								father.hideProgress();
							}

						}.start();
					}
				});

		head_view.findViewById(R.id.button_pppoe_connect).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						father.showProgress(progressText);
						opHelper.loadSSHSettings(father);
						new Thread() {
							@Override
							public void run() {
								boolean needWifiClose = opHelper
										.beforeAction(father);
								opHelper.toggleDial(true);
								opHelper.afterAction(father, needWifiClose,
										true);

								father.hideProgress();
							}

						}.start();
					}
				});

		head_view.findViewById(R.id.button_pppoe_stop).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						father.showProgress(progressText);
						opHelper.loadSSHSettings(father);
						new Thread() {
							@Override
							public void run() {
								boolean needWifiClose = opHelper
										.beforeAction(father);
								opHelper.toggleDial(false);
								opHelper.afterAction(father, needWifiClose,
										true);

								father.hideProgress();
							}

						}.start();
					}
				});

		head_view.findViewById(R.id.button_passwd_update).setOnClickListener(
				new OnClickListener() {
					@Override
					public void onClick(View arg0) {
						father.showProgress(progressText);
						opHelper.loadSSHSettings(father);

						final String psw;

						if (checkBox_psw_override.isChecked()) {
							psw = editText_psw_override.getText().toString();
						} else {
							psw = father.mPSWOperator.getLastPsw(false);
						}

						new Thread() {
							@Override
							public void run() {
								boolean needWifiClose = opHelper
										.beforeAction(father);
								opHelper.updatePSW(psw);
								opHelper.afterAction(father, needWifiClose,
										true);

								father.hideProgress();
							}

						}.start();
					}
				});

		return rootView;
	}

	@Override
	public void onDestroy() {
		father.unregisterFragment(TAG);
		opHelper.flushAll();
		super.onDestroy();
	}

	public void updateStatus(int status) {
		int resId;
		switch (status) {
		case OpenWrtHelper.STATUS_ON:
			resId = R.drawable.status_on;
			break;
		case OpenWrtHelper.STATUS_OFF:
			resId = R.drawable.status_off;
			break;
		case OpenWrtHelper.STATUS_UNKNOWN:
		default:
			resId = R.drawable.status_unknown;
		}

		iv.setImageResource(resId);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (view == head_view) {

		} else if (view == foot_view) {
			MyApplication.getPlanManager().createPlan(father,
					new OnPlanEditListener() {
						@Override
						public void onPlanEdit(Plan plan) {
							((PlanAdapter) ((HeaderViewListAdapter) listView
									.getAdapter()).getWrappedAdapter())
									.notifyDataSetChanged();
						}
					});
		} else {
			MyApplication.getPlanManager().editPlan(father, id,
					new OnPlanEditListener() {
						@Override
						public void onPlanEdit(Plan plan) {
							((PlanAdapter) ((HeaderViewListAdapter) listView
									.getAdapter()).getWrappedAdapter())
									.notifyDataSetChanged();
						}
					});
		}
	}

}
