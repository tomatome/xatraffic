package com.tomato.xatraffic;

import android.util.Log;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;


/**
 * Created by renwei on 2017/1/13.
 */

public class DataCenter {
    private String baseUrl =  "http://www.xaglkp.com.cn/";
    private String busPageUrl = baseUrl + "BusPage/bus_realtime";
    private String busRealUrl = baseUrl + "Bus/GetBusLineByName?buslinename=";
    private String busDataUrl = baseUrl + "Bus/GetRealBusLine";
    private static String key = "ac0dbb002f0c63517e3c6249c114c8f8==0f48be4c86a58c97c6a2340e674a0c0c";
    private String aMapUrl = "http://restapi.amap.com/v3";
    private HttpURLConnection urlConnection;
    private URL reqUrl;
    private static String RequestVerificationToken="__RequestVerificationToken\" type=\"hidden\" value=";
    private char[] token;
    private boolean hasToken = true;
    private Gson gson = null;
    String r = "https://web.chelaile.net.cn/cdatasource/xianQuery?type=1&key=311%E8%B7%AF&lat=&lng=&showName=311%E8%B7%AF";
    public DataCenter() {
        this.gson = new Gson();
    }

    private String doPost(String url, String data) {
        String result = "";

        try {
            reqUrl = new URL(url);
            // 根据URL对象打开链接
            urlConnection = (HttpURLConnection) reqUrl.openConnection();
            // 设置请求的方式
            urlConnection.setRequestMethod("POST");
        } catch (MalformedURLException e) {
            System.out.println("doPost fail:"+ e);
            e.printStackTrace();
        } catch (ProtocolException e) {
            System.out.println("doPost fail:"+ e);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("do Get/Post fail:"+ e);
            e.printStackTrace();
        }
        urlConnection.setReadTimeout(5000);
        urlConnection.setConnectTimeout(5000);
        // 设置请求的头
        urlConnection.setRequestProperty("Connection", "keep-alive");
        // 设置请求的头
        urlConnection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");

        // 设置请求的头
        urlConnection.setRequestProperty("Content-Length",
                    String.valueOf(data.getBytes().length));
        // 设置请求的头
        urlConnection
                    .setRequestProperty("User-Agent",
                            "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:27.0) Gecko/20100101 Firefox/27.0");
        urlConnection.setDoOutput(true); // 发送POST请求必须设置允许输出
        urlConnection.setDoInput(true); // 发送POST请求必须设置允许输入, setDoInput的默认值就是true

        try {
            OutputStream os = urlConnection.getOutputStream();
            os.write(data.getBytes());
            os.flush();

            if (urlConnection.getResponseCode() == 200) {
                // 获取响应的输入流对象
                InputStream is = urlConnection.getInputStream();
                // 返回字符串
                result = inputStreamToString(is, false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private String doGet(String url, boolean filter){
        String result = "";
        try {
            reqUrl = new URL(url);
            // 根据URL对象打开链接
            urlConnection = (HttpURLConnection) reqUrl.openConnection();
            // 设置请求的方式
            urlConnection.setRequestMethod("GET");
            urlConnection.setRequestProperty("Charset", "UTF-8");
            //urlConnection.setRequestProperty("Accept-Encoding", "identity");
            // 设置文件类型
            //urlConnection.setRequestProperty("Content-Type", "text/xml; charset=UTF-8");
            if(urlConnection.getResponseCode() == 200) {
                InputStream is = urlConnection.getInputStream();
                result = inputStreamToString(is,filter);
            }
        } catch (ProtocolException e1) {
            e1.printStackTrace();
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return result;
    }

    private String inputStreamToString(InputStream is, boolean filter) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        // 定义读取的长度
        int len = 0, allLen=0;
        // 定义缓冲区
        byte buffer[] = new byte[10240];
        // 按照缓冲区的大小，循环读取
        try {
            while ((len = is.read(buffer)) > 0) {
                allLen += len;
                if (filter) {
                    if(allLen < 4000)
                        continue;
                }
                // 根据读取的长度写入到os对象中
                byteOut.write(buffer, 0, len);
            }
            // 释放资源
            is.close();
            byteOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 返回字符串
        return new String(byteOut.toByteArray());

    }

    public boolean updateVerifyToken() {
        String result = doGet(busPageUrl, true);
        if (result == "") {
            Log.d("debug", "updateVerifyToken: failed");
            hasToken = false;
            return false;
        }

        token = new char[108];
        int len = RequestVerificationToken.length();
        int n = result.indexOf(RequestVerificationToken);
        result.getChars(n+len+1,n+len+109,token,0);
        Log.d("debug", "updateVerifyToken: "+token);
        hasToken = true;
        return true;
    }

    public BusLine[] getBusLine(String route) {
        String result = doGet(busRealUrl+route, false);
        if (result == "") {
            Log.d("debug", "getBusData: can't get realBus");
            return null;
        }
        Log.d("debug", "getBusLine: "+result);
        BusLine[] bus = gson.fromJson(result,BusLine[].class);
        if (bus.length == 0) {
            Log.d("debug", "getBusData: can't get realBus fromJson");
            return null;
        }
        return bus;
    }

    public BusData getBusData(String realRouteId) {
        String data = "routeid=" + realRouteId;
        String result = doPost(busDataUrl, data);
        if (result == "") {
            Log.d("debug", "getBusData: can't get Busdata");
            return null;
        }
        BusData bData = gson.fromJson(result, BusData.class);
        Log.d("debug", "getBusData:"+result);
        return bData;
    }


}
