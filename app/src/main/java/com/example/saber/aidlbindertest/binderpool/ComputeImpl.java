package com.example.saber.aidlbindertest.binderpool;

import android.os.RemoteException;

/**
 * Created by saber on 2017/7/5.
 */

public class ComputeImpl extends ICompute.Stub {

    @Override
    public int add(int a, int b) throws RemoteException {
        return a+b;
    }
}
