package net.beeroclock;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;

/**
 * Eventually, this will show the details of an individual bar.
 * Things like "get me there" and pricing history, and anything else interesting
 * For now, it does very little
 * @author karl
 * Date: 3/21/11
 * Time: 6:07 PM
 */
public class BarDetailActivity extends ListActivity {
    Bar bar;
    PintyApp pinty;
    TextView tvBarName;
    Button addPriceButton;
    private static final int BAR_ENABLED_DISTANCE = 90;
    public static final int REQUEST_ADD_PRICE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bar_detail);
        tvBarName = (TextView) findViewById(R.id.bar_detail_name);
        addPriceButton = (Button) findViewById(R.id.bar_add_price_btn);
        pinty = (PintyApp)getApplication();

        Long barId = getIntent().getExtras().getLong(Bar.OSM_ID);
        if (barId == null) {
            fail_go_boom();
            return;
        }
        bar = pinty.getBar(barId);
        if (bar == null) {
            fail_go_boom();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO - or, let them click it, but make toast to say why it's off...
        if (pinty.getLastLocation().distanceTo(bar.toLocation()) < BAR_ENABLED_DISTANCE) {
            addPriceButton.setEnabled(true);
        } else {
            addPriceButton.setEnabled(false);
        }
        tvBarName.setText(bar.name);

        PriceArrayAdapter arrayAdapter = new PriceArrayAdapter(this, R.layout.price_row_item, new ArrayList<Price>(bar.prices));
        ListView lv = getListView();
        lv.setAdapter(arrayAdapter);
    }

    public void onClick_showOnMap(View view) {
        Intent i = new Intent(this, ActivityGoogleMap.class);
        i.putExtra(Bar.OSM_ID, bar.osmid);
        startActivity(i);
    }

    public void onClick_addPrice(View view) {
        Intent i = new Intent(this, AddPricingActivity.class);
        i.putExtra(Bar.OSM_ID, bar.osmid);
        startActivityForResult(i, REQUEST_ADD_PRICE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_ADD_PRICE) {
            Toast.makeText(this, R.string.toast_add_price_thanks, Toast.LENGTH_SHORT).show();
        }
    }

    private void fail_go_boom() {
        Toast t = Toast.makeText(getApplicationContext(), R.string.toast_wtf, Toast.LENGTH_SHORT);
        t.show();
        setResult(RESULT_CANCELED);
        finish();
    }

    class PriceArrayAdapter extends ArrayAdapter<Price> {

        private Context context;
        private ArrayList<Price> items;

        public PriceArrayAdapter(Context context, int resourceId, ArrayList<Price> objects) {
            super(context, resourceId, objects);
            this.context = context;
            this.items = objects;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;

            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.price_row_item, null);
            }

            Price price = items.get(position);

            if (price != null) {
                TextView drinkType = (TextView) view.findViewById(R.id.drink_name);
                if (drinkType != null) {
                    drinkType.setText(pinty.drinkNames.get((int) price.id));
                }
                // TODO - colour by age if we have that information?
                TextView priceView = (TextView) view.findViewById(R.id.drink_price);
                if (priceView != null) {
                    priceView.setText(String.format("%4.2f", price.avgPrice));
                }
            }
            return view;
        }

    }
}
