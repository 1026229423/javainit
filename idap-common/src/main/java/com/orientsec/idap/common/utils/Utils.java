package com.orientsec.idap.common.utils;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.StrUtil;

import java.util.*;
import java.util.stream.Collectors;

public class Utils {

    private final static String SPLIT_CHAR = "|";

    public static String convertToMarkdownTable(List<Map<String, Object>> dataList) {
        StringBuilder markdownTable = new StringBuilder();
        if (CollectionUtil.isNotEmpty(dataList)) {
            List<String> headers = new ArrayList<>(dataList.get(0).keySet());

            // 构建表头行和分隔行
            markdownTable.append(SPLIT_CHAR).append(String.join(SPLIT_CHAR, headers)).append(SPLIT_CHAR).append(StrPool.LF);
            markdownTable.append(SPLIT_CHAR).append(headers.stream().map(h -> StrPool.DASHED+StrPool.DASHED+StrPool.DASHED)
                    .collect(Collectors.joining(SPLIT_CHAR))).append(SPLIT_CHAR).append(StrPool.LF);

            // 构建数据行
            for (Map<String, Object> rowMap : dataList) {
                markdownTable.append(SPLIT_CHAR)
                        .append(headers.stream()
                                .map(header -> String.valueOf(rowMap.get(header)))
                                .collect(Collectors.joining(SPLIT_CHAR)))
                        .append(SPLIT_CHAR).append(StrPool.LF);
            }
        }
        return markdownTable.toString();
    }

    public static void isNotNull(Object obj,String msg){
        if(obj != null){
            if(obj instanceof String){
                if(StrUtil.isBlank((String)obj)){
                    throw new IllegalArgumentException(msg);
                }
            }
        }else {
            throw new IllegalArgumentException(msg);
        }
    }
}
