local voucherId = ARGV[1]
local userId = ARGV[2]

-- 库存key
local stockKey = 'seckill:stock:' .. voucherId
-- 订单key
local orderKey = 'seckill:order' .. voucherId
if(tonumber(redis.call('get',stockKey)) <=0) then
    return 1
end
if(redis.call('sismember',orderKey,userId) == 1) then
    return 2
end
--扣库存
redis.call('incrby',stockKey,-1)
--下单，保存用户
redis.call('sadd',orderKey,userId)
