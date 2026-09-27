package com.ecommerce.ecommerceapp.viewmodels

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecommerce.ecommerceapp.models.*
import com.ecommerce.ecommerceapp.network.ApiClient
import kotlinx.coroutines.launch

class LocationManagementViewModel : ViewModel() {

    // Listas de datos
    var countries by mutableStateOf<List<Country>>(emptyList())
    var states by mutableStateOf<List<State>>(emptyList())
    var cities by mutableStateOf<List<City>>(emptyList())

    // Estados de carga
    var isLoadingCountries by mutableStateOf(false)
    var isLoadingStates by mutableStateOf(false)
    var isLoadingCities by mutableStateOf(false)
    var isCreating by mutableStateOf(false)

    // Formularios
    var newCountryName by mutableStateOf("")
    var newStateName by mutableStateOf("")
    var selectedCountryForState by mutableStateOf<Country?>(null)
    var newCityName by mutableStateOf("")
    var selectedCountryForCity by mutableStateOf<Country?>(null)
    // [ERROR 5] ANTES: esta línea existía en el código original, pero al agregar
    // selectedCountryForCity se reemplazó por error en lugar de dejar ambas.
    // CAMBIO: se restaura "var selectedStateForCity by mutableStateOf<State?>(null)".
    // POR QUÉ: la cascada de la pestaña Ciudades tiene dos niveles y cada uno necesita su variable:
    //   1) selectedCountryForCity -> país elegido (filtra qué estados se cargan).
    //   2) selectedStateForCity   -> estado elegido (filtra las ciudades y se envía al crear una ciudad).
    // Sin esta variable, createCity(), updateSelectedCountryForCity(), updateSelectedStateForCity()
    // y la pantalla fallaban con "Unresolved reference 'selectedStateForCity'" (8 errores de compilación).
    var selectedStateForCity by mutableStateOf<State?>(null)


    // Estados de error y éxito
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    init {
        loadCountries()
    }

    fun loadCountries() {
        isLoadingCountries = true
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getCountries()
                if (response.isSuccessful && response.body() != null) {
                    // [ERROR 2] ANTES: countries = response.body()!!
                    // CAMBIO: se toma la lista que viene DENTRO del objeto: response.body()!!.countries
                    // POR QUÉ: getCountries() ahora devuelve CountriesResponse ({"countries": [...]}).
                    // Antes, al fallar la lectura del JSON (ERROR 1), la excepción saltaba al catch, esta
                    // línea nunca se ejecutaba y "countries" se quedaba en emptyList(): la pantalla
                    // dibujaba fielmente una lista vacía. El estado de Compose funcionaba; nunca recibía datos.
                    countries = response.body()!!.countries
                } else {
                    errorMessage = "Error al cargar países"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("LOCATION_DEBUG", "Error loading countries", e)
            } finally {
                isLoadingCountries = false
            }
        }
    }

    fun loadStates(countryId: Int? = null) {
        isLoadingStates = true
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getStates(countryId)
                if (response.isSuccessful && response.body() != null) {
                    // [ERROR 2] ANTES: states = response.body()!!
                    // CAMBIO: se toma la lista dentro del objeto: response.body()!!.states
                    // POR QUÉ: getStates() ahora devuelve StatesResponse ({"states": [...]}).
                    states = response.body()!!.states
                } else {
                    errorMessage = "Error al cargar estados"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("LOCATION_DEBUG", "Error loading states", e)
            } finally {
                isLoadingStates = false
            }
        }
    }

    fun loadCities(stateId: Int? = null) {
        isLoadingCities = true
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getCities(stateId)
                if (response.isSuccessful && response.body() != null) {
                    // [ERROR 2] ANTES: cities = response.body()!!
                    // CAMBIO: se toma la lista dentro del objeto: response.body()!!.cities
                    // POR QUÉ: getCities() ahora devuelve CitiesResponse ({"cities": [...]}).
                    cities = response.body()!!.cities
                } else {
                    errorMessage = "Error al cargar ciudades"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("LOCATION_DEBUG", "Error loading cities", e)
            } finally {
                isLoadingCities = false
            }
        }
    }

    fun createCountry() {
        if (newCountryName.isBlank()) {
            errorMessage = "El nombre del país es requerido"
            return
        }

        isCreating = true
        viewModelScope.launch {
            try {
                val request = CreateLocationRequest(CountryName = newCountryName)
                val response = ApiClient.apiService.createCountry(request)

                if (response.isSuccessful) {
                    successMessage = "País creado exitosamente"
                    newCountryName = ""
                    loadCountries() // Recargar lista
                } else {
                    errorMessage = "Error al crear el país"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("LOCATION_DEBUG", "Error creating country", e)
            } finally {
                isCreating = false
            }
        }
    }

    fun createState() {
        if (newStateName.isBlank()) {
            errorMessage = "El nombre del estado es requerido"
            return
        }

        if (selectedCountryForState == null) {
            errorMessage = "Debe seleccionar un país"
            return
        }

        isCreating = true
        viewModelScope.launch {
            try {
                val request = CreateLocationRequest(
                    StatesName = newStateName,
                    iD_Country = selectedCountryForState!!.iD_Country
                )
                val response = ApiClient.apiService.createState(request)

                if (response.isSuccessful) {
                    successMessage = "Estado creado exitosamente"
                    newStateName = ""
                    // [ERROR 5] ANTES:
                    //     selectedCountryForState = null
                    //     loadStates() // Recargar lista
                    // CAMBIO: ya no se borra el país seleccionado y se recargan solo los estados de ese país.
                    // POR QUÉ: antes, al crear un estado se perdía el país elegido y se recargaban los
                    // estados de TODOS los países, rompiendo el filtro. Para crear varios estados del
                    // mismo país había que volver a elegirlo cada vez.
                    loadStates(selectedCountryForState?.iD_Country)
                } else {
                    errorMessage = "Error al crear el estado"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("LOCATION_DEBUG", "Error creating state", e)
            } finally {
                isCreating = false
            }
        }
    }

    fun createCity() {
        if (newCityName.isBlank()) {
            errorMessage = "El nombre de la ciudad es requerido"
            return
        }

        if (selectedStateForCity == null) {
            errorMessage = "Debe seleccionar un estado"
            return
        }

        isCreating = true
        viewModelScope.launch {
            try {
                val request = CreateLocationRequest(
                    CityName = newCityName,
                    iD_States = selectedStateForCity!!.iD_States
                )
                val response = ApiClient.apiService.createCity(request)

                if (response.isSuccessful) {
                    successMessage = "Ciudad creada exitosamente"
                    newCityName = ""
                    // [ERROR 5] ANTES:
                    //     selectedStateForCity = null
                    //     loadCities() // Recargar lista
                    // CAMBIO: se conserva el estado seleccionado y se recargan solo sus ciudades.
                    // POR QUÉ: antes se borraba el estado (quedando el país elegido sin estado, una
                    // selección inconsistente) y se listaban las ciudades de todos los estados.
                    loadCities(selectedStateForCity?.iD_States)
                } else {
                    errorMessage = "Error al crear la ciudad"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("LOCATION_DEBUG", "Error creating city", e)
            } finally {
                isCreating = false
            }
        }
    }

    // Funciones de actualización
    fun updateNewCountryName(name: String) {
        newCountryName = name
        clearMessages()
    }

    fun updateNewStateName(name: String) {
        newStateName = name
        clearMessages()
    }

    fun updateSelectedCountryForState(country: Country?) {
        selectedCountryForState = country
        clearMessages()
        // [ERROR 4] ANTES: esta línea no existía; la función solo guardaba el país y limpiaba mensajes.
        // CAMBIO: al seleccionar un país se piden sus estados con loadStates(idDelPaís).
        // POR QUÉ: elegir un país no cargaba nada, la lista de estados no se filtraba y seguía mostrando
        // los de todos los países. Ahora se envía GET /api/locations/states?country_id=N.
        loadStates(country?.iD_Country)   // pide solo los estados de ese país
    }

    // [ERROR 5] ANTES: esta función no existía.
    // CAMBIO: nueva función para el selector de país de la pestaña Ciudades. Guarda el país, LIMPIA el
    // estado elegido antes y la lista de estados, y carga los estados del nuevo país.
    // POR QUÉ: la guía pide que al seleccionar un país se carguen sus estados y se limpie la selección
    // anterior. Sin limpiar, si el usuario elegía un estado y luego cambiaba de país, seguía seleccionado
    // un estado de OTRO país y podía crear una ciudad con una ubicación inconsistente.
    fun updateSelectedCountryForCity(country: Country?) {
        selectedCountryForCity = country
        selectedStateForCity = null   // el estado anterior podía ser de otro país
        states = emptyList()
        clearMessages()
        if (country != null) loadStates(country.iD_Country)
    }



    fun updateNewCityName(name: String) {
        newCityName = name
        clearMessages()
    }

    fun updateSelectedStateForCity(state: State?) {
        selectedStateForCity = state
        clearMessages()
        // [ERROR 5] ANTES: esta línea no existía; elegir un estado no cargaba sus ciudades.
        // CAMBIO: al seleccionar un estado se piden solo sus ciudades con loadCities(idDelEstado).
        // POR QUÉ: completa la cascada país -> estado -> ciudad; se envía GET /api/locations/cities?state_id=N.
        loadCities(state?.iD_States)
    }

    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }
}