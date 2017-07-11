package com.example.saber.aidlbindertest.Messenger;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.saber.aidlbindertest.R;
import com.example.saber.aidlbindertest.mulprocess.MyConstants;

public class MessengerActivity extends AppCompatActivity {

    private static final String TAG = "MessengerActivity";

    private Messenger mService;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //根据服务端返回的binder创建Messenger对象
            mService = new Messenger(service);

            Message msg = Message.obtain(null, MyConstants.MSG_FROM_CLIENT);
            Bundle data = new Bundle();
            data.putString("msg","hello,this is client.");
            msg.setData(data);

            //很关键的一点，当客户端发送消息时，需要把接收服务端回复的Messenger通过Message的replyTo参数传递给服务端
            // 需要把接收服务端回复的Messenger通过Message的replyTo传递给服务端
            msg.replyTo = mGetReplyMessenger;

            try {
                //Messenger发送消息给服务端，消息类型为message对象
                mService.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    //接收服务端消息的Messenger
    private Messenger mGetReplyMessenger = new Messenger(new MessengerHandler());
    //接收服务端消息的Handler
    private class MessengerHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MyConstants.MSG_FROM_SERVICE:
                    Log.i(TAG,"receive msg from Service:"+msg.getData().getString("reply"));
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messenger);

        Intent intent = new Intent(this,MessengerService.class);
        bindService(intent,mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        unbindService(mServiceConnection);
        super.onDestroy();
    }
}
