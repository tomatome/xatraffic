package com.tomato.xatraffic;

import java.util.List;

/**
 * Created by renwei on 2017/1/13.
 */

public class BusData {
    private int run_nums; //总车辆
    private int avg_interval; //平均发车时间
    private boolean has_real_bus; //真实车辆
    private String TicketSystem; //票价
    private boolean IsSwipe; //可刷卡
    private String message; //请求成功
    private BusLine busLine;
    private boolean inUp;

    public boolean isInUp() {
        return inUp;
    }

    public void setInUp(boolean inUp) {
        this.inUp = inUp;
    }

    public BusLine getBusLine() {
        return busLine;
    }

    public void setBusLine(BusLine busLine) {
        this.busLine = busLine;
    }

    public List<BusStation> getUp() {
        return up;
    }

    public void setUp(List<BusStation> up) {
        this.up = up;
    }

    public List<BusStation> getDown() {
        return down;
    }

    public void setDown(List<BusStation> down) {
        this.down = down;
    }

    private List<BusStation> up;
    private List<BusStation> down;

    public int getRun_nums() {
        return run_nums;
    }

    public void setRun_nums(int run_nums) {
        this.run_nums = run_nums;
    }

    public int getAvg_interval() {
        return avg_interval;
    }

    public void setAvg_interval(int avg_interval) {
        this.avg_interval = avg_interval;
    }

    public boolean isHas_real_bus() {
        return has_real_bus;
    }

    public void setHas_real_bus(boolean has_real_bus) {
        this.has_real_bus = has_real_bus;
    }

    public String getTicketSystem() {
        return TicketSystem;
    }

    public void setTicketSystem(String ticketSystem) {
        TicketSystem = ticketSystem;
    }

    public boolean isSwipe() {
        return IsSwipe;
    }

    public void setSwipe(boolean swipe) {
        IsSwipe = swipe;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
