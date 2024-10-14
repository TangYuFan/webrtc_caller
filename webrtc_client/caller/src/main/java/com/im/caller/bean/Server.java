package com.im.caller.bean;

import android.os.Parcel;
import android.os.Parcelable;


/**
 *   @desc : 服务器地址存储（signal、turn、stun等服务器地址）
 *   @auth : tyf
 *   @date : 2024-10-08 10:38:45
*/
public class Server implements Parcelable {

    // Parcelable 是对象序列化接口
    public final String uri;
    public final String username;
    public final String password;

    public Server(String uri) {
        this(uri, "", "");
    }

    public Server(String uri, String username, String password) {
        this.uri = uri;
        this.username = username;
        this.password = password;
    }

    protected Server(Parcel in) {
        uri = in.readString();
        username = in.readString();
        password = in.readString();
    }

    // 将服务器地址写入 Parcel
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uri);
        dest.writeString(username);
        dest.writeString(password);
    }

    // 将服务器地址从 Parcel 读取
    public static final Creator<Server> CREATOR = new Creator<Server>() {
        @Override
        public Server createFromParcel(Parcel in) {
            return new Server(in);
        }
        @Override
        public Server[] newArray(int size) {
            return new Server[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    public String getUri() {
        return uri;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}