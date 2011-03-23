package net.beeroclock;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;

/**
 * Shows a list of bars, inviting a click on the correct one.
 *
 * Intended as a child activity
 * @author Karl Palsson, 2011
 *         Date: 2011-03-23
 */
public class ChooseCorrectBarActivity  extends ListActivity {

    private PintyApp pinty;
    ArrayList<Bar> possibleBars;
    private static final String TAG = "ChooseCorrectBarActivity";

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Bar bar = (Bar) v.getTag(R.id.tag_bar);
        Log.d(TAG, "Chose bar and moving on: " + bar.name);
        Intent i = new Intent(this, AddPricingActivity.class);
        i.putExtra(Bar.OSM_ID, bar.osmid);
        startActivity(i);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_correct_bar);
        pinty = (PintyApp) getApplication();

        // Load in all the bars from the intent that launched us, and fill the pick list
        possibleBars = new ArrayList<Bar>();
        for (long barId : getIntent().getExtras().getLongArray(Bar.OSM_ID_SET)) {
            possibleBars.add(pinty.getBar(barId));
        }

        // make and stuff buttons into a list here....

        BarChooser arrayAdapter = new BarChooser(this, R.layout.choose_correct_bar_item, possibleBars);
        ListView lv = getListView();
        lv.setAdapter(arrayAdapter);
    }

    public class BarChooser extends ArrayAdapter<Bar> {
        private Context context;
        private ArrayList<Bar> items;
        private int layoutId;

        public BarChooser(Context context, int textViewResourceId, ArrayList<Bar> objects) {
            super(context, textViewResourceId, objects);
            this.context = context;
            this.items = objects;
            this.layoutId = textViewResourceId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater li = LayoutInflater.from(context);
                convertView = li.inflate(this.layoutId, null);
            }

            Bar bar = items.get(position);
            if (bar != null) {
                TextView tv = (TextView) convertView.findViewById(R.id.choose_correct_bar_item_label);
                if (tv != null) {
                    tv.setText(bar.name);
                }
                convertView.setTag(R.id.tag_bar, bar);
                return convertView;
            } else {
                throw new IllegalStateException("Shouldn't be able to try and choose a bar when there are no bars...");
            }
        }
    }
}
