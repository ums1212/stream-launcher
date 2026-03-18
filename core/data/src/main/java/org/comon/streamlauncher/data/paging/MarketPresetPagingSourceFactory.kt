package org.comon.streamlauncher.data.paging

import androidx.paging.PagingSource
import com.google.firebase.firestore.DocumentSnapshot
import org.comon.streamlauncher.domain.model.preset.MarketPreset

interface MarketPresetPagingSourceFactory {
    fun create(): PagingSource<DocumentSnapshot, MarketPreset>
}
