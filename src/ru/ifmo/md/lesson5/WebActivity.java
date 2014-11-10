package ru.ifmo.md.lesson5;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebActivity extends Activity{
    public static String REQUEST_URL = "ru.ifmo.md.lesson5_URL",
                         REQUEST_TITLE = "ru.ifmo.md.lesson5_TITLE";
    private String url, title;
    WebChromeClient wcc = new WebChromeClient(){

        public void onProgressChanged(WebView view, int progress) {
            setTitle("Loading " + title);
            setProgress(progress * 100);
            if (progress == 100)
                setTitle(title);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        url = getIntent().getStringExtra(REQUEST_URL);
        title = getIntent().getStringExtra(REQUEST_TITLE);

        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        WebView webview = new WebView(this);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setWebViewClient(new WebViewClient(){

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url){
                view.setWebChromeClient(wcc);
                view.loadUrl(url);
                return true;
            }
        });
        setContentView(webview);
        webview.setWebChromeClient(wcc);
        webview.loadUrl(url);
    }
}
