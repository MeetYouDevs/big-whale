package com.meiyou.bigwhale.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import okhttp3.*;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author Suxy
 * @date 2020/3/19
 * @description file description
 */
public class WebHdfsUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebHdfsUtils.class);

    private static final OkHttpClient OK_HTTP_CLIENT;

    static {
        Properties properties = new Properties();
        try (InputStream inputStream = OkHttpUtils.class.getClassLoader().getResourceAsStream("okhttp.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
            boolean proxyEnabled = Boolean.parseBoolean(properties.getProperty("okhttp.proxy.enabled", "false"));
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            if (proxyEnabled) {
                String server = properties.getProperty("okhttp.proxy.server");
                Integer port = Integer.parseInt(properties.getProperty("okhttp.proxy.port", "-1"));
                builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(server, port)));
            }
            OK_HTTP_CLIENT = builder
                    .connectTimeout(3, TimeUnit.SECONDS)
                    .writeTimeout(0, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.SECONDS)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private WebHdfsUtils() {

    }

    public static boolean mkdir(String webhdfsUrls, String path, String username, String owner) {
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        RequestBody body = RequestBody.create(OkHttpUtils.MEDIA_JSON, "");
        Exception exception = null;
        for (String webhdfsUrl : webhdfsUrls.split(",")) {
            String url = appendUrl(webhdfsUrl, path) + "?user.name=" + username + "&op=LISTSTATUS";
            Request request = builder.url(url).build();
            try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
                if (response.code() == 404) {
                    url = appendUrl(webhdfsUrl, path) + "?user.name=" + username + "&op=MKDIRS";
                    request = builder.url(url).put(body).build();
                    try (Response response1 = OK_HTTP_CLIENT.newCall(request).execute()) {
                        if (response1.isSuccessful()) {
                            if (owner != null) {
                                //设置拥有者
                                url = appendUrl(webhdfsUrl, path) + "?user.name=" + username + "&op=SETOWNER&owner=" + owner;
                                body = RequestBody.create(OkHttpUtils.MEDIA_JSON, "");
                                request = builder.url(url).put(body).build();
                                try (Response response2 = OK_HTTP_CLIENT.newCall(request).execute()) {
                                    if (!response2.isSuccessful()) {
                                        LOGGER.warn("SETOWNER unsuccessful, server response: " + response2.message());
                                    }
                                    return response2.isSuccessful();
                                }
                            }
                            return true;
                        } else {
                            LOGGER.warn("MKDIRS unsuccessful, server response: " + response1.message());
                            return false;
                        }
                    }
                } else {
                    return true;
                }
            } catch (Exception e) {
                exception = e;
            }
        }
        if (exception != null) {
            LOGGER.error("request error, last exception", exception);
        }
        return false;
    }

    public static boolean upload(MultipartFile file, String webhdfsUrls, String path, String username, String owner) {
        if (mkdir(webhdfsUrls, path, username, owner)) {
            okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
            RequestBody body = RequestBody.create(OkHttpUtils.MEDIA_OCTET_STREAM, new byte[0]);
            Exception exception = null;
            for (String webhdfsUrl : webhdfsUrls.split(",")) {
                String url = appendUrl(webhdfsUrl, path + "/" + file.getOriginalFilename()) + "?user.name=" + username + "&op=CREATE&overwrite=true";
                Request request = builder.url(url).put(body).build();
                try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
                    if (response.isRedirect()) {
                        url = response.header("Location");
                        body = RequestBody.create(OkHttpUtils.MEDIA_OCTET_STREAM, file.getBytes());
                        request = builder.url(url).put(body).build();
                        try (Response newResponse = OK_HTTP_CLIENT.newCall(request).execute()) {
                            //http协议支持的问题
                            if (newResponse.code() == 100) {
                                //设置拥有者
                                Thread.sleep(500);
                                url = appendUrl(webhdfsUrl, path + "/" + file.getOriginalFilename()) + "?user.name=" + username + "&op=SETOWNER&owner=" + owner;
                                body = RequestBody.create(OkHttpUtils.MEDIA_OCTET_STREAM, new byte[0]);
                                request = builder.url(url).put(body).build();
                                try (Response newResponse1 = OK_HTTP_CLIENT.newCall(request).execute()) {
                                    if (newResponse1.isSuccessful()) {
                                        //设置权限
                                        url = appendUrl(webhdfsUrl, path + "/" + file.getOriginalFilename()) + "?user.name=" + username + "&op=SETPERMISSION&permission=755";
                                        request = builder.url(url).put(body).build();
                                        try (Response newResponse2 = OK_HTTP_CLIENT.newCall(request).execute()) {
                                            if (!newResponse2.isSuccessful()) {
                                                LOGGER.warn("SETPERMISSION unsuccessful, server response: " + newResponse2.message());
                                            }
                                            return newResponse2.isSuccessful();
                                        }
                                    } else {
                                        LOGGER.warn("SETOWNER unsuccessful, server response: " + newResponse1.message());
                                    }
                                }
                            } else {
                                LOGGER.warn("CREATE unsuccessful, server response: " + newResponse.message());
                            }
                        }
                    }
                } catch (Exception e) {
                    exception = e;
                }
            }
            if (exception != null) {
                LOGGER.error("request error, last exception", exception);
            }
        }
        return false;
    }

    public static void download(HttpServletResponse httpServletResponse, String webhdfsUrls, String path, String username) {
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        Exception exception = null;
        for (String webhdfsUrl : webhdfsUrls.split(",")) {
            String url = appendUrl(webhdfsUrl, path) + "?user.name=" + username + "&op=OPEN";
            Request request = builder.url(url).build();
            try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    IOUtils.copy(response.body().byteStream(), httpServletResponse.getOutputStream());
                    return;
                }
                if (response.isRedirect()) {
                    url = response.header("Location");
                    request = builder.url(url).build();
                    try (Response newResponse = OK_HTTP_CLIENT.newCall(request).execute()) {
                        IOUtils.copy(newResponse.body().byteStream(), httpServletResponse.getOutputStream());
                        return;
                    }
                }
            } catch (Exception e) {
                exception = e;
            }
        }
        if (exception != null) {
            LOGGER.error("request error, last exception", exception);
        }
    }

    public static int delete(String webhdfsUrls, String path, String username) {
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        RequestBody body = RequestBody.create(OkHttpUtils.MEDIA_JSON, "");
        Exception exception = null;
        for (String webhdfsUrl : webhdfsUrls.split(",")) {
            String url = appendUrl(webhdfsUrl, path) + "?user.name=" + username + "&op=DELETE&recursive=false";
            Request request = builder.url(url).delete(body).build();
            try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOGGER.warn("DELETE unsuccessful, server response: " + response.message());
                }
                return response.code();
            } catch (Exception e) {
                exception = e;
            }
        }
        if (exception != null) {
            LOGGER.error("request error, last exception", exception);
        }
        return -1;
    }

    public static JSONArray list(String webhdfsUrls, String path, String username) {
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder();
        Exception exception = null;
        for (String webhdfsUrl : webhdfsUrls.split(",")) {
            String url = appendUrl(webhdfsUrl, path) + "?user.name=" + username + "&op=LISTSTATUS";
            Request request = builder.url(url).build();
            try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return JSON.parseObject(response.body().string()).getJSONObject("FileStatuses").getJSONArray("FileStatus");
                } else {
                    if (response.code() != 404) {
                        LOGGER.warn("LISTSTATUS unsuccessful, server response: " + response.message());
                    }
                }
            } catch (Exception e) {
                exception = e;
            }
        }
        if (exception != null) {
            LOGGER.error("request error, last exception", exception);
        }
        return null;
    }

    private static String appendUrl(String webhdfsUrl, String path) {
        if (!webhdfsUrl.endsWith("/")) {
            webhdfsUrl += "/";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return webhdfsUrl + "webhdfs/v1" + path;
    }

}
