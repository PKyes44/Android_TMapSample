package kr.co.sl.tmapsample;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.skt.tmap.TMapAutoCompleteV2;
import com.skt.tmap.TMapData;
import com.skt.tmap.TMapGpsManager;
import com.skt.tmap.TMapInfo;
import com.skt.tmap.TMapLabelInfo;
import com.skt.tmap.TMapPoint;
import com.skt.tmap.TMapTapi;
import com.skt.tmap.TMapView;
import com.skt.tmap.address.TMapAddressInfo;
import com.skt.tmap.overlay.TMapCircle;
import com.skt.tmap.overlay.TMapMarkerItem;
import com.skt.tmap.overlay.TMapMarkerItem2;
import com.skt.tmap.overlay.TMapOverlay;
import com.skt.tmap.overlay.TMapPolyLine;
import com.skt.tmap.overlay.TMapPolygon;
import com.skt.tmap.poi.TMapPOIItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.HashMap;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import kr.co.sl.tmapsample.geofence.GeofenceData;
import kr.co.sl.tmapsample.geofence.Geofencer;
import kr.co.sl.tmapsample.postcode.PostCode;

public class MainActivity extends AppCompatActivity {

    private TMapPoint target = randomTMapPoint2();

    private static final String API_KEY = "bfOaavmTi921ORJ1CnJPg381ztwI4bTn2ntVTQ89"; // SKT

    private TMapPoint initPoint;

    private Button menuButton;
    private DrawerLayout drawerLayout;
    private ImageView zoomInImage;
    private ImageView zoomOutImage;
    private TextView zoomLevelTextView;
    private ImageView locationImage;

    private ImageView centerImage;
    private TextView centerPointTextView;

    private ExpandableListView menuListView;

    private LinearLayout autoCompleteLayout;
    private EditText autoCompleteEdit;
    private ListView autoCompleteListView;
    private AutoCompleteListAdapter autoCompleteListAdapter;

    private LinearLayout autoComplete2Layout;
    private EditText autoComplete2Edit;
    private ListView autoComplete2ListView;
    private AutoComplete2ListAdapter autoComplete2ListAdapter;


    private LinearLayout routeLayout;
    private TextView routeDistanceTextView;
    private TextView routeTimeTextView;
    private TextView routeFareTextView;

    private boolean isReverseGeocoding;

    private TMapView tMapView;
    private int geofencingType;

    private int zoomIndex = -1;

    private boolean isVisibleCenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initTmap();

    }

    private void initTmap() {
        tMapView = new TMapView(this);
        tMapView.setSKTMapApiKey(API_KEY);

        tMapView.setOnMapReadyListener(onMapReadyListener);


        FrameLayout tmapLayout = findViewById(R.id.tmapLayout);
        tmapLayout.addView(tMapView);

        isReverseGeocoding = false;
        geofencingType = 0;


        tMapView.setOnEnableScrollWithZoomLevelListener(new TMapView.OnEnableScrollWithZoomLevelCallback() {
            @Override
            public void onEnableScrollWidthZoomLevel(float v, TMapPoint tMapPoint) {
                int zoom = (int) v;
                zoomLevelTextView.setText("Lv." + zoom);
                zoomIndex = zoom - 6;
                if (isVisibleCenter) {
                    setCenterPoint();
                }
            }
        });

        tMapView.setOnDisableScrollWithZoomLevelListener(new TMapView.OnDisableScrollWithZoomLevelCallback() {
            @Override
            public void onDisableScrollWidthZoomLevel(float v, TMapPoint tMapPoint) {
                reverseGeoCoding(isReverseGeocoding);
            }
        });

        tMapView.setOnClickListenerCallback(new TMapView.OnClickListenerCallback() {
            @Override
            public void onPressDown(ArrayList<TMapMarkerItem> arrayList, ArrayList<TMapPOIItem> arrayList1, TMapPoint tMapPoint, PointF pointF) {

                if (tMapView.isTrackingMode()) {
                    setTrackingMode(false);
                    locationImage.setSelected(false);
                }
            }

            @Override
            public void onPressUp(ArrayList<TMapMarkerItem> arrayList, ArrayList<TMapPOIItem> arrayList1, TMapPoint tMapPoint, PointF pointF) {


            }
        });

    }


    private void reverseGeoCoding(boolean isReverseGeocoding) {

        this.isReverseGeocoding = isReverseGeocoding;

        if (this.isReverseGeocoding) {
            final TMapPoint centerPoint = tMapView.getCenterPoint();
            if (tMapView.isValidTMapPoint(centerPoint)) {
                TMapData tMapData = new TMapData();
                tMapData.reverseGeocoding(centerPoint.getLatitude(), centerPoint.getLongitude(), "A10", new TMapData.OnReverseGeocodingListener() {
                    @Override
                    public void onReverseGeocoding(TMapAddressInfo info) {
                        if (info != null) {
                            //법정동
                            String oldAddress = "법정동 : ";
                            if (info.strLegalDong != null && !info.strLegalDong.equals("")) {
                                oldAddress += info.strCity_do + " " + info.strGu_gun + " " + info.strLegalDong;
                                if (info.strRi != null && !info.strRi.equals("")) {
                                    oldAddress += (" " + info.strRi);
                                }
                                oldAddress += (" " + info.strBunji);
                            } else {
                                oldAddress += "-";
                            }

                            //새주소
                            String newAddress = "도로명 : ";
                            if (info.strRoadName != null && !info.strRoadName.equals("")) {
                                newAddress += info.strCity_do + " " + info.strGu_gun + " " + info.strRoadName + " " + info.strBuildingIndex;
                            } else {
                                newAddress += "-";
                            }

                            setReverseGeocoding(oldAddress, newAddress, centerPoint);

                        }
                    }
                });
            }
        } else {
            tMapView.removeAllTMapMarkerItem2();
        }

    }

    private void setReverseGeocoding(String oldAddress, String newAddress, TMapPoint point) {
        tMapView.removeAllTMapMarkerItem2();

        ReverseLabelView view = new ReverseLabelView(this);
        view.setText(oldAddress, newAddress);

        TMapMarkerItem2 marker = new TMapMarkerItem2("marker2");
        marker.setIconView(view);
        marker.setTMapPoint(point);

        tMapView.addTMapMarkerItem2View(marker);
    }

    private TMapView.OnMapReadyListener onMapReadyListener = new TMapView.OnMapReadyListener() {
        @Override
        public void onMapReady() {

            initPoint = tMapView.getCenterPoint();

            initAll();

            int zoom = tMapView.getZoomLevel();
            zoomIndex = zoom - 6;
            zoomLevelTextView.setText("Lv." + zoom);

          


        }
    };

    private void initView() {
        drawerLayout = findViewById(R.id.drawerLayout);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(onClickListener);
        zoomInImage = findViewById(R.id.zoomInImage);
        zoomInImage.setOnClickListener(onClickListener);
        zoomOutImage = findViewById(R.id.zoomOutImage);
        zoomOutImage.setOnClickListener(onClickListener);

        zoomLevelTextView = findViewById(R.id.zoomLevelText);

        locationImage = findViewById(R.id.locationImage);
        locationImage.setOnClickListener(onClickListener);

        centerImage = findViewById(R.id.centerIconImage);
        centerImage.setVisibility(View.GONE);
        centerPointTextView = findViewById(R.id.centerText);
        centerPointTextView.setVisibility(View.GONE);

        initAutoComplete();
        initAuto2Complete2();

        initRoute();

        menuListView = findViewById(R.id.menuListView);

        ArrayList<MenuItem> menuList = new ArrayList<>();

        MenuItem item0 = new MenuItem("초기화", new ArrayList<>());
        menuList.add(item0);

        ArrayList<String> child2 = new ArrayList<>();
        child2.add("POI자동완성V2");
        MenuItem item2 = new MenuItem("POI", child2);
        menuList.add(item2);

        ArrayList<String> child5 = new ArrayList<>();
        child5.add("자동차경로");
        child5.add("보행자 경로");
        child5.add("경로정보 전체삭제");
        MenuItem item5 = new MenuItem("경로안내", child5);
        menuList.add(item5);

        MenuAdapter adapter = new MenuAdapter(this, menuList);
        menuListView.setAdapter(adapter);

        menuListView.setOnGroupClickListener((expandableListView, view, position, id) -> {

            if (position == 0 || position == 6 || position == 8 || position == 9) {
                selectMenu(position, -1);
                drawerLayout.closeDrawer(Gravity.LEFT);
                menuListView.collapseGroup(position);
            }

            return false;
        });


        menuListView.setOnChildClickListener((expandableListView, view, groupPosition, childPosition, id) -> {
            selectMenu(groupPosition, childPosition);
            drawerLayout.closeDrawer(Gravity.LEFT);
            return false;
        });

    }

    private void initRoute() {
        routeLayout = findViewById(R.id.routeLayout);
        routeDistanceTextView = findViewById(R.id.routeDistanceText);
        routeTimeTextView = findViewById(R.id.routeTimeText);
        routeFareTextView = findViewById(R.id.routeFareText);
    }

    private void initAuto2Complete2() {
        autoComplete2Layout = findViewById(R.id.autoComplete2Layout);
        autoComplete2Layout.setVisibility(View.GONE);
        autoComplete2Edit = findViewById(R.id.autoComplete2Edit);
        autoComplete2ListView = findViewById(R.id.autoComplete2ListView);
        autoComplete2ListAdapter = new AutoComplete2ListAdapter(this);
        autoComplete2ListView.setAdapter(autoComplete2ListAdapter);
        autoComplete2ListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                tMapView.removeAllTMapMarkerItem();

                autoComplete2Layout.setVisibility(View.GONE);

                TMapAutoCompleteV2 item = (TMapAutoCompleteV2) autoComplete2ListAdapter.getItem(position);

                target.setLongitude(Double.parseDouble(item.lon));
                target.setLatitude(Double.parseDouble(item.lat));

                TMapMarkerItem marker = new TMapMarkerItem();
                marker.setId(item.poiId);
                marker.setIcon(BitmapFactory.decodeResource(getResources(), R.drawable.poi_dot));
                marker.setTMapPoint(Double.parseDouble(item.lat), Double.parseDouble(item.lon));
                marker.setCalloutTitle(item.keyword);
                marker.setCalloutSubTitle(item.poiId);
                marker.setCanShowCallout(true);
                marker.setAnimation(true);

                tMapView.addTMapMarkerItem(marker);

                tMapView.setCenterPoint(Double.parseDouble(item.lat), Double.parseDouble(item.lon));

            }
        });


        autoComplete2Edit.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TMapData tMapData = new TMapData();

                String keyword = s.toString();
                double lat = tMapView.getCenterPoint().getLatitude();
                double lon = tMapView.getCenterPoint().getLongitude();

                tMapData.autoCompleteV2(keyword, lat, lon, 0, 100, new TMapData.OnAutoCompleteV2Listener() {
                    @Override
                    public void onAutoCompleteV2(ArrayList<TMapAutoCompleteV2> arrayList) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                autoComplete2ListAdapter.setItemList(arrayList);
                            }
                        });
                    }
                });
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void initAutoComplete() {
        autoCompleteLayout = findViewById(R.id.autoCompleteLayout);
        autoCompleteLayout.setVisibility(View.GONE);
        autoCompleteEdit = findViewById(R.id.autoCompleteEdit);
        autoCompleteListView = findViewById(R.id.autoCompleteListView);
        autoCompleteListAdapter = new AutoCompleteListAdapter(this);
        autoCompleteListView.setAdapter(autoCompleteListAdapter);
        autoCompleteListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String keyword = (String) autoCompleteListAdapter.getItem(position);

                findAllPoi(keyword);
                autoCompleteLayout.setVisibility(View.GONE);
            }
        });

        autoCompleteEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String keyword = s.toString();

                TMapData tMapData = new TMapData();

                tMapData.autoComplete(keyword, new TMapData.OnAutoCompleteListener() {
                    @Override
                    public void onAutoComplete(ArrayList<String> itemList) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                autoCompleteListAdapter.setItemList(itemList);
                            }
                        });

                    }
                });

            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.equals(menuButton)) {
                drawerLayout.openDrawer(Gravity.LEFT);
            } else if (v.equals(zoomInImage)) {
                tMapView.mapZoomIn();
            } else if (v.equals(zoomOutImage)) {
                tMapView.mapZoomOut();
            } else if (v.equals(locationImage)) {
                locationImage.setSelected(!locationImage.isSelected());
                setTrackingMode(locationImage.isSelected());
            }
        }
    };


    private void selectMenu(int groupPosition, int childPosition) {
        if (groupPosition == 0) { // 초기화
            initAll();
        }
        else if (groupPosition == 1) { // POI
            if (childPosition == 0) { // poi 자동완성 v2
                if (autoComplete2Layout.getVisibility() == View.GONE) {
                    autoComplete2Layout.setVisibility(View.VISIBLE);
                    autoComplete2ListAdapter.clear();
                    autoComplete2Edit.setText("");
                } else {
                    autoComplete2Layout.setVisibility(View.GONE);
                }
            }
        }
        else if (groupPosition == 2) { // 경로안내
            if (childPosition == 0) { // 자동차 경로
                findPathAllType(TMapData.TMapPathType.CAR_PATH);
            } else if (childPosition == 1) { // 보행자 경로
                findPathAllType(TMapData.TMapPathType.PEDESTRIAN_PATH);
            } else if (childPosition == 2) { // 경로 지우기
                tMapView.removeTMapPath();
                routeLayout.setVisibility(View.GONE);
            }
        }
    }
    private void initAll() {
        tMapView.removeAllTMapMarkerItem2();
        tMapView.removeAllTMapMarkerItem();
        tMapView.removeAllTMapPolyLine();
        tMapView.removeAllTMapPolygon();
        tMapView.removeAllTMapCircle();
        tMapView.removeAllTMapPOIItem();
        tMapView.removeAllTMapOverlay();
        tMapView.removeTMapPath();

        tMapView.setPOIScale(TMapView.POIScale.NORMAL);

        routeLayout.setVisibility(View.GONE);

        tMapView.setCompassMode(false);
        tMapView.setSightVisible(false);
        tMapView.setZoomLevel(16);
        setTrackingMode(false);

        tMapView.setCenterPoint(initPoint.getLatitude(), initPoint.getLongitude());

        reverseGeoCoding(false);

        isVisibleCenter = false;
        centerPointTextView.setVisibility(View.GONE);
        centerImage.setVisibility(View.GONE);
        setCenterPoint();
    }

    private void setCenterPoint() {
        TMapPoint point = tMapView.getCenterPoint();
        String text = point.getLatitude() + "\n" + point.getLongitude();
        centerPointTextView.setText(text);

    }


    private void findPathAllType(final TMapData.TMapPathType type) {
        TMapPoint startPoint = tMapView.getCenterPoint();

        TMapPoint endPoint;
        if (type == TMapData.TMapPathType.CAR_PATH) {
            endPoint = target;
        } else {
            endPoint = randomTMapPoint();
        }

        /*
        TMapPoint startPoint = new TMapPoint(37.570841, 126.985302);
        TMapPoint endPoint = new TMapPoint(37.551135, 126.988205);

        new TMapData().findPathData(startPoint, endPoint, new TMapData.OnFindPathDataListener() {
            @Override
            public void onFindPathData(TMapPolyLine tMapPolyLine) {
                tMapPolyLine.setLineWidth(3);
                tMapPolyLine.setLineColor(Color.BLUE);
                tMapPolyLine.setLineAlpha(255);

                tMapPolyLine.setOutLineWidth(5);
                tMapPolyLine.setOutLineColor(Color.RED);
                tMapPolyLine.setOutLineAlpha(255);

                tMapView.addTMapPolyLine(tMapPolyLine);
                TMapInfo info = tMapView.getDisplayTMapInfo(tMapPolyLine.getLinePointList());
                tMapView.setZoomLevel(info.getZoom());
                tMapView.setCenterPoint(info.getPoint().getLatitude(), info.getPoint().getLongitude());
            }
        });
        */


        new TMapData().findPathDataAllType(type, startPoint, endPoint, new TMapData.OnFindPathDataAllTypeListener() {
            @Override
            public void onFindPathDataAllType(Document doc) {
                tMapView.removeTMapPath();

                TMapPolyLine polyline = new TMapPolyLine();
                polyline.setID(type.name());
                polyline.setLineWidth(10);
                polyline.setLineColor(Color.RED);
                polyline.setLineAlpha(255);


                if (doc != null) {
                    NodeList list = doc.getElementsByTagName("Document");
                    Element item2 = (Element) list.item(0);
                    String totalDistance = getContentFromNode(item2, "tmap:totalDistance");
                    String totalTime = getContentFromNode(item2, "tmap:totalTime");
                    String totalFare;
                    if (type == TMapData.TMapPathType.CAR_PATH) {
                        totalFare = getContentFromNode(item2, "tmap:totalFare");
                    } else {
                        totalFare = "";
                    }

                    NodeList list2 = doc.getElementsByTagName("LineString");

                    for (int i = 0; i < list2.getLength(); i++) {
                        Element item = (Element) list2.item(i);
                        String str = getContentFromNode(item, "coordinates");
                        if (str == null) {
                            continue;
                        }

                        String[] str2 = str.split(" ");
                        for (int k = 0; k < str2.length; k++) {
                            try {
                                String[] str3 = str2[k].split(",");
                                TMapPoint point = new TMapPoint(Double.parseDouble(str3[1]), Double.parseDouble(str3[0]));
                                polyline.addLinePoint(point);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    tMapView.setTMapPath(polyline);

                    TMapInfo info = tMapView.getDisplayTMapInfo(polyline.getLinePointList());
                    int zoom = info.getZoom();
                    if (zoom > 12) {
                        zoom = 12;
                    }

                    tMapView.setZoomLevel(zoom);
                    tMapView.setCenterPoint(info.getPoint().getLatitude(), info.getPoint().getLongitude());


                    setPathText(totalDistance, totalTime, totalFare);
                }
            }
        });

    }

    private void setPathText(String distance, String time, String fare) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                routeLayout.setVisibility(View.VISIBLE);
                double km = Double.parseDouble(distance) / 1000;
                routeDistanceTextView.setText("총 거리 : " + km + " km");


                int totalSec = Integer.parseInt(time);
                int day = totalSec / (60 * 60 * 24);
                int hour = (totalSec - day * 60 * 60 * 24) / (60 * 60);
                int minute = (totalSec - day * 60 * 60 * 24 - hour * 3600) / 60;
                String t;
                if (hour > 0) {
                    t = hour + "시간 " + minute + "분";
                } else {
                    t = minute + "분 ";
                }
                routeTimeTextView.setText("예상시간 : 약 " + t);

                if (fare != null && !fare.equals("0") && !fare.equals("")) {
                    routeFareTextView.setVisibility(View.VISIBLE);
                    routeFareTextView.setText("유료도로 요금 : " + fare + " 원");
                } else {
                    routeFareTextView.setVisibility(View.GONE);
                }

            }
        });
    }

    private String getContentFromNode(Element item, String tagName) {
        NodeList list = item.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            if (list.item(0).getFirstChild() != null) {
                return list.item(0).getFirstChild().getNodeValue();
            }
        }
        return null;
    }

    private void setTrackingMode(boolean isTracking) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean isGranted = true;
            String[] permissionArr = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
            ArrayList<String> checkPer = new ArrayList<>();
            for (String per : permissionArr) {
                if (checkSelfPermission(per) == PackageManager.PERMISSION_GRANTED) {

                } else {
                    checkPer.add(per);
                    isGranted = false;
                }
            }

            if (isGranted) {
                setTracking(isTracking);
            } else {
                requestPermissions(checkPer.toArray(new String[0]), 100);
            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tMapView != null) {
            tMapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (tMapView != null) {
            tMapView.onPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tMapView != null) {
            tMapView.onDestroy();
        }
    }

    private void setTracking(boolean isTracking) {

        TMapGpsManager manager = new TMapGpsManager(this);
        if (isTracking) {
            manager.setOnLocationChangeListener(locationListener);

            manager.setProvider(TMapGpsManager.PROVIDER_GPS);
            manager.openGps();

            manager.setProvider(TMapGpsManager.PROVIDER_NETWORK);
            manager.openGps();

            tMapView.setTrackingMode(true);
            //tMapView.setSightVisible(true);
        } else {
            tMapView.setTrackingMode(false);
            manager.setOnLocationChangeListener(null);
        }
    }

    private TMapGpsManager.OnLocationChangedListener locationListener = new TMapGpsManager.OnLocationChangedListener() {
        @Override
        public void onLocationChange(Location location) {
            if (location != null) {
                tMapView.setLocationPoint(location.getLatitude(), location.getLongitude());
            }
        }
    };

    public void findAllPoi(String strData) {
        TMapData tmapdata = new TMapData();
        tmapdata.findAllPOI(strData, new TMapData.OnFindAllPOIListener() {
            @Override
            public void onFindAllPOI(ArrayList<TMapPOIItem> poiItemList) {
                showPOIResultDialog(poiItemList);
            }
        });
    }

    private void showPOIResultDialog(final ArrayList<TMapPOIItem> poiItem) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (poiItem != null) {
                    CharSequence[] item = new CharSequence[poiItem.size()];
                    for (int i = 0; i < poiItem.size(); i++) {
                        item[i] = poiItem.get(i).name;
                    }
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("POI 검색 결과")
                            .setIcon(R.drawable.tmark)
                            .setItems(item, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int i) {
                                    dialog.dismiss();
                                    initAll();
                                    TMapPOIItem poi = poiItem.get(i);
                                    TMapMarkerItem marker = new TMapMarkerItem();
                                    marker.setId(poi.id);
                                    marker.setTMapPoint(poi.getPOIPoint());
                                    Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.poi_dot);
                                    marker.setIcon(icon);

                                    marker.setCalloutTitle(poi.getPOIName());
                                    marker.setCalloutSubTitle("id:" + poi.getPOIID());
                                    marker.setCanShowCallout(true);

                                    marker.setAnimation(true);

                                    tMapView.addTMapMarkerItem(marker);
                                    tMapView.setCenterPoint(poi.getPOIPoint().getLatitude(), poi.getPOIPoint().getLongitude());
                                }
                            }).create().show();
                }

            }
        });

    }

    public TMapPoint randomTMapPoint() {
        double latitude = ((double) Math.random()) * (37.575113 - 37.483086) + 37.483086;
        double longitude = ((double) Math.random()) * (127.027359 - 126.878357) + 126.878357;

        latitude = Math.min(37.575113, latitude);
        latitude = Math.max(37.483086, latitude);

        longitude = Math.min(127.027359, longitude);
        longitude = Math.max(126.878357, longitude);

        return new TMapPoint(latitude, longitude);
    }

    public TMapPoint randomTMapPoint2() {
        double latitude = ((double) Math.random()) * (37.770555 - 37.404194) + 37.483086;
        double longitude = ((double) Math.random()) * (127.426043 - 126.770296) + 126.878357;

        latitude = Math.min(37.770555, latitude);
        latitude = Math.max(37.404194, latitude);

        longitude = Math.min(127.426043, longitude);
        longitude = Math.max(126.770296, longitude);

        // LogManager.printLog("randomTMapPoint" + latitude + " " + longitude);

        return new TMapPoint(latitude, longitude);
    }
}