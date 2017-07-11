package com.example.saber.aidlbindertest.binderpool;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.saber.aidlbindertest.R;

public class BInderPoolActivity extends AppCompatActivity {

    private static final String TAG = "BInderPoolActivity";

    //AIDL接口
    private ISecurityCenter mSecurityCenter;
    private ICompute mCompute;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_binder_pool);

        new Thread(){
            @Override
            public void run() {
                doWork();
            }
        }.start();


    }


    //通过BInderPool调用不同的binder对象
    private void doWork() {

        BinderPool binderPool = BinderPool.getInstance(BInderPoolActivity.this);

        //根据binderCode获取对应的binder对象
        IBinder securityBinder = binderPool.queryBinder(BinderPool.BINDER_SECURITY_CENTER);
        //将binder对象转换成对应的AIDL接口
        mSecurityCenter = (ISecurityCenter)SecurityCenterImpl.asInterface(securityBinder);

        Log.d(TAG,"visit ISecurityCenter");
        String msg = "helloword-安卓";
        Log.d(TAG,"content加密前:"+msg);
        try {

            String password = mSecurityCenter.encrypt(msg);
            Log.d(TAG,"encrypt加密后:"+password);
            Log.d(TAG,"decrypt解密后:"+mSecurityCenter.decrypt(password));

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        Log.d(TAG,"visit ICompute");
        IBinder computeBinder = binderPool.queryBinder(BinderPool.BINDER_COMPUTE);
        mCompute = (ICompute)ComputeImpl.asInterface(computeBinder);

        try {
            Log.d(TAG,"3+5="+mCompute.add(3,5));
        } catch (RemoteException e) {
            e.printStackTrace();
        }


    }
}
