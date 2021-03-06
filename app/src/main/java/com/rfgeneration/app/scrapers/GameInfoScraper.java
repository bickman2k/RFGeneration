package com.rfgeneration.app.scrapers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import android.net.Uri;
import android.util.Log;

import com.rfgeneration.app.constants.Constants;
import com.rfgeneration.app.objects.GameInfo;
import com.rfgeneration.app.scrapers.HardwareInfoScraper;

public class GameInfoScraper {
	private static final String TAG = "GameInfoScraper";
	private static GameInfo lastGame = null;
	
	public static GameInfo getGameInfo(String rfgid) throws Exception {
		if(lastGame == null)
			lastGame = new GameInfo();
		
		if(lastGame.getRFGID() != null && lastGame.getRFGID().equals(rfgid)) {
			Log.i(TAG, "Returning cached result for " + rfgid + ".");
			return lastGame;
		} else {
			GameInfo newGame = scrapeGameInfo(rfgid);
			lastGame = newGame;
			return newGame;
		}
	}
	
	private static GameInfo scrapeGameInfo(String rfgid) throws Exception {
		Uri url = Uri.parse(Constants.FUNCTION_GAME_INFO).buildUpon()
				.appendQueryParameter(Constants.PARAM_RFGID, rfgid)
				.build();
		
		Log.i(TAG, "Target URL: " + url);
		Document document = Jsoup.connect(url.toString())
			.timeout(Constants.TIMEOUT)
			.get();
		Log.i(TAG, "Retrieved URL: " + document.baseUri());
		
		Elements tables = document.select("table > tbody > tr:eq(3) > td:eq(1) > table.bordercolor > tbody > tr > td > table.windowbg2 > tbody > tr:eq(3) > td > table");
		if(tables.size() == 0)
		{
			Log.w(TAG, "Unexpected results for " + rfgid + ", switching to HardwareInfoScraper.");
			return HardwareInfoScraper.scrapeHardwareInfo(rfgid);
		}
		
		// Game Details are stored in the first table.
		Elements tableRows = tables.get(0).select("tr");
		Map<String, String> properties = new HashMap<String, String>();
		
		for(int i = 0; i < tableRows.size(); i++) {
			 Elements tableData = tableRows.get(i).select("td");
			 
			 // Sometimes non-US ratings can get mixed up in here and only have one cell.
			 if(tableData.size() < 2)
				 continue;
			 
			 String field = tableData.get(0).text().trim().replace(":", "");
			 String value = tableData.get(1).text().trim();
			 
			 if(field.length() == 0)
				 continue;
			 
			 if(GameInfo.REGION.equals(field)) {
				 StringBuilder sb = new StringBuilder();
				 Elements regions = tableData.select("img");
				 for(int j = 0; j < regions.size(); j++) {
					 sb.append(regions.get(j).attr("title"));
					 if (j < regions.size()) sb.append(',');
				 }
				 //properties.put(field, tableData.get(1).select("img").first().attr("title"));
				 properties.put(field, sb.toString());
			 } else if (value.length() > 0) {
				 properties.put(field, value);
			 }
		}
		
		// Set the title and details.
		GameInfo gameInfo = new GameInfo(properties);
		gameInfo.setTitle(document.select("tr#title div.headline").get(0).text());
		
		// Page Credits are stored in the second to last table.
		tableRows = tables.get(tables.size() - 2).select("tr");
		ArrayList<String> names = new ArrayList<String>();
		ArrayList<String> credits = new ArrayList<String>();
		 
		try {
			for(int i = 0; i < tableRows.size(); i++) {
				Elements tableData = tableRows.get(i).select("td");
				names.add(tableData.get(0).text());
				credits.add(tableData.get(1).text());
			}
		} catch (Exception e)
		{
			names.add("");
			credits.add("Error while loading credits.");
			Log.e(TAG, "Error while loading credits.");
		}
		 
		gameInfo.setNameList(names);
		gameInfo.setCreditList(credits);
		
		// Load the state of which images are present for this game.
		tableRows = document.select("tr#title > td:eq(1) > table.bordercolor td");
		ArrayList<String> imageTypes = new ArrayList<String>();

		if(tableRows.size() == 5) {
			if(tableRows.get(0).select("a").size() > 0)
				 imageTypes.add("bf");
			 if(tableRows.get(1).select("a").size() > 0)
				 imageTypes.add("bb");
			 if(tableRows.get(3).select("a").size() > 0)
				 imageTypes.add("gs");
			 if(tableRows.get(4).select("a").size() > 0)
				 imageTypes.add("ms");
			 if(tableRows.get(2).select("a").size() > 0)
				 imageTypes.add("ss");
		}
		 
		gameInfo.setImageTypes(imageTypes);

		return gameInfo;
	}
}
