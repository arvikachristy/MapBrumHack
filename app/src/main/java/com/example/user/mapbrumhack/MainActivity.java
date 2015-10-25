package com.example.user.mapbrumhack;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookSdk;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.lang.reflect.Array;
import java.security.Provider;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;


public class MainActivity  extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final String DIALOG_ERROR = "dialog_error";
    private static final String STATE_RESOLVING_ERROR = "resolving_error";
    private boolean mResolvingError = false;
    private LatLng latlng = new LatLng(0, 0);    private boolean MapReady = false;
    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    HashMap<Long, MarkerOptions> markers = new HashMap<Long, MarkerOptions>();
    long markerCount = 0;

    public MainActivity() {
    }
//hi
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Facebook.Sdk.sdkInitialize(getApplicationContext());
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        setContentView(R.layout.activity_maps);
        mResolvingError = savedInstanceState != null &&
                savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    @Override
    protected void onStart(){
        super.onStart();
        if(!mResolvingError)
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop(){
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    protected synchronized void buildGoogleApiClient(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener(){

            @Override
            public void onInfoWindowClick(Marker marker) {
                Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/events/924176117663346/"));
                startActivity(viewIntent);
            }
        });
        mMap.setMyLocationEnabled(true);
        CameraUpdate position = CameraUpdateFactory.newLatLng(latlng);
        mMap.moveCamera(position);
        MapReady = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        ArrayList<LatLng> latlngs = new ArrayList<LatLng>();
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        latlng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        if(MapReady) {
            CameraUpdate position = CameraUpdateFactory.newLatLngZoom(latlng, (float)15.0);
            mMap.animateCamera(position);
        }
        for(int i = 0; i < 100; i++)
            latlngs.add(create_lat_lng(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 0.05));

        for(LatLng l : latlngs){
            MarkerOptions m = new MarkerOptions()
                    .position(l)
                    .alpha(0)
                    .title("Brum Hack")
                    .snippet("Attendees: 39\r\nDate:23rd - 25th Oct");
            markers.put(markerCount, m);
            mMap.addMarker(m);
            markerCount++;
        }
        HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                .data(latlngs)
                .radius(40)
                .build();

        TileOverlay mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // Need to disable UI components that depend on Google APIs until
        // onConnected is called
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if(mResolvingError){
            return;
        } else if(connectionResult.hasResolution()){
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult(
                        this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                mGoogleApiClient.connect();
            }
        }
        else{
            showErrorDialog(connectionResult.getErrorCode());
            mResolvingError = true;
        }
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        //
        // More about this in the 'Handle Connection Failures' section.
    }

    private void showErrorDialog(int errorCode){
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getFragmentManager(),"tag");
    }

    public void onDialogDismissed(){
        mResolvingError = false;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == REQUEST_RESOLVE_ERROR){
            mResolvingError = false;
            if (resultCode == RESULT_OK){
                if(!mGoogleApiClient.isConnecting() &&
                        !mGoogleApiClient.isConnected()){
                    mGoogleApiClient.connect();
                }
            }
        }
    }

    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment(){ }

        public Dialog onCreateDialog(Bundle savedInstanceState){
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog){
            ((MainActivity) getActivity()).onDialogDismissed();
        }
    }

    public LatLng create_lat_lng(double lat_seed, double long_seed, double range){
        double result_a, result_b;
        Random random = new Random();
        result_a = (random.nextFloat() % range) - (range / 2);
        result_b = (random.nextFloat() % range) - (range / 2);
        return new LatLng(lat_seed + result_a, long_seed + result_b);
    }
}