package com.du.codewarriorcontact.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

import com.du.codewarriorcontact.R;
import com.du.codewarriorcontact.util.MessageUtilities;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

public class EmergencyShakeSettingFragment extends Fragment implements OnClickListener{
	
	private CheckBox check_shake;
	private RadioGroup shakeRadio;
	LinearLayout shakeLayout;
	private SharedPreferences preferences;
	private Editor editor;
	private Animation in;
	private Animation out;
	RadioButton policeRadio,hospitalRadio,fireStationRadio,relativeRadio;
	
	
	
	
	private static final Logger logger = Logger.getLogger(EmergencyShakeSetting.class.getSimpleName());

	private static ArrayList<String> sensArrayList = null;
	private static ArrayAdapter<CharSequence> modesAdapter = null;

	private static PowerManager powerManager = null;
	private static WakeLock wakeLock = null;

	public static IStepService mService = null;
	public static Intent stepServiceIntent = null;

	private static int sensitivity = 100;
	
	public static Context mContext;
		
	
	
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.setting_emergency_shake, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
    	
    	preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		editor = preferences.edit();

		 in = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
         out = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
        
		
		
		shakeLayout = (LinearLayout) getActivity().findViewById(R.id.shake_setting);
		check_shake = (CheckBox) getActivity().findViewById(R.id.check_shake);
		check_shake.setOnClickListener(this);
		
		String status = preferences.getString("shake", "0");
		
		if(status.equals("on"))
		{
			shakeLayout.setVisibility(View.VISIBLE);
			check_shake.setChecked(true);
					
		}
		else
			shakeLayout.setVisibility(View.INVISIBLE);
		
		
		
		
		
		mContext = getActivity().getBaseContext();
		
		powerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				"EmergencyShakeSetting");

		if (stepServiceIntent == null) {
			Bundle extras = new Bundle();
			extras.putInt("int", 1);
			stepServiceIntent = new Intent(getActivity(), StepService.class);
			stepServiceIntent.putExtras(extras);
		}

		int sensIdx = 0;
		sensIdx = (int)Integer.parseInt(preferences.getString("shake_sensitivity_idx", "0"));
		
		if (sensArrayList == null) {
			String[] sensArray = getResources().getStringArray(
					R.array.sensitivity);
			sensArrayList = new ArrayList<String>(Arrays.asList(sensArray));
		}
		

		Spinner sensSpinner = (Spinner) getActivity().findViewById(R.id.input_sensitivity_spinner);
		modesAdapter = ArrayAdapter.createFromResource(getActivity(),
				R.array.sensitivity, android.R.layout.simple_spinner_item);
		modesAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sensSpinner.setOnItemSelectedListener(sensListener);
		sensSpinner.setAdapter(modesAdapter);
		sensSpinner.setSelection(sensIdx);
		
		
				
		String status1 = preferences.getString("shake", "0");
		if(status.equals("on"))
		{
			start();
		}		

    }
    
    public static Context getContext() {
        return mContext;
    }

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	private OnItemSelectedListener sensListener = new OnItemSelectedListener() {
		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			CharSequence seq = modesAdapter.getItem(arg2);
			String sensString = String.valueOf(seq);
			if (sensString != null) {
				//sensitivity = Integer.parseInt(sensString);
				sensitivity = 4;
				if(arg2==0)	sensitivity = 3;
				if(arg2==1)	sensitivity = 4;
				if(arg2==2)	sensitivity = 6;
				if(arg2==3)	sensitivity = 7;
				if(arg2==4)	sensitivity = 8;
				editor.putString("shake_sensitivity",String.valueOf(sensitivity));
				editor.putString("shake_sensitivity_idx", String.valueOf(arg2));
				editor.commit();
				StepDetector.setSensitivity(sensitivity);
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// Ignore
		}
	};

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	private void start() {
		if (!wakeLock.isHeld())
			wakeLock.acquire();

		startStepService();
		bindStepService();
		       
        editor.putString("service", "en");
        editor.putString("shake", "on");
		editor.commit();
	}

	private void stop() {
		if (wakeLock.isHeld())
			wakeLock.release();

		unbindStepService();
		stopStepService();

    	editor.putString("service", "dis");
    	editor.putString("shake", "off");
		editor.commit();
	}

	private void startStepService() {
		try {
			getActivity().startService(stepServiceIntent);
		} catch (Exception e) {
			logger.info(e.getLocalizedMessage());
		}
	}

	private void stopStepService() {
		try {
			getActivity().stopService(stepServiceIntent);
		} catch (Exception e) {
			logger.info(e.getLocalizedMessage());
		}
	}

	private void bindStepService() {
		try {
			getActivity().bindService(stepServiceIntent, mConnection,
					Context.BIND_AUTO_CREATE);
		} catch (Exception e) {
			logger.info(e.getLocalizedMessage());
		}
	}

	private void unbindStepService() {
		try {
			getActivity().unbindService(mConnection);
		} catch (Exception e) {
			logger.info(e.getLocalizedMessage());
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = IStepService.Stub.asInterface(service);
			try {
				mService.setSensitivity(sensitivity);
			} catch (RemoteException e) {
				logger.info("Exception: " + e.getMessage());
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			mService = null;
		}
	};

	
	private DialogInterface.OnClickListener yesStopClick = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			
			start();
			
			shakeLayout.startAnimation(in);
			shakeLayout.setVisibility(View.VISIBLE);
			editor.putString("shake", "on");
			editor.putString("shake_type", "relative");
			editor.commit();
			
			DualPaneSettingsRefresher.refreshListAdapter(getActivity());
		}
	};

	private DialogInterface.OnClickListener noStopClick = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			
			check_shake.setChecked(false);
			
			/*if (mService != null)
				try {
					startStopButton.setChecked(mService.isRunning());
				} catch (RemoteException e) {
					logger.info("Exception: " + e.getMessage());
				}*/
			
		}
	};
	
	/* All check setting */
	@Override
	public void onClick(View v) {
		
		switch(v.getId())
		{
		
		case R.id.check_shake :
					
			if(check_shake.isChecked())
			{
				MessageUtilities.confirmUser(getActivity(), "Do you really want to activate Shake ?",yesStopClick, noStopClick);	
			}
			
			else {
				
				stop();
				
				shakeLayout.startAnimation(out);
				shakeLayout.setVisibility(View.INVISIBLE);
				
				editor.putString("shake", "off");
				editor.putString("shake_type", "na");
				editor.commit();	
				DualPaneSettingsRefresher.refreshListAdapter(getActivity());
			}	
		break;
		}
		
	}


}
