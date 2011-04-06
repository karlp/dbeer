package net.beeroclock.dbeer.activities;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import net.beeroclock.dbeer.LocalDatabase;
import net.beeroclock.dbeer.PintyApp;
import net.beeroclock.dbeer.R;

/**
 * Shows all the hidden bars, click to pop a dialog for re-enabling or going to the bar
 *
 * @author Karl Palsson, 2011
 *         Date: 2011-04-06
 */
public class HiddenBarEditActivity extends ListActivity {

    private PintyApp pinty;
    private SQLiteDatabase mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hidden_bar_edit);
        pinty = (PintyApp)getApplication();

        mDb = new LocalDatabase(this).getWritableDatabase();
        fillData();
        // only for long clicks... registerForContextMenu(getListView());

    }

    @Override
    protected void onStop() {
        super.onStop();
        mDb.close();
    }

    private void fillData() {
        Cursor c = mDb.query(LocalDatabase.TABLE_HIDDEN_BARS, new String[]{LocalDatabase.HB_ROWID, LocalDatabase.HB_PKUID, LocalDatabase.HB_NAME}, null, null, null, null, LocalDatabase.HB_NAME);
        startManagingCursor(c);

        String[] from = new String[] {LocalDatabase.HB_NAME};
        int[] to = new int[] {R.id.hidden_bar_row_text};

        SimpleCursorAdapter sca = new SimpleCursorAdapter(this, R.layout.hidden_bar_row, c, from, to);
        setListAdapter(sca);
    }

    /**
     * Drop them all from the db, and make sure we update our current state
     * @param view
     */
    public void onClick_clearAll(View view) {
        SQLiteDatabase db = new LocalDatabase(this).getWritableDatabase();
        db.delete(LocalDatabase.TABLE_HIDDEN_BARS, null, null);
        pinty.loadHiddenBars();
        finish();
    }


    /**
     * On clicking a hidden bar, we pop dialog: unhide or go to bar?
     * @param l
     * @param v
     * @param position
     * @param id
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
//        Intent i = new Intent(this, BarDetailActivity.class);
//        Bar b = (Bar) v.getTag(R.id.tag_bar);
//        i.putExtra(Bar.PKUID, b.pkuid);
//        startActivity(i);
        Toast.makeText(this, "Want to pop a dialog here!", Toast.LENGTH_SHORT).show();
    }


}
