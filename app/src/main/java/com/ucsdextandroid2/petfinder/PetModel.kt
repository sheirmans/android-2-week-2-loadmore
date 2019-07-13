package com.ucsdextandroid2.petfinder

/**
 * Created by rjaylward on 2019-07-12
 */

data class PetModel(
    val name: String?,
    val imageUrl: String?,
    val breed: String?,
    val id: Int,
    val location: String?
)