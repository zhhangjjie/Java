package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private ISeckillVoucherService seckillVoucherService;


    @Override
    @Transactional//还存在一人一单的多线程问题
    public Result secKillVoucher(Long voucherId) {
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);//拿到秒杀优惠券的Java实体
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("秒杀尚未开始");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("秒杀已经结束");
        }
        Integer stock = voucher.getStock();
        if (stock<1){
            return Result.fail("秒杀券库存不足");
        }

        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = RandomUtil.randomLong(10000000);
        Long userId = UserHolder.getUser().getId();
        //一人一单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count>0){
            return Result.fail("您已经购买过！");
        }
        boolean success = seckillVoucherService.update().setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).gt("stock",0).update();
        if (!success){
            return Result.fail("没有抢到！");
        }
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        return Result.ok(orderId);
    }
}
