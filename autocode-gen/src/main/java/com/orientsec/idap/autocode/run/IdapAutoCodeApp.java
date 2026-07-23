package com.orientsec.idap.autocode.run;

import com.orientsec.idap.autocode.config.AutoCodeBase;

import java.util.ArrayList;
import java.util.List;

/**
 * 根据数据库表自动生成model,dao,service,service-impl,controller,
 *
 */
public class IdapAutoCodeApp {
    public static void main(String[] args) {
        List<String> table = new ArrayList<>();
        table.add("idap_test_user");
        AutoCodeBase.doStart("idap", table, args);
    }
}
