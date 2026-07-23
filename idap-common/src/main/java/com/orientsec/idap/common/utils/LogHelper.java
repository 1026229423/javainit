package com.orientsec.idap.common.utils;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;

public class LogHelper {

    public static void log(Logger log, Object ... msg) {
        if(log != null) {
            log.info(StrUtil.join(" ",msg));
        }
    }
    public static void error(Logger log,Exception e,Object ... msg) {
        if(log != null) {
            log(log,msg);
            log.error(e.getMessage(), e);
        }
    }
}
