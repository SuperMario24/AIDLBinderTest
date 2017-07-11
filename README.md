# AIDLBinderTest


一.IPC简介

线程是CPU调度的最小单元，同时线程是一种有限的系统资源，而进程一般指一个执行单元，在PC和移动设备上指一个程序或一个应用。一个进程可以包含多个线程，因此
线程和进程是包含和被包含的关系。


二.Android中的多进程模式

（1）开启多进程模式

在Android中多进程是指一个应用中存在多个进程的情况，这里不讨论两个应用之间的多进程情况。首先，在Android中使用多进程只有一个方法，那就是给四大组件
（Activity，Service，Receiver，ContentProvider）在菜单文件中指定android：process属性，除此之外没有其他办法。

            <service
              android:name=".aidl.BookManagerService"
              android:enabled="true"
              android:exported="true"
              android:process=":remote" />         

没有为它指定process属性，那么它运行在默认的进程中，默认进程的进程名是包名。

进程名之间的区别：“：remote” 和 “com.example.saber.aidlbindertext.remote”:
(1)“：”的含义是指要在当前的进程名前面附加加上当前的包名，这是一种简单的方法，它完整的进程名为“com.example.saber.aidlbindertext：remote”。
（2）进程名以“：”开头的进程属于当前应用的私有进程，其他应用的组件不可以和它跑在同一个进程中，而进程名不以“：”开头的进程属于全局进程，其他应用通过
shareUID方式可以和它跑在同一个进程中。

我们知道Android系统会为每一个应用分配一个唯一的UID,具有相同UID的应用才能共享数据。这里要说明的是，两个应用通过shareUID跑在同一个进程中是有要求
的，需要两个应用有相同的shareUID并且签名相同才可以。在这种情况下，他们可以互相访问对方的私有数据，比如data目录，组件信息等，还可以共享内存数据。



（2）多进程模式的运行机制：

Android为每一应用都分配了独立的虚拟机，或者说每个进程都分配一个独立的虚拟机，不同的虚拟机在内存分配上有不同的地址空间，这就导致在不同的虚拟机中访问
同一个类的对象会产生多份副本。不同的进程拥有不同的内存地址，他们间相互不影响。


多进程的主要影响：
所有运行在不同进程中的四大组件，只要他们之间需要通过内存来共享数据，都会共享失败，这也就是多进程所带来的主要影响。一般来说，多进程会造成如下几方面
的问题：

1.静态成员和单例模式完全失效。
2.线程同步机制完全失效。
3.SharePreferences的可靠性下降。
4.Application会多次创建。

第一个问题是因为内存地址不一样。
第二个问题本质上和第一个问题时类似的，既然不是一块内存了，那么不管是锁对象还是锁全局类都无法保证线程同步，因为不同
进程锁的不是同一个对象。
第三个问题是因为SharePreferences不支持两个进程同时去执行写操作，否则会导致一定几率的数据丢失，这是因为SharePreferences底层是通过读写XML文件
来实现的，并发写显然是可能出问题的。
第四个问题也是显而易见的，当一个组件跑在一个新进程的时候，由于系统要在创建新的进程同时分配独立的虚拟机，所以这个过程其实就是启动一个应用的过程。
运行在同一个进程中的两个组件是属于同一个虚拟机和同一个Application的，同理，运行在不同进程中的组件是属于不同的虚拟机和Application的。


实现跨进程的方式很多，比如通过Intent来传递数据，共享文件和SharePreferences，基于Binder的Messager和AIDL以及Socket等。




三.IPC基础概念介绍

1.Serializable接口

Serializable是Java所提供的一个序列化的接口，他是一个空接口，为对象提供序列化和反序列化的操作。使用Serializable来实现序列化相当简单，只需要在
类的声明中指定一个类似下面的标识即可自动实现默认的序列化过程：

      private static final long serialVersion = 8787879878979878779L;

通过Serializable方式来实现对象的序列化，几乎所有工作都被自动完成了，只需要采用ObjectOutputStream和ObjectInputStream即可轻松实现：

    //序列化过程
    User user = new User(0,"jake",true);
    ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("cache.txt"));
    out.writeObject(user);
    out.close();
    
    //反序列化过程
    ObjectInputStream in = new ObjectInputStream(new FileInputStream("cache.txt"));
    User newUser = (User)in.readObject();
    in.close();
    
恢复后的对象newUser和user的内容完全一样，但是两者并不是同一个对象。    
    
serialVersionUID的工作机制是这样的：
序列化的时候，系统会把当前类的serialVersionUID写入序列化的文件当中（也可能是其他中介）当反序列化的时候系统会去检测文件中的serialVersionUID，
看它是否和当前类的serialVersionUID一致，如果一致就说明序列化的类的版本和当前类的类的版本是相同的，这个时候可以成功反序列化，否则就说明当前类和
序列化的类相比发生了某些变换，反序列化会失败。

一般来说，我们应该手动指定serialVersionUID的值，比如1L,也可以让Eclipse根据当前类的结构自动生成它的hash值。如果不手动指定serialVersionUID
的值，反序列化时当前类有所改变，比如增加或者删除了某些成员变量，那么系统就会重新计算当前类的hash值并把它赋值给serialVersionUID，这时候当前
类的serialVersionUID和序列化数据中的serialVersionUID不一样，会导致反序列化失败。当我们手动指定了它以后，就可以在很大程度上避免反序列化过程
的失败。

根据上面的分析，我们可以知道，给serialVersionUID指定为1L和系统自动生成并没有本质区别，一下两点需要注意：
（1）.静态成员变量属于类不属于对象，所以不会参与序列化过程
（2）.其次使用transient关键字标记的成员变量不参与序列化过程。


2.Parcelable接口

一个类只要实现了这个接口，这个类的对象就可以实现序列化并可以通过Intent和Binder传递，下面的示例是一个典型的用法：

    
     public class User implements Parcelable,Serializable {

      public int userId;
      public String userName;
      public boolean isMale;

      private Book book;


      public User(int userId, String userName, boolean isMale) {
          this.userId = userId;
          this.userName = userName;
          this.isMale = isMale;
      }

      protected User(Parcel in) {
          userId = in.readInt();
          userName = in.readString();
          isMale = in.readByte() != 0;
      }

      public static final Creator<User> CREATOR = new Creator<User>() {
          @Override
          public User createFromParcel(Parcel in) {
              return new User(in);
          }

          @Override
          public User[] newArray(int size) {
              return new User[size];
          }
      };

      @Override
      public int describeContents() {
          return 0;
      }

      @Override
      public void writeToParcel(Parcel dest, int flags) {
          dest.writeInt(userId);
          dest.writeString(userName);
          dest.writeByte((byte) (isMale ? 1 : 0));
      }
  } 

    
这个接口的方法Studio可自动生成。 序列化的功能由writeToParcel方法来完成，反序列化功能由CREATEOR来完成，其内部标明了如何创建序列化对象和数组，并
通过Parcel的一系列read方法来完成反序列化过程，内容描述功能由describeContents方法来完成，几乎所有情况下，这个方法都应该返回0。



Serializable和Parcelable的区别：
（1）Serializable是Java中的序列化接口，其使用起来简单但是开销很大，序列化和反序列化过程需要大量的IO操作。
（2）Parcelable是Android中的序列化方式，因此更适合在Android平台上，它的缺点就是使用起来稍微麻烦点，但是它效率很高。

Parcelable主要用在内存序列化上。
Serializable主要用在将对象序列化到存储设备中或者将对象序列化后通过网络传输。Parcelable也可以但是过程会稍显复杂。



3.Binder

Binder是Android中的一种跨进程通信方式，
从Android Framework角度来说，Binder是ServiceManager连接各种Manager（ActivityManager，
WindowManager，等等）和相应ManagerService的桥梁。
从Android应用层来说，Binder是客户端和服务端进行通信的媒介，当bindService的时候，服务端会返回一个包含了服务端业务调用的Binder对象，通过这个Binder
对象，客户端就可以获取服务端提供的服务或者数据，这里的服务包括普通服务和基于AIDL服务。

Android开发中，Binder主要用在Service中，包括AIDL和Messenger，这里选用涉及跨进程通信的AIDL来分析Binder的工作机制。
首先，新建Java包aidl，然后新建三个文件Book.java、IBookManager.aidl，Book.aidl。
Book.java是一个表示图书信息的类，他实现了Parcelable接口。Book.aidl是Book类在AIDL声明。IBookManager.aidl是我们定义的一个接口。里面有两个方法：
getBookList和addBook。
我们可以看到，尽管Book类已经和IBookManager位于相同的包中，但是在IBookManager中仍要导入Book类，这就是AIDL的特殊之处。下面我们看一下系统为
IBookManager.aidl自动生成的Binder类，接下来我们根据系统生成的Binder类来分析Binder的工作机制，代码如下：


/**
 * 自动生成的类继承了IInterface
 */
public interface IBookManager extends android.os.IInterface {
    /**
     * Local-side IPC implementation stub class.
     * 内部类Stub就是一个Binder类,当客户端和服务端都位于同一个进程时,方法调用不会走跨进程的transact过程
     *  而当两者位于不同进程时,方法调用需要走transact过程, 这个逻辑由Stub的内部代理类Proxy来完成
     */
    public static abstract class Stub extends android.os.Binder implements com.example.saber.aidlbindertest.aidl.IBookManager {
        //Binder的唯一标识, 一般使用当前Binder的全限定名来表示
        private static final String DESCRIPTOR = "com.example.saber.aidlbindertest.aidl.IBookManager";


        /** 
         *用于将服务端的Binder对象转换成客户端所需的AIDL接口类型的对象,这种转换过程是区分进程的,如果客户端与服务端是同一进程
         *那么此方法返回的就是服务端的Stub对象本身,否则返回的是系统封装后的Stub.proxy对象
         */
        public static com.example.saber.aidlbindertest.aidl.IBookManager asInterface(android.os.IBinder obj) {
            if ((obj == null)) {
                return null;
            }
            android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (((iin != null) && (iin instanceof com.example.saber.aidlbindertest.aidl.IBookManager))) {
                return ((com.example.saber.aidlbindertest.aidl.IBookManager) iin);
            }
            return new com.example.saber.aidlbindertest.aidl.IBookManager.Stub.Proxy(obj);
        }

        /**
         *返回当前Binder对象
         */
        @Override
        public android.os.IBinder asBinder() {
            return this;
        }

        /**
         *此方法运行在服务端中Binder线程池中,当客户端发起跨进程请求时, 远程请求会通过系统底层封装后交由此方法来处理.
         *@param code 服务端通过code可以确定客户端所请求的目标方法是什么
         *@param data 从data中取出目标方法所需的参数(如果目标方法有形参的话),然后会执行目标方法
         *@param reply 当方法执行完毕后,就向reply中写入返回值,如果有返回值的话.
         *@return  如果为返回false那么客户端的返回会失败,可以通过这个特性来做权限验证
         */
        @Override
        public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_getBookList: {
                    data.enforceInterface(DESCRIPTOR);
                    java.util.List<com.szysky.note.androiddevseek_02.aidl.Book> _result = this.getBookList();
                    reply.writeNoException();
                    reply.writeTypedList(_result);
                    return true;
                }
                case TRANSACTION_addBook: {
                    data.enforceInterface(DESCRIPTOR);
                    com.szysky.note.androiddevseek_02.aidl.Book _arg0;
                    if ((0 != data.readInt())) {
                        _arg0 = com.szysky.note.androiddevseek_02.aidl.Book.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    this.addBook(_arg0);
                    reply.writeNoException();
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        /**
         *这个类的 getBookList和addBook 都是运行在客户端,当客户端调用此方法.1首先创建该方法所需的输入型Parcel对象_data,输出型Parcel对象_reply和返回值对象(如果有).
         *2.然后把该方法的参数信息写入到_data中(如果有), 接着调用transact方法来发起RPC远程调用请求,同时当前线程挂起,然后服务端的onTransact()会被调用,直到RPC返回,
         *当前线程继续执行,并从_reply中取出RPC过程的返回结果,最后返回的_reply的结果
         */
        private static class Proxy implements com.example.saber.aidlbindertest.aidl.IBookManager {
            private android.os.IBinder mRemote;

            Proxy(android.os.IBinder remote) {
                mRemote = remote;
            }

            @Override
            public android.os.IBinder asBinder() {
                return mRemote;
            }

            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override
            public java.util.List<com.example.saber.aidlbindertest.aidl.Book> getBookList() throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                java.util.List<com.example.saber.aidlbindertest.aidl.Book> _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(Stub.TRANSACTION_getBookList, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.createTypedArrayList(com.example.saber.aidlbindertest.aidl.Book.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void addBook(com.example.saber.aidlbindertest.aidl.Book book) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if ((book != null)) {
                        _data.writeInt(1);
                        book.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(Stub.TRANSACTION_addBook, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        //声明了两个整形id来表示这两个方法, 用于在transact处理过程中确定客户端到底请求是哪个方法
        static final int TRANSACTION_getBookList = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
        static final int TRANSACTION_addBook = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);


        /**
         *Construct the stub at attach it to the interface.
         */
        public Stub() {
            this.attachInterface(this, DESCRIPTOR);
        }
    }

    /**
     *声明了在IBookManager中的两个抽象方法
     */
    public java.util.List<com.example.saber.aidlbindertest.aidl.Book> getBookList() throws android.os.RemoteException;

    public void addBook(com.example.saber.aidlbindertest.aidl.Book book) throws android.os.RemoteException;
}


说明：
首先它声明了两个方法getBookList和addBook，显然这就是我们在IBookManager.aidl中所声明的方法，同时它还声明了两个id分别用于标识这两个方法，这两个
id用于标识在transact过程中客户端所请求的到底是哪个方法。接着它声明了一个内部类Stub，这个Stub就是一个Binder类，当客户端和服务端都位于同一个进程
时，方法不会走transact过程，而当两者位于不同进程时，方法调用需要走transact过程，这个逻辑由Stub的内部代理类Proxy完成，这么看来，IBookManager
这个接口确实简单，但是我们也应该认识到这个接口的核心实现就是他的内部类Stub和Stub的内部代理类Proxy。

通过上面的分析，我们应该了解到Binder的工作机制，但是有两点还是需要说明一下：

（1）当客户端发起请求时，由于当前线程会被挂起直至服务端进程返回数据，所以如果一个远程方法时很耗时的，那么不能在UI线程中发起此远程请求
（2）由于服务端的Binder方法运行在BInder线程池中，所以Binder方法不管是否耗时都应采用同步的方式去实现，因为它已经运行在一个线程中了。

下面是一个Binder的工作机制图：

Client发起远程请求，然后自己被挂起，通过Binder写入参数data，然后调用transact方法向服务端获取数据，服务端调用onTransact方法，使用Binder
线程池获取数据后写入结果放在reply中，此时返回数据后，唤醒Client。


Binder的死亡代理，首先声明一个DeathRecipient对象，DeathRecipient是一个接口，内部只有一个方法binderDied，在客户端重写它：

            public IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient(){
                    public void binderDied(){
                          if(mBookManager == null){
                                    return;
                          }
                          //解绑Binder并置空
                          mBookManager.asBinder.unlinkToDeath(mDeathRecipient,0);
                          mBookManager = null;

                          //TODO 这里进行重连操作
                    }
            }

其次在客户端绑定远程服务成功后，给Binder设置死亡代理：

            mServiceHandler = IMessageBoxManager.Stub.asInterface(binder);
            binder.linkToDeath(mDeathRecipient,0);




四.Android中的IPC方式
























