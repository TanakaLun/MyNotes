package io.github.tanakalun.mynotes

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import io.github.tanakalun.mynotes.data.NoteRepository
import io.github.tanakalun.mynotes.utils.PowerSaveModeTracker

class MyNotesApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        NoteRepository.init(this)
        SettingsStore.init(this)
        PowerSaveModeTracker.init(this)
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .build()
    }
}
