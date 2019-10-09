package io.github.cnaos.example.bledevicescan.ui.main

import androidx.databinding.BindingAdapter
import com.lelloman.identicon.view.ClassicIdenticonView


@BindingAdapter("app:hash")
fun ClassicIdenticonView.setHash(hash: String) {
    this.hash = hash.hashCode()
}
