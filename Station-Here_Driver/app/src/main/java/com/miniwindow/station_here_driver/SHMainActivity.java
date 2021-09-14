package com.miniwindow.station_here_driver;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Menu;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SHMainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    Map<String, Marker> stationList = new HashMap<String, Marker>();

    public InterDB IDB;

    boolean BusRouteRefreshed = false;

    ArrayList<Integer> busRouteColorList = new ArrayList<>();

    ArrayList<PolylineOptions> busRoutePolylineOptionList = new ArrayList<>();

    MenuItem searchItem;

    LocationManager locationManager;

    TextView PeopleStation;
    TextView DistanceStation;

    Marker busMarker;

    private ServiceConnection conConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("test", "main service connected"); //바인딩 성공
        }
        public void onServiceDisconnected(ComponentName className) {
            Log.d("test", "Service Disconnected on Main");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shmain);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        PeopleStation = (TextView) findViewById(R.id.stationPeopleNum);
        DistanceStation = (TextView) findViewById(R.id.stationDistance);

        bindService(new Intent(this, ConnectionService.class), conConnection, BIND_AUTO_CREATE); //여기서 바인딩
        ConnectionS.serviceConnection.registerCallback(conCallback);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        IDB = new InterDB(SHMainActivity.this, "StationDB.db", null, 1);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE}, 15);
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //get the latitude
                /*Log.d("test", "Location Changed");
                ConnectionS.busLat = location.getLatitude();
                ConnectionS.busLng = location.getLongitude();

                ConnectionS.serviceConnection.busLatLngUpdate(ConnectionS.busLat, ConnectionS.busLng);*/
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu;

        getMenuInflater().inflate(R.menu.shmain_menu, menu);

        return true;
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent event)
    {
        switch(keycode)
        {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                ConnectionS.serviceConnection.busLateAlarm();
                break;

            case KeyEvent.KEYCODE_VOLUME_UP:
                ConnectionS.busGPS = 0; //for testing
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.buslate) {
        }

        return super.onOptionsItemSelected(item);
    }

    private ConnectionService.ICallback conCallback = new ConnectionService.ICallback() {

        @Override
        public void stationDestroy(String sid) {
            Message msg = new Message();
            Bundle data = new Bundle();
            data.putString("station",sid);
            msg.setData(data);
            msg.what = 2;
            DataRefreshHandler.sendMessage(msg);
        }

        @Override
        public void stationDataUpdate(String sid, int people) {
            StationPoint tempPoint;
            tempPoint = ConnectionS.StationArray.get(ConnectionS.StationArray.indexOf(sid));
            tempPoint.people += people;
            MapRefreshHandler.sendEmptyMessage(1);
        }

        @Override
        public void stationCreated(String sid, double lat, double lng, boolean isF, int rN) {
            ConnectionS.StationArray.add(new StationPoint(lat, lng, sid , 1, isF, rN));
            MapRefreshHandler.sendEmptyMessage(1);
        }


        @Override
        public void clearMap()
        {
            MapRefreshHandler.sendEmptyMessage(4); //Clear Map
        }

        @Override
        public void busRouteLoad() {
            Log.d("test", "bus Route Loading");
            ConnectionS.BusList = IDB.getBusRouteNum();
            ConnectionS.BusRouteList = IDB.getBusRoute();
            ConnectionS.busListLoaded = true;
        }

        @Override
        public void busDataRefresh() {
            //refresh Bus DATA
            MapRefreshHandler.sendEmptyMessage(2);
        }

        @Override
        public void busRouteSave(String busInfoVer) {
            //버스 경로를 mysql에 저장
            int i, j;

            IDB.insertBusRouteNum(ConnectionS.BusList);


            for (i = 0; i < ConnectionS.BusList.size(); i++) {
                for (j = 0; j < ConnectionS.BusRouteList.get(ConnectionS.BusList.get(i)).size(); j++) {
                    IDB.insertBusRoute(Integer.parseInt(ConnectionS.BusList.get(i)), j + 1, Double.toString(ConnectionS.BusRouteList.get(ConnectionS.BusList.get(i)).get(j).lat), Double.toString(ConnectionS.BusRouteList.get(ConnectionS.BusList.get(i)).get(j).lng));
                }
            }
            ConnectionS.BusInfoVerCheckOk = true;

            SharedPreferences pref = getSharedPreferences("SHFD", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.remove("BusInfoVersion");
            editor.putString("BusInfoVersion", busInfoVer);
            editor.commit();
        }

        @Override
        public void stationDataRefresh() {

            int i;/*
            for (i = 0; i < ConnectionS.StationArray.size(); i++) {
                //Station Array 는 진짜 정류장 정보 가지고 있는 Array 이고, stationList 는 실제 마커 정보를 담은 해시맵
                stationList.put(ConnectionS.StationArray.get(i).stationId, new LatLng(ConnectionS.StationArray.get(i).lat, ConnectionS.StationArray.get(i).lng));
            }*/
            MapRefreshHandler.sendEmptyMessage(1);

        }

        @Override
        public void msg_connection_failed() {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                    getApplicationContext());

// 제목셋팅
            alertDialogBuilder.setTitle("프로그램 종료");

// AlertDialog 셋팅
            alertDialogBuilder
                    .setMessage("프로그램을 종료할 것입니까?");
        }

        @Override
        public void msg_show_monoButton(String msg) {
            Message mmsg = new Message();
            Bundle data = new Bundle();
            data.putString("text", msg);
            mmsg.what = 2; //메시지에 what 설정
            mmsg.setData(data);
            msg_handler.sendMessage(mmsg);

        }

        @Override
        public void saveBid(String Bid) {
            SharedPreferences pref = getSharedPreferences("SHFD", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("Bid", Bid);
            editor.commit();
        }

        @Override
        public void saveTempId(String tempId) {
            StaticsVar.TempId = tempId;
        }

        @Override
        public void resumeActivity() {
            //No use
        }

        @Override
        public void toast_show_short(String msg) {
            //Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        }
    };


    final Handler msg_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {


            if (msg.what == 2) {
                String str = msg.getData().getString("text");
                AlertDialog.Builder alert = new AlertDialog.Builder(SHMainActivity.this);
                alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();     //닫기
                    }
                });
                alert.setCancelable(false);
                alert.setMessage(str);
                alert.show();
            }
        }
    };

    final Handler DataRefreshHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //TODO : 버스 진행 방향 확인하기....
                        int near = getNearestStation();

                        if (near != -1) {
                            if (!ConnectionS.nearestStation.equals(ConnectionS.StationArray.get(near).stationId)) {
                                ConnectionS.nearestStation = ConnectionS.StationArray.get(near).stationId; //초기화
                                ConnectionS.alarmGone = false;
                            }
                        }

                        String people = Integer.toString(ConnectionS.StationArray.get(near).people) + " 명";
                        PeopleStation.setText(people);
                        float[] f = new float[1];

                        String dist;
                        Location.distanceBetween(ConnectionS.busLat,ConnectionS.busLng,ConnectionS.StationArray.get(near).lat,ConnectionS.StationArray.get(near).lng,f);

                        dist = Integer.toString((int) f[0]) + " m";
                        DistanceStation.setText(dist);

                        checkDistanceFunc(near);

                        //mMap.clear();
                        //ConnectionS.serviceConnection.requestStationData(); 의미 없어진다. 사용자의 요구때만 리프레쉬
                        //MapRefreshHandler.sendEmptyMessage(2); Marker 업데이트로 필요 X 최초 실행시 한번만 요구됨.

                        if (busMarker == null) {
                            busMarker = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(ConnectionS.busLat, ConnectionS.busLng))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.busmarker_skyblue)));
                            mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(ConnectionS.busLat, ConnectionS.busLng))); //버스 위치 업데이트
                        } else {
                            busMarker.setPosition(new LatLng(ConnectionS.busLat, ConnectionS.busLng));
                        }

                        DataRefreshHandler.sendEmptyMessage(1); //자기자신 반복합니다.
                    }
                },4000);
            }

            if (msg.what == 2) {
                String sid = msg.getData().getString("station");
                stationList.get(sid).remove();
                stationList.remove(sid);
                int i;
                for(i = 0; i < ConnectionS.StationArray.size(); i++) {
                    if (ConnectionS.StationArray.get(i).stationId.equals(sid)) {
                        break;
                    }
                }
                ConnectionS.StationArray.remove(i);

                MapRefreshHandler.sendEmptyMessage(1);
            }
        }
    };

    final Handler FakeGPSHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ConnectionS.busLat = ConnectionS.busLatLng[ConnectionS.busGPS][0];
                        ConnectionS.busLng = ConnectionS.busLatLng[ConnectionS.busGPS][1];
                        if (ConnectionS.busGPS != ConnectionS.busLatLng.length-1)
                        {
                            ConnectionS.busGPS++;
                        }

                        ConnectionS.serviceConnection.busLatLngUpdate(ConnectionS.busLat, ConnectionS.busLng);

                        FakeGPSHandler.sendEmptyMessage(1);
                    }
                },2000);
            }
        }
    };

    public int getNearestStation() {
        int nearest = -1;
        double dist = 999999999;
        float[] f = new float[1];

        for(int i = 0; i < ConnectionS.StationArray.size(); i++ )
        {
            if (ConnectionS.StationArray.get(i).routeNum == ConnectionS.busThis) {
                Location.distanceBetween(ConnectionS.StationArray.get(i).lat, ConnectionS.StationArray.get(i).lng, ConnectionS.busLat, ConnectionS.busLng, f);

                if (dist > (double) f[0]) {
                    dist = (double) f[0];
                    nearest = i;
                }
            }
        }

        return nearest;
    }

    public void checkDistanceFunc(int nearestStationIndex) { //거리 체크해서 300m 안에 있으면 빠빠이 아니면 그냥 남겨두는 센스입니다.
        if (!ConnectionS.alarmGone) {
            float[] f = new float[1];

            Location.distanceBetween(ConnectionS.busLat, ConnectionS.busLng, ConnectionS.StationArray.get(nearestStationIndex).lat, ConnectionS.StationArray.get(nearestStationIndex).lng, f);
            if (f[0] <= 300) { //300 미터 이내 이다면???
                ConnectionS.serviceConnection.alarmUserRequest(ConnectionS.StationArray.get(nearestStationIndex).stationId);
                ConnectionS.alarmGone = true;
            }
        }
    }

    final Handler MapRefreshHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                int i;
                Log.d("test", "stationDataRefreshed");
                //mMap.clear();


                MarkerOptions mrk;

                for (i = 0; i < ConnectionS.StationArray.size(); i++) {
                    //데이터는 array에 쌓이게 됩니다. (Array 는 Station Point 변수 가지고 있다.)

                    if (stationList.get(ConnectionS.StationArray.get(i).stationId) == null) { //정류장이 없을 경우에는
                        mrk = new MarkerOptions()
                                .position(new LatLng(ConnectionS.StationArray.get(i).lat, ConnectionS.StationArray.get(i).lng));
                        if (ConnectionS.stationIn == null) {
                            mrk.title("정류장");
                        }

                        if (ConnectionS.stationIn != null && ConnectionS.stationIn.equals(ConnectionS.StationArray.get(i).stationId)) {
                            mrk.icon(BitmapDescriptorFactory.fromResource(R.drawable.stationmarker_pink));
                            mrk.title("현 정류장");
                        } else {
                            mrk.icon(BitmapDescriptorFactory.fromResource(R.drawable.stationmarker_blue));
                        }

                        if (ConnectionS.StationArray.get(i).routeNum == ConnectionS.busThis)
                        {
                            mrk.visible(true);
                        } else {
                            mrk.visible(false);
                        }

                        if ((ConnectionS.busThis != 0) && (ConnectionS.busThis != ConnectionS.StationArray.get(i).routeNum)) //만약 선택이 되어있고 노선 번호랑 마커랑 일치하지 않을시...
                        {
                            continue; //그냥 마커 표시 안함 ㅇㅇ 어짜피 busThis 자체는 바뀌지 않는다! 그러니 의미가 없다.
                        }
                        stationList.put(ConnectionS.StationArray.get(i).stationId, mMap.addMarker(mrk));
                        stationList.get(ConnectionS.StationArray.get(i).stationId).setTag(ConnectionS.StationArray.get(i).stationId);
                    } else {
                        if ((ConnectionS.StationArray.get(i).routeNum == ConnectionS.busThis) || (ConnectionS.busThis == 0))
                        {
                            stationList.get(ConnectionS.StationArray.get(i).stationId).setVisible(true);
                        } else {
                            stationList.get(ConnectionS.StationArray.get(i).stationId).setVisible(false);
                        }
                    }
                }
                //버스 위치 추가합니다.
            }

            if (msg.what == 2) { //버스 경로 업데이트
                BusRouteRefreshed = true;
                busRoutePolylineOptionList.clear();
                Log.d("test", "busRouteRefreshed");

                int i, j;
                for (i = 0; i < ConnectionS.BusList.size(); i++) {
                    Log.d("LATLNG","BusList size : "+ConnectionS.BusList.size());
                    PolylineOptions busRouteOptions = new PolylineOptions();
                    ArrayList<Point> tempRoute = ConnectionS.BusRouteList.get(ConnectionS.BusList.get(i));
                    //Log.d("LATLNG","TempRoute size : "+tempRoute.size());
                    //임시리스트에 맵정보를 불러옵니다.
                    for (j = 0; j < tempRoute.size(); j++) {
                        busRouteOptions.add(new LatLng(tempRoute.get(j).lat, tempRoute.get(j).lng));
                        //버스 경로를 추가합니다. PolyLineOption에다가.
                    }
                    busRouteOptions.width(10);
                    busRouteOptions.color(busRouteColorList.get(i)).zIndex(2);

                    if (ConnectionS.busThis != 0) { //버스가 선택되어있을경우
                        if (ConnectionS.busThis != Integer.parseInt(ConnectionS.BusList.get(i))) {
                            //busRouteOptions.color(ColorUtils.setAlphaComponent(busRouteColorList.get(i), 50)).zIndex(1);
                            continue;
                        }
                    }
                    mMap.addPolyline(busRouteOptions);
                    busRoutePolylineOptionList.add(busRouteOptions);
                    //화면에 버스 경로를 표시합니다. 이건 처음에 한번만 작업하면 될듯 그러니 놔둬도 상관 무
                }
            }

            /*Message mmsg = new Message();
            Bundle data = new Bundle();
            data.putString("text", "위치 권한을 가져오는데 실패하였습니다. 권한을 다시 얻어주세요.");
            mmsg.what = 2; //메시지에 what 설정
            mmsg.setData(data);
            msg_handler.sendMessage(mmsg);*//*

            if (msg.what == 3) //사용자 정류장 제거
            {
                String stId = msg.getData().getString("stationId");
                stationList.get(stId).remove(); //정류장 마커 제거
                stationList.remove(stId);
                MapRefreshHandler.sendEmptyMessage(1);
            }*/

            //생각해보니까 MapRefreshHandler 때문에 정류장 생성 알림은 필요 없을것 같다.
        }
    };


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        busRouteColorList.add(Color.GREEN);
        busRouteColorList.add(Color.RED);
        busRouteColorList.add(Color.YELLOW);
        busRouteColorList.add(Color.GRAY);
        busRouteColorList.add(Color.CYAN);

        // Add a marker in Sydney and move the camera
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(36.892988, 126.635109), 13));

        mMap.setMinZoomPreference(11.0f);
        mMap.setMaxZoomPreference(17.5f);

        ConnectionS.serviceConnection.requestStationData();

        SharedPreferences pref = getSharedPreferences("SHFD", Activity.MODE_PRIVATE); //Map version Check and Update
        String ver = pref.getString("BusInfoVersion", "");
        ConnectionS.serviceConnection.busInfoUpdateCheck(ver); //지도 화면 전환 후 업데이트 체크를 한다.


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            Message mmsg = new Message();
            Bundle data = new Bundle();
            data.putString("text", "위치 권한을 가져오는데 실패하였습니다. 권한을 다시 얻어주세요.");
            mmsg.what = 2; //메시지에 what 설정
            mmsg.setData(data);
            msg_handler.sendMessage(mmsg);
            finish();
        }

        mMap.getUiSettings().setMapToolbarEnabled(false);

        DataRefreshHandler.sendEmptyMessage(1); //주기적 맵 리프레쉬

        FakeGPSHandler.sendEmptyMessage(1);//FAKE GPS
    }
    public void closeApp(){
        ConnectionS.serviceConnection.endService();
        stopService(ConnectionS.service);
        unbindService(conConnection);
        ActivityCompat.finishAffinity(SHMainActivity.this);
        System.exit(0);
    }

    int exit = 0;

    @Override
    public void onBackPressed() {
        if (exit == 1) {
            super.onBackPressed();
            ConnectionS.serviceConnection.endService();
            stopService(ConnectionS.service);
            unbindService(conConnection);
            ActivityCompat.finishAffinity(SHMainActivity.this);
            System.exit(0);
        } else {
            exit = 1;
            Toast.makeText(SHMainActivity.this, "한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exit = 0;
                }
            }, 2000);
        }
    }
}
