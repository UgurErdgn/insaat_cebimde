package com.sorodeveloper.insaatcebimde

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * InsaatCebimdeApp: Uygulamanın ana giriş noktası.
 * 
 * Neden Yazıldı?: Hilt (Dependency Injection) kütüphanesinin kod üretimi yapabilmesi için
 * uygulamanın bir Application sınıfına sahip olması ve @HiltAndroidApp ile işaretlenmesi gerekir.
 * 
 * Neyi Etkiliyor?: Tüm uygulamanın yaşam döngüsünü ve bağımlılık enjeksiyonu ağacını etkiler.
 * 
 * Nasıl Yazıldı?: Standart Hilt Application sınıfı tanımı kullanıldı.
 * 
 * Amacı Ne Değil?: İş mantığı içermez, sadece Hilt'i tetikler.
 */
@HiltAndroidApp
class InsaatCebimdeApp : Application()
