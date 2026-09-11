package fr.kishiswitch.guildwars.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

object Ui {
    val background = Color.rgb(16, 23, 25)
    val surface = Color.rgb(29, 40, 42)
    val ink = Color.rgb(236, 241, 238)
    val muted = Color.rgb(170, 187, 181)
    val accent = Color.rgb(174, 230, 184)
    fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()
    fun panel(color: Int = surface, radius: Float = 18f) = GradientDrawable().apply { setColor(color); cornerRadius = radius }
    fun text(context: Context, value: String, size: Float = 16f, color: Int = ink): TextView = TextView(context).apply {
        text = value; textSize = size; setTextColor(color); setLineSpacing(0f, 1.16f)
    }
    fun column(context: Context) = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    fun screen(activity: Activity): LinearLayout {
        val previous = activity.findViewById<android.view.ViewGroup>(android.R.id.content)?.getChildAt(0) as? ScrollView
        val previousScroll = previous?.scrollY ?: 0
        val scroll = ScrollView(activity).apply { setBackgroundColor(Ui.background); isFillViewport = true }
        val column = column(activity)
        val padding = dp(activity, 24)
        column.setPadding(padding, padding, padding, padding)
        scroll.addView(column, android.widget.FrameLayout.LayoutParams(-1, -2))
        scroll.setOnApplyWindowInsetsListener { _, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            column.setPadding(padding + bars.left, padding + bars.top, padding + bars.right, padding + bars.bottom)
            insets
        }
        activity.setContentView(scroll)
        if (previousScroll != 0) scroll.post { scroll.scrollTo(0, previousScroll) }
        return column
    }
    fun LinearLayout.heading(value: String, large: Boolean = false) {
        addView(text(context, value, if (large) 32f else 20f).apply {
            setTypeface(null, Typeface.BOLD)
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(context, if (large) 8 else 26); bottomMargin = dp(context, 10) })
    }
    fun LinearLayout.paragraph(value: String, color: Int = muted): TextView {
        return text(context, value, 15f, color).also {
            addView(it, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 12) })
        }
    }
    fun LinearLayout.action(label: String, enabled: Boolean = true, onClick: () -> Unit): Button = Button(context).apply {
        text = label; isAllCaps = false; textSize = 15f; setTextColor(ink)
        minHeight = dp(context, 52); backgroundTintList = android.content.res.ColorStateList.valueOf(surface)
        isEnabled = enabled; alpha = if (enabled) 1f else .45f
        setOnClickListener { onClick() }
        this@action.addView(this, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(context, 6) })
    }
    fun LinearLayout.toggle(label: String, checked: Boolean, enabled: Boolean = true, changed: (Boolean) -> Unit) {
        val switch = android.widget.Switch(context).apply {
            text = label; textSize = 16f; setTextColor(ink); isChecked = checked; isEnabled = enabled
            minHeight = dp(context, 58); setPadding(dp(context, 12), 0, dp(context, 12), 0)
            setOnCheckedChangeListener { _, value -> changed(value) }
        }
        addView(switch, LinearLayout.LayoutParams(-1, -2))
    }
}
