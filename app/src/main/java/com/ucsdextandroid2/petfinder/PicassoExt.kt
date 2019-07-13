package com.ucsdextandroid2.petfinder

import android.widget.ImageView
import com.squareup.picasso.Picasso

/**
 * Created by rjaylward on 2019-07-13
 */

fun ImageView.loadImageUrl(imageUrl: String?) {
    Picasso.get()
        .load(imageUrl)
        .into(this)
}