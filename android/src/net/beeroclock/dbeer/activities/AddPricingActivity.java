package net.beeroclock.dbeer.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import net.beeroclock.dbeer.models.Bar;
import net.beeroclock.dbeer.PintyApp;
import net.beeroclock.dbeer.R;
import net.beeroclock.dbeer.models.PricingReport;
import net.beeroclock.dbeer.models.ReportStatus;
import org.acra.ErrorReporter;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;

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
    private ProgressDialog uploadDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.add_pricing);

        barId = getIntent().getExtras().getLong(Bar.PKUID);
        if (barId == null) {
            throw new IllegalStateException("adding a pricing without a bar!");
        }
        pinty = (PintyApp) getApplication();
        Spinner drinksSpinner = (Spinner) findViewById(R.id.add_pricing_drink_spinner);
        priceEntry = (EditText) findViewById(R.id.add_pricing_price);
        submitButton = (Button)findViewById(R.id.add_pricing_btn_confirm);

        // let people just press "send/done" on their keyboard, rather than having to click the send button
        priceEntry.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    submitButton.performClick();
                }
                return true;
            }
        });

        // yucky magic :(
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, pinty.drinkNames);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        drinksSpinner.setAdapter(adapter);
        drinksSpinner.setSelection(pinty.drinkExternalIds.indexOf(pinty.getFavouriteDrink()));
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
        uploadDialog = ProgressDialog.show(this, getResources().getString(R.string.dialog_upload_title), 
                        getResources().getString(R.string.dialog_upload_message), true, false);
        
        Log.i(TAG, "Submitting a price for " + barId + ", drink=" + pinty.drinkExternalIds.get(chosenDrink) + ", price=" + text);
        // Make sure to update our local pricings?
        // Could get an intent here and add info to return with..
        PricingReport pricingReport = new PricingReport(barId, 
                pinty.getLastLocation().getLatitude(), pinty.getLastLocation().getLongitude(), 
                pinty.drinkExternalIds.get(chosenDrink), new BigDecimal(text));
        setProgressBarIndeterminateVisibility(true);
        new BarPricePoster().execute(pricingReport);
    }

    class BarPricePoster extends AsyncTask<PricingReport, Void, ReportStatus> {

        @Override
        protected ReportStatus doInBackground(PricingReport... pricingReports) {
            String xmlr;
            PricingReport report = pricingReports[0];

            HttpClient client = new DefaultHttpClient();
            client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, pinty.userAgent);
            List<NameValuePair> qparams = new ArrayList<NameValuePair>();
            qparams.add(new BasicNameValuePair("recordedLat", String.valueOf(report.lat)));
            qparams.add(new BasicNameValuePair("recordedLon", String.valueOf(report.lon)));
            qparams.add(new BasicNameValuePair("price_type", String.valueOf(report.drinkTypeId)));
            qparams.add(new BasicNameValuePair("price", String.valueOf(report.priceInLocalCurrency)));
            qparams.add(new BasicNameValuePair("price_date", String.valueOf(report.dateRecorded.getTime())));

            HttpPut request = new HttpPut("http://" + pinty.getServer() + "/bar/" + report.barOsmId + ".xml");
            try {
                request.setEntity(new UrlEncodedFormEntity(qparams, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("if utf-8 is an unsupported encoding, we're all fucked");
            }

            try {
                xmlr = client.execute(request, new BasicResponseHandler());
            } catch (HttpResponseException e) {
                Log.e(TAG, e.getStatusCode() + "-" + e.getMessage(), e);
                ErrorReporter.getInstance().handleException(e);
                return new ReportStatus(false, e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "Crazy error" + e.getMessage(), e);
                ErrorReporter.getInstance().handleException(e);
                return new ReportStatus(false, e.getMessage());
            }
            pinty.addPricingReport(report);
            return new ReportStatus(true, xmlr);
        }

        @Override
        protected void onPostExecute(ReportStatus reportStatus) {
            super.onPostExecute(reportStatus);
            setProgressBarIndeterminateVisibility(false);
            uploadDialog.dismiss();
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
