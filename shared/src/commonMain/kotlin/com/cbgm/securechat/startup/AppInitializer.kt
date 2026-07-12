package com.cbgm.securechat.startup

interface AppInitializer {

    suspend fun initialize():
            Result<AppInitializationResult>
}