package com.example.mimarket.data.remote.firebase

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await

/**
 * Fuente de datos remota: Firebase Storage. Es la ÚNICA clase que habla con FirebaseStorage.
 * Las imágenes de productos se guardan en  productos/{uid}/{marcaDeTiempo}.jpg
 * Cada usuario solo puede escribir dentro de su propia carpeta (reglas de Storage).
 */
class StorageRepository(
    private val contexto: Context,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    /**
     * Sube la imagen elegida en la galería ([uri]) y devuelve su URL de descarga,
     * que luego se guarda en el campo "imagen" del documento del producto en Firestore.
     */
    suspend fun subirImagenProducto(uid: String, uri: Uri): String {
        val referencia = storage.reference.child("productos/$uid/${System.currentTimeMillis()}.jpg")
        // Tipo de archivo real (image/jpeg, image/png...): las reglas solo aceptan imágenes.
        val tipo = contexto.contentResolver.getType(uri) ?: "image/jpeg"
        val metadatos = StorageMetadata.Builder().setContentType(tipo).build()
        referencia.putFile(uri, metadatos).await()
        return referencia.downloadUrl.await().toString()
    }

    /** Borra una imagen subida a Storage (se ignora si la URL no es de Storage o ya no existe). */
    suspend fun eliminarImagen(url: String) {
        if (!esUrlDeStorage(url)) return
        runCatching { storage.getReferenceFromUrl(url).delete().await() }
    }

    private fun esUrlDeStorage(url: String): Boolean =
        url.startsWith("https://firebasestorage.googleapis.com") || url.startsWith("gs://")
}
