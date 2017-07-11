package com.example.saber.aidlbindertest.aidl;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.saber.aidlbindertest.R;

import java.util.List;

public class BookManagerActivity extends AppCompatActivity {

    private static final String TAG = "BookManagerActivity";

    private static final int MESSAGE_NEW_BOOK_ARRIVED = 1;

    private IBookManager bookManager;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            //将服务端返回的binder转换成AIDL接口
            bookManager = IBookManager.Stub.asInterface(service);

            try {
                //获取图书
                List<Book> bookList = bookManager.getBookList();
                Log.i(TAG,bookList.getClass().getCanonicalName());
                Log.i(TAG,"query the booklist:"+bookList.toString());

                //添加新书
                bookManager.addBook(new Book(3,"Android开发艺术探索"));
                List<Book> newBookList = bookManager.getBookList();
                Log.i(TAG,"query the newBookList:"+newBookList.toString());

                //客户端注册接口
                bookManager.registerListener(mOnNewBookArrivedListener);

            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bookManager = null;
            Log.e(TAG,"binder died");
        }
    };

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_NEW_BOOK_ARRIVED:
                    Log.d(TAG,"receive new book:"+msg.obj);
                    break;
            }
        }
    };

    //服务端在客户端的回调方法
    private IOnNewBookArrivedListener mOnNewBookArrivedListener = new IOnNewBookArrivedListener.Stub() {
        @Override
        public void onNewBookArrived(Book book) throws RemoteException {
            //服务端在客户端的回调
            mHandler.obtainMessage(MESSAGE_NEW_BOOK_ARRIVED,book).sendToTarget();

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_manager);

        Intent intent = new Intent(this,BookManagerService.class);
        bindService(intent,mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {

        //销毁时注销接口
        if(bookManager != null && bookManager.asBinder().isBinderAlive()){

            Log.i(TAG,"unregister listener:"+mOnNewBookArrivedListener);
            try {
                bookManager.unregisterListener(mOnNewBookArrivedListener);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
        unbindService(mServiceConnection);

        super.onDestroy();
    }
}
