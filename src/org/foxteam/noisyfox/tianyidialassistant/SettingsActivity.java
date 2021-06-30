package org.foxteam.noisyfox.tianyidialassistant;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.*;
import android.support.v4.app.NavUtils;
import android.text.InputType;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity {
    public static final int RESULT_OK = 0;
//    public static final int RESULT_REQUIRE_PHONEVER = 1;

//    private static final int MSG_PAIRING_REMOVED = 1;
//    private static final int MSG_PAIRING_PAIRED = 2;
//    private static final int MSG_PAIRING_SHOW_SECONDARYCODE_WAIT = 3;
    private static final int MSG_PROCESS_SHOW = 4;
    private static final int MSG_PROCESS_DISSMISS = 5;
    private static final int MSG_TOAST = 6;
//    private static final int MSG_UPDATE_UI = 7;

    private ProgressDialog processDialog = null;

    private Handler mHandler = new MyHander(this);

    private static final Object mSyncObject = new Object();

    static class MyHander extends Handler {
        WeakReference<SettingsActivity> mActivityRef;

        MyHander(SettingsActivity activity) {
            mActivityRef = new WeakReference<SettingsActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            SettingsActivity activity = mActivityRef.get();
            if (activity == null)
                return;
            switch (msg.what) {
//                case MSG_PAIRING_REMOVED:
//                    activity.updateUI();
//                    break;
//                case MSG_PAIRING_PAIRED:
//                    activity.updateUI();
//                    break;
                case MSG_PROCESS_SHOW: {
                    if (activity.processDialog != null) {
                        activity.processDialog.dismiss();
                        activity.processDialog = null;
                    }
                    String text = (String) msg.obj;
                    activity.processDialog = new ProgressDialog(activity);
                    activity.processDialog
                            .setProgressStyle(ProgressDialog.STYLE_SPINNER);// 设置风格为圆形进度条
                    activity.processDialog
                            .setTitle(R.string.dlgSettings_process_title);// 设置标题
                    activity.processDialog.setMessage(text);
                    activity.processDialog.setIndeterminate(true);// 设置进度条是否为不明确
                    activity.processDialog.setCancelable(false);// 设置进度条是否可以按退回键取消
                    activity.processDialog.show();
                }
                break;
                case MSG_PROCESS_DISSMISS:
                    if (activity.processDialog != null) {
                        activity.processDialog.dismiss();
                        activity.processDialog = null;
                    }
                    break;
//                case MSG_PAIRING_SHOW_SECONDARYCODE_WAIT: {
//                    String secondaryCode = (String) msg.obj;
//
//                    AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
//                            activity);
//                    AlertDialog dlg = dialogBuilder.create();
//                    dlg.setTitle(R.string.dlgPairing_secondary_code_title);
//                    dlg.setMessage(secondaryCode);
//                    dlg.setButton(Dialog.BUTTON_POSITIVE,
//                            activity.getText(R.string.button_ok),
//                            new Dialog.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface arg0, int arg1) {
//                                    synchronized (mSyncObject) {
//                                        mSyncObject.notifyAll();
//                                    }
//                                }
//                            });
//                    dlg.show();
//                }
//                break;
                case MSG_TOAST: {
                    String text = (String) msg.obj;
                    Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
                }
                break;
//                case MSG_UPDATE_UI:
//                    activity.updateUI();
//                    break;
            }
        }

    }

    ;

    private void showToast(String message) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_TOAST, message));
    }

    private void showProcess(String message) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_PROCESS_SHOW, message));
    }

    private void dismissProcess() {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_PROCESS_DISSMISS));
    }

    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = false;

    SettingsActivity mActivity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        this.setResult(RESULT_OK);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupSimplePreferencesScreen();
    }

//    private void updateUI() {
//        Preference phoneVer = findPreference("phone_number_verification");
//        PhoneNumberVerification pnv = new PhoneNumberVerification(this);
//        if (pnv.isPhoneNumberConfirmed()) {
//            phoneVer.setTitle(R.string.pref_title_phone_number_verification_ed);
//            phoneVer.setSummary(R.string.pref_description_phone_number_verification_ed);
//        } else {
//            ((CheckBoxPreference) findPreference("enable_pc_assistant"))
//                    .setChecked(false);
//
//            String pnum = pnv.getUnconfrimedNumber();
//            if (pnum != null) {
//                phoneVer.setTitle(R.string.pref_title_phone_number_verification_ing);
//                phoneVer.setSummary(this
//                        .getString(
//                                R.string.pref_description_phone_number_verification_ing,
//                                pnum));
//            } else {
//                phoneVer.setTitle(R.string.pref_title_phone_number_verification);
//                phoneVer.setSummary("");
//            }
//        }
//
//        Preference pcPairing = findPreference("pc_pairing");
//        Preference pcPairing_check = findPreference("pc_pairing_check");
//        EncryptedUploader uploader = new EncryptedUploader(mActivity);
//        if (uploader.isPaired()) {
//            pcPairing_check.setEnabled(false);
//            pcPairing.setTitle(R.string.pref_title_pc_pairing_paired);
//            pcPairing.setSummary(R.string.pref_description_pc_pairing_paired);
//        } else if (uploader.isPairing()) {
//            pcPairing_check.setEnabled(true);
//            pcPairing.setTitle(R.string.pref_title_pc_pairing_pairing);
//            pcPairing.setSummary(R.string.pref_description_pc_pairing_pairing);
//        } else {
//            pcPairing_check.setEnabled(false);
//
//            ((CheckBoxPreference) findPreference("enable_pc_assistant"))
//                    .setChecked(false);
//
//            pcPairing.setTitle(R.string.pref_title_pc_pairing);
//            pcPairing.setSummary("");
//        }
//
//        ListPreference lp_wifi_ssid = (ListPreference) findPreference("wifi_ssid");
//        WifiManager wifiService = (WifiManager) getSystemService(Context.WIFI_SERVICE);
//        List<WifiConfiguration> wcs = wifiService.getConfiguredNetworks();
//        if (wcs == null || wcs.size() == 0) {
//            String ssid = lp_wifi_ssid.getSharedPreferences().getString(
//                    "wifi_ssid", "");
//            String ssids[];
//            if (!ssid.isEmpty()) {
//                ssids = new String[]{ssid};
//            } else {
//                ssids = new String[]{};
//            }
//            lp_wifi_ssid.setEntries(ssids);
//            lp_wifi_ssid.setEntryValues(ssids);
//        } else {
//            ArrayList<String> ssidsl = new ArrayList<String>();
//            for (WifiConfiguration wc : wcs) {
//                ssidsl.add(wc.SSID);
//            }
//            String ssids[] = new String[ssidsl.size()];
//            ssidsl.toArray(ssids);
//
//            lp_wifi_ssid.setEntries(ssids);
//            lp_wifi_ssid.setEntryValues(ssids);
//        }
//    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);
        bindPreferenceSummaryToValue(findPreference("refresh_frequency"));

//        // Add 'pc assistant' preferences.
//        PreferenceCategory fakeHeader = new PreferenceCategory(this);
//        fakeHeader.setTitle(R.string.pref_header_pc_assistant);
//        getPreferenceScreen().addPreference(fakeHeader);
//        addPreferencesFromResource(R.xml.pref_pc_assistant);
//        findPreference("enable_pc_assistant").setOnPreferenceChangeListener(
//                sPCAssistantListener);

//        // Add 'openwrt' preferences.
//        fakeHeader = new PreferenceCategory(this);
//        fakeHeader.setTitle(R.string.pref_header_openwrt);
//        getPreferenceScreen().addPreference(fakeHeader);
//        addPreferencesFromResource(R.xml.pref_openwrt);
//        bindPreferenceSummaryToValue(findPreference("ssh_address"));
//        bindPreferenceSummaryToValue(findPreference("ssh_port"));
//        bindPreferenceSummaryToValue(findPreference("login_user"));
//        bindPreferenceSummaryToValue(findPreference("login_passwd"));
//        bindPreferenceSummaryToValue(findPreference("wan_interface"));
//        bindPreferenceSummaryToValue(findPreference("phone_number"));

        // 读取配置
//        updateUI();
//        bindPreferenceSummaryToValue(findPreference("wifi_ssid"));

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        if (!isSimplePreferences(this)) {
            loadHeadersFromResource(R.xml.pref_headers, target);
        }
    }

    @Override
    @Deprecated
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         Preference preference) {
        boolean result = true;
        String key = preference.getKey();

//        if ("phone_number_verification".equals(key)) {
//            // NavUtils.navigateUpFromSameTask(this);
//            ((CheckBoxPreference) findPreference("enable_pc_assistant"))
//                    .setChecked(false);
//
//            // mParent.mainHandler
//            // .sendMessage(mParent.mainHandler
//            // .obtainMessage(TyMainActivity.MSG_PHONE_NUMBER_VERIFICATION_START));
//            this.setResult(RESULT_REQUIRE_PHONEVER);
//            this.finish();
//        } else if ("pc_pairing".equals(key)) {
//            final EncryptedUploader uploader = new EncryptedUploader(mActivity);
//            if (uploader.isPaired()) {
//                // 解除配对
//                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
//                        mActivity);
//                AlertDialog dlg = dialogBuilder.create();
//                dlg.setTitle(R.string.dlgPairing_remove_title);
//                dlg.setMessage(mActivity
//                        .getText(R.string.dlgPairing_remove_text));
//                dlg.setButton(Dialog.BUTTON_POSITIVE,
//                        mActivity.getText(R.string.button_remove_pairing),
//                        new Dialog.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface arg0, int arg1) {
//                                showProcess(mActivity
//                                        .getString(R.string.dlgPairing_remove_removing));
//                                new Thread() {
//                                    @Override
//                                    public void run() {
//                                        boolean result = uploader
//                                                .removePairing();
//                                        dismissProcess();
//                                        if (result)
//                                            mHandler.sendMessage(mHandler
//                                                    .obtainMessage(MSG_PAIRING_REMOVED));
//                                        showToast(mActivity
//                                                .getString(result ? R.string.toastPairing_remove_successful
//                                                        : R.string.toastPairing_remove_failed));
//                                    }
//                                }.start();
//                            }
//                        });
//                dlg.setButton(Dialog.BUTTON_NEGATIVE,
//                        mActivity.getText(R.string.button_cancel),
//                        (Dialog.OnClickListener) null);
//                dlg.show();
//            } else if (uploader.isPairing()) {
//                // 放弃配对
//                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
//                        mActivity);
//                AlertDialog dlg = dialogBuilder.create();
//                dlg.setTitle(R.string.dlgPairing_giveup_title);
//                dlg.setMessage(mActivity
//                        .getText(R.string.dlgPairing_giveup_text));
//                dlg.setButton(Dialog.BUTTON_POSITIVE,
//                        mActivity.getText(R.string.button_giveup_pairing),
//                        new Dialog.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface arg0, int arg1) {
//                                showProcess(mActivity
//                                        .getString(R.string.dlgPairing_giveup_givingup));
//                                new Thread() {
//                                    @Override
//                                    public void run() {
//                                        uploader.giveupPairing();
//                                        mHandler.sendMessage(mHandler
//                                                .obtainMessage(MSG_UPDATE_UI));
//                                        dismissProcess();
//                                    }
//                                }.start();
//                            }
//                        });
//                dlg.setButton(Dialog.BUTTON_NEGATIVE,
//                        mActivity.getText(R.string.button_cancel),
//                        (Dialog.OnClickListener) null);
//                dlg.show();
//            } else {
//                // 开始配对
//                LayoutInflater inflater = getLayoutInflater();
//                View layout = inflater.inflate(R.layout.pairing_primary_code,
//                        (ViewGroup) findViewById(R.id.edit_primary_code));
//
//                final EditText editText = (EditText) layout
//                        .findViewById(R.id.edit_primary_code);
//
//                new AlertDialog.Builder(mActivity)
//                        .setTitle(R.string.dlgPairing_primary_code_title)
//                        .setIcon(android.R.drawable.ic_dialog_info)
//                        .setView(layout)
//                        .setPositiveButton(
//                                mActivity.getText(R.string.button_ok),
//                                new Dialog.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface arg0,
//                                                        int arg1) {
//                                        new Thread() {
//                                            @Override
//                                            public void run() {
//
//                                                synchronized (mSyncObject) {
//                                                    String primaryCode = editText
//                                                            .getText()
//                                                            .toString();
//
//                                                    showProcess(mActivity
//                                                            .getString(R.string.dlgPairing_requiring_secondary));
//
//                                                    String secondaryCode = uploader
//                                                            .getSecondaryPairingCode(primaryCode);
//
//                                                    dismissProcess();
//
//                                                    if (secondaryCode == null) {
//                                                        String errmsg = uploader
//                                                                .checkLastErrorMessage();
//                                                        showToast("Error: "
//                                                                + errmsg);
//                                                        return;
//                                                    }
//
//                                                    mHandler.sendMessage(mHandler
//                                                            .obtainMessage(
//                                                                    MSG_PAIRING_SHOW_SECONDARYCODE_WAIT,
//                                                                    secondaryCode));
//
//                                                    try {
//                                                        mSyncObject.wait();
//                                                    } catch (InterruptedException e) {
//                                                        e.printStackTrace();
//                                                    }
//                                                    checkPairing(uploader);
//                                                }
//                                            }
//                                        }.start();
//                                    }
//                                })
//                        .setNegativeButton(
//                                mActivity.getText(R.string.button_cancel), null)
//                        .show();
//            }
//        } else if ("pc_pairing_check".equals(key)) {
//            // 重试配对
//            final EncryptedUploader uploader = new EncryptedUploader(mActivity);
//            if (uploader.isPairing()) {
//                new Thread() {
//                    @Override
//                    public void run() {
//                        checkPairing(uploader);
//                    }
//                }.start();
//            }
//        } else if ("checkbox_advertisement".equals(key)) {
//            CheckBoxPreference cp = (CheckBoxPreference) preference;
//            if (cp.isChecked()) {
//                Toast.makeText(this, "感谢您的支持~咱会再接再厉做出更好的app!\n(重启程序生效)",
//                        Toast.LENGTH_SHORT).show();
//            } else {
//                Toast.makeText(this, "555~不要嘛。。咱的广告不会乱弹的啦\n(重启程序生效)",
//                        Toast.LENGTH_SHORT).show();
//            }
//        } else {
            result = super.onPreferenceTreeClick(preferenceScreen, preference);
//        }
        return result;
    }

//    private void checkPairing(EncryptedUploader uploader) {
//        showProcess(mActivity.getString(R.string.dlgPairing_verify));
//        boolean result = uploader.finishPairing();
//
//        dismissProcess();
//
//        if (result) {
//            mHandler.sendMessage(mHandler.obtainMessage(MSG_PAIRING_PAIRED));
//        } else {
//            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_UI));
//        }
//        String msgTost = mActivity
//                .getString(result ? R.string.toastPairing_pair_successful
//                        : R.string.toastPairing_pair_failed);
//        showToast(msgTost);
//    }

//    private Preference.OnPreferenceChangeListener sPCAssistantListener = new Preference.OnPreferenceChangeListener() {
//
//        @Override
//        public boolean onPreferenceChange(Preference preference, Object value) {
//            if (preference.getKey().equals("enable_pc_assistant")) {
//                if (value.equals(true)) {
//                    PhoneNumberVerification pnv = new PhoneNumberVerification(
//                            mActivity);
//                    if (pnv.isPhoneNumberConfirmed()) {
//                        // 启动后台服务
//                    } else {
//                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(
//                                mActivity);
//                        AlertDialog dlg = dialogBuilder.create();
//                        dlg.setTitle(R.string.dlgPhoneVer_need_to_ver_title);
//                        dlg.setMessage(mActivity
//                                .getText(R.string.dlgPhoneVer_need_to_ver_text));
//                        dlg.setButton(Dialog.BUTTON_POSITIVE,
//                                mActivity.getText(R.string.button_ver_now),
//                                new Dialog.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface arg0,
//                                                        int arg1) {
//                                        mActivity
//                                                .setResult(RESULT_REQUIRE_PHONEVER);
//                                        mActivity.finish();
//                                    }
//                                });
//                        dlg.setButton(Dialog.BUTTON_NEGATIVE,
//                                mActivity.getText(R.string.button_cancel),
//                                (Dialog.OnClickListener) null);
//                        dlg.show();
//                        return false;
//                    }
//                } else {
//                    // 结束后台服务
//                }
//                return false;// 目前强制关闭此功能
//            }
//            return true;
//        }
//
//    };

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference
                        .setSummary(index >= 0 ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone
                                .getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else if (preference instanceof EditTextPreference) {
                EditTextPreference p = (EditTextPreference) preference;
                int iType = p.getEditText().getInputType();
                if ((iType & InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0) {
                    char c[] = new char[stringValue.length()];
                    Arrays.fill(c, '*');
                    preference.setSummary(String.valueOf(c));
                } else {
                    preference.setSummary(stringValue);
                }
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference
                .setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(
                preference,
                PreferenceManager.getDefaultSharedPreferences(
                        preference.getContext()).getString(preference.getKey(),
                        ""));
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);
        }
    }

//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
//    public static class PCAssistantPreferenceFragment extends
//            PreferenceFragment {
//        @Override
//        public void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//            addPreferencesFromResource(R.xml.pref_pc_assistant);
//        }
//    }
//
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
//    public static class OpenWrtPreferenceFragment extends PreferenceFragment {
//        @Override
//        public void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//            addPreferencesFromResource(R.xml.pref_openwrt);
//            bindPreferenceSummaryToValue(findPreference("wifi_ssid"));
//            bindPreferenceSummaryToValue(findPreference("ssh_address"));
//            bindPreferenceSummaryToValue(findPreference("ssh_port"));
//            bindPreferenceSummaryToValue(findPreference("login_user"));
//            bindPreferenceSummaryToValue(findPreference("login_passwd"));
//            bindPreferenceSummaryToValue(findPreference("wan_interface"));
//            bindPreferenceSummaryToValue(findPreference("phone_number"));
//            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
//            // to their values. When their values change, their summaries are
//            // updated to reflect the new value, per the Android Design
//            // guidelines.
//            // bindPreferenceSummaryToValue(findPreference("enable_pc_assistant"));
//        }
//    }
}
