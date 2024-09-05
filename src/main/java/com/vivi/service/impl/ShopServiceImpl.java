package com.vivi.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.vivi.dto.Result;
import com.vivi.entity.Shop;
import com.vivi.mapper.ShopMapper;
import com.vivi.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vivi.utils.CacheClient;
import com.vivi.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;


@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CacheClient client;

    @Override
    public Result queryById(Long id) {
//        //1. 从redis中查询缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(RedisConstants.CACHE_SHOP_KEY + id);
//
//        //2. 判断是否存在
//        if(StrUtil.isNotBlank(shopJson)){
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return Result.ok(shop);
//        }
//        //判断是否是空值
//        if(shopJson != null ){
//            return Result.fail("店铺不存在");
//        }
//        Shop shop = null;
//        try {
//            //拿到互斥锁
//            boolean isLock = tryLock(RedisConstants.LOCK_SHOP_KEY + id);
//            if(!isLock){
//                Thread.sleep(50);
//                return queryById(id);
//            }
//            //2.1 不存在，查询数据库
//            //2.2 数据库不存在，返回错误
//            shop = getById(id);
//            if(shop == null){
//                //缓存空值
//                stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return Result.fail("店铺不存在");
//            }
//            stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }finally {
//            unlock(RedisConstants.LOCK_SHOP_KEY + id);
//        }
//        return Result.ok(shop);
        Shop shop = client.queryWithPassThrough(RedisConstants.CACHE_SHOP_KEY, id, Shop.class, shopId -> getById(shopId), RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        if(shop == null){
            return Result.fail("店铺不存在");
        }
        return Result.ok(shop);

    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("店铺信息发生错误");
        }
        //更新数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }

    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfPresent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    /**
     * 保存店铺数据到redis 用逻辑过期的方式
     * @param id
     */
//    private void saveShop2Redis(Long id,Long expireSeconds){
//        Shop shop = getById(id);
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id ,JSONUtil.toJsonStr(redisData));
//    }
}
