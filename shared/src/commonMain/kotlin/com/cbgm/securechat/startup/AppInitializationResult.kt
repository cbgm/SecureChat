package com.cbgm.securechat.startup

data class AppInitializationResult(
    /**
     * True only when startup created a new identity during this run.
     */
    val identityCreated: Boolean
)