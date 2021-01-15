--https://redis.io/commands/eval
redis.replicate_commands()

--queue name that we get uuidlist from
local queueName = KEYS[1]

--the map we use to store processing uuidlist, start processing time to uuidlist
local ingSetName = KEYS[2]

--before how long ago, we treat them as dead and re process, see https://redis.io/commands/time
local backTime = tonumber( KEYS[3] )

--see https://redis.io/commands/time
local nowArr = redis.call( "TIME" )
local now = nowArr[1] * 1000000 + nowArr[2]
local nowStr = string.format("%16.0f", now)

--how many are being processed
local ingSize = redis.call( "HLEN", ingSetName )

local todoLst = nil

if ingSize > 0 then
    local ingKeysLst = redis.call( "HKEYS", ingSetName )
    local deadKey = nil

    local i = 1
    while ingKeysLst[ i ] do
        if tonumber( ingKeysLst[i] ) < now - backTime then
            deadKey = ingKeysLst[i]
            break
        end
        i = i + 1
    end

    --found the list that need to be reprocessed
    if deadKey ~= nil then
        --append processing time to deadkey
        todoLst = redis.call( "HGET", ingSetName, deadKey ) .. "@" .. nowStr
        redis.call( "HDEL", ingSetName, deadKey )
    end
end

--nothing to reprocess, then pop from the queue
if todoLst == nil then
    todoLst = redis.call( "RPOP", queueName )
end

--start processing, put it into processing map
if todoLst ~= nil and todoLst ~= false then
    redis.call( "HSET", ingSetName, now, todoLst )
    return nowStr .. "@" .. todoLst
end

return nil