package ge.elderfriend.launcher

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout

/**
 * Camera screen. Four giant buttons: take a photo, flashlight on/off, photos,
 * home. The flashlight is our own torch control (no extra permission) and is
 * always switched off when the screen closes, so it cannot drain the battery.
 */
class CameraActivity : Activity() {

    private companion object {
        const val TAG = "ElderfriendCamera"
    }

    private var torchOn = false
    private var torchButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_home)
            setPadding(Ui.dp(this@CameraActivity, 8), Ui.dp(this@CameraActivity, 10), Ui.dp(this@CameraActivity, 8), Ui.dp(this@CameraActivity, 8))
        }
        root.addView(
            Ui.text(this, getString(R.string.tile_camera), 24f, Color.parseColor("#FFB9C6D2")),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            Ui.bigButton(this, getString(R.string.take_photo), Color.parseColor("#FF146C3A"), 34f, 140) {
                Apps.takePhoto(this)
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).also {
                it.setMargins(0, Ui.dp(this, 10), 0, 0)
            }
        )

        val torch = Ui.bigButton(
            this,
            getString(R.string.flashlight_off),
            Color.parseColor("#FF8A4B00"),
            32f,
            140
        ) { toggleTorch() }
        torchButton = torch
        root.addView(
            torch,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).also {
                it.setMargins(0, Ui.dp(this, 10), 0, 0)
            }
        )

        root.addView(
            Ui.bigButton(this, getString(R.string.gallery), Color.parseColor("#FF0B4F9C"), 32f, 140) {
                Apps.openPhotos(this)
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f).also {
                it.setMargins(0, Ui.dp(this, 10), 0, 0)
            }
        )

        root.addView(
            Ui.bigButton(this, getString(R.string.home), Color.parseColor("#FF2A2A2A"), 30f, 96) {
                finish()
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.setMargins(0, Ui.dp(this, 10), 0, 0) }
        )

        setContentView(root)
    }

    override fun onPause() {
        super.onPause()
        android.util.Log.i(TAG, "onPause, torchOn=$torchOn")
        if (torchOn) setTorch(false)
    }

    private fun toggleTorch() {
        val id = torchId()
        if (id == null) {
            Ui.info(this, getString(R.string.no_flash))
            return
        }
        setTorch(!torchOn)
    }

    private fun setTorch(on: Boolean) {
        val id = torchId() ?: return
        val manager = getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        try {
            manager.setTorchMode(id, on)
            torchOn = on
            android.util.Log.i(TAG, "setTorch($on) on camera $id -> ok")
            torchButton?.setText(
                if (on) R.string.flashlight_on else R.string.flashlight_off
            )
        } catch (e: Exception) {
            android.util.Log.w(TAG, "setTorch($on) failed", e)
            torchOn = false
            torchButton?.setText(R.string.flashlight_off)
            Ui.info(this, getString(R.string.no_flash))
        }
    }

    /** Prefer the back camera with a flash; any camera with a flash otherwise. */
    private fun torchId(): String? {
        val manager = getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return null
        return try {
            val ids = manager.cameraIdList
            val back = ids.firstOrNull {
                manager.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    manager.getCameraCharacteristics(it)
                        .get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
            }
            back ?: ids.firstOrNull {
                manager.getCameraCharacteristics(it)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
    }
}
