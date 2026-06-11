package com.hd.rag.domain.ingestion.adapter.port;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 对象存储服务端口接口
 */
public interface IFileStoragePort {

    /**
     * 上传文件
     * @param file 文件
     * @return 存储服务中的唯一文件名
     */
    String uploadFile(MultipartFile file) throws Exception;

    /**
     * 获取文件 InputStream
     * @param fileName 文件名
     * @return 文件流
     */
    InputStream downloadFile(String fileName) throws Exception;

    /**
     * 删除文件
     * @param fileName 文件明
     */
    void deleteFile(String fileName) throws Exception;
}
