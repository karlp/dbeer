package net.beeroclock;

import android.app.ListActivity;
import android.content.Context;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bar_detail);
        TextView tvBarName = (TextView) findViewById(R.id.bar_detail_name);
        PintyApp pinty = (PintyApp)getApplication();

        Long barId = getIntent().getExtras().getLong(Bar.OSM_ID);
        if (barId == null) {
            fail_go_boom();
            return;
        }
        Bar bar = pinty.getBar(barId);
        if (bar == null) {
            fail_go_boom();
            return;
        }

        tvBarName.setText(bar.name);


        PriceArrayAdapter arrayAdapter = new PriceArrayAdapter(this, R.layout.price_row_item, new ArrayList<Price>(bar.prices));
        ListView lv = getListView();
        lv.setAdapter(arrayAdapter);

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
                    // FIXME!
                    drinkType.setText("drink type: " + price.id);
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
