package com.im.caller.bean;


/**
 *   @desc : 用户信息，群聊时显示对面用户的名字
 *   @auth : tyf
 *   @date : 2024-10-08 10:18:55
*/
public class User {

    private String id;
    private String name;
    private String avatar;

    public User(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        return this.getId().equals(other.getId());
    }
}
