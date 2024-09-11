package com.vivi.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vivi.dto.Result;
import com.vivi.entity.Shop;
import com.vivi.mapper.ShopMapper;
import com.vivi.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vivi.utils.CacheClient;
import com.vivi.utils.RedisConstants;
import com.vivi.utils.SystemConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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

//    /**
//     * 保存店铺数据到redis 用逻辑过期的方式
//     * @param id
//     */
//    private void saveShop2Redis(Long id,Long expireSeconds){
//        Shop shop = getById(id);
//        RedisData redisData = new RedisData();
//        redisData.setData(shop);
//        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY + id ,JSONUtil.toJsonStr(redisData));
//    }


    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        //判断是否需要根据坐标查询
        if(x == null || y == null){
            Page<Shop> page = this.lambdaQuery().eq(Shop::getTypeId, typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }
        //计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;
        //查询redis，根据距离排序、分页 Result: shopId,distance
        String key = RedisConstants.SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );
        //解析Id
        if(results == null){
            return Result.ok();
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = results.getContent();

        if(content.size() < from){
            //此时无法跳过from条数据,没有下一页了
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = new ArrayList<>(content.size());
        Map<String,Distance> distanceMap = new HashMap<>(content.size());
        content.stream().skip(from).forEach(result -> {
            String shopId = result.getContent().getName();
            Distance distance = result.getDistance();
            ids.add(Long.valueOf(shopId));
            distanceMap.put(shopId,distance);
        });
        //查询Shop
        List<Shop> shopList = this.lambdaQuery().in(Shop::getId, ids).last("ORDER BY FIELD(id," + StrUtil.join(",", ids) + ")").list();
        for (Shop shop : shopList) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        return Result.ok(shopList);
    }
}
