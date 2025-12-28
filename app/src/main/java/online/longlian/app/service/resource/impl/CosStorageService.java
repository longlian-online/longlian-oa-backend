package online.longlian.app.service.resource.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.region.Region;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.pojo.vo.PresignedUpload;
import online.longlian.app.service.resource.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Date;

@Service
public class CosStorageService implements StorageService {

    @Value("${storage.cos.bucket}")
    private String bucket;

    @Value("${storage.cos.region}")
    private String region;

    @Value("${storage.cos.secret-id}")
    private String secretId;

    @Value("${storage.cos.secret-key}")
    private String secretKey;

    @Override
    public PresignedUpload generatePresignedUpload(Resource resource) {

        // 使用 secretId 和 secretKey 创建 COS 凭证对象
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 指定 COS 所在区域，例如 ap-guangzhou
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        //创建 COS 客户端
        COSClient cosClient = new COSClient(cred, clientConfig);
        //设置 URL 过期时间，当前时间 + 10 分钟，过期后 URL 将失效
        Date expiration = new Date(System.currentTimeMillis() + 10 * 60 * 1000);
        //构建生成预签名 URL 的请求对象
        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(bucket, resource.getKey(), HttpMethodName.PUT);
        // 设置 URL 过期时间
        request.setExpiration(expiration);
        //调用 COS 客户端生成预签名 URL
        URL url = cosClient.generatePresignedUrl(request);
        //返回结果封装
        return new PresignedUpload(url.toString(), resource.getKey());
    }

}
