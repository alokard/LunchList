package com.tulusha.lunchlist;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.TabActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

public class LunchList extends TabActivity {
	
	List<Restaurant> restaurants = new ArrayList<Restaurant>();
	RestaurantAdapter adapter = null;
	
	EditText name = null;
	EditText address = null;
	RadioGroup types = null;
	
	int progress = 0;
	AtomicBoolean isActive = new AtomicBoolean(true);
	private RestaurantHelper helper;
	
	Cursor model = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.main);
        
        helper = new RestaurantHelper(this);
        
        Button save = (Button)findViewById(R.id.save_button);
        save.setOnClickListener(onSave );

		name = (EditText)findViewById(R.id.name_edit);
		address = (EditText)findViewById(R.id.address_edit);
		types = (RadioGroup)findViewById(R.id.types_group);
		
        ListView list = (ListView)findViewById(R.id.restaurants);
        
        model = helper.getAll();
        startManagingCursor(model);
        adapter = new RestaurantAdapter(model);
        list.setAdapter(adapter);
        
        list.setOnItemClickListener(onListClick);
        
        TabHost.TabSpec spec = getTabHost().newTabSpec("tag1");
        
        spec.setContent(R.id.restaurants);
        spec.setIndicator("List", getResources().getDrawable(R.drawable.list));
        
        getTabHost().addTab(spec);
        
        spec = getTabHost().newTabSpec("tag2");
        
        spec.setContent(R.id.details);
        spec.setIndicator("Details", getResources().getDrawable(R.drawable.details));
        
        getTabHost().addTab(spec);
        
        getTabHost().setCurrentTab(0);
        
    }
    
    public void onPause()
    {
    	super.onPause();
    	
    	isActive.set(false);
    }
    
    public void onResume()
    {
    	super.onResume();
    	
    	isActive.set(true);
    	
    	if (progress > 0)
    	{
    		startWork();
    	}
    }
    
    public void onDestroy()
    {
    	super.onDestroy();
    	
    	helper.close();
    }
    
    private AdapterView.OnItemClickListener onListClick = new
    AdapterView.OnItemClickListener() {
    	public void onItemClick(AdapterView<?> parent,
    							View view, int position,
    							long id)
    	{
    		model.moveToPosition(position);
    		
    		name.setText(helper.getName(model));
    		address.setText(helper.getAddress(model));
    		
    		if (helper.getType(model).equals("take_out"))
			{
				types.check(R.id.take_out);
			}
			else if (helper.getType(model).equals("sit_down"))
			{
				types.check(R.id.sit_down);
			}
			else if (helper.getType(model).equals("delivery"))
			{
				types.check(R.id.delivery);
			}
    		
    		getTabHost().setCurrentTab(1);
    	}
	};
	
	private void startWork()
	{
		setProgressBarVisibility(true);
		new Thread(longTask).start();
	}
    
	private void doSomeLongWork(final int incr)
	{
		runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				progress += incr;
				setProgress(progress);
			}
		});
		
		SystemClock.sleep(250);
	}
	
	private Runnable longTask = new Runnable() {
		
		@Override
		public void run() {
			for (int i = progress; (i < 10000) && isActive.get(); i+=200)
				doSomeLongWork(200);
			
			if (isActive.get())
			{
				runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						setProgressBarVisibility(false);
						progress = 0;
					}
				});
			}
		}
	};
	
    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(this).inflate(R.menu.options, menu);
		
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.run)
		{
			setProgressBarVisibility(true);
			progress = 0;
			new Thread(longTask).start();
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private View.OnClickListener onSave = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			Restaurant restaurant = new Restaurant();
			String type = "";
			
			switch (types.getCheckedRadioButtonId()) {
			case R.id.take_out:
				restaurant.setType("take_out");
				type = "take_out";
				break;
			case R.id.sit_down:
				restaurant.setType("sit_down");
				type = "sit_down";
				break;
			case R.id.delivery:
				restaurant.setType("delivery");
				type = "delivery";
				break;
			default:
				break;
			}
			
			helper.insert(name.getText().toString(), address.getText().toString(), type, "");
			
			model.requery();
			
			Context context = getApplicationContext();
			CharSequence text = "Update successful";
			int duration = Toast.LENGTH_SHORT;

			Toast toast = Toast.makeText(context, text, duration);
			toast.show();
			
		}
	};
	
	class RestaurantAdapter extends CursorAdapter {
		RestaurantAdapter(Cursor c)
		{
			super(LunchList.this, c);
		}

		@Override
		public View newView(Context context, Cursor c, ViewGroup parent)
		{
			LayoutInflater inflater = getLayoutInflater();
			View row = inflater.inflate(R.layout.row, parent, false);
			RestaurantHolder holder = new RestaurantHolder(row);
			
			row.setTag(holder);
			
			return row;
		}

		@Override
		public void bindView(View row, Context context, Cursor c) {
			RestaurantHolder holder = (RestaurantHolder)row.getTag();
			
			holder.populateForm(c, helper);
		}
	}
	
	static class RestaurantHolder
	{
		private TextView name = null;
		private TextView address = null;
		private ImageView image = null;
		
		public RestaurantHolder(View row) {
			name = (TextView)row.findViewById(R.id.title);
			address = (TextView)row.findViewById(R.id.address);
			image = (ImageView)row.findViewById(R.id.icon);
		}
		
		void populateForm(Cursor c, RestaurantHelper helper)
		{
			name.setText(helper.getName(c));
			address.setText(helper.getAddress(c));
			
			if (helper.getType(c).equals("take_out"))
			{
				image.setImageResource(R.drawable.ball_yellow);
			}
			else if (helper.getType(c).equals("sit_down"))
			{
				image.setImageResource(R.drawable.ball_red);
			}
			else if (helper.getType(c).equals("delivery"))
			{
				image.setImageResource(R.drawable.ball_green);
			}
		}
	}
}
