package com.platform.api;

import com.platform.annotation.LoginUser;
import com.platform.cache.J2CacheUtils;
import com.platform.entity.*;
import com.platform.service.ApiOrderGoodsService;
import com.platform.service.ApiOrderMenuService;
import com.platform.service.ApiOrderService;
import com.platform.util.ApiBaseAction;
import com.platform.util.wechat.WechatRefundApiResult;
import com.platform.util.wechat.WechatUtil;
import com.platform.utils.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * 作者: @author Harmon <br>
 * 时间: 2017-08-11 08:32<br>
 * 描述: ApiIndexController <br>
 */
@Api(tags = "商户支付")
@RestController
@RequestMapping("/api/pay")
public class ApiPayController extends ApiBaseAction {
    private Logger logger = Logger.getLogger(getClass());
    @Autowired
    private ApiOrderService orderService;
    @Autowired
    private ApiOrderGoodsService orderGoodsService;
    @Autowired
    private ApiXetYqmController apiXetYqmController;
    @Autowired
    private ApiOrderController apiOrderController;
    @Autowired
    private ApiOrderMenuService orderMenuService;

    /**
     */
    @ApiOperation(value = "跳转")
    @PostMapping("index")
    public Object index() {
        //
        return toResponsSuccess("");
    }

    /**
     * 获取支付的请求参数
     */
    @ApiOperation(value = "获取商品支付的请求参数")
    @PostMapping("prepay")
    public Object payPrepay(@LoginUser UserVo loginUser, Integer orderId) {
        //
        OrderVo orderInfo = orderService.queryObject(orderId);

        if (null == orderInfo) {
            return toResponsObject(400, "订单已取消", "");
        }

        if (orderInfo.getPay_status() >= 2 || orderInfo.getPay_status() < 0) {
            return toResponsObject(400, "订单已支付，请不要重复操作", "");
        }

        String nonceStr = CharUtil.getRandomString(32);

        //https://pay.weixin.qq.com/wiki/doc/api/wxa/wxa_api.php?chapter=7_7&index=3
        Map<Object, Object> resultObj = new TreeMap();

        try {
            Map<Object, Object> parame = new TreeMap<Object, Object>();
            parame.put("appid", ResourceUtil.getConfigByName("wx.appId"));
            // 商家账号。
            parame.put("mch_id", ResourceUtil.getConfigByName("wx.mchId"));
            String randomStr = CharUtil.getRandomNum(18).toUpperCase();
            // 随机字符串
            parame.put("nonce_str", randomStr);
            // 商户订单编号
            parame.put("out_trade_no", orderInfo.getOrder_sn());
            Map orderGoodsParam = new HashMap();
            orderGoodsParam.put("order_id", orderId);
            // 商品描述
            parame.put("body", "宜厨小程序-支付");
            //订单的商品
            List<OrderGoodsVo> orderGoods = orderGoodsService.queryList(orderGoodsParam);

            if (null != orderGoods) {
                List<HashMap> list = new ArrayList<>();

                for (OrderGoodsVo goodsVo : orderGoods) {
                    HashMap hashMap = new HashMap();
                    hashMap.put("goods_id", String.valueOf(goodsVo.getGoods_id()));
                    hashMap.put("quantity", goodsVo.getNumber());
                    hashMap.put("goods_name", goodsVo.getGoods_name());
                    hashMap.put("price", goodsVo.getRetail_price().multiply(new BigDecimal(100)).intValue());
                    list.add(hashMap);
                }

                Map map = new HashMap();
                map.put("goods_detail", JsonUtil.getJsonByObj(list));
                // 商品描述
                parame.put("detail", JsonUtil.getJsonByObj(map));
            }
            //支付金额
            parame.put("total_fee", orderInfo.getActual_price().multiply(new BigDecimal(100)).intValue());
            // 回调地址
            parame.put("notify_url", ResourceUtil.getConfigByName("wx.notifyUrl"));
            // 交易类型APP
            parame.put("trade_type", ResourceUtil.getConfigByName("wx.tradeType"));
            parame.put("spbill_create_ip", getClientIp());
            parame.put("openid", loginUser.getWeixin_openid());
            String sign = WechatUtil.arraySign(parame, ResourceUtil.getConfigByName("wx.paySignKey"));
            // 数字签证
            parame.put("sign", sign);

            String xml = MapUtils.convertMap2Xml(parame);
            logger.info("xml:" + xml);
            Map<String, Object> resultUn = XmlUtil.xmlStrToMap(WechatUtil.requestOnce(ResourceUtil.getConfigByName("wx.uniformorder"), xml));
            // 响应报文
            String return_code = MapUtils.getString("return_code", resultUn);
            String return_msg = MapUtils.getString("return_msg", resultUn);
            //
            if (return_code.equalsIgnoreCase("FAIL")) {
                return toResponsFail("支付失败," + return_msg);
            } else if (return_code.equalsIgnoreCase("SUCCESS")) {
                // 返回数据
                String result_code = MapUtils.getString("result_code", resultUn);
                String err_code_des = MapUtils.getString("err_code_des", resultUn);
                if (result_code.equalsIgnoreCase("FAIL")) {
                    return toResponsFail("支付失败," + err_code_des);
                } else if (result_code.equalsIgnoreCase("SUCCESS")) {
                    String prepay_id = MapUtils.getString("prepay_id", resultUn);
                    // 先生成paySign 参考https://pay.weixin.qq.com/wiki/doc/api/wxa/wxa_api.php?chapter=7_7&index=5
                    resultObj.put("appId", ResourceUtil.getConfigByName("wx.appId"));
                    resultObj.put("timeStamp", DateUtils.timeToStr(System.currentTimeMillis() / 1000, DateUtils.DATE_TIME_PATTERN));
                    resultObj.put("nonceStr", nonceStr);
                    resultObj.put("package", "prepay_id=" + prepay_id);
                    resultObj.put("signType", "MD5");
                    String paySign = WechatUtil.arraySign(resultObj, ResourceUtil.getConfigByName("wx.paySignKey"));
                    resultObj.put("paySign", paySign);
                    // 业务处理
                    orderInfo.setPay_id(prepay_id);
                    // 付款中
                    orderInfo.setPay_status(1);
                    orderService.update(orderInfo);
                    return toResponsObject(0, "微信统一订单下单成功", resultObj);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return toResponsFail("下单失败,error=" + e.getMessage());
        }
        return toResponsFail("下单失败");
    }
    @ApiOperation(value = "获取门店商品支付的请求参数")
    @PostMapping("mealprepay")
    public Object mealprepay(@LoginUser UserVo loginUser, Integer orderId) {
        //
        OrderVo orderInfo = orderService.queryObject(orderId);

        if (null == orderInfo) {
            return toResponsObject(400, "订单已取消", "");
        }

        if (orderInfo.getPay_status() >= 2 || orderInfo.getPay_status() < 0) {
            return toResponsObject(400, "订单已支付，请不要重复操作", "");
        }

        String nonceStr = CharUtil.getRandomString(32);

        //https://pay.weixin.qq.com/wiki/doc/api/wxa/wxa_api.php?chapter=7_7&index=3
        Map<Object, Object> resultObj = new TreeMap();

        try {
            Map<Object, Object> parame = new TreeMap<Object, Object>();
            parame.put("appid", ResourceUtil.getConfigByName("wx.appId"));
            // 商家账号。
            parame.put("mch_id", ResourceUtil.getConfigByName("wx.mchId"));
            String randomStr = CharUtil.getRandomNum(18).toUpperCase();
            // 随机字符串
            parame.put("nonce_str", randomStr);
            // 商户订单编号
            parame.put("out_trade_no", orderInfo.getOrder_sn());
            Map orderGoodsParam = new HashMap();
            orderGoodsParam.put("order_id", orderId);
            // 商品描述
            parame.put("body", "宜厨小程序-支付");
            //订单的商品
            List<OrderMenuVo> orderMenuEntities =orderMenuService .queryList(orderGoodsParam);

            if (null != orderMenuEntities) {
                List<HashMap> list = new ArrayList<>();

                for (OrderMenuVo orderMenuVo : orderMenuEntities) {
                    HashMap hashMap = new HashMap();
                    hashMap.put("goods_id", String.valueOf(orderMenuVo.getOrderId()));
                    hashMap.put("quantity", orderMenuVo.getNumber());
                    hashMap.put("goods_name", orderMenuVo.getMealname());
                    hashMap.put("price", orderMenuVo.getRetailPrice().multiply(new BigDecimal(100)).intValue());
                    list.add(hashMap);
                }

                Map map = new HashMap();
                map.put("goods_detail", JsonUtil.getJsonByObj(list));
                // 商品描述
                parame.put("detail", JsonUtil.getJsonByObj(map));
            }
            //支付金额
            parame.put("total_fee", orderInfo.getActual_price().multiply(new BigDecimal(100)).intValue());
            // 回调地址
            parame.put("notify_url", ResourceUtil.getConfigByName("wx.notifyUrl"));
            // 交易类型APP
            parame.put("trade_type", ResourceUtil.getConfigByName("wx.tradeType"));
            parame.put("spbill_create_ip", getClientIp());
            parame.put("openid", loginUser.getWeixin_openid());
            String sign = WechatUtil.arraySign(parame, ResourceUtil.getConfigByName("wx.paySignKey"));
            // 数字签证
            parame.put("sign", sign);

            String xml = MapUtils.convertMap2Xml(parame);
            logger.info("xml:" + xml);
            Map<String, Object> resultUn = XmlUtil.xmlStrToMap(WechatUtil.requestOnce(ResourceUtil.getConfigByName("wx.uniformorder"), xml));
            // 响应报文
            String return_code = MapUtils.getString("return_code", resultUn);
            String return_msg = MapUtils.getString("return_msg", resultUn);
            //
            if (return_code.equalsIgnoreCase("FAIL")) {
                return toResponsFail("支付失败," + return_msg);
            } else if (return_code.equalsIgnoreCase("SUCCESS")) {
                // 返回数据
                String result_code = MapUtils.getString("result_code", resultUn);
                String err_code_des = MapUtils.getString("err_code_des", resultUn);
                if (result_code.equalsIgnoreCase("FAIL")) {
                    return toResponsFail("支付失败," + err_code_des);
                } else if (result_code.equalsIgnoreCase("SUCCESS")) {
                    String prepay_id = MapUtils.getString("prepay_id", resultUn);
                    // 先生成paySign 参考https://pay.weixin.qq.com/wiki/doc/api/wxa/wxa_api.php?chapter=7_7&index=5
                    resultObj.put("appId", ResourceUtil.getConfigByName("wx.appId"));
                    resultObj.put("timeStamp", DateUtils.timeToStr(System.currentTimeMillis() / 1000, DateUtils.DATE_TIME_PATTERN));
                    resultObj.put("nonceStr", nonceStr);
                    resultObj.put("package", "prepay_id=" + prepay_id);
                    resultObj.put("signType", "MD5");
                    String paySign = WechatUtil.arraySign(resultObj, ResourceUtil.getConfigByName("wx.paySignKey"));
                    resultObj.put("paySign", paySign);
                    // 业务处理
                    orderInfo.setPay_id(prepay_id);
                    // 付款中
                    orderInfo.setPay_status(1);
                    orderService.update(orderInfo);
                    return toResponsObject(0, "微信统一订单下单成功", resultObj);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return toResponsFail("下单失败,error=" + e.getMessage());
        }
        return toResponsFail("下单失败");
    }
    private void printerOrder(OrderVo orderVo, List<OrderGoodsVo> orderGoodsVoList) {
        try {
            orderService.printerOrder(orderVo, orderGoodsVoList);
        } catch (Exception e) {
            logger.error("订单" + orderVo.getOrder_sn() + "打印失败！");
        }
        try {
            orderService.printerSupplierOrder(orderVo.getId());
        } catch (Exception e) {
            logger.error("供应商订单" + orderVo.getOrder_sn() + "打印失败！");
        }
    }

    /**
     * 微信查询订单状态
     */
    @ApiOperation(value = "查询订单状态")
    @PostMapping("query")
    public Object orderQuery(@LoginUser UserVo loginUser, Integer orderId) {
        OrderVo orderInfo = orderService.queryObject(orderId);

        if (null == orderId || null == orderInfo) {
            return toResponsFail("订单不存在");
        }
        OrderVo orderDetail = orderService.queryObject(orderId);
        Map<Object, Object> parame = new TreeMap<Object, Object>();
        parame.put("appid", ResourceUtil.getConfigByName("wx.appId"));
        // 商家账号。
        parame.put("mch_id", ResourceUtil.getConfigByName("wx.mchId"));
        String randomStr = CharUtil.getRandomNum(18).toUpperCase();
        // 随机字符串
        parame.put("nonce_str", randomStr);
        // 商户订单编号
        parame.put("out_trade_no", orderInfo.getOrder_sn());

        String sign = WechatUtil.arraySign(parame, ResourceUtil.getConfigByName("wx.paySignKey"));
        // 数字签证
        parame.put("sign", sign);

        String xml = MapUtils.convertMap2Xml(parame);
        logger.info("xml:" + xml);
        Map<String, Object> resultUn = null;
        try {
            resultUn = XmlUtil.xmlStrToMap(WechatUtil.requestOnce(ResourceUtil.getConfigByName("wx.orderquery"), xml));
        } catch (Exception e) {
            e.printStackTrace();
            return toResponsFail("查询失败,error=" + e.getMessage());
        }
        // 响应报文
        String return_code = MapUtils.getString("return_code", resultUn);
        String return_msg = MapUtils.getString("return_msg", resultUn);

        if (!"SUCCESS".equals(return_code)) {
            return toResponsFail("查询失败,error=" + return_msg);
        }
        String trade_state = MapUtils.getString("trade_state", resultUn);
        if (return_code.equals("SUCCESS")) {

            if (trade_state.equals("SUCCESS")) {
                // 更改订单状态
                // 业务处理
                if (orderInfo.getPay_status() == 1) {
                    OrderVo orderInfo1 = new OrderVo();
                    orderInfo1.setId(orderId);
                    orderInfo1.setPay_status(2);
                    orderInfo1.setOrder_status(201);
                    orderInfo1.setShipping_status(0);
                    orderInfo1.setPay_time(new Date());
                    orderInfo1.setIs_printer(1);
                    orderService.update(orderInfo1);
                    Map orderGoodsParam = new HashMap();
                    orderGoodsParam.put("order_id", orderInfo.getId());
                    List<OrderGoodsVo> orderGoods = orderGoodsService.queryList(orderGoodsParam);
                    //todo 虚拟卡直接确认收货
                    for (OrderGoodsVo orderGoodsVo : orderGoods) {
                        if (null != orderGoodsVo.getAttribute_category() & orderGoodsVo.getAttribute_category() == 1036003) {

                            XetYqmVo xetYqmVo = apiXetYqmController.getYqm(orderGoodsVo.getBatch_id());
                            if (null != xetYqmVo) {
                                orderGoodsVo.setYqm(xetYqmVo.getInvitationCode());
                                orderGoodsService.update(orderGoodsVo);
                                apiOrderController.confirmOrder(orderId);
                                logger.error("订单" + orderInfo.getOrder_sn() + "虚拟商品发货成功！");
                                break;
                            }
                        }
                    }
                    printerOrder(orderInfo, orderGoods);
                    //apiCardController.activationBayCard(orderDetail.getUser_id(),orderInfo.getId());
                }
                return toResponsMsgSuccess("支付成功");
            } else if (trade_state.equals("USERPAYING")) {
                // 重新查询 正在支付中
                Integer num = (Integer) J2CacheUtils.get(J2CacheUtils.SHOP_CACHE_NAME, "queryRepeatNum" + orderId + "");
                if (num == null) {
                    J2CacheUtils.put(J2CacheUtils.SHOP_CACHE_NAME, "queryRepeatNum" + orderId + "", 1);
                    this.orderQuery(loginUser, orderId);
                } else if (num <= 3) {
                    J2CacheUtils.remove(J2CacheUtils.SHOP_CACHE_NAME, "queryRepeatNum" + orderId);
                    this.orderQuery(loginUser, orderId);
                } else {
                    return toResponsFail("查询失败,error=" + trade_state);
                }

            } else if (trade_state.equals("NOTPAY")) {
                // 更改订单状态
                // 业务处理
                OrderVo orderInfo1 = new OrderVo();
                orderInfo1.setId(orderId);
                orderInfo1.setPay_status(0);
                orderInfo1.setOrder_status(0);
                orderInfo1.setShipping_status(0);
                orderInfo1.setPay_time(new Date());
                orderService.update(orderInfo1);
                return toResponsMsgSuccess("NOTPAY");
            } else {
                // 失败
                return toResponsFail("查询失败,error=" + trade_state);
            }
        } else {
            return toResponsFail("查询失败,error=" + return_msg);
        }

        return toResponsFail("查询失败，未知错误");
    }

    @ApiOperation(value = "更新网络异常情况下订单状态为支付中的订单。")
    @PostMapping("refresh")
    public Object checkOrderErr(@LoginUser UserVo loginUser) {

        Map params = new HashMap();
        params.put("user_id", loginUser.getUserId());
        params.put("pay_status", 1);//支付中
        List<OrderVo> list = orderService.queryList(params);
        if (null != list && list.size() > 0) {
            for (OrderVo orderVo : list) {
                orderQuery(loginUser, orderVo.getId());
            }
        }
        return toResponsMsgSuccess("更新成功");
    }


    /**
     * 微信订单回调接口
     *
     * @return
     */
    @ApiOperation(value = "微信订单回调接口")
    @RequestMapping(value = "/notify", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public void notify(HttpServletRequest request, HttpServletResponse response) {
        try {
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/html;charset=UTF-8");
            response.setHeader("Access-Control-Allow-Origin", "*");
            InputStream in = request.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.close();
            in.close();
            //xml数据
            String reponseXml = new String(out.toByteArray(), "utf-8");
            WechatRefundApiResult result = (WechatRefundApiResult) XmlUtil.xmlStrToBean(reponseXml, WechatRefundApiResult.class);
            String result_code = result.getResult_code();
            if (result_code.equalsIgnoreCase("FAIL")) {
                //订单编号
                String out_trade_no = result.getOut_trade_no();
                logger.error("订单" + out_trade_no + "支付失败");
                response.getWriter().write(setXml("SUCCESS", "OK"));
            } else if (result_code.equalsIgnoreCase("SUCCESS")) {
                //订单编号
                String out_trade_no = result.getOut_trade_no();
                // 业务处理
                Map params = new HashMap();
                params.put("orderSn", out_trade_no);
                params.put("page", 1);
                params.put("limit", 10);
                params.put("sidx", "id");
                params.put("order", "asc");
                //查询列表数据
                Query query = new Query(params);
                List<OrderVo> orderVoList = orderService.queryList(query);
                if (null != orderVoList && orderVoList.size() > 0) {
                    OrderVo orderInfo = orderVoList.get(0);
                    if (orderInfo.getPay_status() == 1) {
                        orderInfo.setPay_status(2);
                        orderInfo.setOrder_status(201);
                        orderInfo.setShipping_status(0);
                        orderInfo.setPay_time(new Date());
                        orderInfo.setIs_printer(1);
                        orderService.update(orderInfo);
                        response.getWriter().write(setXml("SUCCESS", "OK"));
                        logger.error("订单" + out_trade_no + "支付成功");
                        Map orderGoodsParam = new HashMap();
                        orderGoodsParam.put("order_id", orderInfo.getId());
                        List<OrderGoodsVo> orderGoods = orderGoodsService.queryList(orderGoodsParam);

                        //todo 虚拟卡直接确认收货
                        //apiCardController.activationBayCard(orderInfo.getUser_id(),orderInfo.getId());
                        for (OrderGoodsVo orderGoodsVo : orderGoods) {
                            if (null != orderGoodsVo.getAttribute_category() & orderGoodsVo.getAttribute_category() == 1036003) {

                                XetYqmVo xetYqmVo = apiXetYqmController.getYqm(orderGoodsVo.getBatch_id());
                                if (null != xetYqmVo) {
                                    orderGoodsVo.setYqm(xetYqmVo.getInvitationCode());
                                    orderGoodsService.update(orderGoodsVo);
                                    apiOrderController.confirmOrder(orderInfo.getId());
                                    logger.error("订单" + out_trade_no + "虚拟商品发货成功-微信回调");
                                    break;
                                }
                            }
                        }
                        printerOrder(orderInfo, orderGoods);
                    }
                } else {
                    logger.error("订单" + out_trade_no + "支付失败找不到更新订单");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    /**
     * 订单退款请求
     */
    @ApiOperation(value = "订单退款请求")
    @PostMapping("refund")
    public Object refund(Integer orderId) {
        //
        OrderVo orderInfo = orderService.queryObject(orderId);

        if (null == orderInfo) {
            return toResponsObject(400, "订单已取消", "");
        }

        if (orderInfo.getOrder_status() == 401 || orderInfo.getOrder_status() == 402) {
            return toResponsObject(400, "订单已退款", "");
        }

        if (orderInfo.getPay_status() != 2) {
            return toResponsObject(400, "订单未付款，不能退款", "");
        }

        WechatRefundApiResult result = WechatUtil.wxRefund(orderInfo.getId().toString(),
                orderInfo.getActual_price().doubleValue(), orderInfo.getActual_price().doubleValue());
        if (result.getResult_code().equals("SUCCESS")) {
            if (orderInfo.getOrder_status() == 201) {
                orderInfo.setOrder_status(401);
            } else if (orderInfo.getOrder_status() == 300) {
                orderInfo.setOrder_status(402);
            }
            orderService.update(orderInfo);
            return toResponsObject(400, "成功退款", "");
        } else {
            return toResponsObject(400, "退款失败", "");
        }
    }

    //返回微信服务
    public static String setXml(String return_code, String return_msg) {
        return "<xml><return_code><![CDATA[" + return_code + "]]></return_code><return_msg><![CDATA[" + return_msg + "]]></return_msg></xml>";
    }

    //模拟微信回调接口
    public static String callbakcXml(String orderNum) {
        return "<xml><appid><![CDATA[wx2421b1c4370ec43b]]></appid><attach><![CDATA[支付测试]]></attach><bank_type><![CDATA[CFT]]></bank_type><fee_type><![CDATA[CNY]]></fee_type> <is_subscribe><![CDATA[Y]]></is_subscribe><mch_id><![CDATA[10000100]]></mch_id><nonce_str><![CDATA[5d2b6c2a8db53831f7eda20af46e531c]]></nonce_str><openid><![CDATA[oUpF8uMEb4qRXf22hE3X68TekukE]]></openid> <out_trade_no><![CDATA[" + orderNum + "]]></out_trade_no>  <result_code><![CDATA[SUCCESS]]></result_code> <return_code><![CDATA[SUCCESS]]></return_code><sign><![CDATA[B552ED6B279343CB493C5DD0D78AB241]]></sign><sub_mch_id><![CDATA[10000100]]></sub_mch_id> <time_end><![CDATA[20140903131540]]></time_end><total_fee>1</total_fee><trade_type><![CDATA[JSAPI]]></trade_type><transaction_id><![CDATA[1004400740201409030005092168]]></transaction_id></xml>";
    }

}