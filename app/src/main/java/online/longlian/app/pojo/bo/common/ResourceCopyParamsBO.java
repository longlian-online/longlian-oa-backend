package online.longlian.app.pojo.bo.common;

/** 只接受服务端生成的存储 key，目标不能是已签发上传地址的对象。 */
public record ResourceCopyParamsBO(String sourceKey, String targetKey) { }
