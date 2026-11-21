package com.wangshaojie.libretv;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/** 通用网络浏览器：可输入任意网址，默认打开 NAS 上的 LibreTV。遥控器友好。 */
public class BrowserActivity extends Activity {
    private WebView web;
    private EditText address;
    private ProgressBar progress;
    private TextView title;
    private static final String PREFS = "browser";
    private static final String KEY_URL = "last_url";
    private static final String DEFAULT_URL = "http://192.168.50.190:8899";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().setFlags(0x01000000, 0x01000000); // FLAG_HARDWARE_ACCELERATED
        }

        int pad = dp(12);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(18,18,20));

        // 顶部：标题 + 地址栏 + 按钮
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(pad, pad, pad, dp(6));
        bar.setGravity(Gravity.CENTER_VERTICAL);

        title = new TextView(this);
        title.setText("网页浏览");
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setMinWidth(dp(70));
        bar.addView(title);

        address = new EditText(this);
        address.setTextColor(Color.WHITE);
        address.setHintTextColor(Color.GRAY);
        address.setHint("输入网址");
        address.setTextSize(15);
        address.setSingleLine(true);
        address.setBackgroundColor(Color.rgb(40,40,44));
        address.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ap.setMargins(dp(8),0,dp(8),0);
        address.setLayoutParams(ap);
        final Activity act = this;
        address.setOnEditorActionListener((v, actionId, event) -> { loadTyped(); return true; });
        address.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction()==KeyEvent.ACTION_DOWN && keyCode==KeyEvent.KEYCODE_ENTER) { loadTyped(); return true; }
            return false;
        });
        bar.addView(address);

        Button go = btn("打开");
        go.setOnClickListener(v -> loadTyped());
        bar.addView(go);
        Button back = btn("返回");
        back.setOnClickListener(v -> { if (web.canGoBack()) web.goBack(); });
        bar.addView(back);
        Button reload = btn("刷新");
        reload.setOnClickListener(v -> web.reload());
        bar.addView(reload);
        root.addView(bar);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));
        root.addView(progress);

        // WebView
        web = new WebView(this);
        web.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        web.setBackgroundColor(Color.WHITE);
        configureWeb(web);
        root.addView(web);

        setContentView(root);

        SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String url = sp.getString(KEY_URL, DEFAULT_URL);
        address.setText(url);
        web.loadUrl(url);
    }

    private Button btn(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(13);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.rgb(60,60,66));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.setMargins(dp(3),0,dp(3),0);
        b.setLayoutParams(bp);
        b.setMinWidth(dp(56));
        return b;
    }

    private void configureWeb(WebView w) {
        WebSettings s = w.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        if (Build.VERSION.SDK_INT >= 21) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        // 桌面 UA，避免电视被识别为移动设备导致页面降级/黑屏
        String ua = s.getUserAgentString();
        s.setUserAgentString(ua + " (LibreTV-TV-Browser)");

        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= 21) CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);

        if (Build.VERSION.SDK_INT >= 19) WebView.setWebContentsDebuggingEnabled(true);

        w.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                address.setText(url);
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                title.setText(view.getTitle()!=null && !view.getTitle().isEmpty() ? view.getTitle() : "网页浏览");
                getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_URL, url).apply();
            }
        });
        w.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress==100?View.GONE:View.VISIBLE);
            }
            @Override public void onReceivedTitle(WebView view, String t) {
                title.setText(t!=null && !t.isEmpty()? t : "网页浏览");
            }
        });
    }

    private void loadTyped() {
        String u = address.getText().toString().trim();
        if (u.isEmpty()) return;
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://" + u;
        address.setText(u);
        web.loadUrl(u);
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_BACK && web.canGoBack()) { web.goBack(); return true; }
        return super.onKeyDown(keyCode, event);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
