package com.miniwindow.station_here_driver;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by USER on 2017-10-01.
 */

public class InterDB extends SQLiteOpenHelper {

    private Context context;

    // DBHelper 생성자로 관리할 DB 이름과 버전 정보를 받음
    public InterDB(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        this.context = context;
    }

    // DB를 새로 생성할 때 호출되는 함수
    @Override
    public void onCreate(SQLiteDatabase db) {
        // 새로운 테이블 생성
        /* 이름은 MONEYBOOK이고, 자동으로 값이 증가하는 _id 정수형 기본키 컬럼과
        item 문자열 컬럼, price 정수형 컬럼, create_at 문자열 컬럼으로 구성된 테이블을 생성. */
        try {
            db.execSQL("CREATE TABLE BUSROUTEDB (_id INTEGER PRIMARY KEY AUTOINCREMENT, routeNum INTEGER, routeOrder INTEGER, lat TEXT, lng TEXT);");
            db.execSQL("CREATE TABLE BUSROUTENUMDB (_id INTEGER PRIMARY KEY AUTOINCREMENT, busNum INTEGER);");
        } catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    // DB 업그레이드를 위해 버전이 변경될 때 호출되는 함수
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Toast.makeText(context, "업그레이드 중입니다.", Toast.LENGTH_SHORT).show();
    }

    public void insertBusRouteNum(ArrayList<String> BusList)
    {
        SQLiteDatabase db = getWritableDatabase();
        for(int i = 0; i < BusList.size(); i++)
        {
            db.execSQL("INSERT INTO BUSROUTENUMDB VALUES(null, '"+BusList.get(i)+"');");
        }
    }

    public ArrayList<String> getBusRouteNum()
    {
        SQLiteDatabase db = getReadableDatabase();
        ArrayList<String> tempBusList = new ArrayList<>();

        Cursor cursor = db.rawQuery("SELECT busNum FROM BUSROUTENUMDB;", null);
        while (cursor.moveToNext()) {
            tempBusList.add(cursor.getString(0));
        }

        return tempBusList;
    }

    public void insertBusRoute(int routeNo, int routeOrder, String lat, String lng) {
        // 읽고 쓰기가 가능하게 DB 열기
        SQLiteDatabase db = getWritableDatabase();
        // DB에 입력한 값으로 행 추가
        db.execSQL("INSERT INTO BUSROUTEDB VALUES(null, '"+routeNo+"', '"+routeOrder+"', '"+lat+"', '"+lng+"');");
    }

    /*public void update(String item, int price) {
        SQLiteDatabase db = getWritableDatabase();
        // 입력한 항목과 일치하는 행의 가격 정보 수정
        db.execSQL("UPDATE BUSROUTEDB SET price=" + price + " WHERE item='" + item + "';");
        db.close();
    }

    public void delete(String item) {
        SQLiteDatabase db = getWritableDatabase();
        // 입력한 항목과 일치하는 행 삭제
        db.execSQL("DELETE FROM BUSROUTEDB WHERE item='" + item + "';");
        db.close();
    }*/

    public void deleteBusRoute()
    {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE BUSROUTEDB;");
    }

    public Map<String, ArrayList<Point>> getBusRoute(){
        Map<String, ArrayList<Point>> tempBusRouteList = new HashMap<>();
        SQLiteDatabase db = getReadableDatabase();

        int i;
        for(i = 0; i < ConnectionS.BusList.size(); i++)
        {
            tempBusRouteList.put(ConnectionS.BusList.get(i), new ArrayList<Point>());
            Cursor cursor = db.rawQuery("SELECT lat, lng FROM BUSROUTEDB WHERE routeNum = '"+ConnectionS.BusList.get(i)+"';",null);
            while(cursor.moveToNext()){
                tempBusRouteList.get(ConnectionS.BusList.get(i)).add(new Point(Double.parseDouble(cursor.getString(0)), Double.parseDouble(cursor.getString(1))));
            }
        }

        return tempBusRouteList;
    }

    public void closeDB(){

    }


    /*public String getResult() {
        // 읽기가 가능하게 DB 열기
        SQLiteDatabase db = getReadableDatabase();
        String result = "";

        // DB에 있는 데이터를 쉽게 처리하기 위해 Cursor를 사용하여 테이블에 있는 모든 데이터 출력
        Cursor cursor = db.rawQuery("SELECT * FROM MONEYBOOK", null);
        while (cursor.moveToNext()) {
            result += cursor.getString(0)
                    + " : "
                    + cursor.getString(1)
                    + " | "
                    + cursor.getInt(2)
                    + "원 "
                    + cursor.getString(3)
                    + "\n";
        }

        return result;
    }*/
}


