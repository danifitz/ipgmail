package uk.ac.brookes.danielf.pgpmail.activities;

import java.io.File;

import uk.ac.brookes.danielf.pgpmail.internal.PGP;
import uk.ac.brookes.danielf.pgpmail.internal.Settings;
import uk.ac.brookes.danielf.pgpmail.intro.IntroActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.widget.ProgressBar;

import com.example.uk.ac.brookes.danielf.pgpmail.R;

/**
 * This class should be the first displayed
 * and kick off all the initialization procedures
 * such as importing keys from the DB.
 * @author danfitzgerald
 *
 */
public class InitializeActivity extends Activity {
	
	private ProgressBar progBar;
	private int status = 0;
	private Handler handler = new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_initialize);
		
		//TODO: add a 1-2 second progress bar with background loading screen
		
		progBar = (ProgressBar) findViewById(R.id.progressBar1);
		progBar.setMax(100);
		progBar.getIndeterminateDrawable().setColorFilter(0xFF004CDB, android.graphics.PorterDuff.Mode.MULTIPLY);
		
		Settings settings = new Settings(this);
		
		//create 2 folders in Download dir, one for attachments, the other for decoded files
		File attachDir = new File(PGP.getExternalDownloadsDir().getAbsolutePath() + File.separator + "iPGAttachments");
		settings.setAttachDir(attachDir.getAbsolutePath());
		if(!attachDir.exists())
		{
			attachDir.mkdir();
		}
		File decodeDir = new File(PGP.getExternalDownloadsDir().getAbsolutePath() + File.separator + "iPGDecoded");
		settings.setDecodeDir(decodeDir.getAbsolutePath());
		if(!decodeDir.exists())
		{
			decodeDir.mkdir();
		}
		
		Intent myIntent = null;
		if(settings.getFirstRun())
		{
			myIntent = new Intent(this, IntroActivity.class);
		}
		else
		{
			new Thread(new Runnable() {

				@Override
				public void run() {
					PGP.populateKeyRingCollections(InitializeActivity.this);
				}
				
			}).start();
			
			myIntent = new Intent(this, MenuActivity.class);
		}
		
		//start a thread to control the progress bar
		new Thread(new Runnable() {

			@Override
			public void run() {
				while(status < 100)
				{
					status++;
					
					handler.post(new Runnable() {

						@Override
						public void run() {
							progBar.setProgress(status);
						}
						
					});
					
					//sleep for 100ms
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
		}).start();
		
		InitializeActivity.this.startActivity(myIntent);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.initialize, menu);
		return true;
	}

}
