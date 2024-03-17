package ru.nsychev.proxies

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bbottema.javasocksproxyserver.SocksServer
import java.util.Properties

class ProxyService : Service() {
    private lateinit var session: Session
    private lateinit var socksServer: SocksServer
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val mainActivityIntent = Intent(this, MainActivity::class.java)
        mainActivityIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        val pendingIntent = PendingIntent.getActivity(this, 0, mainActivityIntent,
            PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Proxy service is working")
            .setSmallIcon(R.mipmap.proxier)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)

        startServer()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (this::session.isInitialized) session.disconnect()
        if (this::socksServer.isInitialized) socksServer.stop()

        sendToActivity(false)
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun startServer() {
        serviceScope.launch {
            try {
                socksServer = SocksServer()
                socksServer.start(LOCAL_PORT)

                val privateKey = resources.openRawResource(R.raw.private_key).readBytes()
                val publicKey = resources.openRawResource(R.raw.public_key).readBytes()
                val jsch = JSch()
                jsch.addIdentity("id_rsa", privateKey, publicKey, null)

                session = jsch.getSession(SSH_USER, SSH_HOST, SSH_PORT)
                session.timeout = TIMEOUT
                val config = Properties()
                config["StrictHostKeyChecking"] = "no"
                session.setConfig(config)
                while (true) {
                    if (!session.isConnected) {
                        session.connect(TIMEOUT)
                        session.setPortForwardingR(
                            SSH_REMOTE_HOST,
                            SSH_REMOTE_PORT,
                            LOCAL_HOST.hostAddress,
                            LOCAL_PORT
                        )
                        sendToActivity(true)
                    }
                    delay(3000)
                }
            } catch (exc: CancellationException) {
                // no-op
            } catch (exc: Exception) {
                exc.printStackTrace()
                sendToActivity(false, exc.message)
                stopSelf()
            }
        }
    }

    private fun sendToActivity(started: Boolean, reason: String? = null) {
        val intent = Intent(INTENT_ACTION)
        intent.putExtra("started", started)
        reason?.let { intent.putExtra("reason", it) }
        sendBroadcast(intent)
    }

    companion object {
        const val CHANNEL_ID = "ProxyServiceChannel"
        const val TIMEOUT = 5000
    }
}