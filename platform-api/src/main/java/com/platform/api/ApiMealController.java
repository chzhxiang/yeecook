package com.platform.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.platform.annotation.IgnoreAuth;
import com.platform.entity.DishesVo;
import com.platform.entity.MealDisheEntity;
import com.platform.entity.UserNutritionMenuVo;
import com.platform.service.ApiDishesService;
import com.platform.service.ApiMealDisheService;
import com.platform.service.ApiMealService;
import com.platform.util.ApiBaseAction;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.platform.entity.MealEntity;
import com.platform.utils.PageUtils;
import com.platform.utils.Query;
import com.platform.utils.R;

/**
 * Controller
 *
 * @author zoubin
 * @email 9379248@qq.com
 * @date 2019-09-27 16:50:29
 */
@Api(tags = "门店套餐")
@RestController
@RequestMapping("/api/meal")
public class ApiMealController extends ApiBaseAction {
    @Autowired
    private ApiMealService mealService;
    @Autowired
    private ApiMealDisheService mealDisheService;
    @ApiOperation(value = "门店餐单信息")
    @IgnoreAuth
    @PostMapping("mealinfo")
    public Object mealinfo(){
        Map<String,Object> result=new HashMap<>();
        JSONObject storeinfo=this.getJsonRequest();
        try {
            Integer storeid=storeinfo.getInteger("stroeid");
            Integer menutype=storeinfo.getInteger("mealid");
            Map storemap=new HashMap();
            storemap.put("stroeid",storeid);
            storemap.put("mealid",menutype);
            List<MealEntity> mealEntityList=mealService.queryList(storemap);
            Integer  id=0;
            for(MealEntity mealEntity:mealEntityList){
                id=mealEntity.getId();
                Map meal=new HashMap();
                meal.put("mealid",id);
                String dishesname=null;
                List<MealDisheEntity> mealDisheEntityList=mealDisheService.queryList(meal);
                List mealinfo=new ArrayList();
                for(MealDisheEntity mealDisheEntity:mealDisheEntityList){
                    dishesname=mealDisheEntity.getDishesname();
                    mealinfo.add(dishesname);
                }
                mealEntity.setDishesname(mealinfo);
            }
            result.put("mealEntityList",mealEntityList);
        }catch (Exception e){
            return toResponsMsgSuccess("请联系管理员");
        }
        return result;
    }
}
