package net.beeroclock;

import android.app.Activity;
import android.os.Bundle;

/**
 * Activity for adding a new beer price
 * @author karl
 * Date: 3/7/11
 * Time: 9:18 PM
 */
public class AddBeerActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addbeer);
    }
}