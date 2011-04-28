package net.beeroclock.dbeer.activities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import com.google.android.maps.*;
import net.beeroclock.dbeer.models.Bar;
import net.beeroclock.dbeer.PintyApp;
import net.beeroclock.dbeer.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Just show a bar on a google map.
 * @author karlp
 * Date: 3/22/11
 * Time: 12:03 PM
 */
public class ActivityGoogleMap extends MapActivity {

    private MyLocationOverlay hereOverlay;
    private PintyApp pinty;
    private Bar bar;
    private MapView mapView;

    @Override
    protected boolean isRouteDisplayed() {
        // required for google accounting...
        return false;
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.google_map);
        mapView = (MapView) findViewById(R.id.google_map_view);
        mapView.setBuiltInZoomControls(true);

        Bundle extras = getIntent().getExtras();
        Long barId = extras.getLong(Bar.PKUID);
        if (barId == null) {
            throw new IllegalStateException("It should be impossible to get here: 'show on map' with no barid");
        }
        pinty = (PintyApp)getApplication();
        bar = pinty.getBar(barId);

        List<Overlay> existingMapOverlays = mapView.getOverlays();
        Drawable drawable = this.getResources().getDrawable(R.drawable.red_pushpin);
        BarItemizedOverlay barOverlay = new BarItemizedOverlay(drawable, this);
        barOverlay.addBar(bar);

        hereOverlay = new MyLocationOverlay(this, mapView);
        hereOverlay.enableMyLocation();

        existingMapOverlays.add(barOverlay);
        existingMapOverlays.add(hereOverlay);
    }

    @Override
    protected void onResume() {
        super.onResume();

        GeoPoint barGeo = makeGeoPoint(bar);
        Location loc = pinty.getLastLocation();
        GeoPoint here = new GeoPoint((int)(loc.getLatitude() * 1e6), (int)(loc.getLongitude() * 1e6));

        int maxLat = Math.max(barGeo.getLatitudeE6(), here.getLatitudeE6());
        int minLat = Math.min(barGeo.getLatitudeE6(), here.getLatitudeE6());
        int maxLon = Math.max(barGeo.getLongitudeE6(), here.getLongitudeE6());
        int minLon = Math.min(barGeo.getLongitudeE6(), here.getLongitudeE6());
        mapView.getController().zoomToSpan(Math.abs(maxLat - minLat), Math.abs(maxLon - minLon));
        mapView.getController().animateTo(new GeoPoint((maxLat + minLat) / 2, (maxLon + minLon) / 2));
    }

    @Override
    protected void onStop() {
        super.onStop();
        hereOverlay.disableMyLocation();
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
            OverlayItem i = new OverlayItem(geoPoint, bar.name, bar.type);
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
            dialog.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            dialog.show();
            return true;
        }
    }
}
