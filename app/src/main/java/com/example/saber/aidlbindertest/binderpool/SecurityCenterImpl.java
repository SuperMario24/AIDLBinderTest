package com.example.saber.aidlbindertest.binderpool;

import android.os.RemoteException;

/**
 * Created by saber on 2017/7/5.
 */

public class SecurityCenterImpl extends ISecurityCenter.Stub{

    private static final char SECRET_CODE = '^';

    @Override
    public String encrypt(String content) throws RemoteException {

        char[] chars = content.toCharArray();
        for(int i=0;i<chars.length;i++){
            chars[i] ^= SECRET_CODE;
        }
        return new String(chars);
    }

    @Override
    public String decrypt(String password) throws RemoteException {

        return encrypt(password);
    }
}
