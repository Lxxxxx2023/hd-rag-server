package com.hd.rag.domain.ingestion.model.valobj;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据源配置
 */
@Data
public class DataSourceConfigVO {

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ManualUploadDataSourceConfig extends DataSourceConfigVO {
        /**
         * 文件路径
         */
        private String fileUrl;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class OSSDatasourceConfig extends DataSourceConfigVO {
        /**
         * 文件路径
         */
        private String fileUrl;
    }
    public static class FeiShuDataSourceConfig extends DataSourceConfigVO {

        // 暂未对接
    }
}
