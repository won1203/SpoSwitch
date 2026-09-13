package com.example.sposwitch.ui.theme

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.example.sposwitch.R

/** Design tokens and primitive native views shared by every feature. */
internal class MockUi(private val context: Context) {
    private val regularFont = ResourcesCompat.getFont(context, R.font.pretendard_regular)
    private val boldFont = ResourcesCompat.getFont(context, R.font.pretendard_bold)

    val green = Color.rgb(5, 62, 43)
    val lime = Color.rgb(193, 240, 97)
    val accent = Color.rgb(99, 153, 14)
    val muted = Color.rgb(106, 117, 113)
    val pale = Color.rgb(243, 248, 242)
    val line = Color.rgb(227, 233, 228)
    val white = Color.WHITE

    fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()

    fun surface(color: Int = pale, radius: Int = 14) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
    }

    fun column(padding: Int = 0) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
    }

    fun row() = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    fun text(value: String, size: Int = 15, color: Int = green, bold: Boolean = false) = TextView(context).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        typeface = if (bold) boldFont else regularFont
        includeFontPadding = false
        setLineSpacing(dp(3).toFloat(), 1f)
    }

    fun icon(resource: Int, color: Int = green, size: Int = 24) = ImageView(context).apply {
        setImageResource(resource)
        imageTintList = ColorStateList.valueOf(color)
        layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun click(view: View, label: String? = null, action: () -> Unit) {
        view.isClickable = true
        view.isFocusable = true
        if (label != null) view.contentDescription = label
        view.foreground = RippleDrawable(ColorStateList.valueOf(0x18053E2B), null, surface(white))
        view.setOnClickListener { action() }
    }

    fun iconButton(resource: Int, label: String, action: () -> Unit) = FrameLayout(context).apply {
        addView(icon(resource), FrameLayout.LayoutParams(dp(25), dp(25), Gravity.CENTER))
        minimumWidth = dp(48)
        minimumHeight = dp(48)
        click(this, label, action)
    }

    fun button(label: String, primary: Boolean = true, icon: Int = R.drawable.ic_arrow_forward, action: () -> Unit) = row().apply {
        setPadding(dp(18), dp(16), dp(18), dp(16))
        minimumHeight = dp(52)
        background = surface(if (primary) green else pale)
        addView(text(label, 16, if (primary) white else green, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(icon(icon, if (primary) white else green, 23))
        click(this, label, action)
    }

    fun chip(label: String, selected: Boolean, action: () -> Unit) = text(label, 14, if (selected) green else muted, selected).apply {
        gravity = Gravity.CENTER
        minimumHeight = dp(48)
        setPadding(dp(14), dp(10), dp(14), dp(10))
        background = surface(if (selected) lime else pale, 24)
        click(this, label, action)
        isSelected = selected
    }

    fun image(resource: Int) = ImageView(context).apply {
        setImageResource(resource)
        scaleType = ImageView.ScaleType.CENTER_CROP
        background = surface()
        clipToOutline = true
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun add(parent: LinearLayout, child: View, height: Int = -2, top: Int = 0) {
        parent.addView(child, LinearLayout.LayoutParams(-1, if (height >= 0) dp(height) else height).apply { topMargin = dp(top) })
    }

    fun gap(parent: LinearLayout, height: Int) = add(parent, View(context), height)
    fun divider(parent: LinearLayout, top: Int = 16) = add(parent, View(context).apply { setBackgroundColor(line) }, 1, top)

    fun section(parent: LinearLayout, title: String, more: (() -> Unit)? = null) {
        val row = row()
        row.addView(text(title, 20, bold = true), LinearLayout.LayoutParams(0, -2, 1f))
        if (more != null) {
            row.addView(text("더보기", 14, muted).apply {
                gravity = Gravity.CENTER
                minimumHeight = dp(48)
                minimumWidth = dp(60)
                click(this, "$title 더보기", more)
            })
        }
        add(parent, row)
    }

    fun note(parent: LinearLayout, value: String, resource: Int = R.drawable.ic_rainy, action: (() -> Unit)? = null) {
        add(parent, row().apply {
            background = surface()
            setPadding(dp(16), dp(14), dp(16), dp(14))
            addView(icon(resource, green, 24))
            addView(text(value, 13, green), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(12) })
            if (action != null) click(this, value, action)
        })
    }
}
