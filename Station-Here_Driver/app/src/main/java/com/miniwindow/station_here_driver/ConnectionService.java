package com.miniwindow.station_here_driver;


import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.BooleanSupplier;

/**
 * Created by USER on 2017-08-20.
 */

public class ConnectionService extends Service {

    private String html = "";
    private Handler mHandler;

    private Socket socket;

    String msg;

    private BufferedReader networkReader;
    private BufferedWriter networkWriter;

    private String ip = "10.1.40.31"; //169.254.9.185; 10.1.92.137; 203.232.74.87; 10.0.2.2 (local test); working?
    // IP
    private int port = 9943;

    private ConnectionThread thread;

    Thread msgCheck;

    PrintWriter out;
    //서비스 바인더 내부 클래스 선언
    class ConnectionServiceBinder extends Binder {
        ConnectionService getService() {
            return ConnectionService.this; //현재 서비스를 반환.
        }
    }

    private final IBinder conBinder = new ConnectionServiceBinder();

    Intent intent;

    @Override
    public IBinder onBind(Intent intent) {
        this.intent = intent;
        return conBinder;
    }

    interface ICallback{
        void saveBid(String Bid);
        void resumeActivity();
        void saveTempId(String tempId);
        void msg_connection_failed();
        void msg_show_monoButton(String msg);
        void toast_show_short(String msg);
        void stationDataRefresh();
        void busRouteSave(String busInfoVer);
        void busRouteLoad();
        void busDataRefresh();
        void clearMap();
        void stationDestroy(String sid);
        void stationDataUpdate(String sid, int people);
        void stationCreated(String sid, double lat, double lng, boolean isF, int rN);
    }




    private ICallback conCallback; //액티비티로의 콜백 부분

    public void registerCallback(ICallback cb)
    {
        conCallback = cb;
    }

    @Override
    public void onCreate(){
        super.onCreate();
        Log.d("test","ConnectionService Init");

    }

    public void connectionEstablish(){
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        thread = new ConnectionThread();
        thread.setDaemon(true);
        thread.start();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (socket == null || !socket.isConnected() )
                    conCallback.msg_connection_failed();
                else
                    messageCheckingStart();

            }
        },10000);
    }

    public void endService(){
        try {
            socket.close();
            msgCheck.interrupt();
        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private void messageCheckingStart(){
        msgCheck = new Thread(new MsgCheck());
        msgCheck.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d("test","ConnectionService Started");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d("test","ConnectionService Ended");
    }

    public void setSocket(String ip, int port) throws IOException {

        try {
            socket = new Socket(ip, port);

            networkWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            networkReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(networkWriter, true);

            if (networkWriter == null)
            {
                Log.d("NET","Network Writer Error");
            }

            if (networkReader == null)
            {
                Log.d("NET", "Network Reader Error");
            }

            out.println("hello");
        } catch (IOException e) {
            Log.d("test",e.toString());
            e.printStackTrace();
        } catch (Exception e1) {
            e1.printStackTrace();
        }

    }

    private class ConnectionThread extends Thread {
        public void run(){
            try{
                setSocket(ip,port);

            } catch(IOException e1){
                e1.printStackTrace();
            }
        }
    }

    int error = 0;

    public class MsgCheck implements Runnable{
        @Override
        public void run() {
            while(!Thread.currentThread().isInterrupted()) {
                Log.d("MSGCHECK", "checking...");
                try {
                    msg = networkReader.readLine();
                    int i,j;
                    double lat,lng;

                    int dataSize;
                    int people;
                    String stationId;
                    boolean isForward;
                    int routeNum;

                    switch (msg) {
                        case "tid_response":
                            conCallback.saveTempId(networkReader.readLine()); //Temp ID ID
                            Log.d("resume","resume to map");
                            StaticsVar.tempIdGetStatus = true;
                            conCallback.saveBid(ConnectionS.myBusData.Bid); //여기에서 버스 아이디 다시 날라감.
                            conCallback.resumeActivity();
                            break;
                        //항상 임시 id 발급 이후에 resume 시킨다.

                        case "station_destroyed" :
                            String sid = networkReader.readLine();
                            conCallback.stationDestroy(sid);
                            //TODO : start here
                            break;

                        case "station_created" :
                            String _sid = networkReader.readLine();
                            double _lat = Double.parseDouble(networkReader.readLine());
                            double _lng = Double.parseDouble(networkReader.readLine());
                            boolean isFor = Boolean.parseBoolean(networkReader.readLine());
                            int rN = Integer.parseInt(networkReader.readLine());
                            conCallback.stationCreated(_sid,_lat,_lng,isFor,rN);
                            break;


                        case "station_data_response" :
                            //최초 한번만
                            //그 이후는 그냥 소소한 연락으로 뿅뿅뿅
                            dataSize = Integer.parseInt(networkReader.readLine());

                            ConnectionS.StationArray = new ArrayList<>();

                            for(i = 0; i < dataSize; i++) {
                                lat = Double.parseDouble(networkReader.readLine());
                                lng = Double.parseDouble(networkReader.readLine());
                                stationId = networkReader.readLine();
                                people = Integer.parseInt(networkReader.readLine());
                                isForward = Boolean.parseBoolean(networkReader.readLine());
                                routeNum = Integer.parseInt(networkReader.readLine());

                                ConnectionS.StationArray.add(new StationPoint(lat,lng,stationId,people,isForward,routeNum));
                            }

                            conCallback.stationDataRefresh();
                            break;

                        case "stationDataUpdate" :
                            String __sid = networkReader.readLine();
                            int _people = Integer.parseInt(networkReader.readLine());
                            conCallback.stationDataUpdate(__sid,_people);
                            break;

                        case "busInfoUpdateCheck_response" :
                            String result = networkReader.readLine();
                            if (result.equals("need_update"))
                            {
                                busInfoUpdate();
                            } else
                            {
                                ConnectionS.BusInfoVerCheckOk = true;

                                Log.d("test","busInfoVerCheck : "+ (ConnectionS.BusInfoVerCheckOk?'1':'0'));
                                conCallback.busRouteLoad();  ///Exception??? -- solved
                                conCallback.busDataRefresh();
                            }
                            break;

                        case "busInfoUpdate_response" :
                            //최초 한번.
                            int busRouteSize = 0, busPointSize = 0;
                            String busInfoVer = networkReader.readLine();

                            Log.d("BusRoute","Reading Info Updating");


                            busRouteSize = Integer.parseInt(networkReader.readLine());
                            //보낸 버스 경로들의 개수를 반환합니다.
                            String BusNum;

                            for(i = 0; i < busRouteSize; i++) //버스 경로개수만큼 반복
                            {
                                BusNum = networkReader.readLine(); //버스 번호
                                ConnectionS.BusList.add(BusNum); // 버스 리스트에 버스번호를 넣어줍니다.
                                busPointSize = Integer.parseInt(networkReader.readLine()); //경로 포인트 개수
                                ConnectionS.BusRouteList.put(BusNum, new ArrayList<Point>());

                                for(j = 0; j < busPointSize; j++)
                                {
                                    lat = Double.parseDouble(networkReader.readLine());
                                    lng = Double.parseDouble(networkReader.readLine());
                                    ConnectionS.BusRouteList.get(BusNum).add(new Point(lat,lng));
                                    if (i == 3 && j == 12)
                                    {
                                        Log.d("LATLNG","ServerSended(re-parsed) : "+Double.toString(lat)+", "+Double.toString(lng));
                                    }
                                }
                            }
                            //여기까지 됬으면 List Route 다 됬겠지?? 그리고 busList 자체도 ㅇㅇ

                            ConnectionS.busListLoaded = true;

                            conCallback.busRouteSave(busInfoVer);
                            conCallback.busDataRefresh();
                            break; //버스 경로개수,버스이름,버스경로포인트수,버스포인트Lat,버스포인트Lng...


                        case "error_unknown":
                            //TODO : 에러 메시지 박스 만들기. Message 에서 String 인자값 전달.
                            conCallback.msg_show_monoButton("로그인 에러인듯 합니다.");
                            break;

                        case "bus_login_response" :
                            int busNum = Integer.parseInt(networkReader.readLine());
                            String tel = networkReader.readLine();
                            boolean isFF = Boolean.parseBoolean(networkReader.readLine());
                            boolean running = Boolean.parseBoolean(networkReader.readLine());
                            int peop = Integer.parseInt(networkReader.readLine());
                            String BBBid = networkReader.readLine();
                            String Notifi = networkReader.readLine();

                            ConnectionS.myBusData = new BusData(busNum, tel, isFF, running, peop, BBBid, Notifi);
                            ConnectionS.busThis = busNum;

                            out.println("tid_request"); //임시아이디 주세요
                            StaticsVar.loginStatus = true;
                            break;

                        default:
                            break;
                    }
                    error = 0;
                } catch (Exception e) {
                    e.printStackTrace();
                    error ++;

                    if (error > 30) {
                        stopService(ConnectionS.service);
                    }
                } finally {
                    //Log.d("test", "Ended Epoach");
                }
            }
        }
    }

    //Service Defined Function
    public void sendNewUser(String Name, String PW, String tel) //신규 사용자 회원가입..
    {
        out.println("newuser_request");
        out.println(Name);
        out.println(PW);
        out.println(tel);
    }

    public void accountLogin(String Uid)
    {
        try{
            out.println("log_in_request");
            out.println(Uid);}
        catch(Exception e) {e.printStackTrace();}
    }

    public void busInfoUpdateCheck(String version)
    {
        out.println("busInfoUpdateCheck_request");
        out.println(version);
    }

    public void busLatLngUpdate(double lat, double lng) {
        out.println("busLatLngUpdate_request");
        out.println(lat);
        out.println(lng);
    }

    public void busInfoUpdate()
    {
        out.println("busInfoUpdate_request");
    }

    public void requestStationData()
    {
        Log.d("test","station data requesting");
        out.println("station_data_request");
    }

    public void busLogin(String Bid) {
        out.println("bus_login_request");
        out.println(Bid);
    }

    public void alarmUserRequest(String Sid) {
        out.println("alarm_station_request");
        out.println(Sid);
    }

    public void busLateAlarm() {
        out.println("bus_late_alarm_request");
        out.println(ConnectionS.busThis);
    }
}
