package com.chinamobile.ping;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;


import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;


/**
 * Created by liangzhongtai on 2018/5/21.
 */

public class Pings extends CordovaPlugin {
    public final static String TAG = "CSignals_Plugin";
    public final static int RESULTCODE_PERMISSION = 100;
    public final static int RESULTCODE_PHONE_PROVIDER = 200;
    public final static int ONE = 0;
    public final static int CLOSE = 1;
    public final static int LISTENER = 2;
    public final static int LISTENER_COUNT = 3;

    public final static int PINGING     = 0;
    public final static int PING_FINISH = 1;
    public final static int PINGING_ERROR  = 2;


    public CordovaInterface cordova;
    private CallbackContext callbackContext;
    //插件动作：0，测试一次Ping；1，关闭测试；2，长时测试Ping;3, 测试指定次数的Ping
    public int pingType;
    //测试ip
    public String pingIP;
    //测试次数
    public int pingCout;
    //每次测试的发包数
    public int packageCout;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        Log.d(TAG,"执行方法Pings");
        pingCout = 1;
        packageCout = 4;
        if ("coolMethod".equals(action)) {
            pingType = args.getInt(0);
            pingIP   = args.getString(1);
            if(args.length()>2);
            pingCout = args.getInt(2);
            if(args.length()>3)
            packageCout = args.getInt(3);
            PingsUtil.getInstance().removePingsListener();
            PingsUtil.getInstance().init(pingIP,pingCout,pingType,packageCout);
            //权限
            try {
                if(!PermissionHelper.hasPermission(this,Manifest.permission.ACCESS_NETWORK_STATE)) {
                    PermissionHelper.requestPermissions(this,RESULTCODE_PERMISSION,new String[]{
                            Manifest.permission.ACCESS_NETWORK_STATE
                    });
                }else{
                    startWork();
                }
            } catch (Exception e) {
                //权限异常
                callbackContext.error("Ping测试功能异常");
                manager(false);
                return true;
            }

            return true;
        }
        return super.execute(action, args, callbackContext);
    }

    @Override
    public Bundle onSaveInstanceState() {
        return super.onSaveInstanceState();
    }

    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
    }

    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_DENIED) {
                callbackContext.error("缺少网络权限,无法进行Pings测试");
                return;
            }
        }
        switch (requestCode) {
            case RESULTCODE_PERMISSION:
               startWork();
                break;
            default:
                break;
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == RESULTCODE_PHONE_PROVIDER) {
            startWork();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PingsUtil.getInstance().removePingsListener();
    }

    private void startWork() {
        Log.d(TAG,"开启监听");
        if(pingType == CLOSE){

        }else {
            PingsUtil.getInstance().listener = new PingsListener() {
                @Override
                public void sendPingsMessage(JSONArray array) {
                    Pings.this.sendPingsMessage(array);
                }

                @Override
                public void sendFaileMessage(String meassage) {
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR,meassage);
                    pluginResult.setKeepCallback(pingType==LISTENER||pingType==LISTENER_COUNT);
                    callbackContext.sendPluginResult(pluginResult);
                }
            };
            PingsUtil.getInstance().start();
        }
    }

    private void manager(boolean start){
       if(start){
           PingsUtil.getInstance().start();
       }else{
           PingsUtil.getInstance().removePingsListener();
       }
    }

    public void sendPingsMessage(JSONArray array) {
        Log.d(TAG,"Pings="+array);
        //array数组:
        //0.pingType:0=监听一次,1=关闭,2=长时监听,3=监听多次;
        //1.status:0=正在测试，1=测试完成=2，测试中异常;
        //2.即时丢包率;3.即时延时/ms;4.平均丢包率；5.平均延时/ms
        PluginResult pluginResult;
        if(array==null&&pingType<LISTENER){
            pluginResult = new PluginResult(PluginResult.Status.ERROR,"无法获取Ping测试结果");
        }else{
            try {
                array.put(13,pingType);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            pluginResult = new PluginResult(PluginResult.Status.OK,array);
        }
        pluginResult.setKeepCallback(pingType==LISTENER||pingType==LISTENER_COUNT);
        callbackContext.sendPluginResult(pluginResult);
    }

    public interface PingsListener{
        void sendPingsMessage(JSONArray array);
        void sendFaileMessage(String meassage);
    }
}
