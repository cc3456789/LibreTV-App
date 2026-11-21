package com.wangshaojie.libretv;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

/**
 * 自带内核浏览器：使用 Firefox GeckoView 内核，不依赖系统老 WebView。
 * 可在内核为 Chrome 66 等老电视上渲染现代网页。
 */
public class GeckoBrowserActivity extends Activity {
    private GeckoView web;
    private GeckoSession session;
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

        status = new TextView(this);
        status.setTextColor(Color.rgb(25,25,25));
        status.setTextSize(14);
        status.setBackgroundColor(Color.rgb(220,232,248));
        status.setPadding(pad, dp(8), pad, dp(8));
        status.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        root.addView(status);

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
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(0, -2, 1f);
        ap.setMargins(0,0,dp(8),0);
        address.setLayoutParams(ap);
        address.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction()==KeyEvent.ACTION_DOWN &&
                (keyCode==KeyEvent.KEYCODE_ENTER || keyCode==KeyEvent.KEYCODE_DPAD_CENTER || keyCode==KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                loadTyped(); return true;
            }
            return false;
        });
        address.setOnEditorActionListener((v, a, e) -> { loadTyped(); return true; });
        bar.addView(address);
        bar.addView(btn("打开", v -> loadTyped()));
        bar.addView(btn("主页", v -> loadUrlSafe(DEFAULT_URL)));
        bar.addView(btn("刷新", v -> loadUrlSafe(address.getText().toString().trim())));
        bar.addView(btn("信息", v -> Toast.makeText(this,"Firefox GeckoView 自带内核 · 不依赖系统 WebView",Toast.LENGTH_LONG).show()));
        root.addView(bar);

        web = new GeckoView(this);
        web.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        web.setBackgroundColor(Color.rgb(238,238,238));
        root.addView(web);
        setContentView(root);

        setStatus("启动中", "正在初始化 Firefox 内核…");

        try {
            GeckoRuntime runtime = GeckoRuntime.create(this);
            session = new GeckoSession();
            session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
                public void onLocationChange(GeckoSession s, String url) {
                    address.setText(url);
                    setStatus("已打开", url);
                }
                public void onPageLoadStateChanged(GeckoSession s, boolean isLoading, boolean canGoBack, boolean canGoForward) {
                    if (!isLoading) setStatus("完成", address.getText().toString());
                }
            });
            web.setSession(session);
            session.open(runtime);
        } catch (Throwable t) {
            setStatus("内核错误", String.valueOf(t.getMessage()));
            return;
        }

        SharedPreferences sp = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String url = sp.getString(KEY_URL, DEFAULT_URL);
        address.setText(url);
        setStatus("就绪", "Firefox GeckoView 内核已启动");
        loadUrlSafe(url);
    }

    private Button btn(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(13);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(Color.rgb(60,60,66));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-2, -2);
        bp.setMargins(dp(3),0,dp(3),0);
        b.setLayoutParams(bp);
        b.setMinWidth(dp(56));
        b.setOnClickListener(l);
        return b;
    }

    private void setStatus(String label, String detail) {
        status.setText("[" + label + "] " + detail);
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
        if (session==null) { setStatus("未就绪", "内核尚未启动"); return; }
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://" + u;
        setStatus("正在打开", u);
        session.loadUri(u);
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
