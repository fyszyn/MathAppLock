package com.example.mathapplock

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout

class InterceptOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var onBackPressedListener: (() -> Unit)? = null

    override fun onFinishInflate() {
        super.onFinishInflate()
        val etAnswer = findViewById<EditText>(R.id.et_answer)
        val btnVerify = findViewById<Button>(R.id.btn_verify)
        etAnswer?.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnVerify?.performClick()
                true
            } else {
                false
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) {
                // Execute graceful exit: route back to system home launcher screen
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(homeIntent)

                // Call service callback to remove the overlay view
                onBackPressedListener?.invoke()
            }
            return true // Consume back key event
        }
        return super.dispatchKeyEvent(event)
    }
}
