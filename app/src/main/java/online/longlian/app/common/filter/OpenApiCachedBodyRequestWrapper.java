package online.longlian.app.common.filter;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 缓存整个请求体的包装请求：@RequestBody 反序列化可反复读，
 * 幂等哈希与快照都能拿到原始报文。
 */
public class OpenApiCachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    public OpenApiCachedBodyRequestWrapper(HttpServletRequest request, byte[] body) {
        super(request);
        // 防御拷贝：避免调用方在请求处理期间改动同一数组
        this.body = body.clone();
    }

    public byte[] getBody() {
        return body.clone();
    }

    public String getBodyText() {
        return new String(body, StandardCharsets.UTF_8);
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream buffer = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return buffer.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(jakarta.servlet.ReadListener listener) {
                // 同步读取，不需要异步监听
            }

            @Override
            public int read() {
                return buffer.read();
            }
        };
    }

    @Override
    public java.io.BufferedReader getReader() {
        return new java.io.BufferedReader(
                new java.io.InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public int getContentLength() {
        return body.length;
    }

    @Override
    public long getContentLengthLong() {
        return body.length;
    }
}
