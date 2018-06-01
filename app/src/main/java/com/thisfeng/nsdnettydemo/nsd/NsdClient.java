package com.thisfeng.nsdnettydemo.nsd;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import java.net.InetAddress;
import java.util.ArrayList;


/**
 * @author thisfeng
 */

public class NsdClient {

    public static final String TAG = "NsdClient";

    /**
     * NSD_SERVICE_NAME和NSD_SERVER_TYPE需要与服务器端完全一致
     */
    private final String NSD_SERVER_TYPE = "_http._tcp.";

    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.ResolveListener mResolverListener;
    private NsdManager mNsdManager;
    private Context mContext;
    private String mServiceName;
    private IServerFound mIServerFound;

    /**
     * 用来存储解析后的网络对象列表，包含完整数据
     */
    private ArrayList<NsdServiceInfo> mNsdServiceInfoList = new ArrayList<>();

    /**
     * 未解析前搜索到的
     */
    private ArrayList<NsdServiceInfo> mNsdServiceInfoListBefore = new ArrayList<>();


    private static final int MSG_RESOLVER = 1002;

    private static final int MSG_NULL = 1003;


    int count;

    /**
     * @param context      this
     * @param serviceName  客户端扫描 指定的地址 暂时没用到
     * @param iServerFound 回调
     */
    public NsdClient(Context context, String serviceName, IServerFound iServerFound) {
        mContext = context;
        mServiceName = serviceName;
        mIServerFound = iServerFound;
    }

    public void startNSDClient() {
        mNsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);
        initializeDiscoveryListener();
        mNsdManager.discoverServices(NSD_SERVER_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
        initializeResolveListener();
    }

    /**
     * 扫描未被解析前的 NsdServiceInfo
     * 用于服务发现的回调调用接口
     */
    private void initializeDiscoveryListener() {
        mDiscoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                mNsdManager.stopServiceDiscovery(this);
                Log.e(TAG, "onStartDiscoveryFailed():");
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                mNsdManager.stopServiceDiscovery(this);
                Log.e(TAG, "onStopDiscoveryFailed():");
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.e(TAG, "onDiscoveryStarted():");

            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.e(TAG, "onDiscoveryStopped():");

            }

            /**
             *
             * @param serviceInfo
             */
            @Override
            public void onServiceFound(NsdServiceInfo serviceInfo) {

                //根据咱服务器的定义名称，指定解析该 NsdServiceInfo
                if (serviceInfo.getServiceName().equals(mServiceName)) {
                    mNsdManager.resolveService(serviceInfo, mResolverListener);
                } else {
                    mHandler.sendEmptyMessage(MSG_NULL);
                }

                Log.e(TAG, "onServiceFound():");

                mNsdServiceInfoListBefore.add(serviceInfo);

            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "onServiceLost(): serviceInfo=" + serviceInfo);

            }
        };
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_RESOLVER:

                    //回调到主线 进行解析結果的回調
                    NsdServiceInfo serviceInfo = (NsdServiceInfo) msg.obj;
                    if (mIServerFound != null) {
                        mIServerFound.onServerFound(serviceInfo, serviceInfo.getPort());
                    }
                    Log.e(TAG, " 指定onServiceFound（" + mServiceName + ")： Service Info: --> " + serviceInfo);

                    break;
                case MSG_NULL:
                    if (mIServerFound != null) {
                        mIServerFound.onServerFail();
                    }
                    break;
                default:
            }
        }
    };

    /**
     * 解析未 调用未被解析的 NsdServiceInfo
     */
    private void initializeResolveListener() {
        mResolverListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                int port = serviceInfo.getPort();
                InetAddress host = serviceInfo.getHost();
                String serviceName = serviceInfo.getServiceName();
                String hostAddress = serviceInfo.getHost().getHostAddress();
                Log.i(TAG, "onServiceResolved 已解析:" + " host:" + hostAddress + ":" + port + " ----- serviceName: " + serviceName);



                mNsdServiceInfoList.add(serviceInfo);

                //解析的结果 通过Handler发送到主线程
                Message msg = Message.obtain();
                msg.what = MSG_RESOLVER;
                msg.obj = serviceInfo;
                mHandler.sendMessageDelayed(msg, 500);


            }
        };
    }


    public void stopNSDServer() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    public interface IServerFound {

//                void onServerFound(InetAddress host, int port);

        /**
         * 回調 指定解析的结果
         */
        void onServerFound(NsdServiceInfo serviceInfo, int port);

        //        void onServerFoundList(ArrayList<NsdServiceInfo> NsdServiceInfoList);

        /**
         * 無合適 回調失敗
         */
        void onServerFail();
    }
}
