package com.tomato.xatraffic;

/**
 * Created by renwei on 2017/1/13.
 */

public class BusLine {
    private String ROUTEID;
    private String ROUTENAME;
    private String COMPARELINEID;
    private String COMPARELINESERIAL;

    public String getCOMPARELINESERIAL() {
        return COMPARELINESERIAL;
    }

    public void setCOMPARELINESERIAL(String COMPARELINESERIAL) {
        this.COMPARELINESERIAL = COMPARELINESERIAL;
    }

    public boolean isLive() {
        return IsLive;
    }

    public void setLive(boolean live) {
        IsLive = live;
    }

    private boolean IsLive;

    public String getROUTEID() {
        return ROUTEID;
    }

    public void setROUTEID(String ROUTEID) {
        this.ROUTEID = ROUTEID;
    }

    public String getROUTENAME() {
        return ROUTENAME;
    }

    public void setROUTENAME(String ROUTENAME) {
        this.ROUTENAME = ROUTENAME;
    }

    public String getCOMPARELINEID() {
        return COMPARELINEID;
    }

    public void setCOMPARELINEID(String COMPARELINEID) {
        this.COMPARELINEID = COMPARELINEID;
    }
}
