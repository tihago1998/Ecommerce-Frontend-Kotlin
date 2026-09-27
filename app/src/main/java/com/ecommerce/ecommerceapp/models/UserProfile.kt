package com.ecommerce.ecommerceapp.models

import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName("iD_User")
    val iD_User: Int,

    @SerializedName("UserName")
    val UserName: String,

    @SerializedName("Email")
    val Email: String,

    // [ERROR P2] ANTES: val iD_City: Int,
    // CAMBIO: pasa a Int? (puede ser null) con valor por defecto null.
    // POR QUÉ: la columna iD_City de la tabla users permite NULL (usuario sin ciudad). Con Int no nulo,
    // un null del JSON quedaba como 0, y 0 no es una ciudad válida.
    @SerializedName("iD_City")
    val iD_City: Int? = null,

    @SerializedName("city")
    val city: City? = null,

    @SerializedName("roles")
    val roles: List<Role>? = null
)

// [ERROR P1] ANTES: esta clase no existía.
// CAMBIO: se crea una clase con la misma forma que la respuesta del backend.
// POR QUÉ: GET y PUT /api/users/profile NO devuelven el usuario directo; lo envuelven:
//   GET -> {"user": {...}}
//   PUT -> {"message": "Perfil actualizado exitosamente", "user": {...}}
// Como la app esperaba UserProfile directo, Gson no encontraba "UserName", "Email", etc. en el primer
// nivel y los dejaba vacíos: el perfil se veía en blanco aunque el servidor respondía 200 con los datos.
data class UserResponse(
    @SerializedName("message")
    val message: String? = null,

    @SerializedName("user")
    val user: UserProfile? = null
)

data class UpdateProfileRequest(
    @SerializedName("UserName")
    val UserName: String,

    // [ERROR P5] ANTES: este campo no existía; el email nunca se enviaba al backend.
    // CAMBIO: se agrega Email a la petición.
    // POR QUÉ: el backend sí permite actualizarlo (user_routes.py, update_profile) y valida que no
    // esté en uso por otro usuario (responde 409). La guía pide poder editar los datos del perfil.
    @SerializedName("Email")
    val Email: String,

    @SerializedName("iD_City")
    val iD_City: Int
)

data class ChangePasswordRequest(
    @SerializedName("current_password")
    val current_password: String,

    @SerializedName("new_password")
    val new_password: String
)