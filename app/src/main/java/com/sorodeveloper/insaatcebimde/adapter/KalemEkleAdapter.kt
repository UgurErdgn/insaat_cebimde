package com.sorodeveloper.insaatcebimde.adapter

import android.R
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase
import com.sorodeveloper.insaatcebimde.KalemCardModel
import com.sorodeveloper.insaatcebimde.MaterialItem
import com.sorodeveloper.insaatcebimde.databinding.CardviewKalemEklemeBinding
import com.sorodeveloper.insaatcebimde.model.ImagePreviewDialog
import com.sorodeveloper.insaatcebimde.model.SpinnerItem

class KalemEkleAdapter(
    private var spinnerList: List<SpinnerItem>,
    private val mContext : Context,
    private val insaatId: String,
    private val mulkTuru: String,
    private val onUploadClick: (position: Int) -> Unit
) : RecyclerView.Adapter<KalemEkleAdapter.KalemViewHolder>() {
    private val items = mutableListOf<KalemCardModel>()

    fun updateSpinnerList(newList: List<SpinnerItem>) {
        spinnerList = newList
        notifyDataSetChanged()
    }

    fun addNewItem() {
        items.add(0, KalemCardModel())
        notifyItemInserted(0)
    }

    inner class KalemViewHolder(
        val binding: CardviewKalemEklemeBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private lateinit var materialsAdapter : KalemMaterialsAdapter
      /*  private val imageAdapter: SelectedImagesAdapter =
            SelectedImagesAdapter(
                mutableListOf<Uri>()
            ) { uri, position ->

                ImagePreviewDialog(
                    imageUri = uri,
                    isLocal = true,
                ) {
                    imageAdapter.removeAt(position)
                }.show(
                    (binding.root.context as AppCompatActivity)
                        .supportFragmentManager,
                    "preview"
                )
            }
        private val uploadedImageAdapter: UploadedImagesAdapter =
            UploadedImagesAdapter(
                mutableListOf<String>()
            ) { url ->

                ImagePreviewDialog(
                    imageUri = Uri.parse(url),
                    isLocal = false,
                    onDelete = null
                ).show(
                    (binding.root.context as AppCompatActivity)
                        .supportFragmentManager,
                    "preview"
                )
            }*/

      /*  init {
            binding.rvImages.layoutManager =
                LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false)
            binding.rvImages.adapter = imageAdapter
            binding.rvImages.isNestedScrollingEnabled = false
            
            binding.rvLocalImages.layoutManager =
                LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false)
            binding.rvLocalImages.adapter = uploadedImageAdapter
            binding.rvLocalImages.isNestedScrollingEnabled = false
        }*/

        fun bind(model: KalemCardModel) {
            val spinnerAdapter = ArrayAdapter(
                binding.root.context,
                R.layout.simple_spinner_dropdown_item,
                spinnerList
            )
            binding.spinner2.adapter = spinnerAdapter

            binding.spinner2.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {

                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val selected = spinnerList[position]

                        if (model.selectedKalem == selected) {
                            return
                        }

                        model.selectedKalem = selected
                        fetchKalemDetails(selected, model)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }

            binding.btnUploadImage.setOnClickListener {
                onUploadClick(adapterPosition)
            }

            materialsAdapter = KalemMaterialsAdapter(model.malzemeler)

            binding.rvMaterials.layoutManager =
                LinearLayoutManager(mContext)

            binding.rvMaterials.adapter = materialsAdapter


            binding.btnAddSubItem.setOnClickListener {
                materialsAdapter.addItem()
            }
            
        }

        private fun fetchKalemDetails(
            selected: SpinnerItem,
            model: KalemCardModel
        ) {
            val ref = FirebaseDatabase.getInstance()
                .getReference("insaatlar")
                .child(insaatId)
                .child("insaatIsleri")
                .child(mulkTuru)
                .child("isdeneme")
                .child(selected.kalemAdi)
                .child(selected.turAdi)

            ref.get().addOnSuccessListener { snapshot ->
                model.uploadedCizimler.clear()
                model.uploadedCizimler.addAll(
                    snapshot.child("cizimler")
                        .children.mapNotNull {
                            it.getValue(String::class.java)
                        }
                )

                model.malzemeler.clear()
                snapshot.child("malzemeler")
                    .children.forEach {
                        model.malzemeler.add(
                            MaterialItem(
                                name = it.key ?: "",
                                quantity = it.getValue(String::class.java) ?: ""
                            )
                        )
                    }


                model.progress =
                    snapshot.child("progress").value?.toString() ?: "0"

               /* imageAdapter.updateList(model.localCizimler)
                uploadedImageAdapter.updateList(model.uploadedCizimler)
                materialsAdapter.refresh()*/
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KalemViewHolder {
        val binding = CardviewKalemEklemeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return KalemViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: KalemViewHolder, position: Int) {
        holder.bind(items[position])
    }

    fun getItem(position: Int): KalemCardModel {
        return items[position]
    }
}