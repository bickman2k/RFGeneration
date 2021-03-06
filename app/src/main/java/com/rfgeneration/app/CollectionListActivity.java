package com.rfgeneration.app;

import static android.provider.BaseColumns._ID;

import java.util.ArrayList;
import java.util.List;

import com.rfgeneration.app.constants.Constants;
import com.rfgeneration.app.data.RFGenerationData;
import com.rfgeneration.app.data.RFGenerationProvider;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class CollectionListActivity extends AppCompatActivity {
	private static final String TAG = "CollectionListActivity";
	private long folderId;
	private List<Long> folderIdList;
	private List<String> folderList;
	private long consoleId;
	private List<Long> consoleIdList;
	private List<String> consoleList;
	private String type;
	private List<String> typeList;
	private RFGenerationData rfgData;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.collection_fragment);
        
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getSharedPreferences(Constants.PREFS_FILE, 0).getString(Constants.PREFS_USERNAME, "") + "'s Collection");
        
        // Initialize class variables
        folderId = getIntent().getLongExtra(Constants.INTENT_FOLDER, -1);
        folderIdList = new ArrayList<Long>();
        folderList = new ArrayList<String>();
        consoleId = getIntent().getLongExtra(Constants.INTENT_CONSOLE_ID, -1);
        consoleIdList = new ArrayList<Long>();
        consoleList = new ArrayList<String>();
        type = getIntent().getStringExtra(Constants.INTENT_TYPE) != null ? getIntent().getStringExtra(Constants.INTENT_TYPE) : "";
        typeList = new ArrayList<String>();
        rfgData = new RFGenerationData(this);
        
		// Set title bar
        findViewById(R.id.collection_text).setTag(new Object[] { folderId, consoleId, type });
		if(folderId == -1)
			actionBar.setSubtitle("All Folders");
		else if(folderId == 0)
			actionBar.setSubtitle("Owned Folders");
		else {
			ContentResolver db = getContentResolver();
			Cursor folderName = db.query(Uri.withAppendedPath(RFGenerationProvider.FOLDERS_URI, Long.toString(folderId)), 
					new String[] { "folder_name", "is_for_sale", "is_private" }, null, null, null);
			startManagingCursor(folderName);
			if(folderName.moveToNext())
				actionBar.setSubtitle(folderName.getString(0));
		}
		
		if(consoleId > -1) {
			SQLiteDatabase db = rfgData.getReadableDatabase();
			Cursor abbvCursor = db.query("consoles", new String[] { "console_abbv" }, 
					_ID + " = ?", new String[] { Long.toString(consoleId) }, null, null, null);
			startManagingCursor(abbvCursor);
			
			if(abbvCursor.moveToNext()) {
				actionBar.setSubtitle(actionBar.getSubtitle() + " :: " + abbvCursor.getString(0));
			} else {
				actionBar.setSubtitle(actionBar.getSubtitle() + " :: ???");
			}
		}
		
		if(type.equals("H")) {
			actionBar.setSubtitle(actionBar.getSubtitle() + " :: Hardware");
		} else if(type.equals("S")) {
			actionBar.setSubtitle(actionBar.getSubtitle() + " :: Software");
		}
    }
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.collectionmenu, menu);
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.filters:
	    	createFiltersDialog();
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
    
    private void createFiltersDialog() {
    	if(folderIdList.size() == 0) {
    		// If one's empty, they must all be... right?
    		Log.i(TAG, "Loading filter information for the first time.");
    		SQLiteDatabase db = rfgData.getReadableDatabase();
    		
    		// Get information about folders.
    		Cursor folderCursor = db.rawQuery("SELECT folders._id, folder_name, is_owned, count(folder_id) " +
    				"FROM folders LEFT JOIN collection ON collection.folder_id = folders._id " + 
    				"GROUP BY folder_name ORDER BY is_owned DESC, folder_name", null);
    		startManagingCursor(folderCursor);
    		
    		long totalCount = 0, ownedCount = 0;
    		folderIdList.add((long)-1); folderList.add("");
    		folderIdList.add((long)0); folderList.add("");
    		while(folderCursor.moveToNext()) {
    			String folderName;
    			
    			totalCount += folderCursor.getLong(3);
    			if(folderCursor.getInt(2) == 1)
    			{
    				ownedCount += folderCursor.getLong(3);
    				folderName = "    ";
    			}
    			else
    			{
    				folderName = "  ";
    			}
    			
    			folderName += folderCursor.getString(1) + " (" + Long.toString(folderCursor.getLong(3)) + ")";
    			
    			folderIdList.add(folderCursor.getLong(0));
    			folderList.add(folderName);
    		}
    		
    		folderList.set(0, "All Folders (" + totalCount + ")");
    		folderList.set(1, "  Owned Folders (" + ownedCount + ")");
    		
    		// Get information about consoles.
    		Cursor consoleCursor = db.rawQuery("SELECT games.console_id, " +
    				"ifnull(consoles.console_name, 'Unknown Console') as console_name, count(1) " +
    				"FROM collection INNER JOIN games ON collection.game_id = games._id " +
    				"LEFT JOIN consoles ON games.console_id = consoles._id " +
    				"GROUP BY consoles.console_name ORDER BY console_name", null);
    		startManagingCursor(consoleCursor);
    		
    		consoleIdList.add((long)-1); consoleList.add("-----Any Console-----");
    		while(consoleCursor.moveToNext()) {
    			consoleIdList.add(consoleCursor.getLong(0));
    			consoleList.add(consoleCursor.getString(1) + " (" + Long.toString(consoleCursor.getLong(2)) + ")");
    		}
    		
    		// Get information about types.
    		Cursor typeCursor = db.rawQuery("SELECT 'Software', count(1) FROM collection " + 
    				"INNER JOIN games ON collection.game_id = games._id " + 
    				"WHERE type = 'S' UNION ALL " +
    				"SELECT 'Hardware', count(1) FROM collection " + 
    				"INNER JOIN games ON collection.game_id = games._id " + 
    				"WHERE type = 'H'", null);
    		startManagingCursor(typeCursor);
    		
    		typeList.add("--All Types--");
    		while(typeCursor.moveToNext()) {
    			typeList.add(typeCursor.getString(0) + " (" + Long.toString(typeCursor.getLong(1)) + ")");
    		}
    	}
    	
    	LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	View collectionFilter = vi.inflate(R.layout.collection_filter, null);

    	// Set up spinners
    	final Spinner folderSpinner = (Spinner) collectionFilter.findViewById(R.id.folderSpinner);

    	ArrayAdapter<String> folderAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, folderList);
    	folderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	folderSpinner.setAdapter(folderAdapter);
    	folderSpinner.setSelection(folderIdList.indexOf(folderId));
    	
    	final Spinner consoleSpinner = (Spinner) collectionFilter.findViewById(R.id.consoleSpinner);
    	ArrayAdapter<String> consoleAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, consoleList);
    	consoleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	consoleSpinner.setAdapter(consoleAdapter);
    	consoleSpinner.setSelection(consoleIdList.indexOf(consoleId));
    	
    	final Spinner typeSpinner = (Spinner) collectionFilter.findViewById(R.id.typeSpinner);
    	ArrayAdapter<String> typeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, typeList);
    	typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	typeSpinner.setAdapter(typeAdapter);
    	if (type.equals("S"))
    		typeSpinner.setSelection(1);
    	else if (type.equals("H"))
    		typeSpinner.setSelection(2);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(collectionFilter)
			.setTitle("Collection Filters")
		    .setCancelable(true)
		    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
		    	@Override
				public void onClick(DialogInterface dialog, int id) {     	   
		    		Intent myIntent = new Intent(getBaseContext(), CollectionListActivity.class);
		    		myIntent.putExtra(Constants.INTENT_FOLDER, folderIdList.get(folderSpinner.getSelectedItemPosition()));
		    		myIntent.putExtra(Constants.INTENT_CONSOLE_ID, consoleIdList.get(consoleSpinner.getSelectedItemPosition()));
		    		if(typeSpinner.getSelectedItemPosition() == 1)
						myIntent.putExtra(Constants.INTENT_TYPE, "S");
					else if (typeSpinner.getSelectedItemPosition() == 2)
						myIntent.putExtra(Constants.INTENT_TYPE, "H");
					startActivityForResult(myIntent, 0);
				}
		    })
		    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		    	@Override
				public void onClick(DialogInterface dialog, int id) {
		    		dialog.cancel();
		        }
		    });
		AlertDialog alert = builder.create();
		alert.show();
    }
    
    @Override
    public void onDestroy() {
    	if(rfgData != null)
    		rfgData.close();
    	super.onDestroy();
    }
}
