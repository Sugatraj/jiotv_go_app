package com.skylake.skytv.jgorunner.data

import com.skylake.skytv.jgorunner.ui.tvhome.OmniChannel

enum class StartupChannelMode(val preferenceValue: String) {
    LAST_PLAYED("last_played"),
    FIXED_CHANNEL("fixed_channel"),
    NONE("none"),
    LEGACY_FIRST("legacy_first");

    companion object {
        fun fromPreference(value: String?): StartupChannelMode =
            entries.firstOrNull { it.preferenceValue == value } ?: NONE
    }
}

data class LegacyStartupSelection(
    val mode: StartupChannelMode,
    val firstChannelFallback: Boolean
)

fun migrateLegacyStartupSelection(first: Boolean, last: Boolean): LegacyStartupSelection = when {
    last -> LegacyStartupSelection(StartupChannelMode.LAST_PLAYED, first)
    first -> LegacyStartupSelection(StartupChannelMode.LEGACY_FIRST, false)
    else -> LegacyStartupSelection(StartupChannelMode.NONE, false)
}

fun stableChannelId(tvgId: String?, streamUrl: String?): String? {
    tvgId?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    val uri = try {
        java.net.URI(streamUrl ?: return null)
    } catch (_: java.net.URISyntaxException) {
        return null
    }
    val host = uri.host?.lowercase() ?: return null
    if (host != "localhost" && host != "127.0.0.1") return null
    val path = uri.path ?: return null
    val route = when {
        path.startsWith("/live/") -> path.removePrefix("/live/")
        path.startsWith("/play/") -> path.removePrefix("/play/")
        else -> return null
    }
    return route.substringBefore('/').substringBefore('.').trim().takeIf { it.isNotEmpty() }
}

fun resolveFixedChannel(channels: List<OmniChannel>, fixedId: String?): OmniChannel? {
    if (fixedId.isNullOrBlank()) return null
    return channels.firstOrNull { it.id == fixedId }
}

fun resolveLastPlayedChannel(
    channels: List<OmniChannel>,
    channelId: String?,
    legacyName: String?,
    legacyUrl: String?
): OmniChannel? {
    if (!channelId.isNullOrBlank()) {
        return channels.firstOrNull { it.id == channelId }
    }
    val name = legacyName?.trim().orEmpty()
    val url = legacyUrl?.trim().orEmpty()
    if (name.isEmpty() && url.isEmpty()) return null
    return channels.firstOrNull { channel ->
        (name.isNotEmpty() && channel.name?.trim().equals(name, ignoreCase = true)) ||
            (url.isNotEmpty() && listOfNotNull(channel.url, channel.m3u8Url, channel.mpdUrl)
                .any { it.equals(url, ignoreCase = true) })
    }
}
