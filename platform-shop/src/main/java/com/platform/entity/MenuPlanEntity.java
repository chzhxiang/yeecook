package com.platform.entity;

import com.platform.dao.UserNutritionMenuDao;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户膳食计划实体
 * 表名 menu_plan
 *
 * @author zoubin
 * @email 9379248@qq.com
 * @date 2019-06-19 19:14:23
 */
public class MenuPlanEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    //
    private Integer id;
    //
    private Integer nideshopUserId;
    //用户名称
    private String userName;
    //用户头像
    private String avatar;
    //主表食谱类型
    private String nutritionMenuType;
    //计划开始时间
    private Date serviceCycleSt;
    //机构名称
    private String cateringServiceOrgName;
    //食谱名称
    private String menuName;
    //计划结束时间
    private Date serviceCycleEt;
    //服务阶段
    private String serviceStage;
    //0:未签约 1:已签约
    private Integer menuStatus ;
    //检测周期
    private String inspectionCycle;
    //初始体重
    private Double weight;
    //食谱类型
    private String menuType;
    //菜品名称
    private String dishesName;
    //菜品图片
    private String dishesCoverPic;
    //营养原理
    private String nutritionPrinciple;


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
     * 设置：
     */
    public void setNideshopUserId(Integer nideshopUserId) {
        this.nideshopUserId = nideshopUserId;
    }

    /**
     * 获取：
     */
    public Integer getNideshopUserId() {
        return nideshopUserId;
    }
    /**
     * 设置：用户名称
     */
        public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * 获取：用户名称
     */
    public String getUserName() {
        return userName;
    }
    /**
     * 设置：用户头像
     */
    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    /**
     * 获取：用户头像
     */
    public String getAvatar() {
        return avatar;
    }
    /**
     * 设置：食谱类型
     */
    public void setNutritionMenuType(String nutritionMenuType) {
        this.nutritionMenuType = nutritionMenuType;
    }

    /**
     * 获取：食谱类型
     */
    public String getNutritionMenuType() {
        return nutritionMenuType;
    }
    /**
     * 设置：计划开始时间
     */
    public void setServiceCycleSt(Date serviceCycleSt) {
        this.serviceCycleSt = serviceCycleSt;
    }

    /**
     * 获取：计划开始时间
     */
    public Date getServiceCycleSt() {
        return serviceCycleSt;
    }
    /**
     * 设置：机构名称
     */
    public void setCateringServiceOrgName(String cateringServiceOrgName) {
        this.cateringServiceOrgName = cateringServiceOrgName;
    }

    /**
     * 获取：机构名称
     */
    public String getCateringServiceOrgName() {
        return cateringServiceOrgName;
    }
    /**
     * 设置：食谱名称
     */
    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    /**
     * 获取：食谱名称
     */
    public String getMenuName() {
        return menuName;
    }
    /**
     * 设置：计划结束时间
     */
    public void setServiceCycleEt(Date serviceCycleEt) {
        this.serviceCycleEt = serviceCycleEt;
    }

    /**
     * 获取：计划结束时间
     */
    public Date getServiceCycleEt() {
        return serviceCycleEt;
    }
    /**
     * 设置：0:未签约 1:已签约
     */
    public void setMenuStatus(Integer menuStatus) {
        this.menuStatus = menuStatus;
    }

    /**
     * 获取：0:未签约 1:已签约
     */
    public Integer getMenuStatus() {
        return menuStatus;
    }

    /**
     *   服务阶段
     **/
    public String getServiceStage() {
        return serviceStage;
    }

    public void setServiceStage(String serviceStage) {
        this.serviceStage = serviceStage;
    }

    /**
     *  检测周期
     **/
    public String getInspectionCycle() {
        return inspectionCycle;
    }

    public void setInspectionCycle(String inspectionCycle) {
        this.inspectionCycle = inspectionCycle;
    }
   /**
    *  初始体重
    **/
    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }
   /**
    *  食谱类型
    **/
    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }
    /**
     *  菜品名字
     **/
    public String getDishesName() {
        return dishesName;
    }

    public void setDishesName(String dishesName) {
        this.dishesName = dishesName;
    }
    /**
     *  菜品图片
     **/
    public String getDishesCoverPic() {
        return dishesCoverPic;
    }

    public void setDishesCoverPic(String dishesCoverPic) {
        this.dishesCoverPic = dishesCoverPic;
    }
   /**
    *  营养原理
    **/
    public String getNutritionPrinciple() {
        return nutritionPrinciple;
    }

    public void setNutritionPrinciple(String nutritionPrinciple) {
        this.nutritionPrinciple = nutritionPrinciple;
    }
}
