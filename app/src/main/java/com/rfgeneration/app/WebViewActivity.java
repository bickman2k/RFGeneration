package com.rfgeneration.app;

import com.rfgeneration.app.constants.Constants;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.webkit.WebView;

public class WebViewActivity extends AppCompatActivity {
	private static final String TAG = "WebViewActivity";
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.webview);
        
        final ActionBar actionBar = getSupportActionBar();
        setSupportProgressBarIndeterminateVisibility(true);
		
		Log.i(TAG, "Loading URL: " + getIntent().getStringExtra(Constants.INTENT_WEB_URL));
		
		WebView webView = (WebView)findViewById(R.id.webview);
		String webUrl = getIntent().getStringExtra(Constants.INTENT_WEB_URL);
		
		if(webUrl.endsWith(".jpg"))
			webView.loadData("<img src=\"" + getIntent().getStringExtra(Constants.INTENT_WEB_URL) + 
				"\" width=\"100&#37;\">", "text/html", "utf-8");
		else
			webView.loadUrl(webUrl);
		
		webView.getSettings().setBuiltInZoomControls(true);
		webView.getSettings().setUseWideViewPort(true);
		
		actionBar.setTitle(getIntent().getStringExtra(Constants.INTENT_WEB_TITLE));
		actionBar.setSubtitle(getIntent().getStringExtra(Constants.INTENT_WEB_SUBTITLE));
		setSupportProgressBarIndeterminateVisibility(false);
	}
}
