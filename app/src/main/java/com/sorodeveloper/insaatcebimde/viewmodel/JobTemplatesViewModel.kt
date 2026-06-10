package com.sorodeveloper.insaatcebimde.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorodeveloper.insaatcebimde.domain.repository.JobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * JobTemplatesViewModel: Şablon kütüphanesi ekranının durumunu yönetir.
 * 
 * Neden Yazıldı?: JobTemplatesFragment içindeki ağır Firebase ve veri işleme mantığını 
 * Fragment'tan ayırmak için yazıldı.
 * 
 * Neyi Etkiliyor?: JobTemplatesFragment'ın veri yükleme ve kaydetme süreçlerini kontrol eder.
 * 
 * Nasıl Yazıldı?: @HiltViewModel ve StateFlow kullanılarak modern Android mimarisine uygun yazıldı.
 */
@HiltViewModel
class JobTemplatesViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _templates = MutableStateFlow<List<isKollari.MainItem>>(emptyList())
    val templates: StateFlow<List<isKollari.MainItem>> = _templates.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Bu kısım Fragment'taki fetchTemplates mantığının ViewModel versiyonu olacak.
    // Şimdilik sadece iskeleti kuruyoruz.
}
