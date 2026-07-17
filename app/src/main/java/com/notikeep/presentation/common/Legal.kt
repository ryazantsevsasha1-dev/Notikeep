package com.notikeep.presentation.common

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Single source of truth for legal document URLs. */
object Legal {
    const val PRIVACY_POLICY_URL = "https://ryazantsevsasha1-dev.github.io/Notikeep/"
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
