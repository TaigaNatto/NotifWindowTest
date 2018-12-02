package natto.com.notifwindowtest

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.support.v4.app.ActivityCompat.invalidateOptionsMenu
import android.transition.TransitionManager
import android.util.Log
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView


class OverlayService : Service() {
    private var view: View? = null
    private var windowManager: WindowManager? = null
    private var dpScale: Int = 0
    private var typeLayer: Int = 0

    private var isBigView: Boolean = false

    var overlayImgV: ImageView? = null
    var webV: WebView? = null

    override fun onCreate() {
        super.onCreate()

        dpScale = resources.displayMetrics.density.toInt()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val context = applicationContext
        val channelId = "default"
        val title = context.getString(R.string.app_name)

        val pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT)

        //8.0(Oreo)以上用のNotification設定
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                    channelId, title, NotificationManager.IMPORTANCE_DEFAULT)

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel)

                val notification = Notification.Builder(context, channelId)
                        .setContentTitle(title)
                        .setSmallIcon(android.R.drawable.btn_star)
                        .setContentText("APPLICATION_OVERLAY")
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setWhen(System.currentTimeMillis())
                        .build()

                startForeground(1, notification)
            }
        }

        // inflaterの生成
        val layoutInflater = LayoutInflater.from(this)

        //8.0(Oreo)以上のみレイヤータイプを分ける
        typeLayer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT

        windowManager = applicationContext
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                typeLayer,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT)

        params.gravity = Gravity.TOP or Gravity.LEFT

        val nullParent: ViewGroup? = null
        view = layoutInflater.inflate(R.layout.layout_overlay, nullParent)
        //クリックで解除できるように
        view?.findViewById<Button>(R.id.btn_overlay)
                ?.setOnClickListener {
                    // Viewを削除
                    windowManager!!.removeView(view)
                }
        overlayImgV = view?.findViewById<ImageView>(R.id.ic_overlay)
        overlayImgV?.setOnClickListener {
            if (!isBigView) {
                openBigView()
            } else {
                closeBigView()
            }
        }
        webV=view?.findViewById<WebView>(R.id.web_overlay)
        webV?.visibility=View.GONE
        webV?.webViewClient = WebViewClient()
        webV?.loadUrl("https://www.google.com/")

        // ViewにTouchListenerを設定する
        view!!.setOnTouchListener { v, event ->
            Log.d("debug", "onTouch")
            if (event.action == MotionEvent.ACTION_DOWN) {
                Log.d("debug", "ACTION_DOWN")
            }
            if (event.action == MotionEvent.ACTION_UP) {
                Log.d("debug", "ACTION_UP")

                view!!.performClick()

                stopSelf()
            }
            false
        }

        // Viewを画面上に追加
        windowManager!!.addView(view, params)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("debug", "onDestroy")
        // Viewを削除
        windowManager!!.removeView(view)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun openBigView() {
        val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                typeLayer,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT)
        params.gravity = Gravity.TOP or Gravity.LEFT
        windowManager?.updateViewLayout(view, params)

        val cx = overlayImgV?.left!! + overlayImgV?.width!! / 2
        val cy = overlayImgV?.top!! + overlayImgV?.height!! / 2
        val radius = Math.max(webV?.width!!, webV?.height!!)

        val anim = android.view.ViewAnimationUtils.createCircularReveal(webV, cx, cy, 0f, radius.toFloat())

        webV?.visibility = View.VISIBLE
        TransitionManager.beginDelayedTransition(webV?.rootView as ViewGroup)
        anim.start()

        isBigView = true
    }

    private fun closeBigView() {

        val cx = overlayImgV?.left!! + overlayImgV?.width!! / 2
        val cy = overlayImgV?.top!! + overlayImgV?.height!! / 2
        val radius = Math.max(webV?.width!!, webV?.height!!)

        val anim = android.view.ViewAnimationUtils.createCircularReveal(webV, cx, cy, radius.toFloat(), 0f)
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                webV?.visibility = View.GONE
                TransitionManager.beginDelayedTransition(webV?.rootView as ViewGroup)

                val params = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        typeLayer,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.TRANSLUCENT)
                params.gravity = Gravity.TOP or Gravity.LEFT
                windowManager?.updateViewLayout(view, params)
            }
        })
        anim.start()

        isBigView = false
    }
}