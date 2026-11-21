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
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 电视浏览器：可输入任意网址，默认打开 NAS 上的 LibreTV。
 * 顶部状态栏实时显示 WebView 内核版本、加载状态与错误码，便于在电视上定位问题。
 */
public class BrowserActivity extends Activity {
    private WebView web;
    private EditText address;
    private TextView status;
    private static final String PREFS = "browser";
    private static final String KEY_URL = "last_url";
    private static final String DEFAULT_URL = "http://192.168.50.190:8899";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int pad = dp(10);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(18,18,20));

        // 顶部状态栏（醒目、可换行，用于显示错误码）
        status = new TextView(this);
        status.setTextColor(Color.rgb(25,25,25));
        status.setTextSize(14);
        status.setBackgroundColor(Color.rgb(220,232,248));
        status.setPadding(pad, dp(8), pad, dp(8));
        status.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(status);

        // 地址栏
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(pad, dp(8), pad, dp(4));
        bar.setGravity(Gravity.CENTER_VERTICAL);

        address = new EditText(this);
        address.setTextColor(Color.WHITE);
        address.setHintTextColor(Color.GRAY);
        address.setHint("输入网址");
        address.setTextSize(15);
        address.setSingleLine(true);
        address.setBackgroundColor(Color.rgb(40,40,44));
        address.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        ap.setMargins(0,0,dp(8),0);
        address.setLayoutParams(ap);
        address.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction()==KeyEvent.ACTION_DOWN &&
                (keyCode==KeyEvent.KEYCODE_ENTER || keyCode==KeyEvent.KEYCODE_DPAD_CENTER || keyCode==KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                loadTyped();
                return true;
            }
            return false;
        });
        address.setOnEditorActionListener((v, actionId, event) -> { loadTyped(); return true; });
        bar.addView(address);

        bar.addView(btn("打开", v -> loadTyped()));
        bar.addView(btn("返回", v -> { if (web.canGoBack()) web.goBack(); }));
        bar.addView(btn("刷新", v -> web.reload()));
        bar.addView(btn("自检", v -> {
            web.loadUrl("data:text/html;charset=utf-8,<body style='background:%23111;color:%23fff;font-size:96px;text-align:center;padding-top:240px'>WEBVIEW OK</body>");
            setStatus("自检", "加载测试页…");
        }));
        root.addView(bar);

        // 网页区域
        web = new WebView(this);
        web.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        web.setBackgroundColor(Color.rgb(238,238,238));
        configureWeb(web);
        root.addView(web);

        setContentView(root);

        setStatus("就绪", "内核 " + kernel());
        SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String url = sp.getString(KEY_URL, DEFAULT_URL);
        address.setText(url);
        loadUrlSafe(url);
    }

    private Button btn(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(13);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.rgb(60,60,66));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.setMargins(dp(3),0,dp(3),0);
        b.setLayoutParams(bp);
        b.setMinWidth(dp(56));
        b.setOnClickListener(l);
        return b;
    }

    private void setStatus(String label, String detail) {
        status.setText("[" + label + "] " + detail + "\n内核 " + kernel());
    }

    private String kernel() {
        try {
            String ua = web.getSettings().getUserAgentString();
            int i = ua.indexOf("Chrome/");
            if (i>=0) { int j=i+7; while(j<ua.length() && (Character.isDigit(ua.charAt(j))||ua.charAt(j)=='.')) j++; return "Chrome " + ua.substring(i+7,j); }
            int k = ua.indexOf("AppleWebKit/");
            if (k>=0) { int m=k+12; while(m<ua.length() && (Character.isDigit(ua.charAt(m))||ua.charAt(m)=='.'||ua.charAt(m)=='+')) m++; return "WebKit " + ua.substring(k+12,m); }
            return "未知";
        } catch (Exception e) { return "未知"; }
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
        if (Build.VERSION.SDK_INT >= 21) s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString(s.getUserAgentString() + " (LibreTV-TV-Browser)");
        CookieManager.getInstance().setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= 21) CookieManager.getInstance().setAcceptThirdPartyCookies(w, true);

        w.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                address.setText(url);
                return true;
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                setStatus("加载中", url);
            }
            @Override public void onPageFinished(WebView view, String url) {
                setStatus("完成", view.getTitle()!=null && !view.getTitle().isEmpty() ? view.getTitle() : url);
            }
            @Override public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                setStatus("失败", "错误 " + errorCode + " " + description + " · " + failingUrl);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (Build.VERSION.SDK_INT >= 23) setStatus("失败", "错误 " + err.getErrorCode() + " " + err.getDescription() + " · " + req.getUrl());
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest req, WebResourceResponse resp) {
                if (Build.VERSION.SDK_INT >= 21) setStatus("HTTP错误", resp.getStatusCode() + " " + resp.getReasonPhrase() + " · " + req.getUrl());
            }
        });
        w.setWebChromeClient(new WebChromeClient() {
            @Override public void onReceivedTitle(WebView view, String t) {
                if (t!=null && !t.isEmpty()) setStatus("完成", t);
            }
        });
    }

    private void loadTyped() {
        String u = address.getText().toString().trim();
        if (u.isEmpty()) { Toast.makeText(this,"请输入网址",Toast.LENGTH_SHORT).show(); return; }
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://" + u;
        address.setText(u);
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_URL, u).apply();
        loadUrlSafe(u);
    }

    private void loadUrlSafe(String u) {
        setStatus("正在打开", u);
        web.loadUrl(u);
    }

    @Override public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_BACK && web.canGoBack()) { web.goBack(); return true; }
        return super.onKeyDown(keyCode, event);
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
