package com.tomato.xatraffic;

/**
 * Created by renwei on 2017/1/26.
 */

public class BusStation {
    private String station_id;  //站编号
    private String station_name; //站名
    private int order_number; //
    private String running_type;
    private int bus_count; //车数量
    private String bus_codes;

    public String getBus_codes() {
        return bus_codes;
    }

    public void setBus_codes(String bus_codes) {
        this.bus_codes = bus_codes;
    }

    public String getStation_id() {
        return station_id;
    }

    public void setStation_id(String station_id) {
        this.station_id = station_id;
    }

    public String getStation_name() {
        return station_name;
    }

    public void setStation_nam(String station_name) {
        this.station_name = station_name;
    }

    public int getOrder_number() {
        return order_number;
    }

    public void setOrder_number(int order_number) {
        this.order_number = order_number;
    }

    public String getRunning_type() {
        return running_type;
    }

    public void setRunning_type(String running_type) {
        this.running_type = running_type;
    }

    public int getBus_count() {
        return bus_count;
    }

    public void setBus_count(int bus_count) {
        this.bus_count = bus_count;
    }
}

