package com.sorodeveloper.insaatcebimde.adapter

import android.animation.ValueAnimator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.compose.material3.TopAppBar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.sorodeveloper.insaatcebimde.databinding.MainItemBinding
import isKollari
import org.jetbrains.annotations.Debug

class IsKollariAdapter (
    private val list: MutableList<isKollari.MainItem>,
    private val isTemplateMode: Boolean = false,
    private val onAddSubItemClicked: ((String) -> Unit)? = null, // Yeni eklenen kalem ekleme callback'i
    private val onEditSubItemClicked: ((isKollari.SubItem) -> Unit)? = null, // (SubItem nesnesi)
    private val onUploadImageClicked: ((isKollari.SubItem) -> Unit)? = null,
    private val onSaveClicked: ((isKollari.SubItem, String, String, Map<String, String>) -> Unit)? = null,
    private val onMainItemClicked: (isKollari.MainItem) -> Unit
) : RecyclerView.Adapter<IsKollariAdapter.MainViewHolder>() {

    inner class MainViewHolder(val binding: MainItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val binding = MainItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MainViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val item = list[position]
        val subNumber = item.subItems.size
        val totalProgress = item.subItems.sumOf { 
            (it.progress.toIntOrNull() ?: 0) / (it.kacAdet.toIntOrNull() ?: 1) 
        }

        holder.binding.tvTitle.text = item.title
        holder.binding.kalemSayisi.text = subNumber.toString()
        val ilrl = totalProgress
        val percent = if (subNumber > 0) {
            ilrl / subNumber
        } else {
            ilrl
        }

        if (isTemplateMode) {
            holder.binding.frameLayout.visibility = View.GONE
            holder.binding.kalemSayisi.visibility = View.GONE
            holder.binding.constraintLayout.setBackgroundColor(android.graphics.Color.parseColor("#1A1A1A")) // Daha koyu, asil bir siyah
            
            // 10dp vertical padding for MainItem in template mode
            val paddingPx = (10 * holder.itemView.resources.displayMetrics.density).toInt()
            holder.binding.constraintLayout.setPadding(0, paddingPx, 0, paddingPx)
        } else {
            holder.binding.frameLayout.visibility = View.VISIBLE
            holder.binding.kalemSayisi.visibility = View.VISIBLE
            holder.binding.constraintLayout.setBackgroundColor(android.graphics.Color.parseColor("#252525"))

            // Tekrar tekrar animasyon yapmasın diye:
            if (!item.hasAnimated) {
                animateProgress(holder.binding.circleProgress, holder.binding.txtProgressPercent, percent)
                item.hasAnimated = true
            } else {
                holder.binding.txtProgressPercent.text = "${percent}%"
                holder.binding.circleProgress.progress = percent
            }
        }

        holder.binding.rvSubItems.visibility =
            if (item.isExpanded) View.VISIBLE else View.GONE
            
        // Kütüphane modunda isek ve kart açıksa "Kalem Ekle" butonunu göster
        if (isTemplateMode && item.isExpanded) {
            holder.binding.btnAddSubItem.visibility = View.VISIBLE
            holder.binding.btnAddSubItem.setOnClickListener {
                onAddSubItemClicked?.invoke(item.title)
            }
        } else {
            holder.binding.btnAddSubItem.visibility = View.GONE
        }

        val subAdapter = SubAdapter(item.subItems, isTemplateMode, { subItem ->
            onEditSubItemClicked?.invoke(subItem)
        }, { subItem ->
            onUploadImageClicked?.invoke(subItem)
        }, { subItem, cat, typ, mat ->
            onSaveClicked?.invoke(subItem, cat, typ, mat)
        }) { subItem, percent ->
          //  savePercentToFirebase(subItem.firebasePath, percent)
            Log.d("FIREBASE_PATH", "Gelen path: ${subItem.firebasePath}")
            Log.d("FIREBASE_PATH", "İş adı: ${item.title}")
            Log.d("FIREBASE_PATH", "Kalem adı: ${subItem.title}")
            Log.d("FIREBASE_PATH", "Yeni değer: $percent")

            onAltKalemProgressChanged(
                subItem.firebasePath,
                subItem.templateId,
                percent
            )
        }

        holder.binding.rvSubItems.adapter = subAdapter
        holder.binding.rvSubItems.layoutManager =
            LinearLayoutManager(holder.itemView.context)

        toggleLayout(item.isExpanded, holder.itemView, holder.binding.linearLayout)


        holder.binding.root.setOnClickListener {
            Toast.makeText(holder.itemView.context, totalProgress.toString(), Toast.LENGTH_SHORT).show()

            list.forEachIndexed { index, mainItem ->
                mainItem.isExpanded = index == position && !mainItem.isExpanded
            }
            onMainItemClicked(item)

            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = list.size

    fun updateList(newList: List<isKollari.MainItem>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    private fun toggleLayout(isExpanded: Boolean, view: View, layout: LinearLayout) {
        if (isExpanded) {
            layout.visibility = View.VISIBLE
            layout.alpha = 0f
            layout.translationY = -80f // Hafif yukarıdan başlasın
            layout.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .start()
        } else {
            layout.animate()
                .alpha(0f)
                .translationY(0f)
                .setDuration(500)
                .withEndAction { layout.visibility = View.GONE }
                .start()
        }
    }

    private fun animateProgress(
        circleBar: ProgressBar,
        percentText: TextView,
        target: Int
    ) {
        val animator = ValueAnimator.ofInt(0, target)
        animator.duration = 800
        animator.interpolator = android.view.animation.DecelerateInterpolator()

        animator.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            circleBar.progress = value
            percentText.text = "$value%"
        }
        animator.start()

    }


    fun onAltKalemProgressChanged(
        fullProgressPath: String, // en alt progress path'i
        templateId: String,
        newValue: String
    ) {
        val db = FirebaseDatabase.getInstance().reference
        val newInt = newValue.toInt()

        // 1️⃣ Eski değeri oku
        db.child(fullProgressPath).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val oldValueStr = snapshot.getValue(String::class.java) ?: "0"
                val oldInt = oldValueStr.toInt()

                val fark = newInt - oldInt

                // 2️⃣ En alt progress'i güncelle
                db.child(fullProgressPath).setValue(newValue)

                updateProgressAllUpperLevels(fullProgressPath, templateId, fark)

               /* // 3️⃣ crPath = /progress ve /kalem sil → isAdi seviyesine çık
                var crPath = fullProgressPath
                    .substringBeforeLast("/")   // /progress silindi
                    .substringBeforeLast("/")   // /kalem silindi

                // 5️⃣ Blok seviyesine çık (son 4 child sil)
                crPath = removeLastSegments(crPath, 4)
                // 🟢 Şu an: .../blok1
                Log.d("FIREBASE_PATH", "Yeni path: $crPath")
                updateByDelta("$crPath/AltKademeler/$isAdi/$kalemAdi/progress", fark)

                // 6️⃣ Etap seviyesine çık (son 6 child sil)
                crPath = removeLastSegments(crPath, 2)
                // 🟢 Şu an: .../etap1
                updateByDelta("$crPath/AltKademeler/$isAdi/$kalemAdi/progress", fark)

                // 7️⃣ Saha seviyesine çık (son 8 child sil)
                crPath = removeLastSegments(crPath, 2)
                // 🟢 Şu an: .../anaSaha
                updateByDelta("$crPath/AltKademeler/$isAdi/$kalemAdi/progress", fark)*/
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun updateByDelta(path: String, delta: Int) {
        val ref = FirebaseDatabase.getInstance().getReference(path)
        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val currentStr = currentData.getValue(String::class.java) ?: "0"
                val currentInt = currentStr.toIntOrNull() ?: 0
                val newValue = currentInt + delta
                currentData.value = newValue.toString()
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {}
        })
    }
    fun removeLastSegments(path: String, count: Int): String {
        var result = path
        repeat(count) {
            result = result.substringBeforeLast("/")
        }
        return result
    }

    fun getAllUpperAltKademelerPaths(fullProgressPath: String): List<String> {
        val parts = fullProgressPath.split("/").toMutableList()
        val results = mutableListOf<String>()

        // isler seviyesini bul
        val islerIndex = parts.indexOfLast { it == "isler" }
        if (islerIndex == -1) return emptyList()

        // isler'dan bir üst seviyeden başla
        var idx = islerIndex - 1

        //sahalar childına kadar
        while (idx >= 3) {
            val currentNode = parts[idx]
            val basePath = parts.subList(0, idx-1 ).joinToString("/")
            // ✅ Her zaman bulunduğun seviyenin AltKademeler'ini ekle

            // ✅ Eğer anaSaha ise → sahalar/AltKademeler de ekle (ama anaSaha'nınkini silmiyoruz)
            if (currentNode == "anaSaha") {
                val sahalarIndex = parts.indexOf("sahalar")
                if (sahalarIndex != -1) {
                    val sahalarPath = parts.subList(0, sahalarIndex + 1).joinToString("/")
                    results.add("$sahalarPath/AltKademeler")
                }
            }
            else{
                results.add("$basePath/AltKademeler")

            }

            // 🔼 Bir üst mülke çık (daire → blok → etap → saha → sahalar)
            idx -= 2
        }

        return results.distinct()
    }




    fun updateProgressAllUpperLevels(
        fullProgressPath: String,
        templateId: String,
        fark: Int
    ) {
        val altKademelerPaths = getAllUpperAltKademelerPaths(fullProgressPath)

        for (basePath in altKademelerPaths) {
            // Tam yol: .../AltKademeler/templateId/progress
            val targetPath = "$basePath/$templateId/progress"
            updateByDelta(targetPath, fark)
        }
    }




}
