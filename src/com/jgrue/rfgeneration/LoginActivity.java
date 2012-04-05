package com.jgrue.rfgeneration;

import com.jgrue.rfgeneration.constants.Constants;
import com.jgrue.rfgeneration.scrapers.LoginScraper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends FragmentActivity implements OnClickListener {
	private static final String TAG = "LoginActivity";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        // Restore username from preferences if upgrading from v1.
        SharedPreferences settings = getSharedPreferences(Constants.PREFS_FILE, 0);
        String userName = settings.getString(Constants.PREFS_USERNAME, "");
        ((EditText)findViewById(R.id.login_username)).setText(userName);
        
        findViewById(R.id.login_button).setOnClickListener(this);
    }

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.login_button) {
			String userName = ((EditText)findViewById(R.id.login_username)).getText().toString().trim();
			String password = ((EditText)findViewById(R.id.login_password)).getText().toString().trim();
			
			// Store the username.
		    SharedPreferences.Editor editor = getSharedPreferences(Constants.PREFS_FILE, 0).edit();
		    editor.putString(Constants.PREFS_USERNAME, userName);
		    editor.commit();
			
		    // Try logging in with this information.
		    ((Button)v).setEnabled(false);
		    findViewById(R.id.login_progress).setVisibility(View.VISIBLE);
			new LoginTask().execute(v.getContext(), userName, password);
		}
	}
	
	private class LoginTask extends AsyncTask<Object, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Object... arg0) {
			return LoginScraper.validateLogin((Context)arg0[0], (String)arg0[1], (String)arg0[2]);
		}
		
		@Override
		protected void onPostExecute(Boolean validLogin) {
			if(validLogin) {
				// Throw info to RFGenerationActivity.
				Intent myIntent = new Intent(LoginActivity.this, RFGenerationActivity.class);
				startActivityForResult(myIntent, 0);
		        finish();
			} else {
				// Invalid login, blank out the login cookie.
				((EditText)findViewById(R.id.login_password)).setText("");
				SharedPreferences.Editor editor = getSharedPreferences(Constants.PREFS_FILE, 0).edit();
				editor.putString(Constants.PREFS_COOKIE, "");
			    editor.commit();
			    
			    Toast.makeText(LoginActivity.this, "Login failed, please try again.", Toast.LENGTH_SHORT).show();
			    ((Button)findViewById(R.id.login_button)).setEnabled(true);
			    findViewById(R.id.login_progress).setVisibility(View.GONE);
			}
		}
	}
}