package com.miniwindow.station_here_driver;

/**
 * Created by USER on 2017-10-11.
 */

public class BusData {
    int busNum;
    String tel;
    boolean isForward;
    boolean running;
    int people;
    String Bid;
    String Notification;

    BusData(int busNum, String tel, boolean isForward, boolean running, int people, String Bid, String Notification){
        this.busNum = busNum;
        this.tel = tel;
        this.isForward = isForward;
        this.running = running;
        this.people = people;
        this.Bid = Bid;
        this.Notification = Notification;
    }
}
