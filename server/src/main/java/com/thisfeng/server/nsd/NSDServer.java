package com.thisfeng.server.nsd;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;


/**
 * Created by zWX506486 on 2017/12/9.
 * Description:
 */

public class NSDServer {
    public static final String TAG = "NSDServer";
    private NsdManager mNsdManager;
    private NsdManager.RegistrationListener mRegistrationListener;
    private String mServerName;
    private Context mContext;
    private int mPort;
    private String mServiceName;
    private final String mServerType = "_http._tcp.";  // 服务器type，要客户端扫描服务器的一致

    public NSDServer() {
    }

    public void startNSDServer(Context context, String serviceName, int port) {
        initializeRegistrationListener();
        registerService(context, serviceName, port);
    }

    //实例化注册监听器
    private void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {
            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "NsdServiceInfo onRegistrationFailed");
                if (registerState != null) {
                    registerState.onRegistrationFailed(serviceInfo, errorCode);
                }
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.i(TAG, "onUnregistrationFailed serviceInfo: " + serviceInfo + " ,errorCode:" + errorCode);
                if (registerState != null) {
                    registerState.onUnregistrationFailed(serviceInfo, errorCode);
                }
            }

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                mServerName = serviceInfo.getServiceName();
                Log.i(TAG, "onServiceRegistered: " + serviceInfo.toString());
                Log.i(TAG, "mServerName onServiceRegistered: " + mServerName);
                if (registerState != null) {
                    registerState.onServiceRegistered(serviceInfo);
                }
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
                Log.i(TAG, "onServiceUnregistered serviceInfo: " + serviceInfo);
                if (registerState != null) {
                    registerState.onServiceUnregistered(serviceInfo);
                }
            }
        };
    }

    private void registerService(Context context, String serviceName, int port) {
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        NsdServiceInfo serviceInfo = new NsdServiceInfo();
        serviceInfo.setServiceName(serviceName);
        serviceInfo.setPort(port);
        serviceInfo.setServiceType(mServerType);
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    public void stopNSDServer() {
        mNsdManager.unregisterService(mRegistrationListener);
    }


    //NSD服务注册监听接口
    public interface IRegisterState {
        void onServiceRegistered(NsdServiceInfo serviceInfo);     //注册NSD成功

        void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode);   //注册NSD失败

        void onServiceUnregistered(NsdServiceInfo serviceInfo);  //取消NSD注册成功

        void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode); //取消NSD注册失败

    }

    //NSD服务接口对象
    private IRegisterState registerState;


    //设置NSD服务接口对象
    public void setRegisterState(IRegisterState registerState) {
        this.registerState = registerState;
    }
}
