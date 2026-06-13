package com.hd.rag.domain.ingestion.adapter.port;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 对象存储服务端口接口
 */
public interface IFileStoragePort {

    /**
     * 上传文件到根目录
     * @param file 文件
     * @return 存储服务中的文件路径（含路径前缀）
     */
    default String uploadFile(MultipartFile file) throws Exception {
        return uploadFile(file, "");
    }

    /**
     * 上传文件到指定路径
     * @param file       文件
     * @param pathPrefix 存储路径前缀（如 "kb/KB001/"，不以 / 开头，以 / 结尾）
     * @return 存储服务中的文件路径（含路径前缀）
     */
    String uploadFile(MultipartFile file, String pathPrefix) throws Exception;

    /**
     * 获取文件 InputStream
     * @param fileName 文件路径（含路径前缀）
     * @return 文件流
     */
    InputStream downloadFile(String fileName) throws Exception;

    /**
     * 删除文件
     * @param fileName 文件路径（含路径前缀）
     */
    void deleteFile(String fileName) throws Exception;
}
