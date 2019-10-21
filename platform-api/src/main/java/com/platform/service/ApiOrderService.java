package com.platform.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.platform.entity.*;
import com.platform.printer.PrinterSupplierTanks;
import com.platform.printer.yly.PrinterSupplierTemplate;
import com.platform.printer.PrinterTanks;
import com.platform.printer.yly.PrinterTemplate;
import com.platform.printer.vo.YeecookSupplierVo;
import com.platform.printer.vo.YeecookVo;
import com.platform.utils.JsonUtil;
import com.platform.xss.SQLFilter;
import com.qiniu.util.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;
import com.platform.cache.J2CacheUtils;
import com.platform.dao.ApiAddressMapper;
import com.platform.dao.ApiCartMapper;
import com.platform.dao.ApiCouponMapper;
import com.platform.dao.ApiOrderGoodsMapper;
import com.platform.dao.ApiOrderMapper;
import com.platform.util.CommonUtil;


@Service
public class ApiOrderService {
    @Autowired
    private ApiOrderMapper orderDao;
    @Autowired
    private ApiAddressMapper apiAddressMapper;
    @Autowired
    private ApiCartMapper apiCartMapper;
    @Autowired
    private ApiCouponMapper apiCouponMapper;
    @Autowired
    private ApiOrderMapper apiOrderMapper;
    @Autowired
    private ApiOrderGoodsMapper apiOrderGoodsMapper;
    @Autowired
    private ApiProductService productService;
    @Autowired
    private ApiGoodsSpecificationService goodsSpecificationService;
    @Autowired
    private ApiOrderSupplierService apiOrderSupplierService;
    @Autowired
    private ApiOrderService apiOrderService;
    @Autowired
    private ApiOrderGoodsService apiOrderGoodsService;
    @Autowired
    private ApiSysPrinterService sysPrinterService;
    @Autowired
    private ApiStroeService stroeService;
    @Autowired
    private ApiMealService mealService;
    @Autowired
    private ApiOrderMenuService orderMenuService;
    @Autowired
    private ApiCartService cartService;

    public OrderVo queryObject(Integer id) {
        return orderDao.queryObject(id);
    }


    public List<OrderVo> queryList(Map<String, Object> map) {
        return orderDao.queryList(map);
    }


    public int queryTotal(Map<String, Object> map) {
        return orderDao.queryTotal(map);
    }


    public void save(OrderVo order) {
        orderDao.save(order);
    }


    public int update(OrderVo order) {
        return orderDao.update(order);
    }


    public void delete(Integer id) {
        orderDao.delete(id);
    }


    public void deleteBatch(Integer[] ids) {
        orderDao.deleteBatch(ids);
    }


    @Transactional
    public Map<String, Object> submit(JSONObject jsonParam, UserVo loginUser,Integer couponId, String type,String postscript,Integer addressId) {
        Map<String, Object> resultObj = new HashMap<String, Object>();
//        AddressVo addressVo = jsonParam.getObject("checkedAddress",AddressVo.class);
        AddressVo addressVo = apiAddressMapper.queryObject(addressId);
        Integer freightPrice = 0;
        // * 获取要购买的商品
        List<CartVo> checkedGoodsList = new ArrayList<>();
        BigDecimal goodsTotalPrice;
        if (type.equals("cart")) {
            Map<String, Object> param = new HashMap<String, Object>();
            param.put("user_id", loginUser.getUserId());
            param.put("session_id", 1);
            param.put("checked", 1);
            checkedGoodsList = apiCartMapper.queryList(param);
            if (null == checkedGoodsList) {
                resultObj.put("errno", 400);
                resultObj.put("errmsg", "请选择商品");
                return resultObj;
            }
            //统计商品总价
            goodsTotalPrice = new BigDecimal(0.00);
            for (CartVo cartItem : checkedGoodsList) {
                goodsTotalPrice = goodsTotalPrice.add(cartItem.getRetail_price().multiply(new BigDecimal(cartItem.getNumber())));
            }
        } else {
            BuyGoodsVo goodsVo = (BuyGoodsVo) J2CacheUtils.get(J2CacheUtils.SHOP_CACHE_NAME, "goods" + loginUser.getUserId());
            ProductVo productInfo = productService.queryObject(goodsVo.getProductId());
            //计算订单的费用
            //商品总价
            goodsTotalPrice = productInfo.getRetail_price().multiply(new BigDecimal(goodsVo.getNumber()));
            String[] goodsSepcifitionValue = null;
            if (null != productInfo.getGoods_specification_ids()) {
                Map specificationParam = new HashMap();
                specificationParam.put("ids", (productInfo.getGoods_specification_ids()+",").split(","));
                specificationParam.put("goodsId", goodsVo.getGoodsId());
                List<GoodsSpecificationVo> specificationEntities = goodsSpecificationService.queryList(specificationParam);
                goodsSepcifitionValue = new String[specificationEntities.size()];
                for (int i = 0; i < specificationEntities.size(); i++) {
                    goodsSepcifitionValue[i] = specificationEntities.get(i).getValue();
                }
            }
            CartVo cartVo = new CartVo();
            BeanUtils.copyProperties(productInfo, cartVo);
            cartVo.setNumber(goodsVo.getNumber());
            cartVo.setProduct_id(goodsVo.getProductId());
            if (null != goodsSepcifitionValue) {
                cartVo.setGoods_specifition_name_value(StringUtils.join(goodsSepcifitionValue, ";"));
            }
            cartVo.setGoods_specifition_ids(productInfo.getGoods_specification_ids());
            checkedGoodsList.add(cartVo);
        }

        //获取订单使用的优惠券
        BigDecimal couponPrice = new BigDecimal(0.00);
        CouponVo couponVo = null;
        if (couponId != null && couponId != 0) {
            couponVo = apiCouponMapper.getUserCoupon(couponId);
            if (couponVo != null && couponVo.getCoupon_status() == 1) {
                couponPrice = couponVo.getType_money();
            }
        }

        //订单价格计算
        BigDecimal orderTotalPrice = goodsTotalPrice.add(new BigDecimal(freightPrice)); //订单的总价

        BigDecimal actualPrice = orderTotalPrice.subtract(couponPrice);  //减去其它支付的金额后，要实际支付的金额

        Long currentTime = System.currentTimeMillis() / 1000;

        //
        OrderVo orderInfo = new OrderVo();
        orderInfo.setOrder_sn(CommonUtil.generateOrderNumber());
        orderInfo.setUser_id(loginUser.getUserId());
        //收货地址和运费
        orderInfo.setConsignee(addressVo.getUserName());
        orderInfo.setMobile(addressVo.getTelNumber());
        orderInfo.setCountry(addressVo.getNationalCode());
        orderInfo.setProvince(addressVo.getProvinceName());
        orderInfo.setCity(addressVo.getCityName());
        orderInfo.setDistrict(addressVo.getCountyName());
        orderInfo.setAddress(addressVo.getDetailInfo());
        //
        orderInfo.setFreight_price(freightPrice);
        //留言
        orderInfo.setPostscript(postscript);
        //使用的优惠券
        orderInfo.setCoupon_id(couponId);
        orderInfo.setCoupon_price(couponPrice);
        orderInfo.setAdd_time(new Date());
        orderInfo.setGoods_price(goodsTotalPrice);
        orderInfo.setOrder_price(orderTotalPrice);
        orderInfo.setActual_price(actualPrice);
        // 待付款
        orderInfo.setOrder_status(0);
        orderInfo.setShipping_status(0);
        orderInfo.setPay_status(0);
        orderInfo.setShipping_id(0);
        orderInfo.setShipping_fee(new BigDecimal(0));
        orderInfo.setIntegral(0);
        orderInfo.setIntegral_money(new BigDecimal(0));
        if (type.equals("cart")) {
            orderInfo.setOrder_type("1");
        } else {
            orderInfo.setOrder_type("4");
        }

        //开启事务，插入订单信息和订单商品
        apiOrderMapper.save(orderInfo);
        if (null == orderInfo.getId()) {
            resultObj.put("errno", 1);
            resultObj.put("errmsg", "订单提交失败");
            return resultObj;
        }
        //统计商品总价
        List<OrderGoodsVo> orderGoodsData = new ArrayList<OrderGoodsVo>();
        for (CartVo goodsItem : checkedGoodsList) {
            OrderGoodsVo orderGoodsVo = new OrderGoodsVo();
            orderGoodsVo.setOrder_id(orderInfo.getId());
            orderGoodsVo.setGoods_id(goodsItem.getGoods_id());
            orderGoodsVo.setGoods_sn(goodsItem.getGoods_sn());
            orderGoodsVo.setProduct_id(goodsItem.getProduct_id());
            orderGoodsVo.setGoods_name(goodsItem.getGoods_name());
            orderGoodsVo.setList_pic_url(goodsItem.getList_pic_url());
            orderGoodsVo.setMarket_price(goodsItem.getMarket_price());
            orderGoodsVo.setRetail_price(goodsItem.getRetail_price());
            orderGoodsVo.setNumber(goodsItem.getNumber());
            orderGoodsVo.setGoods_specifition_name_value(goodsItem.getGoods_specifition_name_value());
            orderGoodsVo.setGoods_specifition_ids(goodsItem.getGoods_specifition_ids());
            orderGoodsData.add(orderGoodsVo);
            apiOrderGoodsMapper.save(orderGoodsVo);
        }

        //清空已购买的商品
        apiCartMapper.deleteByCart(loginUser.getUserId(), 1, 1);
        resultObj.put("errno", 0);
        resultObj.put("errmsg", "订单提交成功");
        //
        Map<String, OrderVo> orderInfoMap = new HashMap<String, OrderVo>();
        orderInfoMap.put("orderInfo", orderInfo);
        //
        resultObj.put("data", orderInfoMap);
        // 优惠券标记已用
        if (couponVo != null && couponVo.getCoupon_status() == 1) {
            couponVo.setCoupon_status(2);
            apiCouponMapper.updateUserCoupon(couponVo);
        }
        getGroupByGoosforDept(orderInfo.getId());
        return resultObj;
    }
    @Transactional
    public Map<String,Object>  mealsubmit(JSONObject jsonParam, UserVo loginUser,Integer couponId, String type,String postscript,Integer addressId,Integer num,Integer stroeid){
        Map<String, Object> resultObj = new HashMap<String, Object>();
//        AddressVo addressVo = jsonParam.getObject("checkedAddress",AddressVo.class);
        AddressVo addressVo = apiAddressMapper.queryObject(addressId);
        Integer freightPrice = 0;
        // * 获取要购买的商品
        List<CartVo> checkedGoodsList = new ArrayList<>();
        BigDecimal goodsTotalPrice = null;
        BigDecimal total=new BigDecimal(0.00);
        Integer deliveryfee=0;
        if (type.equals("cart")) {
            Map<String, Object> param = new HashMap<String, Object>();
            param.put("user_id", loginUser.getUserId());
            param.put("session_id", 1);
            param.put("checked", 1);
            checkedGoodsList = apiCartMapper.queryList(param);
            if (null == checkedGoodsList) {
                resultObj.put("errno", 400);
                resultObj.put("errmsg", "请选择商品");
                return resultObj;
            }
            //统计商品总价
            goodsTotalPrice = new BigDecimal(0.00);
            for (CartVo cartItem : checkedGoodsList) {
                Integer number=cartItem.getNumber();
                goodsTotalPrice = cartItem.getRetail_price().multiply(new BigDecimal(number));
                deliveryfee=cartItem.getDeliveryfee();
                total=total.add(goodsTotalPrice);
            }
        } else {
//            BuyGoodsVo goodsVo = (BuyGoodsVo) J2CacheUtils.get(J2CacheUtils.SHOP_CACHE_NAME, "goods" + loginUser.getUserId());
//            ProductVo productInfo = productService.queryObject(goodsVo.getProductId());
//            //计算订单的费用
//            //商品总价
            Map mapcat=new HashMap();
            mapcat.put("user_id",loginUser.getUserId());
          List<CartVo>  catlist=cartService.queryList(mapcat);
          for(CartVo cartVoitem:catlist){
              Integer number=cartVoitem.getNumber();
              goodsTotalPrice = cartVoitem.getRetail_price().multiply(new BigDecimal(number));
              deliveryfee=cartVoitem.getDeliveryfee();
                  total=total.add(goodsTotalPrice);
          }

//            String[] goodsSepcifitionValue = null;   .add(new BigDecimal(cartVoitem.getDeliveryfee()))
//            if (null != productInfo.getGoods_specification_ids()) {
//                Map specificationParam = new HashMap();
//                specificationParam.put("ids", (productInfo.getGoods_specification_ids()+",").split(","));
//                specificationParam.put("goodsId", goodsVo.getGoodsId());
//                List<GoodsSpecificationVo> specificationEntities = goodsSpecificationService.queryList(specificationParam);
//                goodsSepcifitionValue = new String[specificationEntities.size()];
//                for (int i = 0; i < specificationEntities.size(); i++) {
//                    goodsSepcifitionValue[i] = specificationEntities.get(i).getValue();
//                }
//            }
//            CartVo cartVo = new CartVo();
//            BeanUtils.copyProperties(productInfo, cartVo);
//            cartVo.setNumber(goodsVo.getNumber());
//            cartVo.setProduct_id(goodsVo.getProductId());
//            if (null != goodsSepcifitionValue) {
//                cartVo.setGoods_specifition_name_value(StringUtils.join(goodsSepcifitionValue, ";"));
//            cartVo.setGoods_specifition_ids(productInfo.getGoods_specification_ids());
//            checkedGoodsList.add(cartVo);
        }

//        //获取订单使用的优惠券
//        BigDecimal couponPrice = new BigDecimal(0.00);
//        CouponVo couponVo = null;//            }
////
//        if (couponId != null && couponId != 0) {
//            couponVo = apiCouponMapper.getUserCoupon(couponId);
//            if (couponVo != null && couponVo.getCoupon_status() == 1) {
//                couponPrice = couponVo.getType_money();
//            }
//        }

        //订单价格计算
        BigDecimal orderTotalPrice = total.add(new BigDecimal(freightPrice)); //订单的总价

//        BigDecimal actualPrice = orderTotalPrice.subtract(couponPrice);  //减去其它支付的金额后，要实际支付的金额

        Long currentTime = System.currentTimeMillis() / 1000;

        //
        OrderVo orderInfo = new OrderVo();
        orderInfo.setOrder_sn(CommonUtil.generateOrderNumber());
        orderInfo.setUser_id(loginUser.getUserId());
        if(num==0){
            //收货地址和运费
            orderInfo.setConsignee(addressVo.getUserName());
            orderInfo.setMobile(addressVo.getTelNumber());
            orderInfo.setCountry(addressVo.getNationalCode());
            orderInfo.setProvince(addressVo.getProvinceName());
            orderInfo.setCity(addressVo.getCityName());
            orderInfo.setDistrict(addressVo.getCountyName());
            orderInfo.setAddress(addressVo.getDetailInfo());
            orderInfo.setFreight_price(deliveryfee);
        }else if(num==1){

            //留言
            orderInfo.setPostscript(postscript);
            //使用的优惠券
            orderInfo.setCoupon_id(couponId);
//        orderInfo.setCoupon_price(couponPrice);
            orderInfo.setAdd_time(new Date());
            orderInfo.setGoods_price(goodsTotalPrice);
            orderInfo.setOrder_price(total);
            orderInfo.setActual_price(total);
            // 待付款
            orderInfo.setOrder_status(0);
            orderInfo.setShipping_status(0);
            orderInfo.setPay_status(0);
            orderInfo.setShipping_id(0);
            orderInfo.setShipping_fee(new BigDecimal(0));
            orderInfo.setIntegral(0);
            orderInfo.setIntegral_money(new BigDecimal(0));
            if (type.equals("cart")) {
                orderInfo.setOrder_type("1");
            } else {
                orderInfo.setOrder_type("4");
            }

            //开启事务，插入订单信息和订单商品
            apiOrderMapper.save(orderInfo);
            if (null == orderInfo.getId()) {
                resultObj.put("errno", 1);
                resultObj.put("errmsg", "订单提交失败");
                return resultObj;
            }
            //统计商品总价
//        List<OrderMenuEntity> orderMenuEntities = new ArrayList<>();
            Map checkmap=new HashMap();
            checkmap.put("user_id",loginUser.getUserId());
            checkedGoodsList=cartService.queryList(checkmap);
            List<OrderGoodsVo> orderGoodsData = new ArrayList<OrderGoodsVo>();
            for (CartVo goodsItem : checkedGoodsList) {
//            OrderMenuEntity orderMenuVo=new OrderMenuEntity();
//            orderMenuVo.setOrderId(orderInfo.getId());
//            orderMenuVo.setMealId(goodsItem.getMealid());
//            orderMenuVo.setMealname(goodsItem.getGoods_name());
//            orderMenuVo.setListPicUrl(goodsItem.getList_pic_url());
//            orderMenuVo.setNumber(goodsItem.getNumber());
//            orderMenuVo.setRetailPrice(goodsItem.getRetail_price());
                OrderGoodsVo orderGoodsVo = new OrderGoodsVo();
                orderGoodsVo.setOrder_id(orderInfo.getId());
                orderGoodsVo.setGoods_id(goodsItem.getGoods_id());
                orderGoodsVo.setGoods_sn(goodsItem.getGoods_sn());
                orderGoodsVo.setProduct_id(goodsItem.getProduct_id());
                orderGoodsVo.setGoods_name(goodsItem.getGoods_name());
                orderGoodsVo.setList_pic_url(goodsItem.getList_pic_url());
                orderGoodsVo.setMarket_price(goodsItem.getMarket_price());
                orderGoodsVo.setRetail_price(goodsItem.getRetail_price());
                orderGoodsVo.setNumber(goodsItem.getNumber());
                orderGoodsVo.setGoods_specifition_name_value(goodsItem.getGoods_specifition_name_value());
                orderGoodsVo.setGoods_specifition_ids(goodsItem.getGoods_specifition_ids());
                orderGoodsVo.setDeliery_time(goodsItem.getDeliverytime());
//            orderMenuEntities.add(orderMenuVo);
//            orderMenuService.save(orderMenuVo);
                orderGoodsData.add(orderGoodsVo);
                apiOrderGoodsService.save(orderGoodsVo);
            }

            //清空已购买的商品
            cartService.deleteByCart(loginUser.getUserId(), 1, 1);
            resultObj.put("errno", 0);
            resultObj.put("errmsg", "订单提交成功");
            //
            Map<String, OrderVo> orderInfoMap = new HashMap<String, OrderVo>();
            orderInfoMap.put("orderInfo", orderInfo);
            //
            resultObj.put("data", orderInfoMap);
//        // 优惠券标记已用
//        if (couponVo != null && couponVo.getCoupon_status() == 1) {
//            couponVo.setCoupon_status(2);
//            apiCouponMapper.updateUserCoupon(couponVo);
//        }
            getGroupBymealforDept(orderInfo.getId());
            return resultObj;
        }
        //
        orderInfo.setFreight_price(deliveryfee);
        //留言
        orderInfo.setPostscript(postscript);
        //使用的优惠券
        orderInfo.setCoupon_id(couponId);
//        orderInfo.setCoupon_price(couponPrice);
        orderInfo.setAdd_time(new Date());
        orderInfo.setGoods_price(goodsTotalPrice);
        orderInfo.setOrder_price(total.add(new BigDecimal(deliveryfee)));
        orderInfo.setActual_price(total.add(new BigDecimal(deliveryfee)));
        // 待付款
        orderInfo.setOrder_status(0);
        orderInfo.setShipping_status(0);
        orderInfo.setPay_status(0);
        orderInfo.setShipping_id(0);
        orderInfo.setShipping_fee(new BigDecimal(0));
        orderInfo.setIntegral(0);
        orderInfo.setIntegral_money(new BigDecimal(0));
        if (type.equals("cart")) {
            orderInfo.setOrder_type("1");
        } else {
            orderInfo.setOrder_type("4");
        }

        //开启事务，插入订单信息和订单商品
        apiOrderMapper.save(orderInfo);
        if (null == orderInfo.getId()) {
            resultObj.put("errno", 1);
            resultObj.put("errmsg", "订单提交失败");
            return resultObj;
        }
        //统计商品总价
//        List<OrderMenuEntity> orderMenuEntities = new ArrayList<>();
        Map checkmap=new HashMap();
        checkmap.put("user_id",loginUser.getUserId());
        checkedGoodsList=cartService.queryList(checkmap);
        List<OrderGoodsVo> orderGoodsData = new ArrayList<OrderGoodsVo>();
        for (CartVo goodsItem : checkedGoodsList) {
//            OrderMenuEntity orderMenuVo=new OrderMenuEntity();
//            orderMenuVo.setOrderId(orderInfo.getId());
//            orderMenuVo.setMealId(goodsItem.getMealid());
//            orderMenuVo.setMealname(goodsItem.getGoods_name());
//            orderMenuVo.setListPicUrl(goodsItem.getList_pic_url());
//            orderMenuVo.setNumber(goodsItem.getNumber());
//            orderMenuVo.setRetailPrice(goodsItem.getRetail_price());
            OrderGoodsVo orderGoodsVo = new OrderGoodsVo();
            orderGoodsVo.setOrder_id(orderInfo.getId());
            orderGoodsVo.setGoods_id(goodsItem.getGoods_id());
            orderGoodsVo.setGoods_sn(goodsItem.getGoods_sn());
            orderGoodsVo.setProduct_id(goodsItem.getProduct_id());
            orderGoodsVo.setGoods_name(goodsItem.getGoods_name());
            orderGoodsVo.setList_pic_url(goodsItem.getList_pic_url());
            orderGoodsVo.setMarket_price(goodsItem.getMarket_price());
            orderGoodsVo.setRetail_price(goodsItem.getRetail_price());
            orderGoodsVo.setNumber(goodsItem.getNumber());
            orderGoodsVo.setGoods_specifition_name_value(goodsItem.getGoods_specifition_name_value());
            orderGoodsVo.setGoods_specifition_ids(goodsItem.getGoods_specifition_ids());
            orderGoodsVo.setDeliery_time(goodsItem.getDeliverytime());
//            orderMenuEntities.add(orderMenuVo);
//            orderMenuService.save(orderMenuVo);
        orderGoodsData.add(orderGoodsVo);
        apiOrderGoodsService.save(orderGoodsVo);
        }

        //清空已购买的商品
        cartService.deleteByCart(loginUser.getUserId(), 1, 1);
        resultObj.put("errno", 0);
        resultObj.put("errmsg", "订单提交成功");
        //
        Map<String, OrderVo> orderInfoMap = new HashMap<String, OrderVo>();
        orderInfoMap.put("orderInfo", orderInfo);
        //
        resultObj.put("data", orderInfoMap);
//        // 优惠券标记已用
//        if (couponVo != null && couponVo.getCoupon_status() == 1) {
//            couponVo.setCoupon_status(2);
//            apiCouponMapper.updateUserCoupon(couponVo);
//        }
        getGroupBymealforDept(orderInfo.getId());
        return resultObj;

    }
    @Transactional
    public Map<String,Object> meanusubmit(JSONObject jsonParam,UserVo loginUser,Integer couponId,String postscript,Integer addressId,Integer num,Integer population,String specification,Integer fate,Integer chacke,Integer stroeid) {
        Map<String, Object> resultObj = new HashMap<>();
        AddressVo addressVo = apiAddressMapper.queryObject(addressId);
        //        String[] specife=specification.split(new char[1]{'/'});
        Double total = new Double(0.00);
        if(chacke==0){
            total = population * (new Double(specification)) * fate*1;
        }else if(chacke==1){
            total = population * (new Double(specification)) * fate*1;
        }else if(chacke==2){
            total = population * (new Double(specification)) * fate*2;
        }
        OrderVo orderInfo = new OrderVo();
        orderInfo.setOrder_sn(CommonUtil.generateOrderNumber());
        orderInfo.setUser_id(loginUser.getUserId());
        //收货地址和运费
        if (num == 0) {
            orderInfo.setConsignee(addressVo.getUserName());
            orderInfo.setMobile(addressVo.getTelNumber());
            orderInfo.setCountry(addressVo.getNationalCode());
            orderInfo.setProvince(addressVo.getProvinceName());
            orderInfo.setCity(addressVo.getCityName());
            orderInfo.setDistrict(addressVo.getCountyName());
            orderInfo.setAddress(addressVo.getDetailInfo());
            orderInfo.setFreight_price(5);
        } else if (num == 1) {
            //留言
            orderInfo.setPostscript(postscript);
            //使用的优惠券
            orderInfo.setCoupon_id(couponId);
//        orderInfo.setCoupon_price(couponPrice);
            orderInfo.setAdd_time(new Date());
            orderInfo.setGoods_price((new BigDecimal(specification)));
            orderInfo.setOrder_price((new BigDecimal(total)));
            orderInfo.setActual_price((new BigDecimal(total)));
            // 待付款
            orderInfo.setOrder_status(0);
            orderInfo.setShipping_status(0);
            orderInfo.setPay_status(0);
            orderInfo.setShipping_id(0);
            orderInfo.setShipping_fee(new BigDecimal(0));
            orderInfo.setIntegral(0);
            orderInfo.setIntegral_money(new BigDecimal(0));
            //开启事务，插入订单信息和订单商品
            apiOrderMapper.save(orderInfo);
            if (null == orderInfo.getId()) {
                resultObj.put("errno", 1);
                resultObj.put("errmsg", "订单提交失败");
                return resultObj;
            }
            //统计商品总价
            List<OrderGoodsVo> orderGoodsData = new ArrayList<>();
            String listpic = "https://yeecook-shop-pl.oss-cn-shenzhen.aliyuncs.com/upload/20190925/1653468560f147.png";
            OrderGoodsVo orderGoodsVo = new OrderGoodsVo();
            orderGoodsVo.setOrder_id(orderInfo.getId());
            orderGoodsVo.setPopulation(population);
            orderGoodsVo.setFate(fate);
            orderGoodsVo.setGoods_name("订餐计划");
            orderGoodsVo.setList_pic_url(listpic);
            orderGoodsVo.setRetail_price((new BigDecimal(total)));
            orderGoodsVo.setNumber(1);
            orderGoodsVo.setGoods_specifition_name_value
                    (specification);
            orderGoodsData.add(orderGoodsVo);
            apiOrderGoodsService.save(orderGoodsVo);
            //清空已购买的商品
            cartService.deleteByCart(loginUser.getUserId(), 1, 1);
            resultObj.put("errno", 0);
            resultObj.put("errmsg", "订单提交成功");
            //
            Map<String, OrderVo> orderInfoMap = new HashMap<String, OrderVo>();
            orderInfoMap.put("orderInfo", orderInfo);
            //
            resultObj.put("data", orderInfoMap);

            getGroupbymenuforDept(orderInfo.getId());
            return resultObj;
        }
        orderInfo.setFreight_price(5);
        //留言
        orderInfo.setPostscript(postscript);
        //使用的优惠券
        orderInfo.setCoupon_id(couponId);
//        orderInfo.setCoupon_price(couponPrice);
        orderInfo.setAdd_time(new Date());
        orderInfo.setGoods_price((new BigDecimal(specification)));
        orderInfo.setOrder_price((new BigDecimal(total)));
        orderInfo.setActual_price((new BigDecimal(total)));
        // 待付款
        orderInfo.setOrder_status(0);
        orderInfo.setShipping_status(0);
        orderInfo.setPay_status(0);
        orderInfo.setShipping_id(0);
        orderInfo.setShipping_fee(new BigDecimal(0));
        orderInfo.setIntegral(0);
        orderInfo.setIntegral_money(new BigDecimal(0));
        //开启事务，插入订单信息和订单商品
        apiOrderMapper.save(orderInfo);
        if (null == orderInfo.getId()) {
            resultObj.put("errno", 1);
            resultObj.put("errmsg", "订单提交失败");
            return resultObj;
        }
        //统计商品总价
        List<OrderGoodsVo> orderGoodsData = new ArrayList<>();
        String listpic = "https://yeecook-shop-pl.oss-cn-shenzhen.aliyuncs.com/upload/20190925/1653468560f147.png";
        OrderGoodsVo orderGoodsVo = new OrderGoodsVo();
        orderGoodsVo.setOrder_id(orderInfo.getId());
        orderGoodsVo.setPopulation(population);
        orderGoodsVo.setFate(fate);
        orderGoodsVo.setGoods_name("订餐计划");
        orderGoodsVo.setList_pic_url(listpic);
        orderGoodsVo.setRetail_price((new BigDecimal(total)));
        orderGoodsVo.setNumber(1);
        orderGoodsVo.setGoods_specifition_name_value
                (specification);
        orderGoodsData.add(orderGoodsVo);
        apiOrderGoodsService.save(orderGoodsVo);
        //清空已购买的商品
        cartService.deleteByCart(loginUser.getUserId(), 1, 1);
        resultObj.put("errno", 0);
        resultObj.put("errmsg", "订单提交成功");
        //
        Map<String, OrderVo> orderInfoMap = new HashMap<String, OrderVo>();
        orderInfoMap.put("orderInfo", orderInfo);
        //
        resultObj.put("data", orderInfoMap);

        getGroupbymenuforDept(orderInfo.getId());
        return resultObj;
    }


private void  getGroupByGoosforDept(Integer orderId){

    OrderVo orderVo =apiOrderService.queryObject(orderId);
    if(null==orderVo){
        return;
    }
    Map params = new HashMap();
    params.put("order_id",orderId);
    params.put("sidx", SQLFilter.sqlInject("Supplier_id"));
    params.put("order", SQLFilter.sqlInject("asc"));
    List<OrderGoodsVo> orderGoodsVoList=apiOrderGoodsService.queryList(params);
    /*根据供应商分组算法**/
    Map<Integer, List<OrderGoodsVo>> supIdMap = new HashMap<>();
    for (OrderGoodsVo orderGoodsVo : orderGoodsVoList) {
        List<OrderGoodsVo> tempList = supIdMap.get(orderGoodsVo.getSupplier_id());
        /*如果取不到数据,那么直接new一个空的ArrayList**/
        if (tempList == null) {
            tempList = new ArrayList<>();
            tempList.add(orderGoodsVo);
            supIdMap.put(orderGoodsVo.getSupplier_id(), tempList);
        }
        else {
            /*某个supplierId之前已经存放过了,则直接追加数据到原来的List里**/
            tempList.add(orderGoodsVo);
        }
    }
    //供应商订单总价
    BigDecimal  orderTotalPrice ;
    //商品总价
    BigDecimal  goodsTotalPrice ;

    Map mapSupplier=new HashMap();
    for(Integer supId : supIdMap.keySet()){
        orderTotalPrice = new BigDecimal(0.00);
        goodsTotalPrice = new BigDecimal(0.00);
        List<OrderGoodsVo> voList=supIdMap.get(supId);
        OrderSupplierVo orderSupplierVo=new OrderSupplierVo(orderVo);
        for (OrderGoodsVo vo : voList){
            orderTotalPrice= orderTotalPrice.add(vo.getRetail_price().multiply(new BigDecimal(vo.getNumber())));
            goodsTotalPrice= goodsTotalPrice.add(vo.getRetail_price().multiply(new BigDecimal(vo.getNumber())));
        }
        orderSupplierVo.setDeptId(voList.get(0).getDept_id());
        orderSupplierVo.setSupplierId(voList.get(0).getSupplier_id().longValue());
        orderSupplierVo.setOrderPrice(orderTotalPrice);
        orderSupplierVo.setGoodsPrice(goodsTotalPrice);
        orderSupplierVo.setOrderSupSn(CommonUtil.generateOrderNumber());
        apiOrderSupplierService.save(orderSupplierVo);
        mapSupplier.put(String.valueOf(orderSupplierVo.getSupplierId()),voList.get(0).getSupplierName()+"供应商待发货中..");
    }
    orderVo.setSupplier_list(JsonUtil.getJsonByObj(mapSupplier));
    apiOrderService.update(orderVo);

}

private void getGroupBymealforDept(Integer orderId){
    OrderVo orderVo =apiOrderService.queryObject(orderId);
    if(null==orderVo){
        return;
    }
    Map params = new HashMap();
    params.put("order_id",orderId);
    params.put("order", SQLFilter.sqlInject("asc"));
    List<OrderMenuEntity> orderMenuEntityList=orderMenuService.queryList(params);
//    Map<Integer, List<OrderGoodsVo>> supIdMap = new HashMap<>();
//    for (OrderGoodsVo orderGoodsVo : orderGoodsVoList) {
//        List<OrderGoodsVo> tempList = supIdMap.get(orderGoodsVo.getSupplier_id());
//        /*如果取不到数据,那么直接new一个空的ArrayList**/
//        if (tempList == null) {
//            tempList = new ArrayList<>();
//            tempList.add(orderGoodsVo);
//            supIdMap.put(orderGoodsVo.getSupplier_id(), tempList);
//        }
//        else {
//            /*某个supplierId之前已经存放过了,则直接追加数据到原来的List里**/
//            tempList.add(orderGoodsVo);
//        }
    //供应商订单总价
//    BigDecimal  orderTotalPrice ;
    //商品总价
    BigDecimal  goodsTotalPrice ;

//    Map mapSupplier=new HashMap();
//    for(Integer supId : supIdMap.keySet()){
//        orderTotalPrice = new BigDecimal(0.00);
        goodsTotalPrice = new BigDecimal(0.00);
//        List<OrderMenuEntity> voList=supIdMap.get(supId);
//        OrderSupplierVo orderSupplierVo=new OrderSupplierVo(orderVo);
    for (OrderMenuEntity vo : orderMenuEntityList){
//        orderTotalPrice= orderTotalPrice.add(vo.getRetailPrice().multiply(new BigDecimal(vo.getNumber())));
        goodsTotalPrice= goodsTotalPrice.add(vo.getRetailPrice().multiply(new BigDecimal(vo.getNumber())));
    }
//        orderSupplierVo.setDeptId(voList.get(0).getDept_id());
//        orderSupplierVo.setSupplierId(voList.get(0).getSupplier_id().longValue());
//        orderSupplierVo.setOrderPrice(orderTotalPrice);
//        orderSupplierVo.setGoodsPrice(goodsTotalPrice);
//        orderSupplierVo.setOrderSupSn(CommonUtil.generateOrderNumber());
//        apiOrderSupplierService.save(orderSupplierVo);
//        mapSupplier.put(String.valueOf(orderSupplierVo.getSupplierId()),voList.get(0).getSupplierName()+"供应商待发货中..");
//    }
//    orderVo.setSupplier_list(JsonUtil.getJsonByObj(mapSupplier));
//    apiOrderService.update(orderVo);
    }
private void getGroupbymenuforDept(Integer orderId){
    OrderVo orderVo =apiOrderService.queryObject(orderId);
    if(null==orderVo){
        return;
    }
    Map params = new HashMap();
    params.put("order_id",orderId);
    params.put("order", SQLFilter.sqlInject("asc"));
    List<OrderMenuplanEntity> orderMenuEntityList=orderMenuService.queryList(params);
//    Map<Integer, List<OrderGoodsVo>> supIdMap = new HashMap<>();
//    for (OrderGoodsVo orderGoodsVo : orderGoodsVoList) {
//        List<OrderGoodsVo> tempList = supIdMap.get(orderGoodsVo.getSupplier_id());
//        /*如果取不到数据,那么直接new一个空的ArrayList**/
//        if (tempList == null) {
//            tempList = new ArrayList<>();
//            tempList.add(orderGoodsVo);
//            supIdMap.put(orderGoodsVo.getSupplier_id(), tempList);
//        }
//        else {
//            /*某个supplierId之前已经存放过了,则直接追加数据到原来的List里**/
//            tempList.add(orderGoodsVo);
//        }
    //供应商订单总价
//    BigDecimal  orderTotalPrice ;
    //商品总价
    BigDecimal  goodsTotalPrice ;

//    Map mapSupplier=new HashMap();
//    for(Integer supId : supIdMap.keySet()){
//        orderTotalPrice = new BigDecimal(0.00);
    goodsTotalPrice = new BigDecimal(0.00);
//        List<OrderMenuEntity> voList=supIdMap.get(supId);
//        OrderSupplierVo orderSupplierVo=new OrderSupplierVo(orderVo);
//        orderSupplierVo.setDeptId(voList.get(0).getDept_id());
//        orderSupplierVo.setSupplierId(voList.get(0).getSupplier_id().longValue());
//        orderSupplierVo.setOrderPrice(orderTotalPrice);
//        orderSupplierVo.setGoodsPrice(goodsTotalPrice);
//        orderSupplierVo.setOrderSupSn(CommonUtil.generateOrderNumber());
//        apiOrderSupplierService.save(orderSupplierVo);
//        mapSupplier.put(String.valueOf(orderSupplierVo.getSupplierId()),voList.get(0).getSupplierName()+"供应商待发货中..");
//    }
//    orderVo.setSupplier_list(JsonUtil.getJsonByObj(mapSupplier));
//    apiOrderService.update(orderVo);
}
    /**供应商订单打印*/
    public void printerSupplierOrder(Integer orderId) {
        OrderVo orderVo =apiOrderService.queryObject(orderId);
        if(null==orderVo){
            return;
        }
        YeecookSupplierVo yeecookVo = new YeecookSupplierVo();
        Map map =new HashMap();
        map.put("orderSn",orderVo.getOrder_sn());
        List<OrderSupplierVo> orderSupplierVoList=apiOrderSupplierService.queryList(map);
        for (OrderSupplierVo orderSupplierVo:orderSupplierVoList
             ) {
            map.put("order_id",String.valueOf(orderVo.getId()));
            map.put("supplier_id",String.valueOf(orderSupplierVo.getSupplierId()));
            List<OrderGoodsVo> orderGoodsVoList=apiOrderGoodsService.queryList(map);
            yeecookVo.setOrderVo(orderSupplierVo);
            yeecookVo.setOrderGoodsVoList(orderGoodsVoList);

            List<SysPrinterUserVo> list= sysPrinterService.getSysPrinterUserVoDept(Long.valueOf(orderSupplierVo.getDeptId()),"0002");
            SysPrinterVo sysPrinterVo=sysPrinterService.getSysPrinterVo("易联云");
            PrinterSupplierTemplate printerTemplate = new PrinterSupplierTemplate(yeecookVo,sysPrinterVo,list);
            PrinterSupplierTanks printerTanks = new PrinterSupplierTanks(printerTemplate);
            printerTanks.run();
        }

    }

    /**用户订单打印*/
    public void printerOrder(OrderVo orderVo, List<OrderGoodsVo> orderGoodsVoList) {
        YeecookVo yeecookVo = new YeecookVo();
        yeecookVo.setOrderVo(orderVo);
        yeecookVo.setOrderGoodsVoList(orderGoodsVoList);
        List<SysPrinterUserVo> list= sysPrinterService.getSysPrinterUserVo(Long.valueOf("1"),"0002");
        SysPrinterVo sysPrinterVo=sysPrinterService.getSysPrinterVo("易联云");
        PrinterTemplate printerTemplate = new PrinterTemplate(yeecookVo,sysPrinterVo,list);
        PrinterTanks printerTanks = new PrinterTanks(printerTemplate);
        printerTanks.run();
    }

}
