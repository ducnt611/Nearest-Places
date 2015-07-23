package com.example.harpal.mynewmap;

        import android.content.DialogInterface;
        import java.io.BufferedReader;
        import java.io.IOException;
        import java.io.InputStream;
        import java.io.InputStreamReader;
        import java.net.HttpURLConnection;
        import java.net.URL;
        import java.util.HashMap;
        import java.util.List;

        import org.json.JSONObject;

        import android.app.AlertDialog;
        import android.app.Dialog;
        import android.app.ProgressDialog;
        import android.content.DialogInterface;
        import android.content.Intent;
        import android.location.Criteria;
        import android.location.Location;
        import android.location.LocationListener;
        import android.location.LocationManager;
        import android.os.AsyncTask;
        import android.os.Bundle;
        import android.support.v4.app.FragmentActivity;
        import android.util.Log;
        import android.view.Menu;
        import android.view.View;
        import android.view.View.OnClickListener;
        import android.widget.ArrayAdapter;
        import android.widget.Button;
        import android.widget.Spinner;
        import android.widget.Toast;

        import com.google.android.gms.common.ConnectionResult;
        import com.google.android.gms.common.GooglePlayServicesUtil;
        import com.google.android.gms.maps.CameraUpdateFactory;
        import com.google.android.gms.maps.GoogleMap;
        import com.google.android.gms.maps.MapsInitializer;
        import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
        import com.google.android.gms.maps.MapView;
        import com.google.android.gms.maps.model.LatLng;
        import com.google.android.gms.maps.model.Marker;
        import com.google.android.gms.maps.model.MarkerOptions;
public class MainActivity extends FragmentActivity implements LocationListener
{
    private MapView map;
    GoogleMap mGoogleMap;
    Spinner mSprPlaceType;   //spinner will work with

    String[] mPlaceType=null;
    String[] mPlaceTypeName=null;

    double mLatitude=0;
    double mLongitude=0;

    HashMap<String, String> mMarkerPlaceLink = new HashMap<String, String>();

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);


        // Array of place types
        mPlaceType = getResources().getStringArray(R.array.place_type);

        // Array of place type names
        mPlaceTypeName = getResources().getStringArray(R.array.place_type_name);

        // Creating an array adapter with an array of Place types
        // to populate the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, mPlaceTypeName);

        // Getting reference to the Spinner
        mSprPlaceType = (Spinner) findViewById(R.id.spr_place_type);

        // Setting adapter on Spinner to set place types
        mSprPlaceType.setAdapter(adapter);

        //KINJ Button btnFind;

        // Getting reference to Find Button
       /* KINJ
        * btnFind = ( Button ) findViewById(R.id.btn_find);
        btnFind.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PlaceDetailsActivity.class);

                // Starting the Place Details Activity
                startActivity(intent);
            }
        });*/

        // Getting Google Play availability status
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getBaseContext());
        if(status!=ConnectionResult.SUCCESS)
        { // Google Play Services are not available

            int requestCode = 10;
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this, requestCode);
            dialog.show();
        }
        else
        {
            // Google Play Services are available
            // Getting reference to the SupportMapFragment
            //KINJ SupportMapFragment fragment = ( SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

            map = (MapView) findViewById(R.id.map);

            map.onCreate(savedInstanceState);

            mGoogleMap = map.getMap();

            MapsInitializer.initialize(getApplicationContext());

            mGoogleMap.setMyLocationEnabled(true);

            // Getting Google Map
            //KINJ mGoogleMap = fragment.getMap();

            // Enabling MyLocation in Google Map
            mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);

            // Getting LocationManager object from System Service LOCATION_SERVICE
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

            // Creating a criteria object to retrieve provider
            Criteria criteria = new Criteria();

            // Getting the name of the best provider
            String provider = locationManager.getBestProvider(criteria, true);

            // Getting Current Location From GPS
            Location location = locationManager.getLastKnownLocation(provider);

            if(location!=null)
            {
                onLocationChanged(location);
            }

            locationManager.requestLocationUpdates(provider, 20000, 0, this);

            /*if(mLatitude == 0 && mLongitude == 0)
            	showOKAlertMsg("", "Please turn on your gps and restart the application", true);*/

            mGoogleMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener()
            {
                @Override
                public void onInfoWindowClick(Marker arg0)
                {
                    Intent intent = new Intent(getBaseContext(), PlaceDetailsActivity.class);
                    String reference = mMarkerPlaceLink.get(arg0.getId());
                    intent.putExtra("reference", reference);

                    // Starting the Place Details Activity
                    startActivity(intent);
                }
            });

            // Setting click event lister for the find button
            findViewById(R.id.btn_find).setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    int selectedPosition = mSprPlaceType.getSelectedItemPosition();
                    String type = mPlaceType[selectedPosition];

                    StringBuilder sb = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
                    sb.append("location="+mLatitude+","+mLongitude);
                    sb.append("&radius=50000");
                    sb.append("&types="+type);
                    sb.append("&sensor=true");
                    sb.append("&key="+"AIzaSyBbdw04GUIhTJ4svDRk9j6u898t14Uawj8");

                    // Creating a new non-ui thread task to download Google place json data
                    PlacesTask placesTask = new PlacesTask();

                    // Invokes the "doInBackground()" method of the class PlaceTask
                    placesTask.execute(sb.toString());
                }
            });
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if(map != null) map.onResume();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if(mGoogleMap != null)
        {
            mGoogleMap.clear();
        }

        if(map != null)
        {
            map.onDestroy();

            map = null;
        }
    }

    @Override
    public void onLowMemory()
    {
        super.onLowMemory();

        if(map != null)map.onLowMemory();
    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException
    {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try
        {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while( ( line = br.readLine()) != null)
            {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }
        catch(Exception e)
        {
            Log.d("Exception while downloading url", e.toString());
        }
        finally
        {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    /** A class, to download Google Places */
    private class PlacesTask extends AsyncTask<String, Integer, String>
    {

        String data = null;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
            showProgressDialog("", "Please wait...");
        }

        // Invoked by execute() method of this object
        @Override
        protected String doInBackground(String... url)
        {
            try
            {
                Log.v("MyNewMap","LOG - PlacesTask : "+url[0]);
                data = downloadUrl(url[0]);
            }
            catch(Exception e)
            {
                Log.d("Background Task",e.toString());
            }

            return data;
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String result)
        {
            cancelProgresDialog();

            ParserTask parserTask = new ParserTask();

            // Start parsing the Google places in JSON format
            // Invokes the "doInBackground()" method of the class ParseTask
            parserTask.execute(result);
            Log.v("MyNewMap","LOG - PlacesTask - onPostExecute : "+result);
        }
    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String,String>>>
    {
        JSONObject jObject;

        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();

            showProgressDialog("", "Please wait...");
        }

        // Invoked by execute() method of this object
        @Override
        protected List<HashMap<String,String>> doInBackground(String... jsonData)
        {
            List<HashMap<String, String>> places = null;
            PlaceJSONParser placeJsonParser = new PlaceJSONParser();
            Log.v("MyNewMap","LOG - ParserTask : ");
            try
            {
                jObject = new JSONObject(jsonData[0]);

                /** Getting the parsed data as a List construct */
                places = placeJsonParser.parse(jObject);

            }
            catch(Exception e)
            {
                Log.d("Exception",e.toString());
            }

            return places;
        }
        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(List<HashMap<String,String>> list)
        {
            cancelProgresDialog();

            Log.v("MyNewMap","LOG - ParserTask - onPostExecute : "+list.size());

            // Clears all the existing markers
            mGoogleMap.clear();

            if(list.size() == 0)
            {
                int selectedPosition = mSprPlaceType.getSelectedItemPosition();
                String type = mPlaceType[selectedPosition];
                showOKAlertMsg("", "No "+type + " available near by your location", false);
            }
            else
            {
                for(int i=0;i<list.size();i++)
                {
                    // Creating a marker
                    MarkerOptions markerOptions = new MarkerOptions();

                    // Getting a place from the places list
                    HashMap<String, String> hmPlace = list.get(i);

                    // Getting latitude of the place
                    double lat = Double.parseDouble(hmPlace.get("lat"));

                    // Getting longitude of the place
                    double lng = Double.parseDouble(hmPlace.get("lng"));

                    // Getting name
                    String name = hmPlace.get("place_name");

                    // Getting vicinity
                    String vicinity = hmPlace.get("vicinity");

                    LatLng latLng = new LatLng(lat, lng);

                    // Setting the position for the marker
                    markerOptions.position(latLng);

                    // Setting the title for the marker.
                    //This will be displayed on taping the marker
                    markerOptions.title(name + " : " + vicinity);

                    // Placing a marker on the touched position
                    Marker m = mGoogleMap.addMarker(markerOptions);

                    // Linking Marker id and place reference
                    mMarkerPlaceLink.put(m.getId(), hmPlace.get("reference"));
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();
        LatLng latLng = new LatLng(mLatitude, mLongitude);

        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(12));
    }

    @Override
    public void onProviderDisabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onProviderEnabled(String provider) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    protected void showProgressDialog(String title, String message)
    {
        if(progressDialog != null && progressDialog.isShowing())
        {
            progressDialog.dismiss();
        }

        progressDialog = new ProgressDialog(this);

        progressDialog.setTitle(title);

        progressDialog.setMessage(message);

        progressDialog.setCancelable(false);

        progressDialog.show();
    }

    protected void cancelProgresDialog()
    {
        if(progressDialog != null && progressDialog.isShowing())
        {
            progressDialog.dismiss();
        }
    }

    protected void showOKAlertMsg(String title,String msg, final boolean isFinish)
    {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        dialogBuilder.setNeutralButton("Okay", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                if(isFinish)
                    finish();
                else
                    dialog.dismiss();
            }
        });

        dialogBuilder.setTitle(title);

        dialogBuilder.setMessage(msg);

        dialogBuilder.show();
    }
}
