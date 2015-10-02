package com.xperia64.twinge;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class TwingeActivity extends AppCompatActivity {

	Button button;
	EditText edittext;
	CheckBox checkbox;
	final String regex1 = "^(http://|)([w]{3}\\.|)(twitch.tv/).*(/v/)\\d{7,8}$";
	final String regex2 = "^\\d{7,8}$";
	final String regex3 = "^(http://).*(/v1/AUTH_system).*(m3u8)$";
	final String apiTmplUrl = "https://api.twitch.tv/api/vods/%s/access_token";
	final String usherTmplUrl = "http://usher.twitch.tv/vod/%s?nauthsig=%s&allow_source=true&nauth=%s";
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_twinge);
		getSupportActionBar().setDisplayShowHomeEnabled(true);
		getSupportActionBar().setLogo(R.drawable.ic_launcher);
		getSupportActionBar().setDisplayUseLogoEnabled(true);
		getSupportActionBar().setTitle("   " + getResources().getString(R.string.app_name));
		button = (Button) findViewById(R.id.button1);
		edittext = (EditText) findViewById(R.id.editText1);
		checkbox = (CheckBox) findViewById(R.id.checkBox1);
		button.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0)
			{
				String s = edittext.getText().toString();
				if(s.toLowerCase(Locale.US).matches(regex1))
					attemptToDownload(s.substring(s.lastIndexOf('/')+1));
				else if(s.matches(regex2))
					attemptToDownload(s);
				else
					Toast.makeText(TwingeActivity.this, "This doesn't look like a new twitch video system VOD URL/ID", Toast.LENGTH_SHORT).show();
			}
			
		});
	}

	public class HttpsDownloadTask extends AsyncTask<String, Integer, String> {

	    private Context context;
	    private PowerManager.WakeLock mWakeLock;
	    private ProgressDialog prog;
	    public HttpsDownloadTask(Context context) {
	        this.context = context;
	    }
	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        // take CPU lock to prevent CPU from going off if the user 
	        // presses the power button during download
	        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
	        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
	             getClass().getName());
	        mWakeLock.acquire();
	        
	  	  prog = new ProgressDialog(context);
	  	  prog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
	  		    @Override
	  		    public void onClick(DialogInterface dialog, int which) {
	  		    	
	  		        dialog.dismiss();
	  		    }
	  		});
	  	prog.setOnCancelListener(new DialogInterface.OnCancelListener() {
	  	    @Override
	  	    public void onCancel(DialogInterface dialog) {
	  	        HttpsDownloadTask.this.cancel(true);
	  	    }
	  	});
	  	  prog.setTitle("Grabbing API token...");
	  	  prog.setMessage("Loading...");       
	  	  prog.setCancelable(false);
	  	  prog.show();
	    }
		@Override
		protected String doInBackground(String... params)
		{
			try{
			URL url = null;
			url = new URL(params[0]);
			HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
	        connection.connect();
	        if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
	        	Toast.makeText(context, "Bad API connection", Toast.LENGTH_SHORT).show();
	            return null;
	        }
	        InputStream input = connection.getInputStream();
	        return convertStreamToString(input);
			}catch(Exception e){e.printStackTrace();}
			return null;
		}
		@Override
		protected void onPostExecute(String result) {
			 mWakeLock.release();
		        prog.dismiss();
		        ((TwingeActivity)context).httpsDownloadFinished(result);
		    }
	}
	public class HttpDownloadTask extends AsyncTask<String, Integer, String> {

	    private Context context;
	    private PowerManager.WakeLock mWakeLock;
	    private ProgressDialog prog;
	    public HttpDownloadTask(Context context) {
	        this.context = context;
	    }
	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        // take CPU lock to prevent CPU from going off if the user 
	        // presses the power button during download
	        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
	        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
	             getClass().getName());
	        mWakeLock.acquire();
	        
	  	  prog = new ProgressDialog(context);
	  	  prog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
	  		    @Override
	  		    public void onClick(DialogInterface dialog, int which) {
	  		    	
	  		        dialog.dismiss();
	  		    }
	  		});
	  	prog.setOnCancelListener(new DialogInterface.OnCancelListener() {
	  	    @Override
	  	    public void onCancel(DialogInterface dialog) {
	  	        HttpDownloadTask.this.cancel(true);
	  	    }
	  	});
	  	  prog.setTitle("Loading playlists...");
	  	  prog.setMessage("Loading...");       
	  	  prog.setCancelable(false);
	  	  prog.show();
	    }
		@Override
		protected String doInBackground(String... params)
		{
			try{
			//System.out.println(params[0]);
				Log.v("Twinge","Loading: "+params[0]);
			URL url = null;
			url = new URL(params[0]);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	        connection.connect();
	        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
	        	Toast.makeText(context, "Bad VOD connection", Toast.LENGTH_SHORT).show();
	            return null;
	        }
	        InputStream input = connection.getInputStream();
	        return convertStreamToString(input);
			}catch(Exception e){e.printStackTrace();}
			return null;
		}
		@Override
		protected void onPostExecute(String result) {
			mWakeLock.release();
			prog.dismiss();
		        ((TwingeActivity)context).httpDownloadFinished(result);
		    }
		
	}
	public void attemptToDownload(final String vodId)
	{
		final HttpsDownloadTask downloadTask = new HttpsDownloadTask(this);
		String apiUrl = String.format(apiTmplUrl,vodId);
		downloadTask.execute(apiUrl);
        
	}
	public void httpDownloadFinished(String result)
	{
		if(result==null||TextUtils.isEmpty(result)||!result.startsWith("#EXTM3U"))
		{
			Toast.makeText(TwingeActivity.this, "Bad twitch VOD result", Toast.LENGTH_SHORT).show();
			return;
		}
		String[] lines = result.split("\n");
		final ArrayList<String> qualities = new ArrayList<String>();
		final ArrayList<String> urls = new ArrayList<String>();
		for(String line : lines)
		{
			if(line.matches(regex3))
			{
				//Video playlist line
				int i = line.lastIndexOf('/');
				if (i > 0) i = line.lastIndexOf('/', i - 1);
				String qual = (i >= 0) ? line.substring(i + 1) : null;
				qual = qual.substring(0,qual.lastIndexOf('/'));
				if(qual==null)
					continue;
				qual = Character.toUpperCase(qual.charAt(0)) + qual.substring(1);
				if(qual.equals("Chunked"))
					qual = "Chunked (Raw)";
				qualities.add(qual);	
				urls.add(line);
			}
		}
		if(!(qualities.size()==urls.size()&&qualities.size()!=0))
			return;
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                TwingeActivity.this);
        builderSingle.setIcon(R.drawable.ic_launcher);
        builderSingle.setTitle("Select a video quality");
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                TwingeActivity.this,
                android.R.layout.select_dialog_item);
        for(String qualcomm : qualities )
        	arrayAdapter.add(qualcomm);
        builderSingle.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(arrayAdapter,
                new DialogInterface.OnClickListener() {

                    @SuppressLint("NewApi")
					@SuppressWarnings("deprecation")
					@Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Toast.makeText(TwingeActivity.this, urls.get(which), Toast.LENGTH_LONG).show();
                        if(checkbox.isChecked())
                        {
                        	if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                        		ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE); 
                            	ClipData clip = ClipData.newPlainText("Twinge Text", urls.get(which));
                            	clipboard.setPrimaryClip(clip);
                            } else {
								android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                clipboard.setText(urls.get(which));
                            }
                        	
                        }
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(urls.get(which)));
                        startActivity(i);
                    }
                });
        builderSingle.show();
		
	}
	public void httpsDownloadFinished(String result)
	{
		//Toast.makeText(TwingeActivity.this, result, Toast.LENGTH_SHORT).show();
		//System.out.println(result.length());
		if(result==null||TextUtils.isEmpty(result)||!((result.length()==183||result.length()==184)&&result.startsWith("{\"token\":\"{")&&(result.endsWith("\"}"))))
		{
			Toast.makeText(TwingeActivity.this, "Bad twitch API result (Is this a valid video ID?)", Toast.LENGTH_SHORT).show();
			return;
		}
		String token = result.substring(10,result.length()==184?133:132);
		token = token.replace("\\", "");
		String tokenSig = result.substring(result.length()==184?142:141,result.length()==184?182:181);
		String vodId = result.substring(39,result.length()==184?47:46);
		String usherUrl = String.format(usherTmplUrl, vodId, tokenSig, token);
		final HttpDownloadTask downloadTask = new HttpDownloadTask(this);
		downloadTask.execute(usherUrl);
	}
	
	public static String convertStreamToString(InputStream is) {
	    @SuppressWarnings("resource")
		java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
	    String sc = "";
	    if(s.hasNext())
	    {
	    	sc = s.next();
	    }
	    s.close();
	    return sc;
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.twinge, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();
		if (id == R.id.action_help)
		{
			AlertDialog dialog = new AlertDialog.Builder(this).create();
		    dialog.setTitle("Help");
		    dialog.setMessage("Paste either the entire /v/ twitch URL or just the numbers at the end and press the button.\n\n" +
		    		"The checkbox lets you decide whether you want to copy the playlist URL to your clipboard.\n\n" +
		    		"I recommend using MX Player to view streams.\n\n" +
		    		"VODs work in the official Twitch app now, and there are other 3rd party Twitch clients available, but I still use this app myself for various reasons.\n\n");
		    dialog.setCancelable(true);
		    dialog.setButton(DialogInterface.BUTTON_POSITIVE, getResources().getString(android.R.string.ok), new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int buttonId) {

		        }
		    });
		    dialog.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
