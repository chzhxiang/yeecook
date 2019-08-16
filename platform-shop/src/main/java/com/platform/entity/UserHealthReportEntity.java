package com.platform.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户健康报告表
实体
 * 表名 user_health_report
 *
 * @author zoubin
 * @email 9379248@qq.com
 * @date 2019-06-16 14:14:48
 */
@Data
public class UserHealthReportEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    //
    private Integer id;
    //用户id
    private Integer nideshopUserId;
    //用户名称
    private  String nickname;
    //检测时间
    private Date detectionTime;
    //更新时间
    private Date updateTime;
    //体重
    private Double weight;
    //
    private Double bmi;
    //体脂率
    private Double bodyFatRade;
    //皮下脂肪率
    private Double subFatPercentage;
    //基础代谢量
    private Double basicMetabolism;
    //内脏脂肪等级
    private String visFatGrade;
    //体水分率
    private Double bodyWaterRate;
    //蛋白质
    private String protein;
    //骨量
    private Double boneMass;
    //骨骼肌率
    private String skeletalMuscle;
    //身体体型
    private String bodyShape;
    //身体年龄
    private String bodyAge;
    //健康评分
    private String sclscore;
    /**
     * 设置：
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * 获取：
     */
    public Integer getId() {
        return id;
    }
    /**
     * 设置：用户id
     */
    public void setNideshopUserId(Integer nideshopUserId) {
        this.nideshopUserId = nideshopUserId;
    }

    /**
     * 获取：用户id
     */
    public Integer getNideshopUserId() {
        return nideshopUserId;
    }
    /**
     * 设置：检测时间
     */
    public void setDetectionTime(Date detectionTime) {
        this.detectionTime = detectionTime;
    }

    /**
     * 获取：检测时间
     */
    public Date getDetectionTime() {
        return detectionTime;
    }
    /**
     * 设置：更新时间
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 获取：更新时间
     */
    public Date getUpdateTime() {
        return updateTime;
    }
    /**
     * 设置：体重
     */
    public void setWeight(Double weight) {
        this.weight = weight;
    }

    /**
     * 获取：体重
     */
    public Double getWeight() {
        return weight;
    }
    /**
     * 设置：
     */
    public void setBmi(Double bmi) {
        this.bmi = bmi;
    }

    /**
     * 获取：
     */
    public Double getBmi() {
        return bmi;
    }
    /**
     * 设置：体脂率
     */
    public void setBodyFatRade(Double bodyFatRade) {
        this.bodyFatRade = bodyFatRade;
    }

    /**
     * 获取：体脂率
     */
    public Double getBodyFatRade() {
        return bodyFatRade;
    }
    /**
     * 设置：皮下脂肪率
     */
    public void setSubFatPercentage(Double subFatPercentage) {
        this.subFatPercentage = subFatPercentage;
    }

    /**
     * 获取：皮下脂肪率
     */
    public Double getSubFatPercentage() {
        return subFatPercentage;
    }
    /**
     * 设置：基础代谢量
     */
    public void setBasicMetabolism(Double basicMetabolism) {
        this.basicMetabolism = basicMetabolism;
    }

    /**
     * 获取：基础代谢量
     */
    public Double getBasicMetabolism() {
        return basicMetabolism;
    }
    /**
     * 设置：内脏脂肪等级
     */
    public void setVisFatGrade(String visFatGrade) {
        this.visFatGrade = visFatGrade;
    }

    /**
     * 获取：内脏脂肪等级
     */
    public String getVisFatGrade() {
        return visFatGrade;
    }
    /**
     * 设置：体水分率
     */
    public void setBodyWaterRate(Double bodyWaterRate) {
        this.bodyWaterRate = bodyWaterRate;
    }

    /**
     * 获取：体水分率
     */
    public Double getBodyWaterRate() {
        return bodyWaterRate;
    }
    /**
     * 设置：蛋白质
     */
    public void setProtein(String protein) {
        this.protein = protein;
    }

    /**
     * 获取：蛋白质
     */
    public String getProtein() {
        return protein;
    }
    /**
     * 设置：骨量
     */
    public void setBoneMass(Double boneMass) {
        this.boneMass = boneMass;
    }

    /**
     * 获取：骨量
     */
    public Double getBoneMass() {
        return boneMass;
    }
    /**
     * 设置：骨骼肌率
     */
    public void setSkeletalMuscle(String skeletalMuscle) {
        this.skeletalMuscle = skeletalMuscle;
    }

    /**
     * 获取：骨骼肌率
     */
    public String getSkeletalMuscle() {
        return skeletalMuscle;
    }
}
