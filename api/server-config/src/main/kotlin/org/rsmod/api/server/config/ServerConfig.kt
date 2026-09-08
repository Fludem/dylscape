package org.rsmod.api.server.config

public data class ServerConfig(
    val realm: String,
    val world: Int,
    val firstLaunch: Boolean,
    val port: Int = DEFAULT_PORT,
) {
    override fun toString(): String = "ServerConfig(realm='$realm', world=$world, port=$port)"

    public companion object {
        public const val DEFAULT_PORT: Int = 43594
    }
}
