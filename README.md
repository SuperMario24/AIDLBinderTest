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


1.使用Bundle

四大组件中的三大组件（Activity，Service，Receiver）都是支持在Intent中传递Bundle数据的，由于Bundle实现了Parcelable接口，所以他可以方便的在
不同进程中传输。


2.使用文件共享

文件共享也是一种不错的进程间通信的方式，两个进程通过读写一个文件来交换数据，比如A进程把数据写入文件，B进程通过读取这个文件来获取数据。文件共享
除了可以交换一些文本信息外，我们还可以序列化一个对象到文件系统中的同时另一个进程中恢复这个对象，下面就展示这种使用方法。

我们在MainActivity的onResume中序列化一个User对象到sd卡上的一个文件里，然后再SecondActivity的onResume中去反序列化这个对象：

MainActivity：

                 /**
                 * 序列化对象，并写入文件
                 */
                private void persistToFile() {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            User user = new User(1,"hello world",false);
                            File dir = new File(MyConstants.CHAPTER_2_PATH);
                            if(!dir.exists()){
                                dir.mkdirs();
                            }

                            File cachedFile = new File(MyConstants.CACHE_FILE_PATH);
                            try {
                                cachedFile.createNewFile();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            ObjectOutputStream objectOutputStream = null;

                            try {
                                if(cachedFile.exists()){
                                    objectOutputStream = new ObjectOutputStream(new FileOutputStream(cachedFile));
                                    objectOutputStream.writeObject(user);
                                    Log.d(TAG,"persist user:"+user);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }finally {
                                if (objectOutputStream != null ){
                                    try {
                                        objectOutputStream.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }).start();
                }

SecondActivity：

                 /**
                 * 反序列化对象，从文件中读取数据
                 */
                private void recoverFromFile() {

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            User user = null;
                            File cachedFile = new File(MyConstants.CACHE_FILE_PATH);
                            if(cachedFile.exists()){
                                ObjectInputStream objectInputStream = null;
                                try {
                                    objectInputStream = new ObjectInputStream(new FileInputStream(cachedFile));
                                    user = (User) objectInputStream.readObject();
                                    Log.d(TAG, "recover user:"+user);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                } catch (ClassNotFoundException e) {
                                    e.printStackTrace();
                                }finally {
                                    if(objectInputStream != null){
                                        try {
                                            objectInputStream.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    }).start();

                }

缺陷：存在并发读写的问题。包括SharePreferences。


3.使用Messenger

Messenger是一个轻量级的IPC方案，它的底层实现是AIDL。从它的构造方法就可以看出来：

            public Messenger(Handler target){
                 mTarget = target.getIMessenger();
            }
            public Messenger(IBinder target){
                 mTarget = IMessenger.Stub.asInterface(target);
            }

Messenger的使用方法很简单，他对AIDL做了封装。同时，它由于一次处理一个请求，因此在服务端我们不用考虑线程同步的问题，因为服务端中不存在并发的情形，
实现一个Messenger有如下几个步骤，分为服务端和客户端。

1.服务端进程：

首先，我们需要在服务端创建一个Service来处理客户端的连接请求，同时创建一个Handler并通过它来创建一个Messenger对象，然后再Service的onBinder中返回
这个Messenger对象底层的Binder即可。

2.客户端进程：
（1）首先要绑定服务端的Service
（2）绑定成功后用服务端返回的Binder对象创建一个Messenger
（3）通过这个Messenger就可以向服务端发送消息了，发送消息类型为Message对象
（4）如果需要服务端能够回应客户端，，就和服务端一样，我们还需要创建一个Handler并创建一个新的Messenger，并把这个Messenger对象通过Message
的replyTo参数传递给服务端，服务端通过这个replyTo参数就可以回应客户端。

下面看服务端的代码：

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

客户端代码：

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


总结一下Messenger的工作原理：

客户端通过服务端传递过来的IBinder构造一个Messenger对象，然后Messenger通过Messenger.send(msg)发送消息给服务端，消息类型为message对象，如果
希望服务端还能回应客户端，就在msg中通过msg.replyTo = Messenger再封装一个Messenger对象，服务端再通过Messenger= msg.replyTo取出这个Messenger
对象，再通过这个Messenger对象发消息给客户端。



4.使用AIDL

Messenger是以串行的方式处理客户端发来的消息，如果大量的消息同时发送到服务端，服务端也只能一个个处理，如果大量的并发请求，那么用Messenger就不太
合适了，这时我们可以使用AIDL来实现跨进程的方法调用，先介绍AIDL来进行进程间通信的流程，分为服务端和客户端：

（1）.服务端：
首先要创建一个Service用来监听客户端的连接请求，然后创建一个AIDL文件，将暴露给客户端的接口在这个AIDL文件中声明，最后在Service中实现这个AIDL接口。

（2）.客户端：
首先需要绑定服务端的Service，绑定成功后，将服务端返回的Binder对象转成AIDL接口所属的类型，接着就可以调用AIDL中的方法了。


（3）.AIDL接口的创建：

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

AIDL文件所支持的数据类型：
（1）基本数据类型
（2）String和CharSequence
（3）List，只支持ArrayList，里面每个元素都必须能被AIDL支持
（4）Map，只支持HashMap，里面每个元素都必须被AIDL支持
（5）Parcelable：所有实现了Parcelable接口的对象
（6）AIDL,所有的AIDL接口本身也可以在AIDL文件中使用

注意：（1）其中自定义的Parcelable对象和AIDl对象必须要显示import进来。
（2）如果AIDL文件中用到了自定义的Parcelable对象，那么必须新建一个和它同名的AIDL文件，并在其中声明他为Parcelable类型：
            
            // Book1.aidl
            package com.example.saber.aidlbindertest.aidl;
            parcelable Book;

（3）AIDL中除了基本类型，其他类型参数必须标上方向：in，out或者inout，in表示输入型参数，out表示输出型参数，inout表示输入输出型参数。


（4）.远程服务端Service的实现：

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

AIDL方法是在服务端的Binder线程池中进行的，因此当多个客户端同时连接的时候，需要进行线程同步。这里使用CopyOnWriteArrayList进行自动的线程同步。


（5）.客户端的实现：

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


AIDL中无法使用普通接口，因为跨进程BInder会把客户端传递过来的对象重新转化生成一个新的对象，对象是不能跨进程传输的，对象的跨进程传输本质上是反序列化的
过程。

RemoteCallbackList是系统专门提供的用于删除跨进程listener的接口。它的工作原理很简单，在它内部有一个Map结构专门用来保存所有的AIDL回调，这个Map
的key是IBinder类型，value是Callback类型。对象不同，但是底层的Binder是同一个，利用这个特性只要找出那个和注册listener具有相同Binder对象的服务端
listener删掉即可。另外RemoteCallbackList内部自动实现了线程同步功能。

注意：遍历RemoteCallbackList时，我们不能像List一样去操作它，其中beginBroadcast和finishBroadcast必须要配对使用。哪怕我们仅仅要获取它的个数。

1.若服务端是耗时操作，客户端调用服务端的方法时，应该开工作线程。
2.服务端的方法本身就运行在Binder线程池中，不需要再服务端开工作线程。

设置死亡代理和inServiceDisconnect的区别：一个运行在主线程一个运行在Binder线程池中。




（6）.AIDL的权限认证：
（1）在onBind中进行验证：

    @Override
    public IBinder onBind(Intent intent) {
        //onBind中验证权限
        int check = checkCallingOrSelfPermission("com.example.saber.aidlbindertest.permission.ACCESS_BOOK_SERVICE");
        if(check == PackageManager.PERMISSION_DENIED){
            return null;
        }

        return mBinder;
    }



5.使用ContentProvider

ContentProvider的底层实现依然是Binder。

创建一个自定义的Provider很简单，只要继承ContentProvider类并实现六个抽象方法即可：onCreate，query，update，insert，delete和getType。
这六个方法都很好理解，onCreate代表ContentProvider的创建，一般做一些初始化的工作；getType用来返回一个Uri请求所对应的MIME类型（媒体类型），比如
图片，视频等，剩下四个方法返回CRUD操作，即数据表的增删改查功能。根据Binder的工作原理，我们知道这六个方法均运行在ContentProvider的进程中，除了
onCreate由系统回调运行在主线程里，其他五个方法都由外界回调，运行在Binder线程池中。

注册ContentProvider：

            <provider
            android:name=".provider.BookProvider"
            android:authorities="com.example.saber.aidlbindertest.book.provider"
            android:permission="com.example.saber.PROVIDER"
            android:process=":remote" />

其中android：authorities是ContentProvider的唯一标识，通过这个属性，外部应用就可以访问我们的ContentProvider，因此android：authorities
必须是唯一的，这里建议读者在命名的时候加上包名前缀。

外界想访问我们的ContentProvider就必须声明"com.example.saber.PROVIDER"这个权限。

其他进程访问它的时候，必须获取ContentProvider的android：authorities属性所指定的Uri：

       Uri bookUri = Uri.parse("content://com.example.saber.aidlbindertest.book.provider/book");

ContentProvider通过Uri来区分外界要访问的数据集合，在本例中支持外界对BookProvider中的book表和user表进行访问，为了知道外界要访问的是哪个表，我们
需要为它们定义单独的Uri和Uri_Code，并将Uri和Uri_Code相关联，我们可以使用UriMatcher的addUri方法将Uri和Uri_Code关联到一起。这样，当外界请求访问
BookProvider时，我们就可以根据请求的Uri来得到Uri_Code，有了Uri_Code我们就可以知道外界想要访问哪张表，然后就可以进行相应的数据操作了，具体代码
如下：

    public static final String AUTHORITY = "com.example.saber.aidlbindertest.book.provider";

    //Uri来区分外界要访问的数据集合
    public static final Uri BOOK_CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/book");
    public static final Uri USER_CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/user");

    public static final int BOOK_URI_CODE = 0;
    public static final int USER_URI_CODE = 1;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(AUTHORITY,"book",BOOK_URI_CODE);//第一个参数为provider的authority，第二个为path，表名，第三个为自定义的code
        sUriMatcher.addURI(AUTHORITY,"user",USER_URI_CODE);
    }

将Uri和Uri_Code关联后，我们就可以通过如下方式来获取外界所要访问的数据源，根据Uri先取出Uri_Code，根据Uri_Code在得到数据表的名称，知道表名，就
可以进行操作了：


             public String getType(Uri uri) {
                    Log.d(TAG,"getType");

                    //返回要外界想要查询的数据，表名
                    String tableName = null;
                    switch (sUriMatcher.match(uri)){
                        case BOOK_URI_CODE:
                            tableName = DbOpenHelper.BOOK_TABLE_NAME;
                            break;
                        case USER_URI_CODE:
                            tableName = DbOpenHelper.USER_TABLE_NAME;
                            break;
                    }
                    return tableName;
                }

对ContentProvider所在进程做操作：

        Uri bookUri = Uri.parse("content://com.example.saber.aidlbindertest.book.provider/book");

        //插入
        ContentValues values = new ContentValues();
        values.put("_id", 6);
        values.put("name", "程序设计的艺术");
        getContentResolver().insert(bookUri, values);


注意：CRUD操作是存在多线程并发的，应该做好同步操作。



6.使用Socket


Socket分为流式套接字和用户数据报套接字两种，分别对应于网络的传输控制层中的TCP和UDP协议。TCP协议是面向连接的协议，提供稳定的双向通信功能，
TCP连接的建立需要经过“三次握手”才能完成，其本身提供了超时重传机制，具有很高的稳定性。
UDP是无连接的，提供不稳定的单向通信功能，当然UDP也可以实现双向通信功能，在性能上，UDP具有更好的效率，其缺点是不能保证数据一定能够正确传输，尤其在
网络拥塞的情况下。

Socket实现跨进程通信的前提是这个设备之间的IP地址相互可见。



五.Binder连接池




























































































                            







