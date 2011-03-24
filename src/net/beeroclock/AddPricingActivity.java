package net.beeroclock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

/**
 * Handles taking in pricing details, packaging it up for the server, and posting it off.
 *
 * @author Karl Palsson, 2011
 *         Date: 2011-03-23
 */
public class AddPricingActivity extends Activity {

    private static final String TAG = "AddPricingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_pricing);

        Long barId = getIntent().getExtras().getLong(Bar.OSM_ID);
        if (barId == null) {
            throw new IllegalStateException("adding a pricing without a bar!");
        }
        Spinner drinksSpinner = (Spinner) findViewById(R.id.add_pricing_drink_spinner);
        // yucky magic :(
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.drink_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        drinksSpinner.setAdapter(adapter);
        drinksSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Selcted pos:" + position + " id:" + id);
                Toast t = Toast.makeText(parent.getContext(), "The planet is " +
                        parent.getItemAtPosition(position).toString(), Toast.LENGTH_SHORT);
                t.show();
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                // nothing
            }
        });

    }

    public void submitPrice(View v) {
        // Could get an intent here and add info to return with..
        setResult(RESULT_OK);
        finish();
    }

}
