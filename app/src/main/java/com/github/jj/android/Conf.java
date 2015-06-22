package com.github.jj.android;

public class Conf {

    public static final String API_HOST = Server.SERVER + ":" + Server.PORT;
    public static final String API_REGISTER = API_HOST + "/auth/register";
    public static final String API_LOGIN = API_HOST + "/auth/login";
    public static final String API_INIT = API_HOST + "/auth/init";

    public static final int RESULT_OK = 200;
    public static final int RESULT_FAILED = 400;
    public static final int RESULT_MAINTAIN = 500;

    public static final String SALT = "9a219ee6-15d6-11e5-af9c-525424be6a56";
    public static final byte[] AES_IV = "b1d15254f0f0417d".getBytes();

}
