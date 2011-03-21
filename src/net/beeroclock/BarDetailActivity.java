package net.beeroclock;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.TextView;

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
        tvBarName.setText("Looking at bar: " + getIntent().getExtras().getString("barname"));
    }
}
