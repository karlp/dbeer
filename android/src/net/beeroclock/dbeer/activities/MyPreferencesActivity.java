package net.beeroclock.dbeer.activities;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import net.beeroclock.dbeer.PintyApp;
import net.beeroclock.dbeer.R;
import net.beeroclock.dbeer.Utils;
import net.beeroclock.dbeer.ws.DBeerServiceStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Handles all our application preferences
 *
 * @author Karl Palsson, 2011
 *         Date: 2011-04-06
 */
public class MyPreferencesActivity extends PreferenceActivity {

    public static final String PREF_LAST_DATA_UPDATE = "last_data_update";
    private Preference lastUpdatedPref;
    private PintyApp pinty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pinty = (PintyApp) getApplication();

        addPreferencesFromResource(R.xml.preferences);

        lastUpdatedPref = findPreference(PREF_LAST_DATA_UPDATE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String lastUpdated = prefs.getString("last_data_update", "unknown");
        lastUpdatedPref.setSummary(lastUpdated);

        ListPreference lp = (ListPreference) findPreference("server");
        lp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object o) {
                pinty.getKnownBars().clear();
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO - this should only fetch via http if we last checked more than a day ago or so.
        // this is just inviting an overloaded server.
        new DBeerServiceStatusFetcher().execute(null);
    }

    class DBeerServiceStatusFetcher extends AsyncTask<Void, Void, DBeerServiceStatus> {

        @Override
        protected DBeerServiceStatus doInBackground(Void... voids) {
            String xmlr;

            HttpClient client = new DefaultHttpClient();
            client.getParams().setParameter(CoreProtocolPNames.USER_AGENT, pinty.userAgent);
            URI uri;
            try {
                uri = URIUtils.createURI("http", pinty.getServer(), -1, "/status", null, null);
            } catch (URISyntaxException e) {
                return new DBeerServiceStatus("How did this happen? URI Syntax exception?!", e);
            }
            HttpGet request = new HttpGet(uri);

            try {
                xmlr = client.execute(request, new BasicResponseHandler());
            } catch (HttpResponseException e) {
                return new DBeerServiceStatus("invalid response from the server: " + e.getStatusCode(), e);
            } catch (IOException e) {
                return new DBeerServiceStatus("Craziness? " + e.getMessage(), e);
            }
            try {
                return Utils.parseStatus(xmlr);
            } catch (Exception e) {
                return new DBeerServiceStatus("Failed to parse the xml reply", e);
            }
        }

        @Override
        protected void onPostExecute(DBeerServiceStatus result) {
            super.onPostExecute(result);
            if (result.success) {
                lastUpdatedPref.setSummary(result.lastUpdated);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MyPreferencesActivity.this);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(PREF_LAST_DATA_UPDATE, result.lastUpdated);
                editor.commit();
            } else {
                // If we couldn't get the server status, while we do care, we don't really want to try and start
                // notifying the server about it either.
                lastUpdatedPref.setSummary("unknown");
            }
        }

    }

}
