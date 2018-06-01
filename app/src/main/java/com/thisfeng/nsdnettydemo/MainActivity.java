package com.thisfeng.nsdnettydemo;

import android.net.nsd.NsdServiceInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.thisfeng.nsdnettydemo.nettyClient.NettyClient;
import com.thisfeng.nsdnettydemo.nettyClient.NettyListener;
import com.thisfeng.nsdnettydemo.nsd.NsdClient;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * 客戶端連接 Netty 需要通过NSD自动 查找到指定名称，解析 IP地址和端口，再进行Netty连接
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Client";

    public static final String SERVER_NAME = "AGSystem";

    TextView tvNetty;
    TextView tvConnect;

    View vNettyStatus;

    /**
     * Nsd 客户端搜索
     */
    private NsdClient nsdClient;

    /**
     * Netty 客户端连接处理
     */
    private NettyClient mNettyClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvNetty = findViewById(R.id.tvNetty);
        tvConnect = findViewById(R.id.tvConnect);
        vNettyStatus = findViewById(R.id.vNettyStatus);


        searchNsdServer();


        findViewById(R.id.btnSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMsgToServer();
            }
        });


    }

    private void sendMsgToServer() {

        if (!mNettyClient.getConnectStatus()) {
            Toast.makeText(getApplicationContext(), "未连接,请先连接", LENGTH_SHORT).show();
        } else {
            final String msg = "你不是我兄dei，这条消息 来自NettyClient";


            mNettyClient.sendMsgToServer(msg, new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {                //4
                        Log.d(TAG, "Write auth successful");
                    } else {
                        Log.d(TAG, "Write auth error");
                    }
                }
            });
        }
    }

    /**
     * 通過 Nsd 搜索註冊過的服務端名称 解析后拿到 IP 和端口 ，進行 NettySocket 的連接
     */
    private void searchNsdServer() {

        nsdClient = new NsdClient(MainActivity.this, SERVER_NAME, new NsdClient.IServerFound() {
            @Override
            public void onServerFound(NsdServiceInfo info, int port) {
                if (info != null) {
                    String hostAddress = info.getHost().getHostAddress();

                    tvConnect.setText("NSD查询到指定服务器信息：\n" + info.toString());

                    //获取到指定的地址，进行Netty的连接
                    connectNettyServer(hostAddress, port);

                    if (info.getServiceName().equals(SERVER_NAME)) {
                        //掃描到對應後過後停止Nsd掃描
                        nsdClient.stopNSDServer();
                    }
                }
            }

            @Override
            public void onServerFail() {

            }
        });

        nsdClient.startNSDClient();
    }


    /**
     * 连接Netty 服务端
     *
     * @param host 服务端地址
     * @param port 服务端端口 默认两端约定一致
     */
    private void connectNettyServer(String host, int port) {

        mNettyClient = new NettyClient(host, port);

        Log.i(TAG, "connectNettyServer");
        if (!mNettyClient.getConnectStatus()) {
            mNettyClient.setListener(new NettyListener() {
                @Override
                public void onMessageResponse(Object msg) {
                    Log.i(TAG, "onMessageResponse:" + msg);
                    /**
                     *   接收服务端发送过来的 json数据解析
                     */
                    // TODO: 2018/6/1  do something
                    // QueueShowBean    queueShowBean = JSONObject.parseObject((String) msg, QueueShowBean.class);

                    //需要在主线程中刷新
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Toast.makeText(MainActivity.this, msg + "", Toast.LENGTH_SHORT).show();

                            tvNetty.setText("Client received:" + msg);
                        }
                    });


                }

                @Override
                public void onServiceStatusConnectChanged(int statusCode) {
                    /**
                     * 回调执行还在子线程中
                     */
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (statusCode == NettyListener.STATUS_CONNECT_SUCCESS) {
                                Log.e(TAG, "STATUS_CONNECT_SUCCESS:");
                                vNettyStatus.setSelected(true);
                            } else {
                                Log.e(TAG, "onServiceStatusConnectChanged:" + statusCode);
                                vNettyStatus.setSelected(false);
                            }
                        }
                    });

                }
            });

            mNettyClient.connect();//连接服务器
        }
    }
}
