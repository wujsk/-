package com.sky.controller.user;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sky.exception.BaseException;
import com.sky.properties.WeChatProperties;
import com.sky.service.OrderService;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import jakarta.annotation.Resource;
import org.apache.http.entity.ContentType;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * @author: cyy
 * @create: 2025-02-18 13:51
 * 支付回调相关接口
 **/
@RestController
@RequestMapping("/notify")
public class PayNotifyController {

    @Resource
    private OrderService orderService;

    @Resource
    private WeChatProperties weChatProperties;

    /**
     * 支付成功回调
     *
     * @param request
     */
    @PostMapping("/paySuccess")
    public Mono<Void> paySuccessNotify(ServerHttpRequest request, ServerHttpResponse response) {
        return request.getBody()
                .map(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    return new String(bytes, StandardCharsets.UTF_8);
                })
                .single()
                .flatMap(body -> {
                    System.out.println("支付成功回调：" + body);
                    try {
                        String plainText = decryptData(body);
                        System.out.println("解密后的文本：" + plainText);
                        JSONObject jsonObject = JSON.parseObject(plainText);
                        String outTradeNo = jsonObject.getString("out_trade_no");//商户平台订单号
                        String transactionId = jsonObject.getString("transaction_id");//微信支付交易号
                        System.out.println("商户平台订单号：" + outTradeNo);
                        System.out.println("微信支付交易号：" + transactionId);
                        return orderService.paySuccess(outTradeNo);
                    } catch (Exception e) {
                        return Mono.error(new BaseException("响应微信失败"));
                    }
                    try {
                        return responseToWeixin(response);
                    } catch (Exception e) {
                        return Mono.error(new BaseException("响应微信失败"));
                    }
                });
    }

    /**
     * 数据解密
     *
     * @param body
     * @return
     * @throws Exception
     */
    private String decryptData(String body) throws Exception {
        JSONObject resultObject = JSON.parseObject(body);
        JSONObject resource = resultObject.getJSONObject("resource");
        String ciphertext = resource.getString("ciphertext");
        String nonce = resource.getString("nonce");
        String associatedData = resource.getString("associated_data");

        AesUtil aesUtil = new AesUtil(weChatProperties.getApiV3Key().getBytes(StandardCharsets.UTF_8));
        //密文解密
        String plainText = aesUtil.decryptToString(associatedData.getBytes(StandardCharsets.UTF_8),
                nonce.getBytes(StandardCharsets.UTF_8),
                ciphertext);

        return plainText;
    }

    /**
     * 给微信响应
     * @param response
     */
    private Mono<Void> responseToWeixin(ServerHttpResponse response) throws Exception {
        response.setStatusCode(org.springframework.http.HttpStatus.OK);
        HashMap<Object, Object> map = new HashMap<>();
        map.put("code", "SUCCESS");
        map.put("message", "SUCCESS");
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] responseBytes = JSON.toJSONString(map).getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(responseBytes)));
    }
}
