package com.sorodeveloper.insaatcebimde

import android.R.attr.spacing
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.RecyclerView
import com.caverock.androidsvg.SVG
import com.google.android.gms.tasks.Tasks
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.sorodeveloper.insaatcebimde.adapter.InsaatDetayAdapter
import com.sorodeveloper.insaatcebimde.databinding.FragmentDenemeBinding
import com.sorodeveloper.insaatcebimde.databinding.FragmentInsaatdetayIsbolumleriBinding
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.sqrt


class deneme : Fragment() {

    private lateinit var binding: FragmentDenemeBinding

    private val storageRef = FirebaseStorage.getInstance().reference.child("svgs")

    private val pickSvgLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                uploadSvgToFirebase(it)
                showSvgInImageView(it)   // 👉 Burada hemen ImageView’e de basıyoruz
            }
        }

    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    private val NONE = 0
    private val DRAG = 1
    private val ZOOM = 2
    private var mode = NONE

    private var start = PointF()
    private var mid = PointF()
    private var oldDist = 1f

    private lateinit var scaleDetector: ScaleGestureDetector

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // SVG düzgün çizilsin diye
        binding.imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        binding.button2.setOnClickListener {
            pickSvgLauncher.launch("image/svg+xml")
        }
        loadLastUploadedSvgFromFirebase()


        scaleDetector = ScaleGestureDetector(requireContext(), object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                matrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
                binding.imageView.imageMatrix = matrix
                return true
            }
        })

        binding.imageView.scaleType = ImageView.ScaleType.MATRIX

        binding.imageView.setOnTouchListener { _, event ->
            scaleDetector.onTouchEvent(event)

            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    savedMatrix.set(matrix)
                    start.set(event.x, event.y)
                    mode = DRAG
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    oldDist = spacing(event)
                    if (oldDist > 10f) {
                        savedMatrix.set(matrix)
                        midPoint(mid, event)
                        mode = ZOOM
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mode == DRAG) {
                        matrix.set(savedMatrix)
                        matrix.postTranslate(event.x - start.x, event.y - start.y)
                    } else if (mode == ZOOM) {
                        val newDist = spacing(event)
                        if (newDist > 10f) {
                            matrix.set(savedMatrix)
                            val scale = newDist / oldDist
                            matrix.postScale(scale, scale, mid.x, mid.y)
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = NONE

                    // 👉 Burada tek tıklama yakalayabiliriz
                    if (event.eventTime - event.downTime < 200) {
                        onSvgTapped(event.x, event.y)
                    }
                }
            }

            binding.imageView.imageMatrix = matrix
            true
        }

    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt(x * x + y * y)
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }
    private fun onSvgTapped(x: Float, y: Float) {
        // Buraya birazdan SVG içi çakışma kontrolü ekleyeceğiz
        Log.d("SVG_TAP", "Tıklandı: x=$x y=$y")
    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentDenemeBinding.inflate(inflater, container, false)

        val pickSvgLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                uploadSvgToFirebase(uri)
            }
        }

        binding.button2.setOnClickListener {
            pickSvgLauncher.launch(arrayOf("image/svg+xml"))
        }




        return binding.root
    }

    fun uploadSvgToFirebase(fileUri: Uri) {
        val fileName = "drawing_${System.currentTimeMillis()}.svg"
        val svgRef = storageRef.child(fileName)

        svgRef.putFile(fileUri)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "SVG yüklendi", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Yükleme başarısız", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showSvgInImageView(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val svg = SVG.getFromInputStream(inputStream)
            val picture = svg.renderToPicture()
            val drawable = PictureDrawable(picture)
            binding.imageView.setImageDrawable(drawable)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "SVG gösterilemedi", Toast.LENGTH_SHORT).show()
        }
    }
    private fun loadLastUploadedSvgFromFirebase() {
        val folderRef = FirebaseStorage.getInstance().reference.child("svgs")

        folderRef.listAll()
            .addOnSuccessListener { result ->
                if (result.items.isEmpty()) return@addOnSuccessListener

                var newestRef: StorageReference? = null
                var newestTime = 0L

                val tasks = result.items.map { ref ->
                    ref.metadata.addOnSuccessListener { meta ->
                        val time = meta.creationTimeMillis
                        if (time > newestTime) {
                            newestTime = time
                            newestRef = ref
                        }
                    }
                }

                Tasks.whenAllSuccess<StorageMetadata>(tasks)
                    .addOnSuccessListener {
                        newestRef?.downloadUrl?.addOnSuccessListener { url ->
                          //  showSvgFromUrl(url.toString())
                        }
                    }
            }
    }

    private fun showSvgFromUrl(url: String) {
        Thread {
            try {
                val inputStream = URL(url).openStream()
                val svg = SVG.getFromInputStream(inputStream)
                val picture = svg.renderToPicture()
                val drawable = PictureDrawable(picture)

                requireActivity().runOnUiThread {
                    binding.imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                    binding.imageView.setImageDrawable(drawable)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }





}