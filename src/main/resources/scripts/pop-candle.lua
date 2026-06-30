local key = KEYS[1]

if redis.call('EXISTS', key) == 0 then
    return {}
end

local data = redis.call('HGETALL', key)
redis.call('DEL', key)
return data
