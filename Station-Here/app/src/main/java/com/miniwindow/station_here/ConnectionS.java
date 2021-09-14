package com.miniwindow.station_here;

import android.app.Application;
import android.content.Intent;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by USER on 2017-09-04.
 */

public class ConnectionS extends Application{
    public static ConnectionService serviceConnection;
    public static ArrayList<StationPoint> StationArray = new ArrayList<>();
    public InterDB interDB;
    public static ArrayList<String> BusList = new ArrayList<>(); //버스 리스트 (경로)
    public static Map<String, ArrayList<Point>> BusRouteList = new HashMap<>(); //버스 경로 실제 지점 저장 (포인트)
    public static Map<String, BusData> BusDataList = new HashMap<>();
    public static ArrayList<String> BusIdList = new ArrayList<>(); //버스 아이디 리스트
    public static boolean BusInfoVerCheckOk = false;
    public static Intent service;
    public static boolean busListLoaded = false;
    public static boolean getForward = false;
    public static boolean busGoOut = false;
    public static boolean busLocUpated = false;

    //실제 기능 수행 변수-----------------------

    public static double posLat = 0;
    public static double posLng = 0;
    public static double userLat = 0;
    public static double userLng = 0;
    public static int selectedBus = 13; //선택됬다고 가정 ㅇㅇ. 0은 선택 안된거임.
    public static boolean stationCreating = false;
    public static boolean isF = false;
    public static String stationIn = "A";

    public static double bus1lat = 126.635;
    public static double bus1lng = 36.896;

}
