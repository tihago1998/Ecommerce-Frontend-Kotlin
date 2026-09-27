package com.ecommerce.ecommerceapp.models

import com.google.gson.annotations.SerializedName

data class Country(
    @SerializedName("iD_Country")
    val iD_Country: Int,

    @SerializedName("CountryName")
    val CountryName: String
)

data class State(
    @SerializedName("iD_States")
    val iD_States: Int,

    @SerializedName("StatesName")
    val StatesName: String,

    @SerializedName("iD_Country")
    val iD_Country: Int,

    // [ERROR 3] ANTES: val country: Country? = null
    // CAMBIO: el tipo pasa de objeto Country? a String? (texto).
    // POR QUÉ: el backend arma cada estado con 'country': self.country.CountryName (models.py), es decir,
    // envía solo el NOMBRE del país: "country": "Colombia". Gson esperaba un objeto "{" y encontraba
    // texto "\"", lanzaba "Expected BEGIN_OBJECT but was STRING" y la lista de estados fallaba.
    // Este error estaba escondido detrás del ERROR 1: solo aparece cuando la lista empieza a leerse.
    @SerializedName("country")
    val country: String? = null
)

data class CreateLocationRequest(
    @SerializedName("CountryName")
    val CountryName: String? = null,

    @SerializedName("StatesName")
    val StatesName: String? = null,

    @SerializedName("CityName")
    val CityName: String? = null,

    @SerializedName("iD_Country")
    val iD_Country: Int? = null,

    @SerializedName("iD_States")
    val iD_States: Int? = null
)




// [ERROR 1] ANTES: estas tres clases (CountriesResponse, StatesResponse, CitiesResponse) no existían.
// CAMBIO: se crean clases con la MISMA FORMA que el JSON que envía el backend.
// POR QUÉ: CAUSA RAÍZ del Reto 1. El backend (location_routes.py) no devuelve la lista sola, la envuelve
// en un objeto:
//     GET /api/locations/countries -> {"count": 5, "countries": [ {...}, {...} ]}
//     GET /api/locations/states    -> {"count": n, "states":    [ ... ]}
//     GET /api/locations/cities    -> {"count": n, "cities":    [ ... ]}
// La app esperaba una lista directa [ ... ]. Gson leía "{" donde esperaba "[" y lanzaba
// "Expected BEGIN_ARRAY but was BEGIN_OBJECT"; por eso los desplegables quedaban vacíos aunque el
// servidor respondía 200. Estas clases le indican a Gson dónde está la lista dentro del objeto.
data class CountriesResponse(
    @SerializedName("countries") val countries: List<Country> = emptyList(),
    @SerializedName("count") val count: Int = 0
)

data class StatesResponse(
    @SerializedName("states") val states: List<State> = emptyList(),
    @SerializedName("count") val count: Int = 0
)

data class CitiesResponse(
    @SerializedName("cities") val cities: List<City> = emptyList(),
    @SerializedName("count") val count: Int = 0
)
