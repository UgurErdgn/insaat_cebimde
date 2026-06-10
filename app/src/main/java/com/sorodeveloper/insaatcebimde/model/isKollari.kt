class isKollari {
    data class MainItem(
        val title: String, // Örn: "Mobilya"
        val subItems: MutableList<SubItem> = mutableListOf(),
        var ilerleme: String,
        var isExpanded: Boolean = false,
        val kacAdet: String,
        var hasAnimated: Boolean = false // 🔥 yeni alan

    )

    data class SubItem(
        val templateId: String = "", // 🔥 Yeni Barkod (ID) Sistemi Referansı
        val title: String,         // Örn: "Vestiyer"
        var progress: String,     // %0, %25, %50...
        var kacAdet: String,
        val firebasePath: String,  // Güncelleme yapılacak tam konum
        val isEditable: Boolean,   // Yüzde seçici butonu görünsün mü?
        val trade: String,        // Örn: "Delektrik"
        val category: String,     // Örn: "priz"
        val type: String,          // Örn: "a"
        var isTabOpen: Boolean = false,
        var isEditing: Boolean = false, // 🔥 Düzenleme modunda mı?
        val drawingUrls: MutableList<String> = mutableListOf(), // Birleşen çizimler
        val initialDrawingUrls: MutableList<String> = mutableListOf(), // 🔥 Silinenleri tespit etmek için başlangıç hali
        val localDrawingUris: MutableList<android.net.Uri> = mutableListOf(), // 🔥 Yeni seçilen yerel resimler
        val materials: Map<String, String>? = null // Malzemeler: "Priz" -> "5 Adet"
    )



}