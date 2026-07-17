package com.notikeep.presentation.common

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Single source of truth for legal document URLs. */
object Legal {
    // TODO: replace with the real GitHub Pages URL before release.
    const val PRIVACY_POLICY_URL = "https://REPLACE_ME.github.io/notikeep-privacy/"
}

/** Opens [url] in the user's browser; a no-op if no browser can handle it. */
fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
