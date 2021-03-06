package org.jw.bean.mvc;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler处理结果，返回如果是jsp
 * Created by CaiDongYu on 2016/4/11.
 */
public class View {
    /**
     * 视图路径
     */
    private String path;

    /**
     * 模型数据
     */
    private Map<String,Object> model;

    public View(String path){
        this.path = path;
        model = new HashMap<>();
    }

    public  View addModel(String key,Object value){
        model.put(key,value);
        return this;
    }

    public String getPath() {
        return path;
    }

    public Map<String, Object> getModel() {
        return model;
    }
}
