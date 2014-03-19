package uk.ac.brookes.danielf.pgpmail.internal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Xml;

public class ResultsParser extends AsyncTask<String, Integer, ArrayList<SearchResult>> {

	Context context;
	ProgressDialog pd;
	
	public ResultsParser(Context context) {
		this.context = context;
	}
	
	public interface ResultsListener
	{
		public void onResultsParsed(ArrayList<SearchResult> resultsList);
	}

	@Override
	protected ArrayList<SearchResult> doInBackground(String... xmlDoc) {
		
		SearchResult result = null;
		ArrayList<SearchResult> resultsList = new ArrayList<SearchResult>();
		
		try {
			//get a pull parser from the factory, set a few properties
			//so we can parse HTML
			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setValidating(false);
			factory.setFeature(Xml.FEATURE_RELAXED, true);
			
			XmlPullParser parser = factory.newPullParser();
			
			//set input source
			InputStream is = new ByteArrayInputStream(xmlDoc[0].getBytes());
			parser.setInput(is, null);
			
			/*
			 * there's a quirk in the xml returned that each record
			 * is enclosed in a <pre> tag except that the first
			 * <pre> doesn't contain a record of a key, rather a
			 * useless header...hmph
			 */
			boolean firstPre = true;
			
			//get the event type
			int eventType = parser.getEventType();
			
			int progressCounter = 0;
			//process while we don't reach the end of the document
			while(eventType != XmlPullParser.END_DOCUMENT)
			{
				switch(eventType)
				{
					//at start: START_DOCUMENT
					case XmlPullParser.START_DOCUMENT:
						break;
					
					//at start of a tag: START_TAG	
					case XmlPullParser.START_TAG:
						//get the name of the tag
						String tag = parser.getName();
						
						/*
						 * Each individual record returned is enclosed in a <pre>
						 * tag. Each bit of data we are interested in (key type, date,
						 * key id and user id) are separated by <a> tags. The full
						 * key id is actually contained within the href attribute of
						 * the <a> tag.
						 */
						if(tag.equalsIgnoreCase("pre"))
						{
							if(firstPre)
								firstPre = false;
							else
							{
								//new result
								result = new SearchResult();
								
								//get the type
								if(parser.getEventType() == XmlPullParser.START_TAG)
									parser.next();
								if(parser.getEventType() == XmlPullParser.TEXT)
									result.setType(formatString(parser.getText()));
								
								//onto the 1st link
								parser.next();
								String linkone = parser.getName();
								if(linkone.equalsIgnoreCase("a"))
								{
									//get the keylink from the link href attrib
									result.setKeyLink(parser.getAttributeValue("", "href"));
									
									//get the key id from the link text and end up on the closing tag
									result.setKeyId(formatString(parser.nextText()));
								}
								
								//advance to the next text element, the date
								parser.next();
								
								//get the date
								if(parser.getEventType() == XmlPullParser.TEXT)
									result.setDate(formatString(parser.getText()));
								
								//onto the last link
								parser.next();
								
								//move onto the final text element and get the user id
								parser.next();
								result.setUserId(formatString(parser.getText()));
								
								//and we're done!
								resultsList.add(result);
								
								//increment the progress counter and publish it
								progressCounter++;
								publishProgress(progressCounter);
							}
						}
				}
				
				//next parser event
				eventType = parser.next();
				
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return resultsList;
	}
	
	@Override
	protected void onPreExecute()
	{
		//before executing show a progress dialog
		pd = new ProgressDialog(context);
		pd.setTitle("Key Search");
		pd.setMessage("processing results...");
		pd.setCancelable(false);
		pd.setIndeterminate(true);
		pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pd.show();
	}
	
	@Override
	protected void onProgressUpdate(Integer...progress)
	{
		Integer i = progress[0];
		pd.setProgress(i);
	}
	
	@Override
	protected void onPostExecute(ArrayList<SearchResult> result)
	{
		//once the task is finished dismiss the progress dialog
		if(pd.isShowing())
		{
			pd.dismiss();
		}
		
		ResultsListener rListener = (ResultsListener) context;
		rListener.onResultsParsed(result);
	}
	
	/**
	 * Removes all newlines characters and trailing whitespace
	 * @return
	 */
	private String formatString(String a)
	{
		if(a == null)
			return "info missing";
		else
		{
			a.trim();
			a.replaceAll("\n", "");
			a.replaceAll("\r\n", "");
		}
		return a;
	}
}
