package br.dev.marconi.lyricalia.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ExampleBCReceiver: BroadcastReceiver() {
    val LOG = "popopo"

    override fun onReceive(context: Context?, intent: Intent?) {
        val data = intent?.getStringExtra("data") ?: return
        Log.d("IF1001_P3_LYRICALIA", "Data received -> $data");
    }
}