package com.chinamobile.ping;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Created by liangzhongtai on 2018/5/22.
 */

public class PingsUtil {
    private volatile static PingsUtil uniqueInstance;
    //Cordova监听器
    public Pings.PingsListener listener;
    public String pingIP;
    public int pingCout;
    public int pingType;
    public int packageCout;
    public float toalLost;
    public float toalDelay;
    public boolean stop;

    private PingsUtil() {
    }

    //采用Double CheckLock(DCL)实现单例
    public static PingsUtil getInstance() {
        if (uniqueInstance == null) {
            synchronized (PingsUtil.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new PingsUtil();
                }
            }
        }
        return uniqueInstance;
    }

    public void init(String pingIP,int pingCout,int pingType,int packageCout){
        uniqueInstance.pingIP = pingIP;
        uniqueInstance.pingCout = pingCout;
        uniqueInstance.pingType = pingType;
        uniqueInstance.packageCout = packageCout;
    }

    //初始化LTE信号
    public void getPings(boolean finish,int count) {
        float lost = 0.0f;
        float delay = 0.0f;
        Process p = null;
        JSONArray array = new JSONArray();
        try {
            p = Runtime.getRuntime().exec("ping -c "+packageCout+" " + pingIP);
            BufferedReader buf = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String str;
            while((str=buf.readLine())!=null){
                Log.d(Pings.TAG,"Pings="+str);
                Log.d(Pings.TAG,"Pings="+str.contains("packet loss"));
                if(str.contains("packet loss")){
                    int i= str.indexOf("received");
                    int j= str.indexOf("%");
                    Log.d(Pings.TAG,"Pings="+"丢包率:"+str.substring(i+10, j));
                    lost = Float.valueOf(str.substring(i+10, j));
                }

                if(str.contains("avg")){
                    int i= str.indexOf("/", 20);
                    int j= str.indexOf(".", i);
                    Log.d(Pings.TAG,"Pings="+"延迟:"+str.substring(i+1, j));
                    delay = Float.valueOf(str.substring(i+1, j));
                }
            }
            if(listener!=null){
                toalLost  += lost;
                toalDelay += delay;
                sendMessage(pingType,finish?Pings.PING_FINISH:Pings.PINGING,lost,delay,toalLost/count,toalDelay/count);
            }
            p.destroy();
        } catch (Exception e) {
            e.printStackTrace();
            if(listener!=null){
                try {
                    array.put(0,pingType);
                    array.put(1,Pings.PINGING_ERROR);
                    array.put(2,0);
                    array.put(3,0);
                    array.put(4,0);
                    array.put(5,0);
                    sendMessage(pingType,Pings.PINGING_ERROR,0.0f,0.0f,0.0f,0.0f);
                } catch (JSONException e1) {
                    e1.printStackTrace();
                }
                listener.sendPingsMessage(array);
            }
        }
    }

    //开始Ping测试
    public void start() {
        stop = false;
        toalLost = 0;
        toalDelay = 0;
        if(pingType==Pings.ONE) {
            new Thread(() ->
                getPings(true, 1)
            ).start();
        }else if(pingType==Pings.LISTENER){
            new Thread(() -> {
                int count = 0;
                while (!stop){
                    count++;
                    getPings(false,count);
                }
            }).start();
        }else if(pingType==Pings.LISTENER_COUNT){
            new Thread(() -> {
                int count = 0;
                while (count < pingCout&&!stop) {
                    count++;
                    getPings(count == pingCout, count);
                }
            }).start();
        }
    }


    //移除Ping监听
    public void removePingsListener() {
        stop = true;
        uniqueInstance = null;
        listener = null;
    }


    private void sendMessage(int pingType,int status,float lost,float delay,float averLost,float averDelay){
        Bundle bundle = new Bundle();
        bundle.putInt("pingType", pingType);
        bundle.putInt("status",  status);
        bundle.putFloat("lost",  lost);
        bundle.putFloat("delay",  delay);
        bundle.putFloat("averLost", averLost);
        bundle.putFloat("averDelay", averDelay);
        Message msg = new Message();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private JSONArray getJSONArray(Message msg){
        Bundle bundle = msg.getData();
        JSONArray array = new JSONArray();
        try {
            array.put(0,bundle.getInt("pingType"));
            array.put(1,bundle.getInt("status"));
            array.put(2,bundle.getFloat("lost"));
            array.put(3,bundle.getFloat("delay"));
            array.put(4,bundle.getFloat("averLost"));
            array.put(5,bundle.getFloat("averDelay"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return  array;
    }

    private Handler mHandler = new Handler(){
        public void dispatchMessage(Message msg) {
            if(listener!=null){
                listener.sendPingsMessage(getJSONArray(msg));
            }
        }
    };
}
