package br.dev.marconi.lyricalia.utils

import android.content.Context
import android.content.Intent
import br.dev.marconi.lyricalia.activities.MainActivity

class NavigationUtils {
    companion object {
        fun navigateToLogin(packageContext: Context) {
            val intent = Intent(packageContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            packageContext.startActivity(intent)
        }
    }
}