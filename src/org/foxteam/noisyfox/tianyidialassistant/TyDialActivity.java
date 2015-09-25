package org.foxteam.noisyfox.tianyidialassistant;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockActivity;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Noisyfox on 2015/9/25.
 */
public class TyDialActivity extends SherlockActivity {

    private PSWOperator mPswOper;
    private TextView mTextView_psw, mTextView_status;
    private WebView mWebView;

    private ProgressDialog mProgressDialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ty_dial);

        mPswOper = new PSWOperator(this);

        mTextView_psw = (TextView) findViewById(R.id.textView_psw);
        mTextView_status = (TextView) findViewById(R.id.textView_status);
        mWebView = (WebView) findViewById(R.id.webView_ty);

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);   //在当前的webview中跳转到新的url
                return true;
            }
        });

        updateMainText();
        Thread t = new Thread(){
            @Override
            public void run() {
                refreshInternetStatus();
            }
        };

        t.setDaemon(true);
        t.start();
    }

    public void updateMainText() {
        String psw = mPswOper.getLastPsw(false);

        if (psw.equals("")) {
            mTextView_psw.setText(getString(R.string.label_psw, getString(R.string.label_no_psw)));
        } else {
//            Long time = mPswOper.getRecordTime();
//            SimpleDateFormat formatter = new SimpleDateFormat(
//                    "yyyy年MM月dd日\nHH:mm:ss", Locale.getDefault());
//            Date curDate = new Date(time);
//            String str = formatter.format(curDate);
//            String w = "当前密码:\n" + psw + "\n获取时间:\n" + str;
//            Long dTime_get = System.currentTimeMillis() - time;
//            if (dTime_get > 5.5 * 60 * 60 * 1000) {
//                w += "\n密码可能已经过期!";
//            }
            mTextView_psw.setText(getString(R.string.label_psw, psw));
        }
    }

    private void refreshInternetStatus() {
        showProgress("刷新网络状态");

        try {
            doRefreshInternetStatus();
        } catch (Exception e) {

        }

        hideProgress();
    }

    private void doRefreshInternetStatus(){
        URL url = null;
        try {
            URL u = new URL("http://www.baidu.com");
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("GET");
            c.getResponseCode();
            url = c.getURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(url == null){
            updateStatus("未知错误，请坐和放宽。");
            return;
        }

        String ut = url.toString();
        Pattern urlPartten = Pattern.compile("http://pre\\.f-young\\.cn(?:/)?\\?.*?paramStr=(.*?)(&.*)*\\z");
        Matcher matcher = urlPartten.matcher(ut);

        if(matcher.matches()){
            updateStatus("未连接互联网");

            // save param
            mPswOper.storeLoginParam(matcher.group(1));

            showWeb(true);
        } else if(ut.contains("baidu")){
            updateStatus("已连接互联网");
            showWeb(false);
        } else {
            updateStatus("未知错误，请坐和放宽。");
        }
    }

    private void showProgress(final String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog == null) {
                    mProgressDialog = ProgressDialog.show(TyDialActivity.this, null,
                            msg, true, false);
                } else {
                    mProgressDialog.setMessage(msg);
                }
            }
        });
    }

    private void hideProgress(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mProgressDialog != null){
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
            }
        });
    }

    private void updateStatus(final String status){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextView_status.setText(status);
            }
        });
    }

    private void showWeb(final boolean login){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String param = mPswOper.getLoginParam();
                String url = login ? "http://202.102.13.97/style/portalv4/index.jsp" : "http://202.102.13.97/style/portalv4/logon.jsp";
                if(param != null) {
                    url += "?paramStr=";
                    url += param;
                }
                mWebView.setVisibility(View.VISIBLE);
                mWebView.loadUrl(url);
            }
        });
    }
}