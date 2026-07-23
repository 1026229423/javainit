package com.orientsec.idap.core.constant;

import cn.hutool.core.util.ArrayUtil;
import com.orientsec.idap.common.constants.YesNoEnum;
import com.orientsec.idap.core.constant.bean.ConstDesc;
import com.orientsec.idap.core.constant.bean.ValueDesc;
import freemarker.template.TemplateExceptionHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.beanutils.BeanUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class GenEnumjsMain {

    private static final String _PROJECT_PATH = System.getProperty("user.dir")+"/idap-ddl";//项目在硬盘上的基础路径
    private static final String TEMPLATE_FILE_PATH = _PROJECT_PATH + "/template";//模板位置

    public static void main(String[] args) throws Exception{
        genEnumJs("common.js", YesNoEnum.class);
    }


    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static ConstDesc getEnumValues(Class t) throws Exception {

        ConstDesc e = new ConstDesc();
        e.setName(t.getSimpleName());
        List<ValueDesc> values = new ArrayList<>();

        List<Enum> list = new ArrayList<Enum>(EnumSet.allOf(t));

        for (Enum o : list) {
            ValueDesc e1 = new ValueDesc();
//            String name = o.name();
            String value = BeanUtils.getProperty(o, "value");
            String name = BeanUtils.getProperty(o, "name");
            String desc = name;
            try {
                desc = BeanUtils.getProperty(o, "desc");
            }catch(Exception exp) {

            }
            e1.setName(name);
            e1.setValue(value);
            e1.setDesc(desc);
            values.add(e1);
        }
        e.setValues(values);
        return e;
    }

    public static void genEnumJs(String fileName, Class... types) {
        List<ConstDesc> list = new ArrayList<>();
        if (ArrayUtil.isNotEmpty(types)) {
            Arrays.stream(types).forEach(new Consumer<Class>() {
                @Override
                public void accept(Class aClass) {
                    try {
                        list.add(getEnumValues(aClass));
                    } catch (Exception e) {
                        e.printStackTrace();
                        ;
                    }
                }
            });
            genEnumJs(list, fileName);
        }
    }

    private static void genEnumJs(List<ConstDesc> list,String fileName) {
        try {
            freemarker.template.Configuration cfg = getConfiguration();

            Map<String, Object> data = new HashMap<>();
            data.put("list", list);

            if(StringUtils.isBlank(fileName)){
                fileName = "enums.js";
            }
            String fullpath = _PROJECT_PATH+"/../idap-ui/src/const/"+fileName;
            File jsfile = new File(fullpath);
            if(jsfile.exists()) {
                jsfile.delete();
            }
            System.out.println("generating const js "+fullpath);
            cfg.getTemplate("enum.js.ftl").process(data,new FileWriter(jsfile));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static freemarker.template.Configuration getConfiguration() throws IOException {
        freemarker.template.Configuration cfg = new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_23);
        cfg.setDirectoryForTemplateLoading(new File(TEMPLATE_FILE_PATH));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE_HANDLER);
        return cfg;
    }
}
