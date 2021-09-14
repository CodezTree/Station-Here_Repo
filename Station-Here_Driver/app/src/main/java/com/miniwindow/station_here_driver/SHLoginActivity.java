package com.miniwindow.station_here_driver;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class SHLoginActivity extends AppCompatActivity {


    //GRAPHICS--------------

    ImageView icon;
    ImageView imgname;
    EditText name;
    Button join;
    TextInputLayout TLName;

    //----------------------

    public static String tempID;
    //private ConnectionService conService; //서비스 클래스 생성

    //Intent service;

    private ServiceConnection conConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            ConnectionService.ConnectionServiceBinder binder = (ConnectionService.ConnectionServiceBinder) service;
            ConnectionS.serviceConnection = binder.getService(); //서비스 접근을 위해 getService로 서비스를 받아온다.
            ConnectionS.serviceConnection.registerCallback(conCallback); //콜백을 서비스와 연동한다.

            Log.d("test","Login service connected");

            ConnectionS.serviceConnection.connectionEstablish(); //서버와 연결을 시도합니다.
        }

        public void onServiceDisconnected(ComponentName className) {
            //여기서 conncecionS null 처리하면 안됩니다!!!!
        }
    };
    //service 종료를 원하면 unbindService(conConnection);

    final Handler msg_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) { //접속 실패
                AlertDialog.Builder alert = new AlertDialog.Builder(SHLoginActivity.this);
                alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();     //닫기

                        closeApp();
                    }
                });
                alert
                        .setMessage("서버접속에 실패하였습니다.")
                        .setCancelable(false);
                alert.show();
            }
        }
    };

    int exit = 0;

    @Override
    public void onBackPressed()
    {
        if (exit == 1) {
            super.onBackPressed();
            closeApp();
        }
        else
        {
            exit = 1;
            Toast.makeText(SHLoginActivity.this, "한번 더 누르시면 종료됩니다.",Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exit = 0;
                }
            },2000);
        }
    }


    //서비스에서 아래 콜백함수를 호출한다. 여기에선 액티비티 처리내용 입력.
    //만약 다음 액티비티도 ConnectionService가 필요하면 Intent로 넘겨줄 생각.
    private ConnectionService.ICallback conCallback = new ConnectionService.ICallback() {


        @Override
        public void busRouteLoad()
        {
            //No use
            Log.d("test","No using route load called");
        }

        @Override
        public void busDataRefresh(){
            //No use
            Log.d("test","No using route refresh called");
        }

        @Override
        public void busRouteSave(String n)
        {
            //No Use
        }

        @Override
        public void clearMap(){}

        @Override
        public void stationDestroy(String sid) {

        }

        @Override
        public void stationDataUpdate(String sid, int people) {

        }

        @Override
        public void stationCreated(String sid, double lat, double lng, boolean isF, int rN) {

        }
        //Nouse

        @Override
        public void stationDataRefresh(){
//No Use~
        }

        @Override
        public void msg_connection_failed() {
            msg_handler.sendEmptyMessage(1);
        }

        @Override
        public void msg_show_monoButton(String msg){
            //No use
            /*AlertDialog.Builder alert = new AlertDialog.Builder(getApplicationContext());
            alert.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();     //닫기
                }
            });
            alert.setMessage(msg);
            alert.show();*/
        }

        @Override
        public void saveBid(String Bid) {
            SharedPreferences pref = getSharedPreferences("SHFD", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("Bid", Bid);
            editor.commit();
            Log.d("test","Bid Saved");
        }

        @Override
        public void saveTempId(String tempId) {
            StaticsVar.TempId = tempId;
        }

        @Override
        public void resumeActivity() {
            unbindService(conConnection);
            Log.d("test","unbinded at login");

            Intent intent = new Intent(SHLoginActivity.this, SHMainActivity.class);
            startActivity(intent);
            finish();
        }


        @Override
        public void toast_show_short(String msg){
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        }
    };

    public void startConnectionService() {
        ConnectionS.service = new Intent(this, ConnectionService.class);
        startService(ConnectionS.service); //여기서 서비스 실행
        bindService(ConnectionS.service, conConnection, BIND_AUTO_CREATE); //여기서 바인딩
    }
    //서비스 시작. 바인딩

    /*
    액티비티에서 서비스 함수를 호출하려면,
    mService.함수이름 요렇게 호출 가능*/

    /*Button register;
    EditText nameText;
    EditText PWText;*/
    String Bid;
    ImageView Logo;

    boolean canRegister = false;

    boolean regClicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shlogin);





        //GRAPHICS----------
        name = (EditText) findViewById(R.id.Name);
        join=(Button) findViewById(R.id.join);
        icon=(ImageView) findViewById(R.id.icon);
        imgname=(ImageView) findViewById(R.id.imgname);

        TLName = (TextInputLayout) findViewById(R.id.nicknamelayout);


        name.setVisibility(View.GONE);
        join.setVisibility(View.GONE);
        imgname.setVisibility(View.GONE);
        TLName.setVisibility(View.GONE);

        //-----------

        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!name.getText().toString().equals("")) {
                    ConnectionS.serviceConnection.busLogin(name.getText().toString());
                }
            }
        });



        startConnectionService();

        SharedPreferences pref = getSharedPreferences("SHFD", Activity.MODE_PRIVATE);
        Bid = pref.getString("Bid", null);

        if (Bid == null) //버스 등록 안됨
        {
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("BusInfoVersion", "");
            editor.commit();
            //처음 회원가입이라면 당연히 데이터 없을꺼니깐 업데이트 합시다!~(그래서 "" 저장)

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {

                    Animation translate = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.login_translate);
                    icon.startAnimation(translate);

                    name.setVisibility(View.VISIBLE);
                    join.setVisibility(View.VISIBLE);
                    imgname.setVisibility(View.VISIBLE);
                    TLName.setVisibility(View.VISIBLE);


                    final Animation login = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.login);
                    Toast.makeText(getApplicationContext(), "Login Loading...", Toast.LENGTH_SHORT).show();

                    name.startAnimation(login);
                    join.startAnimation(login);
                    imgname.startAnimation(login);
                    TLName.startAnimation(login);

                    login.setFillAfter(true);
                    translate.setFillAfter(true);
                }
            }, 2000);
        }
        else
        {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    ConnectionS.serviceConnection.busLogin(Bid);
                }
            },2000);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

    }

    public void closeApp() {
        ConnectionS.serviceConnection.endService();
        stopService(ConnectionS.service);
        unbindService(conConnection);
        ActivityCompat.finishAffinity(SHLoginActivity.this);
        System.exit(0);
    }

    //신규 유저 핸들링 -----------------
}