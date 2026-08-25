-- Example DeltaX plugin
-- Demonstrates the onEnable / onDisable lifecycle and the log() helper.

function onEnable()
    log("INFO", "DeltaX example plugin '" .. SCRIPT_NAME .. "' enabled")
end

function onDisable()
    log("INFO", "DeltaX example plugin '" .. SCRIPT_NAME .. "' disabled")
end
