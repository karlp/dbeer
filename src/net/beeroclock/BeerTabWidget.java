package net.beeroclock;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

/**
 * Parent for the tab apps.
 *
 * Wholesale from http://developer.android.com/resources/tutorials/views/hello-tabwidget.html
 * but with R.string.blah for tab titles.
 * @author karl
 * Date: 3/9/11
 * Time: 1:34 AM
 */
public class BeerTabWidget extends TabActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost();  // The activity TabHost
        TabHost.TabSpec spec;  // Resusable TabSpec for each tab
        Intent intent;  // Reusable Intent for each tab

        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, WhereBeerActivity.class);

        // Initialize a TabSpec for each tab and add it to the TabHost
        spec = tabHost.newTabSpec("where").setIndicator(res.getString(R.string.tab_title_where),
                res.getDrawable(R.drawable.ic_tab_wherebeer))
                .setContent(intent);
        tabHost.addTab(spec);

        // Do the same for the other tabs
        intent = new Intent().setClass(this, ConfirmBarActivity.class);
        spec = tabHost.newTabSpec("add").setIndicator(res.getString(R.string.tab_title_add),
                res.getDrawable(R.drawable.ic_tab_addbeer))
                .setContent(intent);
        tabHost.addTab(spec);

        tabHost.setCurrentTab(0);
    }
}
