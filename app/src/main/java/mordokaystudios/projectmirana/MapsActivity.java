package mordokaystudios.projectmirana;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Debug;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    //the JSON object returned from PHP querry to mySQL database
    JSONObject myJSON;

    private class GetAllLocations extends AsyncTask<String, Void, String> {
        protected void onPreExecute() {

        }
        @Override
        protected String doInBackground(String... arg0) {

            String link;
            BufferedReader bufferedReader;
            String result = "";

            try {
                link = "http://web.ist.utl.pt/~ist165821/databaseConnection/getAllLocations.php";
                URL url = new URL(link);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();

                bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));
                result = bufferedReader.readLine();

                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }
        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject jsonObj = new JSONObject();
                if (result != null) {
                    String jsonString = result.substring(1, result.length() - 1);
                    myJSON = new JSONObject(jsonString);
                    //System.out.println("Gonna get long: " + jsonObj.getString("long"));
                }
                LocationObject locations = readMySQLResult(result);
                mProvider = new HeatmapTileProvider.Builder().data(locations.list).build();

                for(int i = 0 ; i < locations.list.size(); i++){
                    LatLng markerLocation = locations.list.get(i);
                    String markerName = locations.eventName.get(i);
                    String markerDescription = locations.eventDescription.get(i);

                    String wraped_description = "";
                    Pattern regex = Pattern.compile("(.{1,10}(?:\\s|$))|(.{0,10})", Pattern.DOTALL);
                    Matcher regexMatcher = regex.matcher(markerDescription);
                    while (regexMatcher.find()) {
                        wraped_description +=  regexMatcher.group() + "\n";
                    }
                    System.out.println("Match List "+wraped_description);

                    myMarker = mMap.addMarker(new MarkerOptions().position(markerLocation).title(markerName).snippet(markerDescription));
                }

                //mProvider = new HeatmapTileProvider.Builder().data(readItems(result)).build();
                mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
            }
            catch(JSONException e){
                e.printStackTrace();
            }
        }
    }

    private GoogleMap mMap;

    /**
     * Alternative radius for convolution
     */
    private static final int ALT_HEATMAP_RADIUS = 10;

    /**
     * Alternative opacity of heatmap overlay
     */
    private static final double ALT_HEATMAP_OPACITY = 0.4;

    /**
     * Alternative heatmap gradient (blue -> red)
     * Copied from Javascript version
     */
    private static final int[] ALT_HEATMAP_GRADIENT_COLORS = {
            Color.argb(0, 0, 255, 255),// transparent
            Color.argb(255 / 3 * 2, 0, 255, 255),
            Color.rgb(0, 191, 255),
            Color.rgb(0, 0, 127),
            Color.rgb(255, 0, 0)
    };

    public static final float[] ALT_HEATMAP_GRADIENT_START_POINTS = {
            0.0f, 0.10f, 0.20f, 0.60f, 1.0f
    };

    public static final Gradient ALT_HEATMAP_GRADIENT = new Gradient(ALT_HEATMAP_GRADIENT_COLORS,
            ALT_HEATMAP_GRADIENT_START_POINTS);

    private HeatmapTileProvider mProvider;
    private TileOverlay mOverlay;

    private boolean mDefaultGradient = true;
    private boolean mDefaultRadius = true;
    private boolean mDefaultOpacity = true;

    private Marker myMarker;
    Integer clickCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    public void changeRadius(View view) {
        if (mDefaultRadius) {
            mProvider.setRadius(ALT_HEATMAP_RADIUS);
        } else {
            mProvider.setRadius(HeatmapTileProvider.DEFAULT_RADIUS);
        }
        mOverlay.clearTileCache();
        mDefaultRadius = !mDefaultRadius;
    }

    public void changeGradient(View view) {
        if (mDefaultGradient) {
            mProvider.setGradient(ALT_HEATMAP_GRADIENT);
        } else {
            mProvider.setGradient(HeatmapTileProvider.DEFAULT_GRADIENT);
        }
        mOverlay.clearTileCache();
        mDefaultGradient = !mDefaultGradient;
    }

    public void changeOpacity(View view) {
        if (mDefaultOpacity) {
            mProvider.setOpacity(ALT_HEATMAP_OPACITY);
        } else {
            mProvider.setOpacity(HeatmapTileProvider.DEFAULT_OPACITY);
        }
        mOverlay.clearTileCache();
        mDefaultOpacity = !mDefaultOpacity;
    }

    private class LocationObject{
        ArrayList<LatLng> list;
        ArrayList<String> eventName;
        ArrayList<String> eventDescription;
        ArrayList<Integer> userID;

        LocationObject(){
            list = new ArrayList<LatLng>();
            eventName = new ArrayList<String>();
            eventDescription = new ArrayList<String>();
            userID = new ArrayList<Integer>();
        }
    }

    private LocationObject readMySQLResult(String mysql_result) throws JSONException{
        LocationObject locations = new LocationObject();
        JSONArray array = new JSONArray(mysql_result);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            double lat = object.getDouble("lat");
            double lng = object.getDouble("long");
            //System.out.println("Event Name: " + object.getString("eventName") + "Description: " + object.getString("eventDescription") +
            //        "user ID: " + object.getInt("user_id"));
            locations.list.add(new LatLng(lat, lng));
            locations.eventName.add(object.getString("eventName"));
            locations.eventDescription.add(object.getString("eventDescription"));
            locations.userID.add(object.getInt("user_id"));
        }
        return locations;
    }
    //Gets the JSON file from MySQL JSON
    private ArrayList<LatLng> readItems(String mysql_result) throws JSONException {
        ArrayList<LatLng> list = new ArrayList<LatLng>();
        JSONArray array = new JSONArray(mysql_result);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            double lat = object.getDouble("lat");
            double lng = object.getDouble("long");
            System.out.println("Event Name: " + object.getString("eventName") + "Description: " + object.getString("eventDescription") +
                    "user ID: " + object.getString("user_id"));
            list.add(new LatLng(lat, lng));
        }
        return list;
    }

    //Gets the local JSON file from Resources
    private ArrayList<LatLng> readItems(int resource) throws JSONException {

        ArrayList<LatLng> list = new ArrayList<LatLng>();
        InputStream inputStream = getResources().openRawResource(resource);
        String json = new Scanner(inputStream).useDelimiter("\\A").next();
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            double lat = object.getDouble("lat");
            double lng = object.getDouble("lng");
            list.add(new LatLng(lat, lng));
        }
        return list;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        try {
            makeMap();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(-37.1886, 145.708)));

        /*
        LatLng police = new LatLng(-37.1886, 145.708);
        myMarker = mMap.addMarker(new MarkerOptions().position(police).title("Police Stuff"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(police));

        // Set a listener for marker click.
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker marker) {

                // Check if a click count was set, then display the click count.
                if (clickCount != null) {
                    clickCount = clickCount + 1;

                    System.out.println(myMarker.getTitle() + " --> " + clickCount.toString());
                    myMarker.setTitle(" -->>> " + clickCount.toString());
                }

                // Return false to indicate that we have not consumed the event and that we wish
                // for the default behavior to occur (which is for the camera to move such that the
                // marker is centered and for the marker's info window to open, if it has one).
                return false;
            }
        });
        */
    }

    /** Called when the user clicks a marker. */
    @Override
    public boolean onMarkerClick(final Marker marker) {

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    private void makeMap() throws JSONException{

        GetAllLocations allLocations = new GetAllLocations();
        allLocations.execute();

        //mProvider = new HeatmapTileProvider.Builder().data(readItems(R.raw.police)).build();
        //mOverlay = mMap.addTileOverlay (new TileOverlayOptions().tileProvider(mProvider));

    }
}
