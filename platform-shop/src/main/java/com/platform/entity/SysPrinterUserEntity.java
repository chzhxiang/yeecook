package com.platform.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

/**
 * 实体
 * 表名 sys_printer_user
 *
 * @author zoubin
 * @email 9379248@qq.com
 * @date 2018-11-28 14:31:26
 */
public class SysPrinterUserEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    //
    private Long id;
    //打印机设备码
    private String machineCode;
    //关联用户id
    private Long userId;
    //关联用户名称
    private String userName;
    //打印机设备密钥
    private String machineKey;
    //是否启用：0关闭1开启
    private Integer isOpen;
    //打印机名称
    private String printName;
    //打印机类型：0001条码打印，0002普通打印，0003咕咕机打印
    private String printType;

    private Long deptId;
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
     * 设置：打印机设备码
     */
    public void setMachineCode(String machineCode) {
        this.machineCode = machineCode;
    }

    /**
     * 获取：打印机设备码
     */
    public String getMachineCode() {
        return machineCode;
    }
    /**
     * 设置：关联用户id
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * 获取：关联用户id
     */
    public Long getUserId() {
        return userId;
    }
    /**
     * 设置：关联用户名称
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * 获取：关联用户名称
     */
    public String getUserName() {
        return userName;
    }
    /**
     * 设置：打印机设备密钥
     */
    public void setMachineKey(String machineKey) {
        this.machineKey = machineKey;
    }

    /**
     * 获取：打印机设备密钥
     */
    public String getMachineKey() {
        return machineKey;
    }
    /**
     * 设置：是否启用：0关闭1开启
     */
    public void setIsOpen(Integer isOpen) {
        this.isOpen = isOpen;
    }

    /**
     * 获取：是否启用：0关闭1开启
     */
    public Integer getIsOpen() {
        return isOpen;
    }
    /**
     * 设置：打印机名称
     */
    public void setPrintName(String printName) {
        this.printName = printName;
    }

    /**
     * 获取：打印机名称
     */
    public String getPrintName() {
        return printName;
    }
    /**
     * 设置：打印机类型：0001条码打印，0002普通打印，0003咕咕机打印
     */
    public void setPrintType(String printType) {
        this.printType = printType;
    }

    /**
     * 获取：打印机类型：0001条码打印，0002普通打印，0003咕咕机打印
     */
    public String getPrintType() {
        return printType;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }
}
