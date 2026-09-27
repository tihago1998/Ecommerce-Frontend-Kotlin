package com.ecommerce.ecommerceapp.models

import com.google.gson.annotations.SerializedName

data class UsersResponse(
    @SerializedName("users")
    val users: List<UserProfile> = emptyList(),

    @SerializedName("pagination")
    val pagination: Pagination? = null,

    @SerializedName("total")
    val total: Int = 0
)

data class Pagination(
    @SerializedName("page")
    val page: Int,

    @SerializedName("per_page")
    val per_page: Int,

    // [ERROR U3] ANTES: @SerializedName("total_pages")
    // CAMBIO: la anotación ahora lee la clave "pages". El nombre de la variable (total_pages) se mantiene.
    // POR QUÉ: el backend envía {"pagination": {"page":1, "pages":1, "per_page":10, "total":10}}.
    // Gson no encontraba "total_pages", dejaba 0 y la condición "totalPages > 1" nunca se cumplía,
    // así que los botones Anterior/Siguiente no aparecían aunque hubiera más de 10 usuarios.
    // @SerializedName indica qué clave del JSON se lee; el nombre en Kotlin puede ser distinto.
    @SerializedName("pages")
    val total_pages: Int,

    // [ERROR C7] ANTES: este campo no existía.
    // CAMBIO: se lee "total" (cantidad total de registros) que el backend envía dentro de "pagination".
    // POR QUÉ: el historial usaba SalesResponse.total, que no existe en el JSON, y mostraba 0 pedidos.
    // Esta clase la comparten Gestión de Usuarios y el Historial de pedidos.
    @SerializedName("total")
    val total: Int = 0
)

data class UpdateUserRoleRequest(
    @SerializedName("role_ids")
    val role_ids: List<Int>
)

// [ERROR U1] ANTES: esta clase no existía.
// CAMBIO: se crea una clase con la misma forma que el JSON de GET /api/auth/roles.
// POR QUÉ: el backend responde un OBJETO que contiene la lista: {"roles": [{"iDRole":1,"TypeRole":"Administrador"}, ...]}
// y no una lista directa [...]. Esta clase le dice a Gson dónde está la lista.
data class RolesResponse(
    @SerializedName("roles")
    val roles: List<Role> = emptyList()
)