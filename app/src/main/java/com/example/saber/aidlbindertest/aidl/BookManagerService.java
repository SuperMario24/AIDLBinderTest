package com.example.saber.aidlbindertest.aidl;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class BookManagerService extends Service {

    private static final String TAG = "BookManagerService";

    //在这个Boolean值的变化的时候不允许在之间插入，保持操作的原子性.用于多线程
    private AtomicBoolean mIsServiceDestoryed = new AtomicBoolean(false);

    //并发读写的集合
    private CopyOnWriteArrayList<Book> mBookList = new CopyOnWriteArrayList<>();

    //系统专门提供的用于删除跨进程listener接口，，内部有一个map，key是binder，value是callback
    private RemoteCallbackList<IOnNewBookArrivedListener> mListenerList = new RemoteCallbackList<>();


    private Binder mBinder = new IBookManager.Stub(){

        @Override
        public List<Book> getBookList() throws RemoteException {
            return mBookList;
        }

        @Override
        public void addBook(Book book) throws RemoteException {
            mBookList.add(book);
        }

        @Override
        public void registerListener(IOnNewBookArrivedListener listener) throws RemoteException {
            mListenerList.register(listener);

            int N = mListenerList.beginBroadcast();
            mListenerList.finishBroadcast();
            Log.d(TAG,"registerListener,current size:"+N);
        }

        @Override
        public void unregisterListener(IOnNewBookArrivedListener listener) throws RemoteException {
            mListenerList.unregister(listener);

            Log.d(TAG,"unregister success.");
            int N = mListenerList.beginBroadcast();
            mListenerList.finishBroadcast();
            Log.d(TAG,"unregisterListener,current size:"+mListenerList.beginBroadcast());
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        //onBind中验证权限
        int check = checkCallingOrSelfPermission("com.example.saber.aidlbindertest.permission.ACCESS_BOOK_SERVICE");
        if(check == PackageManager.PERMISSION_DENIED){
            return null;
        }

        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mBookList.add(new Book(1,"Android"));
        mBookList.add(new Book(2,"Ios"));

        new Thread(new ServiceWorker()).start();

    }

    private class ServiceWorker implements Runnable{

        @Override
        public void run() {
            //每隔5s检查一次有没有新书
            while(!isDeviceProtectedStorage()){
                try {
                    Thread.sleep(5000);

                    int bookId = mBookList.size()+1;
                    Book newBook = new Book(bookId,"new book#"+bookId);

                    //有新书来了发送通知
                    try {
                        onNewBookArrived(newBook);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 有新书来了，通知所有客户端
     * @param newBook
     */
    private void onNewBookArrived(Book newBook) throws RemoteException {

        mBookList.add(newBook);

        //RemoteCallbackList的总数
        final int N = mListenerList.beginBroadcast();
        for(int i=0;i<N;i++){
            IOnNewBookArrivedListener listener = mListenerList.getBroadcastItem(i);
            if(listener != null){
                listener.onNewBookArrived(newBook);
            }
        }
        //beginBroadcast()必须和finishBroadcast()配对使用
        mListenerList.finishBroadcast();

    }

    @Override
    public void onDestroy() {
        //标记service已经销毁
        mIsServiceDestoryed.set(true);
        super.onDestroy();
    }
}
