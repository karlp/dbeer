package net.beeroclock.dbeer.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.google.ads.AdRequest;
import com.google.ads.AdView;
import net.beeroclock.dbeer.models.Bar;
import net.beeroclock.dbeer.PintyApp;
import net.beeroclock.dbeer.models.Price;
import net.beeroclock.dbeer.R;

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
    TextView tvBarType;
    Button addPriceButton;
    Button toggleHiddenButton;
    private static final int BAR_ENABLED_DISTANCE = 150;
    public static final int REQUEST_ADD_PRICE = 1;
    private static final int DIALOG_HELP = 1;


    /**
     * Handle the menu options for this activity
     * @param item the menu item clicked
     * @return see super()
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_bar_detail_help:
                showDialog(DIALOG_HELP);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_bar_detail, menu);
        return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch (id) {
            case DIALOG_HELP:
                return builder.setMessage(R.string.bar_detail_help_text)
                    .setTitle(R.string.dialog_help_title).create();
        }
        return super.onCreateDialog(id);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bar_detail);
        tvBarName = (TextView) findViewById(R.id.bar_detail_name);
        tvBarType = (TextView) findViewById(R.id.bar_detail_type);
        addPriceButton = (Button) findViewById(R.id.bar_add_price_btn);
        toggleHiddenButton = (Button) findViewById(R.id.bar_toggle_hidden);
        pinty = (PintyApp)getApplication();

        Long barId = getIntent().getExtras().getLong(Bar.PKUID);
        if (barId == null) {
            fail_go_boom();
            return;
        }
        bar = pinty.getBar(barId);
        if (bar == null) {
            // TODO You can get hereby clicking on a "hidden" bar in preferences, that is not currently in pinty's brane.
            // should fetch that bar on demand?  (use a progress dialog with a bar fetcher!)
            fail_go_boom();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Location lastLocation = pinty.getLastLocation();
        AdView adView = (AdView) findViewById(R.id.bar_detail_ad_view);
        AdRequest adRequest = new AdRequest();
        adRequest.setLocation(lastLocation);
        adRequest.setTesting(pinty.ads_test_mode);
        adView.loadAd(adRequest);

        // TODO - or, let them click it, but make toast to say why it's off...
        if (lastLocation.distanceTo(bar.toLocation()) < BAR_ENABLED_DISTANCE) {
            addPriceButton.setEnabled(true);
        } else {
            addPriceButton.setEnabled(false);
        }
        if (pinty.isAllowed(bar.pkuid)) {
            toggleHiddenButton.setText(R.string.bar_detail_btn_hide_bar);
        } else {
            toggleHiddenButton.setText(R.string.bar_detail_btn_show_bar);
        }
        tvBarName.setText(bar.name);
        tvBarType.setText(bar.type);

        PriceArrayAdapter arrayAdapter = new PriceArrayAdapter(this, R.layout.price_row_item, new ArrayList<Price>(bar.prices));
        ListView lv = getListView();
        lv.setAdapter(arrayAdapter);
    }

    public void onClick_showOnMap(View view) {
        Intent i = new Intent(this, ActivityGoogleMap.class);
        i.putExtra(Bar.PKUID, bar.pkuid);
        startActivity(i);
    }

    public void onClick_addPrice(View view) {
        Intent i = new Intent(this, AddPricingActivity.class);
        i.putExtra(Bar.PKUID, bar.pkuid);
        startActivityForResult(i, REQUEST_ADD_PRICE);
    }

    public void onClick_toggleHidden(View view) {
        if (pinty.isAllowed(bar.pkuid)) {
            pinty.hideBar(bar);
            finish();
            Toast.makeText(this, "Ok, bar hidden from view", Toast.LENGTH_SHORT).show();
        } else {
            pinty.unhideBar(bar);
            toggleHiddenButton.setText(R.string.bar_detail_btn_hide_bar);
            Toast.makeText(this, "Ok, bar will show up again", Toast.LENGTH_SHORT).show();
        }
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
                    drinkType.setText(pinty.getDrinkNameForExternalId(price.drinkTypeId));
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
