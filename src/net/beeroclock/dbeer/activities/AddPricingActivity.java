package net.beeroclock.dbeer.activities;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import net.beeroclock.dbeer.models.Bar;
import net.beeroclock.dbeer.PintyApp;
import net.beeroclock.dbeer.R;
import net.beeroclock.dbeer.models.PricingReport;
import net.beeroclock.dbeer.models.ReportStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
    private Button submitButton;

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
        submitButton = (Button)findViewById(R.id.add_pricing_btn_confirm);

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
        Log.i(TAG, "Submitting a price for " + barId + ", drink=" + pinty.drinkExternalIds.get(chosenDrink) + ", price=" + text);
        // Make sure to update our local pricings?
        // Could get an intent here and add info to return with..
        PricingReport pricingReport = new PricingReport(barId, 
                pinty.getLastLocation().getLatitude(), pinty.getLastLocation().getLongitude(), 
                pinty.drinkExternalIds.get(chosenDrink), new BigDecimal(text));
        new BarPricePoster().execute(pricingReport);
    }

    class BarPricePoster extends AsyncTask<PricingReport, Void, ReportStatus> {

        @Override
        protected ReportStatus doInBackground(PricingReport... pricingReports) {
            String xmlr;
            PricingReport report = pricingReports[0];

            HttpClient client = new DefaultHttpClient();
            List<NameValuePair> qparams = new ArrayList<NameValuePair>();
            qparams.add(new BasicNameValuePair("recordedLat", String.valueOf(report.lat)));
            qparams.add(new BasicNameValuePair("recordedLon", String.valueOf(report.lon)));
            qparams.add(new BasicNameValuePair("price_type", String.valueOf(report.drinkExternalId)));
            qparams.add(new BasicNameValuePair("price", String.valueOf(report.priceInLocalCurrency)));
            qparams.add(new BasicNameValuePair("price_date", String.valueOf(report.dateRecorded.getTime())));

            HttpPut request = new HttpPut("http://tera.beeroclock.net/bar/" + report.barOsmId + ".xml");
            try {
                request.setEntity(new UrlEncodedFormEntity(qparams, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("if utf-8 is an unsupported encoding, we're all fucked");
            }

            try {
                xmlr = client.execute(request, new BasicResponseHandler());
            } catch (HttpResponseException e) {
                Log.e(TAG, e.getStatusCode() + "-" + e.getMessage(), e);
                return new ReportStatus(false, e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "Crazy error" + e.getMessage(), e);
                return new ReportStatus(false, e.getMessage());
            }
            pinty.addPricingReport(report);
            return new ReportStatus(true, xmlr);
        }

        @Override
        protected void onPostExecute(ReportStatus reportStatus) {
            super.onPostExecute(reportStatus);
            if (reportStatus.success) {
                submitButton.setText(R.string.add_beer_btn_confirm);
                setResult(RESULT_OK);
                finish();

            } else {
                // should maybe move all this out into a method on the parent?
                submitButton.setText(R.string.add_beer_btn_retry);
                Toast.makeText(AddPricingActivity.this, R.string.add_price_failure, Toast.LENGTH_SHORT).show();
            }
        }
    }


}