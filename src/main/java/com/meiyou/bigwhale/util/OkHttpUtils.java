package com.meiyou.bigwhale.util;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Suxy
 * @date 2019/10/28
 * @description file description
 */
public class OkHttpUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(OkHttpUtils.class);
    public static final MediaType MEDIA_JSON = MediaType.parse("application/json; charset=utf-8");
    public static final MediaType MEDIA_OCTET_STREAM = MediaType.parse("application/octet-stream; charset=utf-8");
    public static final MediaType MEDIA_TEXT = MediaType.parse("text/plain; charset=utf-8");

    private static final OkHttpClient OK_HTTP_CLIENT;

    static {
        Properties properties = new Properties();
        try (InputStream inputStream = OkHttpUtils.class.getClassLoader().getResourceAsStream("okhttp.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
            boolean proxyEnabled = Boolean.parseBoolean(properties.getProperty("okhttp.proxy.enabled", "false"));
            long connectTimeout = Long.parseLong(properties.getProperty("okhttp.connectTimeout", "10"));
            long writeTimeout = Long.parseLong(properties.getProperty("okhttp.writeTimeout", "10"));
            long readTimeout = Long.parseLong(properties.getProperty("okhttp.readTimeout", "10"));
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            if (proxyEnabled) {
                String server = properties.getProperty("okhttp.proxy.server");
                Integer port = Integer.parseInt(properties.getProperty("okhttp.proxy.port", "-1"));
                builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(server, port)));
            }
            OK_HTTP_CLIENT = builder
                    .connectTimeout(connectTimeout, TimeUnit.SECONDS)
                    .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                    .readTimeout(readTimeout, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        okhttp3.Request request = chain.request();
                        Response response = chain.proceed(request);
                        if (response.priorResponse() != null) {
                            if (response.priorResponse().code() >= 300 && response.priorResponse().code() < 400) {
                                //获取重定向的地址
                                String location = response.priorResponse().headers().get("Location");
                                //重新构建请求
                                Request newRequest = request.newBuilder().url(location).build();
                                //关闭前一个连接
                                response.close();
                                response = chain.proceed(newRequest);
                            }
                        }
                        if (response.code() >= 300 && response.code() < 400) {
                            //获取重定向的地址
                            String location = response.headers().get("Location");
                            //重新构建请求
                            Request newRequest = request.newBuilder().url(location).build();
                            //关闭前一个连接
                            response.close();
                            response = chain.proceed(newRequest);
                        }
                        return response;
                    })
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private OkHttpUtils() {

    }

    public static Result doGet(String url, Map<String, Object> params, Map<String, String> headers) {
        if (params != null && params.size() > 0) {
            StringBuilder stringBuilder = new StringBuilder("?");
            params.forEach((k, v) -> stringBuilder.append(k).append("=").append(v).append("&"));
            String param = stringBuilder.toString();
            url = url + param.substring(0, param.lastIndexOf("&"));
        }
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        if (headers != null && headers.size() > 0) {
            headers.forEach(builder::header);
        }
        return call(url, builder);
    }

    public static Result doPost(String url, MediaType mediaType, String content, Map<String, String> headers) {
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        if (headers != null && headers.size() > 0) {
            headers.forEach(builder::header);
        }
        RequestBody body = RequestBody.create(mediaType, content);
        builder.post(body);
        return call(url, builder);
    }

    public static Result doPut(String url, MediaType mediaType, String content, Map<String, String> headers) {
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        if (headers != null && headers.size() > 0) {
            headers.forEach(builder::header);
        }
        RequestBody body = RequestBody.create(mediaType, content);
        builder.put(body);
        return call(url, builder);
    }

    public static Result doDelete(String url, MediaType mediaType, String content, Map<String, String> headers) {
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        if (headers != null && headers.size() > 0) {
            headers.forEach(builder::header);
        }
        RequestBody body = RequestBody.create(mediaType, content);
        builder.delete(body);
        return call(url, builder);
    }

    public static Result call(String url, okhttp3.Request.Builder builder) {
        Request request = builder.url(url).build();
        try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                LOGGER.warn("request failed! url: " + url + ", code: " + response.code());
            }
            return Result.create(response.isSuccessful(), response.body().string());
        } catch (IOException e) {
            LOGGER.error("request error! url: " + url);
            return Result.error(e.getMessage());
        }
    }

    public static class Result {
        public final boolean error;
        public final boolean isSuccessful;
        public final String content;

        private Result(boolean error, boolean isSuccessful, String content) {
            this.error = error;
            this.isSuccessful = isSuccessful;
            this.content = content;
        }

        private static Result create(boolean isSuccessful, String content) {
            return new Result(false, isSuccessful, content);
        }

        private static Result error(String content) {
            return new Result(true, false, content);
        }
    }

}
