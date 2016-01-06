package com.naturpark;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.naturpark.data.Obstacle;
import com.naturpark.data.Poi;
import com.naturpark.data.PoiType;
import com.naturpark.data.Route;

import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.PathOverlay;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MapListener, View.OnLayoutChangeListener {

    public PathOverlay parseGpxFile(Route route) {

        PathOverlay pathOverlay = new PathOverlay(Color.BLUE, this);

        ArrayList<GeoPoint> track = route.getTrack(this);
        for (GeoPoint point : track) {
            pathOverlay.addPoint(point);
        }

        return pathOverlay;
    }

    @Override
    public boolean onScroll(ScrollEvent scrollEvent) {
        System.out.println("SCROOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOL");
        return false;
    }

    @Override
    public boolean onZoom(ZoomEvent zoomEvent) {
        System.out.println("ZOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOM");
        return false;
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        System.out.println("LLLLLLLLLLLLLLLLLLLLLLLLLLLL");
        System.out.println("LLLLLLLLLLLLLLLLLLLLLLLLLLLL");
        System.out.println("W:" + map.getWidth() +"H:"+ map.getHeight());

        if (_route_id != 0 && map.getWidth() != 0) {
            map.zoomToBoundingBox(_get_route(_route_id).boundingBox(this));
            _route_id = 0;
        }
    }

    // GPSTracker class
    GPSTracker gps;

    private boolean _creating;
    private SharedPreferences _preferences;

    private Toolbar toolbar;
    private MapView map;
    private NavigationView navigationView;
    private DrawerLayout drawerLayout;

    private LocationManager locationManager;

    private List<Route> _list_route;
    private List<PoiType> _list_poi_type;
    private List<Poi> _list_poi;
    private List<Obstacle> _list_obstacle;

    int _route_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _creating = true;
        _preferences = getSharedPreferences("naturpark.prf", MODE_PRIVATE);

        GPSManager gps = new GPSManager(
                MainActivity.this);
        gps.start();

        setContentView(R.layout.activity_main);

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPQUESTOSM);
        map.getController().setZoom(_preferences.getInt("ZoomLevel", 10));
        map.getController().setCenter(new GeoPoint(_preferences.getFloat("Latitude", (float) 51.080414), _preferences.getFloat("Longitude", (float) 10.434239)));
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        map.setUseDataConnection(true);
        map.addOnLayoutChangeListener(this);

        // Initializing Toolbar and setting it as the actionbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initializing Drawer Layout and ActionBarToggle
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer);

        //Initializing NavigationView
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        navigationView.setNavigationItemSelectedListener(new NavigationViewListener(this, drawerLayout));

        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.openDrawer, R.string.closeDrawer) {

            @Override
            public void onDrawerClosed(View drawerView) {
                // Code here will be triggered once the drawer closes as we dont want anything to happen so we leave this blank
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                // Code here will be triggered once the drawer open as we dont want anything to happen so we leave this blank

                super.onDrawerOpened(drawerView);
            }
        };

        //Setting the actionbarToggle to drawer layout
        drawerLayout.setDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
    }

    @Override
    protected  void onStart() {
        super.onStart();

        System.out.println("####################################################################################### onStart");

        _route_id = getIntent().getIntExtra("Route", 0);
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!:route:" + _route_id);


        DbManager dbManager = new DbManager(this, _creating);
        _creating = false;

        _list_route = dbManager.queryRouteList();
        _list_poi_type = dbManager.queryPoiTypeList();
        _list_poi = dbManager.queryPoiList();
        _list_obstacle = dbManager.queryObstacleList();
        System.out.println("num route:" + _list_route.size());
        System.out.println("num_poi_type:" + _list_poi_type.size());
        System.out.println("num_poi:" + _list_poi.size());
        System.out.println("num_obstacle:" + _list_obstacle.size());

        map.getOverlays().clear();
        _addPoiToMap(map);
        _addObstaclesToMap(map);
        _addRoutesToMap(map);
    }

    @Override
    protected  void onResume() {
        super.onResume();

        System.out.println("####################################################################################### onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        System.out.println("####################################################################################### onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("####################################################################################### onStop");

        SharedPreferences.Editor editor = _preferences.edit();
        editor.putFloat("Latitude", (float)map.getMapCenter().getLatitude());
        editor.putFloat("Longitude", (float)map.getMapCenter().getLongitude());
        editor.putInt("ZoomLevel", map.getZoomLevel());
        editor.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("####################################################################################### onDestroy");
    }

    private PoiType _getPoiType(int id) {
        for (PoiType poiType : _list_poi_type) {

            if (poiType.id() == id)
                return poiType;
        }

        return null;
    }

    private void _addPoiToMap(MapView map) {
        ArrayList overlayItemArray = new ArrayList<OverlayItem>();

        for (Poi poi : _list_poi) {
            OverlayItem item = new OverlayItem(poi.name(), poi.address(),
                    new GeoPoint(poi.location().getLatitude(), poi.location().getLongitude()));
            PoiType poiType = _getPoiType(poi.type());
            if (poiType != null)
                item.setMarker(getResources().getDrawable(getResources().getIdentifier(poiType.iconName(), "drawable", getPackageName())));

            if (poiType.is_visible())
                overlayItemArray.add(item);
        }

        ItemizedIconOverlay.OnItemGestureListener gestureListener = new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
            @Override
            public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                //TextView item_info =  (TextView)findViewById(R.id.item_info_window);
                //item_info.setText(item.getTitle());
                Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            public boolean onItemLongPress(final int index, final OverlayItem item) {
                Toast.makeText(getApplicationContext(), item.getSnippet(), Toast.LENGTH_LONG).show();
                return true;
            }
        };

        map.getOverlays().add(new ItemizedIconOverlay<OverlayItem>(overlayItemArray, gestureListener, new CustomResourceProxy(this)));
    }

    private void _addObstaclesToMap(MapView map) {
        ArrayList overlayItemArray = new ArrayList<OverlayItem>();

        for (Obstacle obstacle : _list_obstacle) {
            OverlayItem item = new OverlayItem(obstacle.name(), obstacle.name(),
                    new GeoPoint(obstacle.location().getLatitude(), obstacle.location().getLongitude()));
            switch (obstacle.type()) {
                case 1:
                    item.setMarker(getResources().getDrawable(R.drawable.marker_yellow));
                    break;
                case 2:
                    item.setMarker(getResources().getDrawable(R.drawable.marker_blue));
                    break;
                case 3:
                    item.setMarker(getResources().getDrawable(R.drawable.marker_red));
                    break;
                default:
                    item.setMarker(getResources().getDrawable(R.drawable.marker_default));
                    break;
            }

            overlayItemArray.add(item);
        }

        ItemizedIconOverlay.OnItemGestureListener gestureListener = new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
            @Override
            public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                Toast.makeText(getApplicationContext(), item.getTitle(), Toast.LENGTH_SHORT).show();
                return true;
            }

            @Override
            public boolean onItemLongPress(final int index, final OverlayItem item) {
                Toast.makeText(getApplicationContext(), item.getSnippet(), Toast.LENGTH_LONG).show();
                return true;
            }
        };

        map.getOverlays().add(new ItemizedIconOverlay<OverlayItem>(overlayItemArray, gestureListener, new CustomResourceProxy(this)));
    }

    private void _addRoutesToMap(MapView map) {
        for (Route route : _list_route) {
            PathOverlay path = parseGpxFile(route);
            switch (route.rating()) {
                case 1:
                    path.setColor(Color.RED);
                    break;

                case 2:
                    path.setColor(Color.YELLOW);
                    break;

                case 3:
                    path.setColor(Color.GREEN);
                    break;

                default:
                    path.setColor(Color.RED);
            }

            if (route.id() == _route_id)
                path.getPaint().setStrokeWidth(4);

            map.getOverlays().add(path);
        }
    }

    private Route _get_route(int id) {
        for (Route route : _list_route) {
            if (route.id() == id)
                return route;
        }

        return null;
    }

    public void onClickgetLocation (View view){

        GPSTracker gpst = new GPSTracker(MainActivity.this);
        // check if GPS enabled
        if (gpst.canGetLocation()) {

            double latitude = gpst.getLatitude();
            double longitude = gpst.getLongitude();

            map.getController().setCenter(new GeoPoint(latitude, longitude));
            map.getController().setZoom(15);
            GeoPoint defaultPoint = new GeoPoint(latitude, longitude);
            map.getController().animateTo(defaultPoint);
            Marker startMarker = new Marker(map);
            startMarker.setPosition(new GeoPoint(latitude, longitude));
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            startMarker.setIcon(getResources().getDrawable(R.drawable.location));
            startMarker.setTitle("Lat" + gpst.getLatitude() + "Lon" + gpst.getLongitude());
            map.getOverlays().add(startMarker);


        } else {
            gpst.stopUsingGPS();
        }
    }


}