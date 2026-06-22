-- 캔들 OHLCV 누적 (원자적 실행)
-- KEYS[1] : Redis 해시 키
-- ARGV[1] : price (체결가, string)
-- ARGV[2] : volumeDelta (이번 틱의 거래량 증가분, string)
-- ARGV[3] : bucketStart (yyyyMMddHHmm, 이 캔들이 속한 구간의 시작점을 가리키는 식별자)

local key       = KEYS[1]
local priceNum  = tonumber(ARGV[1])
local volumeNum = tonumber(ARGV[2])
local bucket    = string.sub(ARGV[3], 1, 8)

if redis.call('EXISTS', key) == 0 then
    redis.call('HMSET', key,
        'open',      ARGV[1],
        'high',      ARGV[1],
        'low',       ARGV[1],
        'close',     ARGV[1],
        'volume',    ARGV[2],
        'startTime', ARGV[3])
    return
end

-- stale 감지: 저장된 구간 식별자가 이번 구간과 다르면 키 초기화 (이전 구간이 flush 안 되고 남아있던 경우)
local storedStart = redis.call('HGET', key, 'startTime')
if storedStart and string.sub(storedStart, 1, 8) ~= bucket then
    redis.call('DEL', key)
    redis.call('HMSET', key,
        'open',      ARGV[1],
        'high',      ARGV[1],
        'low',       ARGV[1],
        'close',     ARGV[1],
        'volume',    ARGV[2],
        'startTime', ARGV[3])
    return
end

local high = tonumber(redis.call('HGET', key, 'high'))
local low  = tonumber(redis.call('HGET', key, 'low'))
local vol  = tonumber(redis.call('HGET', key, 'volume'))

local newHigh = priceNum > high and priceNum or high
local newLow  = priceNum < low  and priceNum or low

redis.call('HMSET', key,
    'high',   tostring(math.floor(newHigh)),
    'low',    tostring(math.floor(newLow)),
    'close',  ARGV[1],
    'volume', tostring(math.floor(vol + volumeNum)))
