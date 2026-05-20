package com.hd.rag.domain.index.model.valObj;

import lombok.Getter;

@Getter
public enum DataSourceType {
   ;

    private String type;
    private String desc;

    DataSourceType(String type, String desc) {
    }

}
