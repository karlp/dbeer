package net.beeroclock.dbeer.activities;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import net.beeroclock.dbeer.models.Bar;
import net.beeroclock.dbeer.PintyApp;
import net.beeroclock.dbeer.R;
import org.apache.commons.lang.StringUtils;

/**
 * Handles taking in pricing details, packaging it up for the server, and posting it off.
 *
 * @author Karl Palsson, 2011
 *         Date: 2011-03-23
 */
public class AddPricingActivity extends Activity {

    private static final String TAG = "AddPricingActivity";
    PintyApp pinty;
    int chosenDrink = -1;
    Long barId;
    private EditText priceEntry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_pricing);

        barId = getIntent().getExtras().getLong(Bar.OSM_ID);
        if (barId == null) {
            throw new IllegalStateException("adding a pricing without a bar!");
        }
        pinty = (PintyApp) getApplication();
        Spinner drinksSpinner = (Spinner) findViewById(R.id.add_pricing_drink_spinner);
        priceEntry = (EditText) findViewById(R.id.add_pricing_price);

        // yucky magic :(
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, pinty.drinkNames);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        drinksSpinner.setAdapter(adapter);
        drinksSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                chosenDrink = position;
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                // nothing
            }
        });

    }

    public void submitPrice(View v) {
        String text = priceEntry.getText().toString();
        // let numeic be handled by android itself, it can then deal with , vs . for locales and shit...
        if (chosenDrink == -1 || StringUtils.isEmpty(text)) {
            // Actually, just don't even enable the button until this happens?
            Toast.makeText(this, "Please enter a price!", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(TAG, "Submitting a price for " + barId + ", drink=" + chosenDrink + ", price=" + text);
        // TODO - actually post the data.  probably need a dialog on top to say we're busy?
        // Could also just use an asynctask, which simply makes toast when it's done?
        // Make sure to update our local pricings?
        // Could get an intent here and add info to return with..
        setResult(RESULT_OK);
        finish();
    }

}
