// IBinderPool.aidl
package com.example.saber.aidlbindertest.binderpool;

// Declare any non-default types here with import statements

interface IBinderPool {
    //根据不同的业务需求去调用不同的binder
    IBinder queryBinder(int binderCode);
}
