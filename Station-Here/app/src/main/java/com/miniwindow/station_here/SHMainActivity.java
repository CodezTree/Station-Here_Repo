package com.miniwindow.station_here;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.*;


public class SHMainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {

    private GoogleMap mMap;

    Map<String, Marker> stationList = new HashMap<String, Marker>();

    public InterDB IDB;

    boolean BusRouteRefreshed = false;

    ArrayList<Integer> busRouteColorList = new ArrayList<>();

    Map<Integer, Polyline> busRoutePolylineList = new HashMap<>();
    //ArrayList<PolyLine> busRoutePolylineList = new ArrayList<>();

    ListView busListView;

    MenuItem searchItem;

    LocationManager locationManager;

    FloatingActionButton stationCreateFAB;

    Marker busMarker;


    //public static Map<String, ArrayList<LatLng>> BusRoute = new HashMap<>();  //버스 번호당 버스 노선을 담을 리스트
    //public static ArrayList<String> BusList = new ArrayList<>(); //버스 번호를 담을 리스트

    double currentZoom;

    private ServiceConnection conConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("test", "main service connected"); //바인딩 성공
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d("test", "Service Disconnected on Main");
        }
    };

    ProgressDialog mapLoadDia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shmain);

        mapLoadDia = ProgressDialog.show(SHMainActivity.this, "", "정보 로딩중 입니다...", true);
        mapLoadDia.setCancelable(false); //로딩바???
        mapLoadDia.show();

        bindService(new Intent(this, ConnectionService.class), conConnection, BIND_AUTO_CREATE); //여기서 바인딩
        ConnectionS.serviceConnection.registerCallback(conCallback);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //ConnectionS.serviceConnection.sendHello();

        busListView = (ListView) findViewById(R.id.busRouteSearchListView);

        ArrayAdapter<String> busRouteAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, ConnectionS.BusList);
        busListView.setAdapter(busRouteAdapter);

        busListView.setVisibility(View.GONE);

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
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30, 30, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //get the latitude
                ConnectionS.userLat = location.getLatitude();
                ConnectionS.userLng = location.getLongitude();
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
        // Inflate the menu; this adds items to the action bar if it is present.
        /*
        like..
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        */
        getMenuInflater().inflate(R.menu.menu_map, menu);


        //SearchView를 searchable에 연동시킨다.
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        //서치뷰 설정
        searchView.setSubmitButtonEnabled(false);
        searchView.setQueryHint("버스번호");

        /*searchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (searchView.isIconified()) {
                    Log.d("test","Menu IconiFied");
                    busListView.setVisibility(View.VISIBLE);
                } else {
                    Log.d("test","Menu search not Iconified");
                    busListView.setVisibility(View.GONE);
                }
            }
        });*/ //Not working

        MenuItemCompat.setOnActionExpandListener(menu.findItem(R.id.action_search), new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                Log.d("test", "Menu Expand");
                if (ConnectionS.busListLoaded) { //for safety
                    Log.d("test","List visible");
                    busListView.setVisibility(View.VISIBLE);
                }
                else{
                    Log.d("test","ListNope");
                }
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                Log.d("test", "Menu Collapse");
                if (ConnectionS.busListLoaded) {
                    busListView.setVisibility(View.GONE);
                }
                return true;
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (TextUtils.isEmpty(s)) {
                    if (ConnectionS.busListLoaded) {
                        busListView.clearTextFilter();
                    }
                } else {
                    if (ConnectionS.busListLoaded) {
                        busListView.setFilterText(s.toString());
                    }
                }
                return false;
            }
        });

        stationCreateFAB = (FloatingActionButton) findViewById(R.id.stationCreateButton);
        stationCreateFAB.setImageResource(R.drawable.stationadd);
        stationCreateFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //정류장 생성
                if (ConnectionS.stationIn.equals("A")) {
                    ConnectionS.stationCreating = true;
                    final String[] items = new String[]{"다시 띄우지 않기"};
                    final boolean[] never = {false};

                    boolean alert1;
                    SharedPreferences pref = getSharedPreferences("SH", Activity.MODE_PRIVATE);
                    alert1 = pref.getBoolean("alert1",false);
                    /*SharedPreferences.Editor editor = pref.edit();
                    editor.commit();*/

                    if (alert1) {
                        new AlertDialog.Builder(SHMainActivity.this)
                                .setTitle("알림")
                                .setMessage("버스 노선 선택 후 지도위 노선을 클릭해 정류장 생성위치를 지정하세요.")
                                .setMultiChoiceItems(items, new boolean[]{false}, new DialogInterface.OnMultiChoiceClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                        if (isChecked) {
                                            never[0] = true;
                                        } else {
                                            never[0] = false;
                                        }
                                    }
                                })
                                .setPositiveButton("알겠습니다", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                })
                                .setCancelable(false)
                                .show();
                        if (never[0]) {
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putBoolean("alert1",true);
                            editor.commit();
                        }
                    }


                    //ConnectionS.selectedBus = 0;
                    //여기서 찾기 탈 버스....
                    MenuItemCompat.expandActionView(searchItem);//서칭을 활성화 시킨다. (검색하시오! 버스)
                } else { //이미 정류장에 탑승해있는 경우???
                    new AlertDialog.Builder(SHMainActivity.this)
                            .setTitle("정류장 나오기")
                            .setMessage("현재 대기중인 정류장에서 나오시겠습니까?")
                            .setPositiveButton("예", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    ConnectionS.serviceConnection.goOutStation(ConnectionS.stationIn);
                                    stationCreateFAB.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotatebackward));
                                }
                            })
                            .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .setCancelable(false)
                            .show();
                }
            }
        });

        return true;
    }

    final Handler DataRefreshHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //mMap.clear();
                        //ConnectionS.serviceConnection.requestStationData();
                        ConnectionS.serviceConnection.busLocationUpdate("DANG1JIN1ABC");

                        if (ConnectionS.busLocUpated) {
                            if (busMarker == null) {
                                busMarker = mMap.addMarker(new MarkerOptions()
                                        .position(new LatLng(ConnectionS.bus1lat, ConnectionS.bus1lng))
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.busmarker_skyblue)));
                                Log.d("test","new Bus Marker Added");
                            } else {
                                busMarker.setPosition(new LatLng(ConnectionS.bus1lat, ConnectionS.bus1lng));
                            }
                        }
                        ConnectionS.busLocUpated = false;

                        DataRefreshHandler.sendEmptyMessage(1);
                        //checkBusIn();
                    } //4 초마다 데이터 갱신
                }, 4000);
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

    private boolean checkReady() {
        if (mMap == null) {
            Toast.makeText(this, "Map not Ready!!", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
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
                        if (ConnectionS.stationIn.equals("A")) {
                            mrk.title("정류장");
                        }

                        if (!ConnectionS.stationIn.equals("A") && ConnectionS.stationIn.equals(ConnectionS.StationArray.get(i).stationId)) {
                            mrk.icon(BitmapDescriptorFactory.fromResource(R.drawable.stationmarker_pink));
                            mrk.title("현 정류장");
                        } else {
                            mrk.icon(BitmapDescriptorFactory.fromResource(R.drawable.stationmarker_blue));
                        }

                        if (ConnectionS.StationArray.get(i).routeNum == ConnectionS.selectedBus)
                        {
                            mrk.visible(true);
                        } else {
                            mrk.visible(false);
                        }
                        stationList.put(ConnectionS.StationArray.get(i).stationId, mMap.addMarker(mrk));
                        stationList.get(ConnectionS.StationArray.get(i).stationId).setTag(ConnectionS.StationArray.get(i).stationId);
                    } else {
                        Marker t = stationList.get(ConnectionS.StationArray.get(i).stationId);

                        if (!ConnectionS.stationIn.equals("A") && ConnectionS.stationIn.equals(ConnectionS.StationArray.get(i).stationId)) {
                            t.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.stationmarker_pink));
                            t.setTitle("현 정류장");
                        } else {
                            t.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.stationmarker_blue));
                        }

                        if ((ConnectionS.StationArray.get(i).routeNum == ConnectionS.selectedBus) || (ConnectionS.selectedBus == 0))
                        {
                            t.setVisible(true);
                        } else {
                            t.setVisible(false);
                        }
                    }
                    //버스 정류장을 띄운다.
                    //Toast.makeText(getApplicationContext(), "Station Data Refreshed", Toast.LENGTH_SHORT).show();
                }
            }

            if (msg.what == 2) {
                BusRouteRefreshed = true;
                Log.d("test", "busRouteRefreshed");
                if (busRoutePolylineList.size() == 0) {

                    int i, j;
                    for (i = 0; i < ConnectionS.BusList.size(); i++) {
                        //Log.d("LATLNG","BusList size : "+ConnectionS.BusList.size());
                        PolylineOptions busRouteOptions = new PolylineOptions();
                        ArrayList<Point> tempRoute = ConnectionS.BusRouteList.get(ConnectionS.BusList.get(i));
                        //Log.d("LATLNG","TempRoute size : "+tempRoute.size());
                        //임시리스트에 맵정보를 불러옵니다.
                        for (j = 0; j < tempRoute.size(); j++) {
                            busRouteOptions.add(new LatLng(tempRoute.get(j).lat, tempRoute.get(j).lng));
                            //버스 경로를 추가합니다. PolyLineOption에다가.
                            /*
                            if ((i == 3) && (j == 12))
                            {
                                Log.d("LATLNG","Refreshed : "+Double.toString(tempRoute.get(j).lat)+", "+Double.toString(tempRoute.get(j).lng));
                            }*/
                        }
                        busRouteOptions.width(10);
                        busRouteOptions.color(busRouteColorList.get(i)).zIndex(2);

                        if (ConnectionS.selectedBus != 0) { //버스가 선택되어있을경우
                            if (ConnectionS.selectedBus != Integer.parseInt(ConnectionS.BusList.get(i))) {
                                busRouteOptions.color(ColorUtils.setAlphaComponent(busRouteColorList.get(i), 50)).zIndex(1);
                            }
                        }

                        busRoutePolylineList.put(Integer.parseInt(ConnectionS.BusList.get(i)), mMap.addPolyline(busRouteOptions));
                        //화면에 버스 경로를 표시합니다.
                    }
                } else {
                    for(int i = 0; i < ConnectionS.BusList.size(); i++) {
                        Polyline tempP = busRoutePolylineList.get(Integer.parseInt(ConnectionS.BusList.get(i)));

                        tempP.setWidth(10);
                        tempP.setColor(busRouteColorList.get(i));
                        tempP.setZIndex(2);

                        if (ConnectionS.selectedBus != 0) { //버스가 선택되어있을경우
                            if (ConnectionS.selectedBus != Integer.parseInt(ConnectionS.BusList.get(i))) {
                                tempP.setColor(ColorUtils.setAlphaComponent(busRouteColorList.get(i), 50));
                                tempP.setZIndex(1);
                            }
                        }
                    }
                } //선택 경로 변경을 위해서.

                //버스를 띄운다.
            }

            if (msg.what == 3){  //ListView Clicked and refreshing
                ArrayAdapter<String> busRouteAdapter = new ArrayAdapter<>(SHMainActivity.this, android.R.layout.simple_list_item_1, ConnectionS.BusList);
                busListView.setAdapter(busRouteAdapter);

                busListView.setTextFilterEnabled(true);

                busListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView parent, View v, int position, long id) { //item click 작동안됨 ㅠㅠㅠㅠㅠㅠ //TODO : 여기여기 작동시키기
                        ConnectionS.selectedBus = Integer.parseInt((String) parent.getItemAtPosition(position));
                        mMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(new LatLng(
                                                ConnectionS.BusRouteList
                                                        .get(Integer.toString(ConnectionS.selectedBus))
                                                        .get(ConnectionS.BusRouteList.get(Integer.toString(ConnectionS.selectedBus)).size() / 2).lat,
                                                ConnectionS.BusRouteList
                                                        .get(Integer.toString(ConnectionS.selectedBus))
                                                        .get(ConnectionS.BusRouteList.get(Integer.toString(ConnectionS.selectedBus)).size() / 2).lng),
                                        17.0f));
                        MapRefreshHandler.sendEmptyMessage(2);
                        MenuItemCompat.collapseActionView(searchItem);
                    }
                });
            }

            if (msg.what == 4) {
                mMap.clear();
            }
        }
    };

    final Handler msg_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) { //순방향 역방향 지정
                new AlertDialog.Builder(SHMainActivity.this)
                        .setTitle("방향 선택")
                        .setMessage("순방향 / 역방향")
                        .setPositiveButton("역방향", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ConnectionS.isF = false;
                                ConnectionS.serviceConnection.newStationCreateOK();
                                ConnectionS.stationCreating = false;
                            }
                        })
                        .setNegativeButton("순방향", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ConnectionS.isF = true;
                                ConnectionS.serviceConnection.newStationCreateOK();
                                ConnectionS.stationCreating = false;
                            }
                        })
                        .setCancelable(false)
                        .show();
                stationCreateFAB.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotateforward));
            }

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

            if (msg.what == 3) {
                mapLoadDia.dismiss();
            }

            if (msg.what == 4) {
                /*NotificationCompat.Builder builder = new NotificationCompat.Builder(SHMainActivity.this);

                builder.setContentTitle("버스가 현재 진입중입니다!")
                        .setContentText("버스 -> 정류장")
                        .setTicker("버스 알림")
                        .setWhen(System.currentTimeMillis())
                        .setDefaults(Notification.DEFAULT_ALL);


                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                nm.notify(1234, builder.build());*/

                Resources res = getResources();

                NotificationCompat.Builder Notifi = new NotificationCompat.Builder(SHMainActivity.this);

                Notifi.setContentTitle("버스가 현재 진입중입니다!")
                        .setContentText("버스 -> 정류장")
                        .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ic_stat_name))
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setTicker("버스 알림")
                        .setWhen(System.currentTimeMillis())
                        .build();

                NotificationManager Notifi_M = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Notifi_M.notify( 777 , Notifi.build());
            }

            if (msg.what == 5) {
                String str = msg.getData().getString("text");
                Toast.makeText(SHMainActivity.this,str,Toast.LENGTH_LONG).show();
            }

            if (msg.what == 6) {
                Resources res = getResources();

                NotificationCompat.Builder Notifi = new NotificationCompat.Builder(SHMainActivity.this);

                Notifi.setContentTitle("버스운행이 현재 지연되고 있습니다.")
                        .setContentText("버스 지연")
                        .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ic_stat_name))
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setTicker("버스 알림")
                        .setWhen(System.currentTimeMillis())
                        .build();

                NotificationManager Notifi_M = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Notifi_M.notify( 778 , Notifi.build());
            }

            if (msg.what == 7) {
                Resources res = getResources();

                NotificationCompat.Builder Notifi = new NotificationCompat.Builder(SHMainActivity.this);

                Notifi.setContentTitle("정류장과 너무 멀리 떨어져 있어 정류장에서 자동으로 나가졌습니다.")
                        .setContentText("정류장 유지 불가")
                        .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ic_stat_name))
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setTicker("버스 알림")
                        .setWhen(System.currentTimeMillis())
                        .build();

                NotificationManager Notifi_M = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Notifi_M.notify( 779 , Notifi.build());
            }

        }
    };

    private ConnectionService.ICallback conCallback = new ConnectionService.ICallback() {

        @Override
        public void msg_show_station_far() {
            msg_handler.sendEmptyMessage(7);
            stationCreateFAB.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotatebackward));
        }

        @Override
        public void alarmBusLate() { msg_handler.sendEmptyMessage(6);}

        @Override
        public void msg_show_toast(String msg) {
            Message mmsg = new Message();
            Bundle data = new Bundle();
            data.putString("text", msg);
            mmsg.what = 5; //메시지에 what 설정
            mmsg.setData(data);
            msg_handler.sendMessage(mmsg);
        }

        @Override
        public void alarmBusComing() {
            msg_handler.sendEmptyMessage(4);
        }

        @Override
        public void dismissDialog() {
            msg_handler.sendEmptyMessage(3);
        }

        @Override
        public void msg_get_forward() {
            msg_handler.sendEmptyMessage(1); //forward getting
        }

        @Override
        public void clearMap()
        {
            //MapRefreshHandler.sendEmptyMessage(4); //Clear Map
        }

        @Override
        public void busRouteLoad() {
            Log.d("test", "bus Route Loading");
            ConnectionS.BusList = IDB.getBusRouteNum();
            ConnectionS.BusRouteList = IDB.getBusRoute();
            ConnectionS.busListLoaded = true;
            applyAdapter();
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
            applyAdapter();

            SharedPreferences pref = getSharedPreferences("SH", Activity.MODE_PRIVATE);
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
        public void saveUid(String Uid) {
            SharedPreferences pref = getSharedPreferences("SH", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("Uid", Uid);
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

        @Override
        public void stationDestroy(String sid) {
            Message mmsg = new Message();
            Bundle data = new Bundle();
            data.putString("station",sid);
            mmsg.what = 2; //메시지에 what 설정
            mmsg.setData(data);
            DataRefreshHandler.sendMessage(mmsg);
        }

        @Override
        public void stationDataUpdate(String sid, int people) {
            StationPoint tempPoint;
            int i;
            for(i = 0; i < ConnectionS.StationArray.size(); i++) {
                if (ConnectionS.StationArray.get(i).stationId.equals(sid)) {
                    break;
                }
            }
            tempPoint = ConnectionS.StationArray.get(i);
            tempPoint.people += people;
            MapRefreshHandler.sendEmptyMessage(1);
        }

        @Override
        public void stationCreated(String sid, double lat, double lng, boolean isF, int rN) {
            ConnectionS.StationArray.add(new StationPoint(lat, lng, sid , 1, isF, rN));
            MapRefreshHandler.sendEmptyMessage(1);
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.showAllRoute) {
            ConnectionS.selectedBus = 0;
            MapRefreshHandler.sendEmptyMessage(1);
            MapRefreshHandler.sendEmptyMessage(2);
        }
/*
        if (id == R.id.refresh) {
            ConnectionS.serviceConnection.requestStationData();
        }*/
        /*//no inspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_maps) {
            return true;
        }
        if (id == R.id.action_station){
            return true;
        }*/

        return super.onOptionsItemSelected(item);
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

    public Marker createMarker;

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        busRouteColorList.add(Color.GREEN);
        busRouteColorList.add(Color.RED);
        busRouteColorList.add(Color.YELLOW);
        busRouteColorList.add(Color.GRAY);
        busRouteColorList.add(Color.CYAN);

        //LatLng mark = new LatLng(36.808412, 127.114036);
        //mMap.setBuildingsEnabled(true);
        //mMap.setTrafficEnabled(true);
        //updateMyLocation();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(36.892988, 126.635109), 13));

        Log.d("test", "Camera has moved");
        mMap.setMinZoomPreference(11.0f);
        mMap.setMaxZoomPreference(17.5f);
        //testing...haha

        mMap.setOnInfoWindowClickListener(this);


        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            int i;

            @Override
            public void onMapClick(LatLng latLng) {
                if (ConnectionS.stationCreating) { //정류장 생성중이다..
                    for (i = 0; i < busRoutePolylineList.size(); i++) {
                        if (ConnectionS.BusList.get(i).equals(Integer.toString(ConnectionS.selectedBus))) {
                            Polyline polyline = busRoutePolylineList.get(Integer.parseInt(ConnectionS.BusList.get(i)));
                            if (PolyUtil.isLocationOnPath(latLng, polyline.getPoints(), true, 10)) {// 해당 폴리라인을 클릭했다면, 여기부터
                                //참고로 polylineoptions 순서는 buslist 순서랑 같음 ㅇㅇ
                                ConnectionS.posLat = latLng.latitude;
                                ConnectionS.posLng = latLng.longitude;

                                if (createMarker == null) {

                                    MarkerOptions mrkO;
                                    mrkO = new MarkerOptions()
                                            .position(new LatLng(ConnectionS.posLat, ConnectionS.posLng))
                                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.station_create_mark))
                                            .title("정류장 생성");
                                    Marker m;
                                    m = mMap.addMarker(mrkO);
                                    m.setTag("create");
                                    createMarker = m;
                                } else {
                                    createMarker.setPosition(new LatLng(ConnectionS.posLat, ConnectionS.posLng));
                                }

                                createMarker.showInfoWindow();
                                //TODO : 여기다가 아이콘 설정하기
                            }
                        }
                    }
                }

                /*ConnectionS.selectedBus = 0;
                ConnectionS.posLat = latLng.latitude;
                ConnectionS.posLng = latLng.longitude;
                Log.d("test","map touched");
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
                MapRefreshHandler.sendEmptyMessage(1); //Marker Refresh
                MapRefreshHandler.sendEmptyMessage(2); //BusRoute Refresh 구지? 해야되네 ㅠ
                //FOR DEBUGGING*/ //for testing
            }
        });

        /*mMap.setOnPolylineClickListener(new GoogleMap.OnPolylineClickListener() {
            @Override
            public void onPolylineClick(Polyline polyline) {
                if (!ConnectionS.stationCreating)
                {
                    Log.d("test","Polyline Clicked => "+polyline.getTag());
                    ConnectionS.selectedBus = Integer.parseInt((String) polyline.getTag());
                }
            }
        });*/ //Not using

        if (stationList.size() == 0) {
            ConnectionS.serviceConnection.requestStationData();
        }

        SharedPreferences pref = getSharedPreferences("SH", Activity.MODE_PRIVATE); //Map version Check and Update
        String ver = pref.getString("BusInfoVersion", "");
        ConnectionS.serviceConnection.busInfoUpdateCheck(ver); //지도 화면 전환 후 업데이트 체크를 한다.

        mMap.setOnCameraIdleListener(() -> {
            currentZoom = (double) mMap.getCameraPosition().zoom;
            //use zoomLevel value..
        });


        mapLoadDia.dismiss();



        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            Message mmsg = new Message();
            Bundle data = new Bundle();
            data.putString("text", "위치 권한을 가져오는데 실패하였습니다. 앱 종료후 권한을 다시 얻어주세요.");
            mmsg.what = 2; //메시지에 what 설정
            mmsg.setData(data);
            msg_handler.sendMessage(mmsg);
        }
        mMap.getUiSettings().setMapToolbarEnabled(false);

        DataRefreshHandler.sendEmptyMessage(1);
    }

    @Override
    public void onInfoWindowClick(Marker marker) { //마커 인포 클릭시
        if ((marker.getTag() != null) && (!marker.getTag().equals("create")) && (ConnectionS.stationIn.equals("A"))) {
            String id = (String) marker.getTag();
            new AlertDialog.Builder(SHMainActivity.this)
                    .setTitle("정류장 합류")
                    .setMessage("이 정류장에 정말 합류하시겠습니까?")
                    .setPositiveButton("예", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ConnectionS.serviceConnection.joinStation(id);
                            mapLoadDia = ProgressDialog.show(SHMainActivity.this, "", "정류장 합류 중입니다.", true);
                            mapLoadDia.setCancelable(false);
                            mapLoadDia.show();
                            stationCreateFAB.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotateforward));
                        }
                    })
                    .setNegativeButton("아니요", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .setCancelable(false)
                    .show();
        }
        if ((marker.getTag() != null) && ((String) marker.getTag()).equals("create")) { //생성하는 마커입니다.
            ConnectionS.serviceConnection.newStationCreate();
            marker.remove();
            createMarker = null;
        }
    }

    public void applyAdapter() {

        Log.d("test","Bus List : "+ConnectionS.BusList.get(0) +" , "+ConnectionS.BusList.get(1)+" , "+ConnectionS.BusList.get(2)+" , "+ConnectionS.BusList.get(3));

        MapRefreshHandler.sendEmptyMessage(3);
    }
}
