package com.sorodeveloper.insaatcebimde.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.HorizontalScrollView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import isKollari
import com.bumptech.glide.Glide

class TabsPagerAdapter(
    private val context: Context,
    private val item: isKollari.SubItem
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class GenericViewHolder(val view: android.view.View) : RecyclerView.ViewHolder(view)

    override fun getItemViewType(position: Int): Int = position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            0 -> {
                // GENEL SEKME (İpucu yazısı çıkılabilir)
                val tv = TextView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    gravity = Gravity.CENTER
                    text = "Genel ilerlemeyi yukarıdaki barlardan ayarlayabilirsiniz."
                    setTextColor(Color.GRAY)
                }
                return GenericViewHolder(tv)
            }
            1 -> {
                // DETAY SEKME - Malzemeler Listesi
                val scrollView = ScrollView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
                val container = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(48, 48, 48, 48)
                }

                if (item.materials.isNullOrEmpty()) {
                    val emptyTv = TextView(context).apply {
                        text = "Bu iş kalemine ait tanımlı malzeme bulunamadı."
                        gravity = Gravity.CENTER
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setPadding(0, 100, 0, 0)
                        setTextColor(Color.GRAY)
                    }
                    container.addView(emptyTv)
                } else {
                    item.materials!!.forEach { (name, amount) ->
                        val matContainer = LinearLayout(context).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply {
                                setMargins(0, 0, 0, 24)
                            }
                        }
                        val nameTv = TextView(context).apply {
                            text = name
                            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                            setTypeface(null, Typeface.BOLD)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                            setTextColor(Color.BLACK)
                        }
                        val amountTv = TextView(context).apply {
                            text = amount
                            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                            setTextColor(Color.DKGRAY)
                            setBackgroundColor(Color.parseColor("#E0E0E0"))
                            setPadding(16, 8, 16, 8)
                        }
                        matContainer.addView(nameTv)
                        matContainer.addView(amountTv)
                        container.addView(matContainer)
                    }
                }
                scrollView.addView(container)
                return GenericViewHolder(scrollView)
            }
            else -> {
                // EKSTRA SEKME - Çizimler/Resimler Yatay Galeri
                val scrollView = HorizontalScrollView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    isFillViewport = true

                    // ViewPager2 ile yatay kaydırma çakışmasını önleyen kritik dokunuş
                    setOnTouchListener { v, event ->
                        when (event.action) {
                            android.view.MotionEvent.ACTION_DOWN,
                            android.view.MotionEvent.ACTION_MOVE -> {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                            android.view.MotionEvent.ACTION_UP,
                            android.view.MotionEvent.ACTION_CANCEL -> {
                                v.parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }
                        false
                    }
                }
                val container = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(32, 32, 32, 32)
                    gravity = Gravity.CENTER_VERTICAL
                }

                if (item.drawingUrls.isEmpty()) {
                    val emptyTv = TextView(context).apply {
                        text = "Eklenmiş çizim veya resim bulunamadı."
                        gravity = Gravity.CENTER
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setPadding(100, 100, 100, 100)
                        setTextColor(Color.GRAY)
                    }
                    return GenericViewHolder(emptyTv)
                } else {
                    item.drawingUrls.forEach { url ->
                        val card = androidx.cardview.widget.CardView(context).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150f, resources.displayMetrics).toInt(),
                                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150f, resources.displayMetrics).toInt()
                            ).apply {
                                setMargins(16, 16, 16, 16)
                            }
                            radius = 24f
                            cardElevation = 8f
                        }
                        val iv = ImageView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        Glide.with(context)
                            .load(url)
                            .into(iv)
                            
                        card.addView(iv)
                        container.addView(card)
                    }
                }
                scrollView.addView(container)
                return GenericViewHolder(scrollView)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Tüm görünümler onCreate içerisinde dinamik çizildiği için bind içi boştur.
    }

    override fun getItemCount() = 3
}
