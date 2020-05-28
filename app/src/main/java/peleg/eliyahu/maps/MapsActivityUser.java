package peleg.eliyahu.maps;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.IntentSender;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.ContactsContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class MapsActivityUser extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    //used for "center" button.
    View locationButton;

    //google maps activity creator.
    GoogleMap mMap;

    //library of the markers by the ids. every marker have id- the contact id.
    Map<Long, Marker> friendsMarkersById = new HashMap<Long, Marker>();

    //correct_location will save the value of the last phone location.
    static LatLng correct_location = new LatLng(0, 0);

    //using to get the google maps api.
    private GoogleApiClient googleApiClient;

    //list of the friends's that around locations.
    private List<String> friendsLocations = new ArrayList<>();

    //dialog like "wait a second" with loading gif.
    ProgressDialog dialog;

    //circle of the radius- will show on the map to mark the radius.
    Circle circle;

    //only for "SuperUser"- button to change the radius.
    Button set_radius;

    //only for "SuperUser"- spinner of radius from 1 to 15 KM.
    Spinner set_radius_spinner;

    //used to get the location, and to check f the location is enable.
    LocationManager manager = null;

    //false until getting the last location from the server.
    private boolean got_first_location;

    //the radius of the friends around. the  default is 1. the SuperUser can change the radius, from 1 to 15.
    private int radius = 1;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //show on screen the file "activity_maps.xml"
        setContentView(R.layout.activity_maps);

        //define the dialog.
        dialog = new ProgressDialog(MapsActivityUser.this);

        //build new googleApiClient. used to enable the gps.
        googleApiClientBuilder();

        //used to show the map by xml file.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        //if the map exist..
        if (mapFragment != null) {

            //load the map.
            mapFragment.getMapAsync(this);

            //used to locate the "center" button.
            locationButton = ((View) Objects.requireNonNull(mapFragment.getView()).findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
        }

        //define the location manager- to turn on the gps.
        manager= (LocationManager) MapsActivityUser.this.getSystemService(Context.LOCATION_SERVICE);

        //enable gps, if unable.
        enable_gps();

        //find the radius button and spinner in the xml, and "connect" them with the java code.
        set_radius = findViewById(R.id.set_radius);
        set_radius_spinner = findViewById(R.id.set_radius_spinner);

        //"User" can't change the radius- vanished the button and the spinner.
        if(MainActivity.userType.equals("U")){
            set_radius.setVisibility(View.GONE);
            set_radius_spinner.setVisibility(View.GONE);
        }

        //when the "set_radius" button clicked..
        set_radius.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //drop down the spinner.
                set_radius_spinner.performClick();
            }
        });

        //when the user selecting item from the spinner..
        set_radius_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //set the radius to the new item that the user selected.
                radius = position + 1;
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        //start the timer task. will run every 0.1 second.
        //the timer task used for location updates (to server) and friends updates (from server).
        Timer timer = new Timer();
        timer.schedule(timerTask, 0, 100);

    }

    //the timer task used for location updates (to server) and friends updates (from server).
    private TimerTask timerTask = new TimerTask() {

        @Override
        public void run() {

            //before getting the first location, the server should't get data. trying to get the firsl location until getting..
            //the first location is the last location that the server saved.
            if(got_first_location) {

                //send the location and the radius to the server.
                MainActivity.DATA_TO_SERVER = correct_location.toString() + ";" + radius+ ";";
                new MainActivity.send().start();
            }

            //if(got_first_location) the server will send the friends and they locations.
            //else' the server will send the first_location.
            new MainActivity.receive().start();

            //if already got first location, update the radius and the circle.
            if(got_first_location) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //"updateCircle" getting the radius in meters. the "radius" is KMS.
                        updateCircle(radius*1000);

                    }
                });
            }

            //if the "DATA_FROM_SERVER" is "U". "S" or "1", the "DATA_FROM_SERVER" din'nt updated yet to the first_location..
            if(!MainActivity.DATA_FROM_SERVER.equals("U") && !MainActivity.DATA_FROM_SERVER.equals("S") && !MainActivity.DATA_FROM_SERVER.equals("1") ) {

                //if the first char is "F" (FirstLocation)..
                if(MainActivity.DATA_FROM_SERVER.charAt(0) == 'F') {

                    //split the "DATA_FROM_SERVER" every ",", and save as list. the list should be like: "F, x, y, radius".
                    List<String> splited = Arrays.asList(MainActivity.DATA_FROM_SERVER.split(","));

                    //if the location from the server is not (0, 0) (the default registering location)..
                    if(Double.parseDouble(splited.get(1)) != 0 && Double.parseDouble(splited.get(2)) != 0.0){

                        //save the location from the server (the last know location) into "correct_location".
                        //used to save time with the location updates.
                        correct_location = new LatLng(Double.parseDouble(splited.get(1)),
                                Double.parseDouble(splited.get(2)));
                    }

                    //set the radius to the last know radius from the server.
                    radius = Integer.parseInt(splited.get(3));

                    //set all the places that the radius used to the new radius.
                    runOnUiThread(new Runnable() {
                        @Override public void run() {

                            //change the circle size to the new radius.
                            updateCircle(radius*1000);

                            //set the radius spinner to the right radius.
                            set_radius_spinner.setSelection(radius-1);
                        }
                    });

                    //got the first location. next time starting with the updates.
                    got_first_location = true;

                }
                //is already got the first location..
                else {

                    //save the last time friends list in "oldFriendsLocations".
                    List<String> oldFriendsLocations = friendsLocations;

                    //saving to a list the friends locations. "DATA_FROM_SERVER" should be in the format: "x'y'id;x'y'id;"..
                    friendsLocations = Arrays.asList(MainActivity.DATA_FROM_SERVER.split(";"));

                    //using HashSet for the "removeAll" after.

                    //!!!important!!!
                    //
                    //the HashSet using also to update friends locations-
                    //if the location is not the same location, the "removeAll" will not remove this friend, and he will marker again.
                    //
                    //!!!important!!!


                    //will save the irrelevant friends that got out of the radios.
                    final HashSet<String> irrelevantFriends = new HashSet<>(oldFriendsLocations);

                    //using "oldFriendsLocations" twice because the "irrelevantFriends" will not save all the "oldFriendsLocations" list that needed to create "newFriends".
                    final HashSet<String> oldFriendsList = new HashSet<>(oldFriendsLocations);

                    //using "friendsLocations" value twice, because the "newFriends" will not save all the "friendsLocations" list that needed to create "irrelevantFriends".
                    final HashSet<String> friendsList = new HashSet<>(friendsLocations);

                    //will save the friends that just now came here.
                    final HashSet<String> newFriends = new HashSet<>(friendsLocations);

                    //by removing the "new friends" from the "old friends", getting the irrelevant friends.
                    irrelevantFriends.removeAll(friendsList);

                    //by removing the "old friends" from the "new friends", getting the new friends.
                    newFriends.removeAll(oldFriendsList);

                    //if there are irrelevant friends..
                    if (!irrelevantFriends.toString().equals("[']") && !irrelevantFriends.isEmpty())

                        //cancel there markers. (false- cancel markers. true- marker new)
                        markerFriendsHere(irrelevantFriends, false);

                    //if there are new friends..
                    if (!newFriends.toString().equals("[']") && !newFriends.isEmpty()) {

                        //marker them. (false- cancel markers. true- marker new)
                        markerFriendsHere(newFriends, true);
                    }
                    //if the list is empty ("[']") clear the list- the next time needed to start from clear, and then the "'" will stay in the list.
                    else {

                        //clear the friends list- for the next update.
                        friendsList.clear();
                    }
                }
            }
        }
    };

    //will marker HashSet of locations from the type: x'y'id
    private void markerFriendsHere(HashSet<String> newFriends, final boolean newMarker) {

        //convert the HashSet to list.
        List<String> newFriendsList = new ArrayList<>(newFriends);

        //run through the list. one friend every time.
        for(int i = 0; i < newFriendsList.size(); i++){

            //split the friend data into list. the format: x'y'id
            List<String> a = Arrays.asList(newFriendsList.get(i).split("'"));

            //save the first place from the split list- the "X" into x.
            String x = a.get(0);

            //save the second place from the split list- the "y" into y.
            String y = a.get(1);

            //save the contact id (place 3) from the split list. "id".
            final long contact_id = Long.parseLong(a.get(2));

            //create new LatLng by the "X" and the "Y".
            final LatLng friendLatLng = new LatLng(Double.parseDouble(x), Double.parseDouble(y));

            //if newMarker- needed to mark the friend.
            if(newMarker){

                //the marker can be used only by the main thread.
                //marker the friend with the title of his name.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {marker_friend(friendLatLng, name_by_id(contact_id), contact_id);}
                    });
            }else {
                //the marker can be used only by the main thread.
                //cancel tyhe marker of the friend with this specifically id.
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {remove_marker_friend(contact_id); }
                });
            }

        }
    }

    //get the cantact name by his id.
    //using the id, because the numbers are the same in all the languages, and the name can crash the server.
    String name_by_id(long id){

        //will save the contact name.
        String name = null;

        //new content to get a cursor for the contacts.
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        //if at less one contact..
        if (Objects.requireNonNull(cursor).getCount() > 0) {

            //go to the next contact in the list.
            while (cursor.moveToNext()) {

                //get the contact id.
                String contact_id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

                //if the contact is the one that we are looking for...
                if(contact_id.equals(String.valueOf(id))){

                    //save the name.
                    name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    break;
                }
            }

            //close the cursor only after the loop.
            cursor.close();
        }

        //if there is a name..
        if(name != null) return name;

        //if the name is null, return some string- crash-proof.
        else return "no name";
    }


    //build new googleApiClient.
    void googleApiClientBuilder() {

        //if google api is null, create new one.
        if (googleApiClient == null) {

            //
            googleApiClient = new GoogleApiClient.Builder(MapsActivityUser.this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override public void onConnected(Bundle bundle) {}
                        @Override public void onConnectionSuspended(int i) { googleApiClient.connect(); }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}
                    }).build();

            //connect to google api and get client data.
            googleApiClient.connect();
        }
    }


    //turning on the gps.
    private void enable_gps(){

            //create new gps request
            LocationRequest locationRequest = LocationRequest.create();

            //type of gps location- hi level. wifi + gps + 4G
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            //build builder of location-settings request
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            //new result of location settings.
            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());

            //when the request ending..
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {

                @Override
                //when getting result of "result" (LocationSettingsResult)...
                public void onResult(@NonNull LocationSettingsResult result) {

                    //save in "status" the status of "result"
                    final Status status = result.getStatus();

                    try {
                        //if the gps unable..
                        if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER))

                            //ask the user to turn on the gps.
                            status.startResolutionForResult(MapsActivityUser.this, 0);

                    } catch (IntentSender.SendIntentException e) {
                        e.printStackTrace();
                    }
                }
            });
    }


    //remove marker by specifically id.
    public void remove_marker_friend(long contact_id) {

        //get the marker from the markers list by his id.
        Marker a = (friendsMarkersById.get(contact_id));

        //if the marker is'nt null, unable the marker.
        Objects.requireNonNull(a).setVisible(false);
    }

    //marker friend by the id, with the title "title", and in the location "location"
    public void marker_friend(LatLng location, String title, long contact_id) {

        //if the friend not in the markers list yet..
        if (!friendsMarkersById.containsKey(contact_id)) {

            //create new marker with the title "title", and the position "location".
            Marker friend_marker = mMap.addMarker(new MarkerOptions().position(location).title(title));

            //create new value in the list- the key is the "id", and the marker is the new one.
            friendsMarkersById.put(contact_id, friend_marker);

        }
        //if the friend's marker already in the list..
        else {

            //get the marker of this friend by the id, into marker.
            Marker marker = (friendsMarkersById.get(contact_id));

            //if the marker is not Visible, and not null..
            if(!Objects.requireNonNull(marker).isVisible()){

                //show the marker on the map.
                marker.setVisible(true);
            }
            //set the position to the new location.
            marker.setPosition(location);

            //set the title to the new one.
            marker.setTitle(title);
        }
    }


    //updating the circle of the "radius"- showing on the map. red color.
    //getting the new radius in meters.
    void updateCircle(int radiusMeters) {

        //set the radius to the new one.
        circle.setRadius(radiusMeters);

        //set visitable to on (it's off in the start of the program- until getting the first radius from the server.
        circle.setVisible(true);

        //set the center to the correct location.
        circle.setCenter(correct_location);
    }

    //creating the first circle when getting the first location.
    class first_circle extends Thread{
        @Override
        public void run() {

            //wait until getting the first normal location.
            while(correct_location.longitude == 0) {

                //get the last know location.
                Location a = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                //if not null- the location updated. use the update.
                if(a != null) correct_location = new LatLng(a.getLatitude(), a.getLongitude());

            }

            //can move the camera only by the main thread.
            runOnUiThread(new Runnable() {

                @Override
                public void run() {

                    //move the camera to the correct location.
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(correct_location, 11.3f));

                    //update the circle to move the center.
                    updateCircle(radius*1000);
                }
            });
        }
    }

    //using for location updates in the background.
    protected void startLocationUpdates() {

        //Create the location request to start receiving updates.
        //min time for update- 1 minuets.
        //min distance for update- 1 meters.
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(1);
        mLocationRequest.setSmallestDisplacement(1);


        //Create LocationSettingsRequest object using location request.
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        //Check whether location settings are satisfied.
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        //request location updates.
        getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {

            //when got result..
            @Override
            public void onLocationResult(LocationResult locationResult) {

                //get the location of the last update.
                Location location = locationResult.getLastLocation();

                //update the location to the new one.
                correct_location = new LatLng(location.getLatitude(), location.getLongitude());

                //update for the center update.
                updateCircle(radius*1000);

            }
        }, Looper.myLooper());
    }

    @Override
    //if "back" button pressed, move the app to the background.
    public void onBackPressed()
    { moveTaskToBack(true);}

    @Override
    //when the map getting ready..
    public void onMapReady(GoogleMap googleMap) {

        //def the map.
        mMap = googleMap;

        //show the button of "center" and the correct location (blue circle).
        mMap.setMyLocationEnabled(true);

        //set OnMyLocationChangeListener of the activity.
        mMap.setOnMyLocationChangeListener(myLocationChangeListener);

        //connect to the layout- needed to move the "center" button.
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                locationButton.getLayoutParams();

        // move the position to right bottom
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1);

        //create new circle that will show the radius.
        circle = mMap.addCircle(new CircleOptions().center(correct_location).strokeColor(Color.RED));

        //don't show the circle until getting the radius from the server.
        circle.setVisible(false);

        //start the "first_circle" to wait until the location updating.
        new first_circle().start();

        //start the location updates.
        startLocationUpdates();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    @Override
    public void onConnected(@Nullable Bundle bundle) {}

    @Override
    public void onConnectionSuspended(int i) {}

    private GoogleMap.OnMyLocationChangeListener myLocationChangeListener = new GoogleMap.OnMyLocationChangeListener() {
        @Override
        public void onMyLocationChange(Location location) {

            //updating the location to the new one.
            correct_location = new LatLng(location.getLatitude(), location.getLongitude());

            //updating for the center update.
            updateCircle(radius*1000);
        }
    };

}

