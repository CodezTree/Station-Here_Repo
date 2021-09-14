package com.miniwindow.station_here;

/**
 * Created by USER on 2017-09-17.
 */

public class StationPoint{
    public StationPoint(double lat, double lng, String stationId, int people, boolean isForward, int routeNum)
    {
        this.lat = lat;
        this.lng = lng;
        this.stationId = stationId;
        this.people = people;
        this.isForward = isForward;
        this.routeNum = routeNum;
    }
    public double lat;
    public double lng;
    public String stationId;
    public int people;
    public boolean isForward;
    public int routeNum;
}