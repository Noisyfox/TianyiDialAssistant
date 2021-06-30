package org.foxteam.noisyfox.tianyidialassistant;

import android.app.ProgressDialog;
import android.os.Build;
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

    private final static String JS_ShowLogin = "document.getElementById('form1').getElementsByClassName('loginb1')[1].removeAttribute('style');";
    private final static String JS_EnterPsw = "document.getElementById('PassWord1').value='%s'";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ty_dial);

        mPswOper = new PSWOperator(this);

        mTextView_psw = (TextView) findViewById(R.id.textView_psw);
        mTextView_status = (TextView) findViewById(R.id.textView_status);
        mWebView = (WebView) findViewById(R.id.webView_ty);

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    if (view.getUrl().contains("index.jsp")) {
                        String psw = mPswOper.getLastPsw(false);
                        String pswJs = "undefined;";
                        if(!psw.isEmpty()){
                            pswJs = String.format(JS_EnterPsw, psw);
                        }
                        if (Build.VERSION.SDK_INT >= 19) {
                            mWebView.evaluateJavascript(JS_ShowLogin, null);
                            mWebView.evaluateJavascript(pswJs, null);
                        } else {
                            mWebView.loadUrl("javascript:" + JS_ShowLogin + ";undefined;");
                            mWebView.loadUrl("javascript:" + pswJs + ";undefined;");
                        }
                    }
                }
            }
        });
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
            mTextView_psw.setText(getString(R.string.label_psw, psw));
        }
    }

    private void refreshInternetStatus() {
        showProgress("刷新网络状态");

        try {
            doRefreshInternetStatus();
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("未知错误，请坐和放宽。");
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
                mWebView.requestFocus();
                mWebView.requestFocusFromTouch();
            }
        });
    }
}