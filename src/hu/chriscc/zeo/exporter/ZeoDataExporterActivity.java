package hu.chriscc.zeo.exporter;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ZeoDataExporterActivity extends ListActivity {
    
	private ArrayList<HashMap<String, String>> sleepData;
	private HashMapAdaptor adaptor;
	
	public void toastMessage(String msg){
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Log.i("zeoExp","Start");
        sleepData = new ArrayList<HashMap<String, String>>();
        
        final TextView mainText = (TextView) this.findViewById(R.id.mainText);
        Button exp = (Button) this.findViewById(R.id.exportBtn);
        
        adaptor = new HashMapAdaptor(this,
				R.layout.sleeplist,
				sleepData);
        
        exp.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				sleepData = new ArrayList<HashMap<String, String>>();
				//=============================================================================
				//==============================GET ROOT=======================================
				//=============================================================================
				Process p;  
		        try {  
		           // Preform su to get root privledges  
		           p = Runtime.getRuntime().exec("su");   
		          
		           // Attempt to write a file to a root-only  
		           DataOutputStream os = new DataOutputStream(p.getOutputStream());  
		           os.writeBytes("cp /data/data/com.myzeo.android/databases/zeo.db /mnt/sdcard/zeo.db\n"); 
		           os.writeBytes("chmod 777 /mnt/sdcard/zeo.db\n");
		          
		           // Close the terminal  
		           os.writeBytes("exit\n");  
		           os.flush();  
		           try {  
		              p.waitFor();  
		                   if (p.exitValue() != 255) {  
		                      // TODO Code to run on success  
		                      mainText.setText(mainText.getText()+"\n"+"You have root! Database successfuly copied!");  
		                   }  
		                   else {  
		                       // TODO Code to run on unsuccessful  
		                       mainText.setText(mainText.getText()+"\n"+"Not root :(");  
		                   }  
		           } catch (InterruptedException e) {  
		              // TODO Code to run in interrupted exception  
		               mainText.setText(mainText.getText()+"\n"+"Not root :(");  
		           }  
		        } catch (IOException e) {  
		           // TODO Code to run in input/output exception  
		            mainText.setText(mainText.getText()+"\n"+"Not root :(");  
		        }  
		        
		        File dir = new File("/mnt/sdcard");
				File[] filelist = dir.listFiles();
				int i;
				for(i=0;i<filelist.length;i++){
					Log.i("zeoExp",filelist[i].toString());
				}
		        SQLiteDatabase myDB = SQLiteDatabase.openDatabase("/mnt/sdcard/zeo.db", null, SQLiteDatabase.OPEN_READONLY);
		        Cursor c = myDB.rawQuery("SELECT * FROM sleep_records", null);
				int con = c.getColumnIndex("created_on");
				int bhyp = c.getColumnIndex("base_hypnogram");
				// Check if our result was valid.
				c.moveToFirst();
				int ennyi = 0;
				if (c != null) {
					ennyi = c.getCount();
					Log.i("zeoExp","Ennyi rekord: "+ennyi);
					if(ennyi==0){
						//Toast.makeText(this, "No recorded sleeps found!", Toast.LENGTH_LONG).show();
					}else{
						//Log.i("zeoExp","["+c.getString(con)+"] "+c.getString(bhyp));
						
						Calendar cal = Calendar.getInstance();
						cal.setTimeInMillis(c.getLong(con));
						Date date = (Date) cal.getTime();
						
						byte[] b = c.getBlob(bhyp);
						String theblob = "";
						for(i=0;i<b.length;i++){
							theblob += ""+b[i];
						}
						HashMap<String, String> hm = new HashMap<String, String>();
						hm.put("date",date.toString());
						hm.put("data", theblob);
						hm.put("ts", c.getString(con));
						sleepData.add(hm);
						
						while(c.moveToNext()){
							
							cal = Calendar.getInstance();
							cal.setTimeInMillis(c.getLong(con));
							date = (Date) cal.getTime();
							
							byte[] bs = c.getBlob(bhyp);
							String theblobs = "";
							for(i=0;i<bs.length;i++){
								theblobs += ""+bs[i];
							}
							hm = new HashMap<String, String>();
							hm.put("date",date.toString());
							hm.put("data", theblobs);
							hm.put("ts", c.getString(con));
							sleepData.add(hm);
							
							
							//mainText.setText(mainText.getText()+"\n"+theblobs);
							Log.i("zeoExp","["+c.getString(con)+"] "+theblobs);
						}
						
						adaptor.clear();
						for(HashMap<String, String> itm : sleepData){
							adaptor.add(itm);
						}
						adaptor.notifyDataSetChanged();
						mainText.setText("Data exported successfuly! Tap a row in the list to save it.");
					}
				}
			}
        	
        });
        ListView lv = getListView();
        lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				HashMap<String,String> sel = sleepData.get(position);
				Intent i=new Intent(android.content.Intent.ACTION_SEND);

				i.setType("text/plain"); i.putExtra(Intent.EXTRA_SUBJECT, "Zeo data of "+sel.get("date"));
				i.putExtra(Intent.EXTRA_TEXT, sel.get("ts") + ": " + sel.get("data"));

				startActivity(Intent.createChooser(i, sel.get("date")));
			}
		});
        
        
        setListAdapter(adaptor);
        
    }
    
    private class HashMapAdaptor extends ArrayAdapter<HashMap<String,String>> {
		private ArrayList<HashMap<String,String>> lista;
		
				
		public HashMapAdaptor(Context context, int textViewResourceId,
				ArrayList<HashMap<String,String>> items) {
			super(context, textViewResourceId, items);
			this.lista = items;

		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.sleeplist, null);
			}
			
			TextView label = (TextView) v.findViewById(R.id.sleepname);
			
			String tmp = sleepData.get(position).get("data");
			tmp = tmp.replace("0", "");
			if(tmp.length() < 90) label.setText("Nap at "+sleepData.get(position).get("date")); 
			else if(tmp.length() < 450) label.setText("Core at "+sleepData.get(position).get("date"));
			else label.setText("Mono sleep at "+sleepData.get(position).get("date"));
			

			return v;
		}
	}
}