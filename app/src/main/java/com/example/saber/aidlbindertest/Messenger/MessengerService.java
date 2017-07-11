package com.example.saber.aidlbindertest.Messenger;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.example.saber.aidlbindertest.mulprocess.MyConstants;

public class MessengerService extends Service {
    private static final String TAG = "MessengerService";

    //通过MessengerHandler创建Messenger对象
    private final Messenger mMessenger = new Messenger(new MessengerHandler());

    private static class MessengerHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MyConstants.MSG_FROM_CLIENT:
                    Log.i(TAG,"receiver msg from Client:"+msg.getData().getString("msg"));

                    //回复客户端发送来的内容
                    //1. 通过接收到的到客户端的Message对象获取到Messenger信使
                    Messenger client = msg.replyTo;
                    // 2. 创建一个信息Message对象,并把一些数据加入到这个对象中
                    Message replyMessage = Message.obtain(null,MyConstants.MSG_FROM_SERVICE);
                    Bundle bundle = new Bundle();
                    bundle.putString("reply","嗯，你的消息我已经收到，稍后会回复你。");
                    replyMessage.setData(bundle);

                    // 3. 通过信使Messenger发送封装好的Message信息
                    try {
                        client.send(replyMessage);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        //返回Messenger中binder对象
        return mMessenger.getBinder();
    }
}
