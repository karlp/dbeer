package net.beeroclock.dbeer.activities;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;
import net.beeroclock.dbeer.PintyApp;
import net.beeroclock.dbeer.R;

/**
 * Handles all our application preferences
 *
 * @author Karl Palsson, 2011
 *         Date: 2011-04-06
 */
public class MyPreferencesActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        ListPreference lp = (ListPreference) findPreference("server");
        lp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object o) {
                // FIXME - this does _NOT_ correctly restart the whole app cleanly :(
                Toast.makeText(MyPreferencesActivity.this, "need to dump and load :(", Toast.LENGTH_SHORT).show();
                PintyApp pinty = (PintyApp) getApplication();
                pinty.getKnownBars().clear();
                Log.i("prefs", "cleared out pinty's brannne!");
                return true;
            }
        });
    }
}
