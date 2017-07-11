// IBookManager.aidl
package com.example.saber.aidlbindertest.aidl;

import com.example.saber.aidlbindertest.aidl.Book;
import com.example.saber.aidlbindertest.aidl.IOnNewBookArrivedListener;

interface IBookManager {
        List<Book> getBookList();//从远程服务器获取图书列表
        void addBook(in Book book);//往图书列表中添加一本书

        void registerListener(IOnNewBookArrivedListener listener);//注册接口
        void unregisterListener(IOnNewBookArrivedListener listener);//注销接口
}
