package com.thisfeng.server;

import android.net.nsd.NsdServiceInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.thisfeng.server.nettyServer.EchoServer;
import com.thisfeng.server.nettyServer.NettyListener;
import com.thisfeng.server.nsd.NSDServer;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * 无论两端哪边先结束应用再打开 Netty 都可以 可实现断线重连
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Server";


    TextView tvNetty;
    TextView tvContent;

    View vOpenAppGetCode;

    /**
     * 注册 NSD 服务的名称 和 端口 这个可以设置默认固定址，用于客户端通过 NSD_SERVER_NAME 筛选得到服务端地址和端口
     */
    public static String NSD_SERVER_NAME = "AGSystem";

    public static int NSD_PORT = 8088;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvNetty = findViewById(R.id.tvNetty);
        tvContent = findViewById(R.id.tvContent);
        vOpenAppGetCode = findViewById(R.id.vOpenAppGetCode);
        findViewById(R.id.btnSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMsgToClient();
            }
        });

        registerNsdServer();

        initNetty();

    }


    /**
     * 开启Socket 服务需要开启一个线程处理，等待客户端连接
     */
    private void initNetty() {

        if (!EchoServer.getInstance().isServerStart()) {
            new NettyThread().start();
        }

    }


    class NettyThread extends Thread {
        @Override
        public void run() {
            super.run();

            EchoServer.getInstance().setListener(new NettyListener() {
                @Override
                public void onMessageResponse(Object msg) {

                    Log.i(TAG, "Server received: " + (String) msg);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            tvContent.setText("Server received: " + msg);

                            Toast.makeText(MainActivity.this, String.valueOf(msg), LENGTH_SHORT).show();
                        }
                    });

                }

                @Override
                public void onChannel(Channel channel) {
                    //设置通道连接到封装的类中
                    EchoServer.getInstance().setChannel(channel);

                    Log.i(TAG, "建立连接 onChannel(): " + "接收(" + channel.toString() + ")");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvNetty.setText("接收:(" + channel.toString() + ")");

                        }
                    });
                }

                @Override
                public void onStartServer() {
                    Log.i(TAG, "Netty Server started 已开启");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Netty Server 已開啟", LENGTH_SHORT).show();
                        }
                    });

                }

                @Override
                public void onStopServer() {
                    Log.i(TAG, "Netty Server started 已开启");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Netty Server 未連接", LENGTH_SHORT).show();

                        }
                    });
                }

                @Override
                public void onServiceStatusConnectChanged(int statusCode) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (statusCode == NettyListener.STATUS_CONNECT_SUCCESS) {
                                Log.i(TAG, "STATUS_CONNECT_SUCCESS:");
                                //标记连接的状态
                                vOpenAppGetCode.setSelected(true);
                            } else {
                                Log.i(TAG, "onServiceStatusConnectChanged:" + statusCode);
                                tvNetty.setText("接收:");
                                vOpenAppGetCode.setSelected(false);

                            }
                        }
                    });
                }
            });
            //入口 开启Netty Server
            EchoServer.getInstance().start();
        }
    }


    /**
     * 服务器端注册一个可供NSD探测到的网络 Ip 地址，便于给展示叫号机连接此socket
     */
    Runnable nsdServerRunnable = new Runnable() {
        @Override
        public void run() {

            NSDServer nsdServer = new NSDServer();
            nsdServer.startNSDServer(MainActivity.this, NSD_SERVER_NAME, NSD_PORT);

            nsdServer.setRegisterState(new NSDServer.IRegisterState() {
                @Override
                public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                    Log.i(TAG, "已注册服务onServiceRegistered: " + serviceInfo.toString());
                    //已经注册可停止该服务
//                    nsdServer.stopNSDServer();
                }
                @Override
                public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {

                }
                @Override
                public void onServiceUnregistered(NsdServiceInfo serviceInfo) {

                }
                @Override
                public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {

                }
            });
        }
    };

    private void registerNsdServer() {

        new Thread(nsdServerRunnable).start();
    }


    /**
     * 测试发送给客户端的消息
     */
    private void sendMsgToClient() {
        if (!EchoServer.getInstance().getConnectStatus()) {
            Toast.makeText(getApplicationContext(), "未连接,请先连接", LENGTH_SHORT).show();
        } else {
            final String msg = "我是你兄dei，这条消息 来自NettyServer";
            if (TextUtils.isEmpty(msg.trim())) {
                return;
            }
            EchoServer.getInstance().sendMsgToServer(msg, new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (channelFuture.isSuccess()) {
                        Log.i(TAG, "Write auth successful");

                    } else {
                        Log.i(TAG, "Write auth error");
                    }
                }
            });

        }
    }

}
