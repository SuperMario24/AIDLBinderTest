// IOnNewBookArrivedListener.aidl
package com.example.saber.aidlbindertest.aidl;

import com.example.saber.aidlbindertest.aidl.Book;

interface IOnNewBookArrivedListener {
    void onNewBookArrived(in Book book);
}
