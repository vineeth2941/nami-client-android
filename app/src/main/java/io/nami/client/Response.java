package io.nami.client;

import com.google.gson.Gson;

public class Response<T> {
    private T data;
    private Boolean error;
    private Integer code;
    private String info;

    public Response(T data) {
        error = false;
        this.data = data;
    }

    public Response(Integer code, String info) {
        error = true;
        this.code = code;
        this.info = info;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
