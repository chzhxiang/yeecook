package com.platform.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;
import com.platform.entity.UserEntity;
import com.platform.service.UserService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.platform.entity.UserHealthReportEntity;
import com.platform.service.UserHealthReportService;
import com.platform.utils.PageUtils;
import com.platform.utils.Query;
import com.platform.utils.R;

/**
 * 用户健康报告表
Controller
 *
 * @author zoubin
 * @email 9379248@qq.com
 * @date 2019-06-16 14:14:48
 */
@RestController
@RequestMapping("userhealthreport")
public class UserHealthReportController {
    @Autowired
    private UserHealthReportService userHealthReportService;
    @Autowired
    private UserService userService;
    /**
     * 查看列表
     */
    @RequestMapping("/list")
    @RequiresPermissions("userhealthreport:list")
    public R list(@RequestParam Map<String, Object> params) {
        //查询列表数据
        Query query = new Query(params);

        List<UserHealthReportEntity> userHealthReportList = userHealthReportService.queryList(query);
        int total = userHealthReportService.queryTotal(query);

        PageUtils pageUtil = new PageUtils(userHealthReportList, total, query.getLimit(), query.getPage());

        return R.ok().put("page", pageUtil);
    }

    /**
     * 查看信息
     */
    @RequestMapping("/info/{id}")
    @RequiresPermissions("userhealthreport:info")
    public R info(@PathVariable("id") Integer id) {
        UserHealthReportEntity userHealthReport = userHealthReportService.queryObject(id);

        return R.ok().put("userHealthReport", userHealthReport);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    @RequiresPermissions("userhealthreport:save")
    public R save(@RequestBody UserHealthReportEntity userHealthReport) {
        String nickname=userHealthReport.getNickname();
        Map params=new HashMap();
        params.put("nickname",nickname);
        List<UserEntity> userEntityList=userService.queryList(params);
        for(UserEntity userEntityiTem:userEntityList){
            Integer userid=userEntityiTem.getId();
            userHealthReport.setNideshopUserId(userid);
        }
        userHealthReportService.save(userHealthReport);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @RequiresPermissions("userhealthreport:update")
    public R update(@RequestBody UserHealthReportEntity userHealthReport) {
        String nickname=userHealthReport.getNickname();
        Map params=new HashMap();
        params.put("nickname",nickname);
        List<UserEntity> userEntityList=userService.queryList(params);
        for(UserEntity userEntityiTem:userEntityList){
            Integer userid=userEntityiTem.getId();
            userHealthReport.setNideshopUserId(userid);
        }
        userHealthReportService.update(userHealthReport);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    @RequiresPermissions("userhealthreport:delete")
    public R delete(@RequestBody Integer[] ids) {
        userHealthReportService.deleteBatch(ids);

        return R.ok();
    }

    /**
     * 查看所有列表
     */
    @RequestMapping("/queryAll")
    public R queryAll(@RequestParam Map<String, Object> params) {

        List<UserHealthReportEntity> list = userHealthReportService.queryList(params);

        return R.ok().put("list", list);
    }

    /**
     *  查看用户最新的身体数据
     **/
    @RequestMapping("/queryUserReport/{id}")
    public R queryUserReport(@PathVariable("id") Integer id){
        if (id !=null){
       UserHealthReportEntity userReport=userHealthReportService.queryUserReport(id);
        return R.ok().put("userReport",userReport);
        }else {
            String msg="请重新选择用户";
        return R.error().put("msg",msg);
        }
    }
}
