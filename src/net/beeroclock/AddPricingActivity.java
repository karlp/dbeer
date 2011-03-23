package net.beeroclock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

/**
 * To change this template use File | Settings | File Templates.
 *
 * @author Karl Palsson, 2011
 *         Date: 2011-03-23
 */
public class AddPricingActivity extends Activity {

    private static final String TAG = "AddPricingActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);    //To change body of overridden methods use File | Settings | File Templates.
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
        Toast t = Toast.makeText(this, "hoho, adding a price!", Toast.LENGTH_SHORT);
        t.show();
        // now, go allll the way back to nearby bars...
        // (or, back to add pricing view, let them add another price?)
        // maybe?
        // FIXME - This works, but it tosses out the nearby bar list in the application...
        // Actually, it doesn't seem to, but wherebeeractivity probably doesn't have the right lifecycle to handle it properly
        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(getBaseContext().getPackageName());
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
        startActivity(i);
    }

}
