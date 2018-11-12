package com.platform.entity;

import java.io.Serializable;
import java.util.Date;

/**
 * 实体
 * 表名 sys_printer
 *
 * @author zoubin
 * @email 9379248@qq.com
 * @date 2018-11-09 12:15:00
 */
public class SysPrinterEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    //
    private Long id;
    //易联云应用ID
    private String appId;
    //易联云应用秘钥
    private String appKey;

    /**
     * 设置：
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取：
     */
    public Long getId() {
        return id;
    }
    /**
     * 设置：易联云应用ID
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * 获取：易联云应用ID
     */
    public String getAppId() {
        return appId;
    }
    /**
     * 设置：易联云应用秘钥
     */
    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    /**
     * 获取：易联云应用秘钥
     */
    public String getAppKey() {
        return appKey;
    }
}
