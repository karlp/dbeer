package net.beeroclock;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;
import com.google.android.maps.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Just show a bar on a google map.
 * @author karlp
 * Date: 3/22/11
 * Time: 12:03 PM
 */
public class ActivityGoogleMap extends MapActivity {

    // FIXME - how do I share this?
    private void fail_go_boom() {
        Toast t = Toast.makeText(getApplicationContext(), R.string.toast_wtf, Toast.LENGTH_SHORT);
        t.show();
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    protected boolean isRouteDisplayed() {
        // required for google accounting...
        return false;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.google_map);
        MapView mapView = (MapView) findViewById(R.id.google_map_view);
        mapView.setBuiltInZoomControls(true);

        Bundle extras = getIntent().getExtras();
        Long barId = extras.getLong(Bar.OSM_ID);
        if (barId == null) {
            fail_go_boom();
            return;
        }
        PintyApp pinty = (PintyApp)getApplication();
        Bar bar = pinty.getBar(barId);

        List<Overlay> existingMapOverlays = mapView.getOverlays();
        // FIXME - again with the whole artwork thing, a bar icon...
        Drawable drawable = this.getResources().getDrawable(R.drawable.ic_menu_goto);
        BarItemizedOverlay barOverlay = new BarItemizedOverlay(drawable, this);
        barOverlay.addBar(bar);
        existingMapOverlays.add(barOverlay);
        mapView.getController().animateTo(makeGeoPoint(bar));
        mapView.getController().setZoom(16);  // this seems reasonable?
    }

    private GeoPoint makeGeoPoint(Bar bar) {
        return new GeoPoint((int)(bar.lat * 1e6), (int)(bar.lon * 1e6));
    }

    class BarItemizedOverlay extends ItemizedOverlay {

        ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
        private Context context;

        public BarItemizedOverlay(Drawable defaultMarker, Context context) {
            super(boundCenterBottom(defaultMarker));
            this.context = context;
        }

        public void addBar(Bar bar) {
            GeoPoint geoPoint = makeGeoPoint(bar);
            OverlayItem i = new OverlayItem(geoPoint, bar.name, "I hear it's a cool place...");
            items.add(i);
            populate();
        }

        @Override
        protected OverlayItem createItem(int i) {
            return items.get(i);
        }

        @Override
        public int size() {
            return items.size();
        }

        @Override
        protected boolean onTap(int index) {
            OverlayItem item = items.get(index);
            // FIXME - this could perhaps kick off a "get directions" too?
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle(item.getTitle());
            dialog.setMessage(item.getSnippet());
            dialog.show();
            return true;
        }
    }
}
