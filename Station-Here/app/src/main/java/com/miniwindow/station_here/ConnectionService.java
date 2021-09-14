package com.miniwindow.station_here;


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

    private String ip = "10.1.40.31"; //169.2 6654.9.185; 10.1.92.137; 203.232.74.87; 10.0.2.2 (local <></>est);
    // IP
    //Test
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
        void saveUid(String Uid);
        void resumeActivity();
        void saveTempId(String tempId);
        void msg_connection_failed();
        void msg_show_monoButton(String msg);
        void toast_show_short(String msg);
        void stationDataRefresh();
        void busRouteSave(String busInfoVer);
        void busRouteLoad();
        void busDataRefresh();
        void msg_get_forward();
        void clearMap();
        void dismissDialog();
        void alarmBusComing();
        void msg_show_toast(String msg);
        void alarmBusLate();
        void msg_show_station_far();
        void stationDestroy(String sid);
        void stationCreated(String s, double lt, double ln, boolean i, int r);
        void stationDataUpdate(String s, int p);
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

    public void connectionEstblish(){
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
                            conCallback.saveTempId(networkReader.readLine());
                            Log.d("resume","resume to map");
                            StaticsVar.tempIdGetStatus = true;
                            conCallback.resumeActivity();
                            break;
                        //항상 임시 id 발급 이후에 resume 시킨다.
                        case "log_in_response":
                            out.println("tid_request"); //임시아이디 주세요
                            StaticsVar.loginStatus = true;
                            break;
                        //로그인 응답(성공)

                        case "station_destroyed" :
                            String sid = networkReader.readLine();
                            Log.d("test","message destroy received");
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

                        case "newuser_response":
                            String Uid = networkReader.readLine();
                            conCallback.saveUid(Uid);
                            Log.d("test", "saved UID");
                            accountLogin(Uid);
                            //save Own ID
                            break;
                        //새 유저 요청

                        case "newuser_response_failed" :
                            conCallback.msg_show_monoButton("계정 등록에 실패하였습니다. (원인 : 동일한 전화번호 혹은 아이디가 존재합니다)");
                            break;


                        case "station_data_response" :
                            //최초 한번만
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

                            conCallback.stationDataRefresh(); // station Datat refresheing
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

                        case "bus_late_alarm" :
                            int line = Integer.parseInt(networkReader.readLine());
                            int ii = 0;
                            for (i = 0; i < ConnectionS.StationArray.size(); i++) {
                                if (ConnectionS.StationArray.get(i).stationId.equals(ConnectionS.stationIn)){ //현정류장을 i 값 빼오기
                                    ii = i;
                                }
                            }
                            if (ConnectionS.StationArray.get(ii).routeNum == line) {
                                conCallback.alarmBusLate();
                                Log.d("test","BUS!! LATE!!!");
                            }
                            break;

                        case "station_create_response" :
/*
                            Log.d("test","station create responding...");
                            dataSize = Integer.parseInt(networkReader.readLine());

                            for(i = 0; i < dataSize; i++) {
                                lat = Double.parseDouble(networkReader.readLine());
                                lng = Double.parseDouble(networkReader.readLine());
                                stationId = networkReader.readLine();
                                people = Integer.parseInt(networkReader.readLine());
                                isForward = Boolean.parseBoolean(networkReader.readLine());
                                routeNum = Integer.parseInt(networkReader.readLine());

                                ConnectionS.StationArray.add(new StationPoint(lat,lng,stationId,people,isForward,routeNum));
                            } //정류장 정보 업데이트 이 시점에서 posLat, posLng은 이미 생성위치로 정해짐 ㅇㅇ
                            */
                            //-----2017/11/21 일자로 필요없어진 코드

                            int OKOK = 0;

                            for( i = 0 ; i < ConnectionS.StationArray.size(); i++) {
                                if (ConnectionS.StationArray.get(i).routeNum == ConnectionS.selectedBus) //선택된 노선에 한에서만 체킹한다
                                {
                                    float d[] = new float[1];
                                    Location.distanceBetween(ConnectionS.StationArray.get(i).lat,ConnectionS.StationArray.get(i).lng,ConnectionS.posLat,ConnectionS.posLng,d);
                                    if ( d[0] < 300 ) { //300 미터 보다 작으면...?
                                        OKOK = 1;
                                        Log.d("test","little than 300 meters");
                                    }
                                }
                            }

                            if (OKOK == 1)
                            {
                                Log.d("test","cannot create. there are other station nearby 300m");
                                conCallback.msg_show_monoButton("근처 정류장으로 300m 이내는 생성할 수 없습니다.");
                            } else
                            {
                                Log.d("test","station create OK");
                                ConnectionS.getForward = false;
                                conCallback.msg_get_forward(); //멈출 방법찾기예에에에에에 (찾음)
                            }
                            break;

                        case "station_create_return_id" : //정류장 생성 완료
                            //TODO : 정류장 id 받고 ConnectionS에 저장하기. 그리고 현재 존재정류장 빨간색 마커로 하기. 300m 체킹 시작합니다람쥐위위위위
                            ConnectionS.stationIn = networkReader.readLine();
                            //requestStationData(); //여기서 이미 station data refresh 작동된다.
                            //conCallback.clearMap();
                            //conCallback.busDataRefresh();
                            break;

                        case "stationDataUpdate" :
                            String __sid = networkReader.readLine();
                            int _people = Integer.parseInt(networkReader.readLine());
                            conCallback.stationDataUpdate(__sid,_people);
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

                        case "some_error" :
                            conCallback.msg_show_monoButton("무언가 에러가 발생했습니다.");
                            break;

                        case "station_outing_error" :
                            conCallback.msg_show_monoButton("정류장에서 빠져나가는 과정에 에러가 발생했습니다.");
                            break;

                        case "station_outing_response" :
                            //ConnectionS.StationArray.get(ConnectionS.StationArray.indexOf(ConnectionS.stationIn)).people -= 1;
                            ConnectionS.stationIn = "A";
                            //conCallback.clearMap();
                            //conCallback.busDataRefresh();
                            //requestStationData();
                            break;

                        case "bus_coming_alarm" :
                            Log.d("test", "bus arriving");
                            conCallback.alarmBusComing(); //TODO : 이제 효과음 같은거 넣기...

                            Log.d("test", "Station Remain Request");
                            float[] f = new float[1];
                            int in = 0;
                            for( i = 0; i < ConnectionS.StationArray.size(); i++)
                            {
                                if (ConnectionS.StationArray.get(i).stationId.equals(ConnectionS.stationIn)) { //현재 들어가있는 정류장 계산
                                    in = i;
                                }
                            }
                            Location.distanceBetween(ConnectionS.userLat, ConnectionS.userLng, ConnectionS.StationArray.get(in).lat,ConnectionS.StationArray.get(in).lng,f);

                            if (f[0] > 100) { //사용자가 정류장에 없다????!!?!??!
                                goOutStation(ConnectionS.stationIn); //자동으로 스테이션 내보낸다.
                                Log.d("test", "User is not here station");
                                conCallback.msg_show_toast("버스 도착 중, 정류장에서 거리가 너무 멀어 자동으로 정류장에서 나가졌습니다.");
                                conCallback.msg_show_station_far();
                            }
                            break;

                        case "station_none_error" :
                            conCallback.msg_show_monoButton("존재하지 않는 정류장 입니다!");
                            break;

                        case "station_joining_response" :
                            if (networkReader.readLine().equals("joining_OK"))
                            {
                                Log.d("test","dialog dismiss");
                                ConnectionS.stationIn = networkReader.readLine();
                            } else {
                                conCallback.msg_show_monoButton("정류장 합류 과정에서 에러가 발생했습니다.");
                            }
                            //conCallback.clearMap();
                            //conCallback.busDataRefresh();
                            //conCallback.stationDataRefresh();
                            conCallback.dismissDialog();
                            break;

                        case "bus_location_response" :
                            String Bid = networkReader.readLine();
                            ConnectionS.bus1lat = Double.parseDouble(networkReader.readLine());
                            ConnectionS.bus1lng = Double.parseDouble(networkReader.readLine());
                            ConnectionS.busLocUpated = true;
                            Log.d("test","Bus Location Updated");
                            //conCallback.clearMap();
                            //conCallback.stationDataRefresh();
                            //conCallback.busDataRefresh(); //버스 경로 리프레슁
                            break;

                        /*case "station_remain_request" :
                            Log.d("test", "Station Remain Request");
                            float[] f = new float[1];
                            int in = 0;
                            for( i = 0; i < ConnectionS.StationArray.size(); i++)
                            {
                                if (ConnectionS.StationArray.get(i).stationId.equals(ConnectionS.stationIn)) { //현재 들어가있는 정류장 계산
                                    in = i;
                                }
                            }
                            Location.distanceBetween(ConnectionS.userLat, ConnectionS.userLng, ConnectionS.StationArray.get(in).lat,ConnectionS.StationArray.get(in).lng,f);

                            if (f[0] > 300) { //사용자가 정류장에 없다????!!?!??!
                                goOutStation(ConnectionS.stationIn); //자동으로 스테이션 내보낸다.
                                Log.d("test","User is not here station");
                                conCallback.msg_show_toast("버스 도착 중, 정류장에서 거리가 너무 멀어 자동으로 정류장에서 나가졌습니다.");
                                conCallback.msg_show_station_far();
                            }
                            break;*/

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

    public void busInfoUpdate()
    {
        out.println("busInfoUpdate_request");
    }

    public void sendHello()
    {
        out.println("Hello");
    }

    public void requestStationData()
    {
        out.println("station_data_request");
    }

    public void newStationCreate() {
        out.println("station_create_request");
    }

    public void goOutStation(String station) {
        int i;
        for(i = 0; i < ConnectionS.StationArray.size(); i++) {
            if (ConnectionS.StationArray.get(i).stationId.equals(station)) {
                break;
            }
        }
        out.println("station_outing_request");
        out.println(station);
        out.println(ConnectionS.StationArray.get(i).people);
    }

    public void joinStation(String station) {
        out.println("station_joining_request");
        out.println(station);
    }

    public void newStationCreateOK()
    {
        out.println("station_create_checked_ok");
        out.println(ConnectionS.posLat);
        out.println(ConnectionS.posLng);
        out.println(ConnectionS.isF);
        out.println(ConnectionS.selectedBus);
    }

    public void busLocationUpdate(String Bid) {
        Log.d("test","bus location request");
        out.println("bus_location_request");
        out.println(Bid);
    }

    public void busDataUpdate(String Bid) {
        out.println("bus_data_request");
        out.println(Bid);
    }
}
