package com.chairtrax.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class RadiusCalibration extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_radius_calibration);
		
		Intent intent = this.getIntent();
		
		Button mStart = (Button) findViewById(R.id.start_radius_calibration);
		Button mFinish = (Button) findViewById(R.id.finish_radius_calibration);
		mFinish.setEnabled(false);
		
		if (intent.getBooleanExtra("finish", false)) {
			mFinish.setEnabled(true);
			mStart.setEnabled(false);
		}

		mStart.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent returnIntent = new Intent();
				returnIntent.putExtra("start", true);
				setResult(RESULT_OK, returnIntent);
				finish();	
			}
		});
		
		mFinish.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent returnIntent = new Intent();
				returnIntent.putExtra("finish", true);
				setResult(RESULT_OK, returnIntent);
				finish();	
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		return super.onOptionsItemSelected(item);
	}
}
