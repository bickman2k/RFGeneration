package com.jgrue.rfgeneration.scrapers;

import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.jgrue.rfgeneration.objects.GameInfo;
import com.jgrue.rfgeneration.scrapers.HardwareInfoScraper;

public class GameInfoScraper {
	private static GameInfo lastGame = null;
	
	public static GameInfo getGameInfo(String rfgid) throws Exception {
		if(lastGame == null)
			lastGame = new GameInfo();
		
		if(lastGame.getRFGID() != null && lastGame.getRFGID().equals(rfgid)) {
			return lastGame;
		} else {
			GameInfo newGame = scrapeGameInfo(rfgid);
			lastGame = newGame;
			return newGame;
		}
	}
	
	private static GameInfo scrapeGameInfo(String rfgid) throws Exception {
		GameInfo gameInfo = new GameInfo();
		gameInfo.setRFGID(rfgid);
		
		URL url = new URL("http://www.rfgeneration.com/cgi-bin/getinfo.pl?ID=" + rfgid);
		Document document = Jsoup.parse(url, 30000);
		
		Elements tables = document.select("table tr:eq(3) td:eq(1) table.bordercolor tr td table.windowbg2 tr:eq(3) td table");
		if(tables.size() <= 4)
			return HardwareInfoScraper.scrapeHardwareInfo(rfgid);
		
		Element table = tables.get(4);
		Elements tableRows = table.select("tr");
		 
		Element title = document.select("div.headline").get(0);
		gameInfo.setTitle(title.text());
		
		// Check for a variation title
		Pattern variantRegex = Pattern.compile(" \\[.*\\]$");
		Matcher matcher = variantRegex.matcher(gameInfo.getTitle());
		if(matcher.find())
		{
			gameInfo.setVariationTitle(matcher.group().substring(2, matcher.group().length() - 1));
			gameInfo.setTitle(gameInfo.getTitle().substring(0, gameInfo.getTitle().length() - gameInfo.getVariationTitle().length() - 3));
		}
		 
		for(int i = 0; i < tableRows.size(); i++) {
			 Elements tableData = tableRows.get(i).select("td");
			 String field = tableData.get(0).text();
			 String value = tableData.get(1).text();
			 
			 if(field.contains("Console"))
				 gameInfo.setConsole(value);
			 else if(field.contains("Region"))
				 gameInfo.setRegion(tableData.get(1).select("img").first().attr("title"));
			 else if(field.contains("Year"))
				 gameInfo.setYear(Integer.parseInt(value));
			 else if(field.contains("Part"))
				 gameInfo.setPartNumber(value);
			 else if(field.contains("UPC"))
			 	 gameInfo.setUPC(value);
		 	 else if(field.contains("Publisher"))
		 		 gameInfo.setPublisher(value);
		 	 else if(field.contains("Developer"))
		 		 gameInfo.setDeveloper(value);
		 	 else if(field.contains("Rating"))
		 		 gameInfo.setRating(value);
		 	 else if(field.contains("Genre"))
		 		 gameInfo.setGenre(value);
		 	 else if(field.contains("Sub-genre"))
		 		 gameInfo.setSubGenre(value);
		 	 else if(field.contains("Players"))
		 		 gameInfo.setPlayers(value);
		 	 else if(field.contains("Controller"))
		 		 gameInfo.setControlScheme(value);
		 	 else if(field.contains("Media Format"))
		 		 gameInfo.setMediaFormat(value);
		 	 else if(field.contains("Alternate Title"))
		 		 gameInfo.setAlternateTitle(value);
		}
		
		table = tables.get(tables.size() - 2);
		tableRows = table.select("tr");
		 
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
