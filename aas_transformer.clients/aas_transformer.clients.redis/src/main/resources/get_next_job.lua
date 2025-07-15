local job_queue_name = "jobs"

-- Hole die Liste aus Redis
local listItems = redis.call('LRANGE', job_queue_name, 0, -1)

-- Iteriere über die JSON-Strings in der Liste
for _, jsonString in ipairs(listItems) do
    -- Parse den JSON-String
    local jsonData = cjson.decode(jsonString)

    -- Extrahiere den spezifischen Wert
    local targetSubmodelId = jsonData['targetSubmodelId']

    if jsonData['targetSubmodelId'] == cjson.null then
        return jsonString
    end

    -- Überprüfe, ob der lock für targetSubmodelId existiert
    local cursor = "0"
    repeat
        -- Führe SCAN mit dem Cursor aus
        local scanResult = redis.call("SCAN", cursor, "MATCH", "*" .. targetSubmodelId .. "*")
        cursor = scanResult[1]
        local keys = scanResult[2]

        if #keys == 0 then
            return jsonString
        end
    until cursor == "0"
end

return ""