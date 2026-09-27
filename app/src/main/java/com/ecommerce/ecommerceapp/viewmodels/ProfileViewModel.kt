package com.ecommerce.ecommerceapp.viewmodels

import android.util.Log
import android.util.Patterns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecommerce.ecommerceapp.models.*
import com.ecommerce.ecommerceapp.network.ApiClient
import com.ecommerce.ecommerceapp.utils.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileViewModel(private val sessionManager: SessionManager) : ViewModel() {

    var userProfile by mutableStateOf<UserProfile?>(null)
    var userName by mutableStateOf("")

    // [ERROR P5] ANTES: esta variable no existía; el email solo se leía de userProfile y no se podía editar.
    // CAMBIO: se guarda el email en su propio estado editable.
    // POR QUÉ: permite modificarlo en el formulario, validarlo y enviarlo al backend.
    var email by mutableStateOf("")

    // [ERROR P2] ANTES: var selectedCityId by mutableStateOf(1)
    // CAMBIO: pasa a Int? y empieza en null (ninguna ciudad elegida).
    // POR QUÉ: con el valor por defecto 1 (o 0 cuando el perfil llegaba vacío) se enviaba una ciudad
    // que el usuario nunca eligió. En la base de datos el usuario 9 quedó con iD_City = 0 (ciudad
    // inexistente). Con null se puede validar "debes elegir una ciudad" antes de guardar.
    var selectedCityId by mutableStateOf<Int?>(null)

    // [ERROR P4] ANTES: solo existía "cities" con TODAS las ciudades, sin país ni estado.
    // CAMBIO: se agregan las listas y selecciones de país y estado para la cascada país -> estado -> ciudad.
    // POR QUÉ: la guía pide que el país y el estado seleccionados (reto 1) se usen en la actualización
    // del perfil. El backend guarda iD_City; el país y el estado sirven para llegar a una ciudad válida.
    var countries by mutableStateOf<List<Country>>(emptyList())
    var states by mutableStateOf<List<State>>(emptyList())
    var cities by mutableStateOf<List<City>>(emptyList())
    var selectedCountry by mutableStateOf<Country?>(null)
    var selectedState by mutableStateOf<State?>(null)

    // Estados de carga
    var isLoadingProfile by mutableStateOf(false)
    var isUpdatingProfile by mutableStateOf(false)
    var isChangingPassword by mutableStateOf(false)
    var isLoadingCities by mutableStateOf(false)
    // [ERROR P4] ANTES: no existían. CAMBIO: indicadores de carga de países y estados.
    // POR QUÉ: la pantalla muestra "Cargando..." en cada desplegable mientras llega la respuesta.
    var isLoadingCountries by mutableStateOf(false)
    var isLoadingStates by mutableStateOf(false)

    // Estados de error y éxito
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    // Cambio de contraseña
    var currentPassword by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    // [ERROR P8] ANTES: no existía.
    // CAMBIO: bandera que se pone en true cuando el backend confirma el cambio de contraseña.
    // POR QUÉ: la pantalla la observa para cerrar el diálogo en el momento correcto (ver ProfileScreen).
    var passwordChanged by mutableStateOf(false)

    // [ERROR P4] ANTES:
    //     init {
    //         loadProfile()
    //         loadCities()
    //     }
    // CAMBIO: primero se cargan los países y después el perfil, en la misma corrutina (en orden).
    // POR QUÉ: para preseleccionar el país del usuario hay que buscarlo en la lista de países, así que
    // esa lista debe existir antes de procesar el perfil. Las ciudades ya no se cargan todas de una vez:
    // se cargan solo las del estado elegido.
    init {
        viewModelScope.launch {
            countries = fetchCountries()
            loadProfileInternal()
        }
    }

    fun loadProfile() {
        viewModelScope.launch { loadProfileInternal() }
    }

    private suspend fun loadProfileInternal() {
        isLoadingProfile = true
        try {
            val token = sessionManager.token.first()
            if (token != null) {
                val response = ApiClient.apiService.getProfile("Bearer $token")

                // [ERROR P1] ANTES:
                //     if (response.isSuccessful && response.body() != null) {
                //         userProfile = response.body()
                // CAMBIO: se valida y se toma response.body()?.user
                // POR QUÉ: los datos del usuario vienen dentro de la clave "user". Antes se asignaba el
                // envoltorio completo y UserName, Email y roles quedaban vacíos.
                val profile = response.body()?.user
                if (response.isSuccessful && profile != null) {
                    userProfile = profile
                    userName = profile.UserName
                    email = profile.Email
                    // [ERROR P2] ANTES: selectedCityId = userProfile?.iD_City ?: 1
                    // CAMBIO: se toma la ciudad real del usuario (o null si no tiene) y se preselecciona
                    // país, estado y ciudad.
                    // POR QUÉ: "?: 1" inventaba la ciudad 1 si faltaba el dato.
                    preseleccionarUbicacion(profile.city)
                    Log.d("PROFILE_DEBUG", "Perfil cargado: ${profile.UserName} / ciudad=${profile.iD_City}")
                } else {
                    // [ERROR P7] ANTES: errorMessage = "Error al cargar el perfil"
                    // CAMBIO / POR QUÉ: mensaje según el código HTTP (401 = sesión vencida).
                    errorMessage = mensajeDeError(response.code(), "cargar el perfil")
                }
            } else {
                // [ERROR P7] ANTES: este else no existía; sin token la pantalla quedaba vacía sin explicación.
                errorMessage = "No hay sesión activa. Inicia sesión de nuevo"
            }
        } catch (e: Exception) {
            errorMessage = "Error de conexión: ${e.message}"
            Log.e("PROFILE_DEBUG", "Error loading profile", e)
        } finally {
            isLoadingProfile = false
        }
    }

    // [ERROR P4] ANTES: esta función no existía.
    // CAMBIO: a partir de la ciudad guardada del usuario se preseleccionan su país, estado y ciudad.
    // POR QUÉ: el JSON de la ciudad trae {"iD_City", "CityName", "iD_States", "state", "country"}, donde
    // "country" es el NOMBRE del país. Se busca ese nombre en la lista de países para obtener su id,
    // luego se cargan los estados de ese país y las ciudades de ese estado.
    private suspend fun preseleccionarUbicacion(city: City?) {
        if (city == null) return
        val country = countries.find { it.CountryName == city.country } ?: return
        selectedCountry = country
        states = fetchStates(country.iD_Country)
        selectedState = states.find { it.iD_States == city.iD_States } ?: return
        cities = fetchCities(city.iD_States)
        selectedCityId = city.iD_City
    }

    fun updateProfile() {
        // [ERROR P6] ANTES: solo se validaba if (userName.isBlank()).
        // CAMBIO: se valida nombre (obligatorio y mínimo 3 caracteres), email (obligatorio y con formato
        // válido) y ciudad (obligatoria).
        // POR QUÉ: la guía indica que una app que guarda datos vacíos o inválidos no está terminada.
        // Antes se podía guardar sin ciudad (se enviaba 0) o con un email inválido.
        val nombre = userName.trim()
        val correo = email.trim()
        if (nombre.isEmpty()) {
            errorMessage = "El nombre de usuario es requerido"
            return
        }
        if (nombre.length < 3) {
            errorMessage = "El nombre de usuario debe tener al menos 3 caracteres"
            return
        }
        if (correo.isEmpty()) {
            errorMessage = "El correo es requerido"
            return
        }
        // [ERROR P5] Validación de formato con el patrón de email incluido en Android.
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            errorMessage = "El formato del correo no es válido (ejemplo: nombre@dominio.com)"
            return
        }
        val cityId = selectedCityId
        if (cityId == null) {
            errorMessage = "Selecciona país, estado y ciudad"
            return
        }

        isUpdatingProfile = true
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                if (token != null) {
                    // [ERROR P5] ANTES: UpdateProfileRequest(UserName = userName, iD_City = selectedCityId)
                    // CAMBIO: se envían el nombre y el email sin espacios sobrantes, y la ciudad validada.
                    // POR QUÉ: el email ahora se puede editar y debe llegar al backend.
                    val updateRequest = UpdateProfileRequest(
                        UserName = nombre,
                        Email = correo,
                        iD_City = cityId
                    )

                    val response = ApiClient.apiService.updateProfile("Bearer $token", updateRequest)

                    // [ERROR P1] ANTES: userProfile = response.body()
                    // CAMBIO: se toma response.body()?.user
                    // POR QUÉ: el PUT responde {"message": "...", "user": {...}}.
                    val updated = response.body()?.user
                    if (response.isSuccessful && updated != null) {
                        userProfile = updated

                        // [ERROR P3] ANTES:
                        //     isAdmin = userProfile?.roles?.any { it.TypeRole == "Administrador" } ?: false,
                        //     roles = userProfile?.roles?.map { it.TypeRole } ?: emptyList()
                        // CAMBIO: si la respuesta no trae roles, se conserva lo que ya había en la sesión.
                        // POR QUÉ: como el perfil llegaba vacío (P1), "roles" era null y "?: false" guardaba
                        // isAdmin = false. El administrador que editaba su perfil perdía el menú de
                        // administración hasta volver a iniciar sesión. Con P1 corregido los roles llegan;
                        // este respaldo evita que se repita si algún día no vienen.
                        val roles = updated.roles
                        sessionManager.saveUserSession(
                            token = token,
                            userName = updated.UserName,
                            userEmail = updated.Email,
                            isAdmin = roles?.any { it.TypeRole == "Administrador" } ?: sessionManager.isAdmin.first(),
                            roles = roles?.map { it.TypeRole } ?: sessionManager.userRoles.first()
                        )

                        successMessage = "Perfil actualizado exitosamente"
                        Log.d("PROFILE_DEBUG", "Perfil guardado: ${updated.UserName} / ${updated.Email} / ciudad=${updated.iD_City}")
                    } else {
                        // [ERROR P7] ANTES: errorMessage = "Error al actualizar el perfil"
                        // CAMBIO: mensaje según el código HTTP; 409 = el email ya lo usa otro usuario.
                        // POR QUÉ: el backend responde 409 si el email está en uso; antes el usuario no sabía por qué falló.
                        errorMessage = if (response.code() == 409) {
                            "Ese correo ya está en uso por otro usuario"
                        } else {
                            mensajeDeError(response.code(), "actualizar el perfil")
                        }
                    }
                } else {
                    // [ERROR P7] ANTES: este else no existía.
                    errorMessage = "No hay sesión activa. Inicia sesión de nuevo"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("PROFILE_DEBUG", "Error updating profile", e)
            } finally {
                isUpdatingProfile = false
            }
        }
    }

    fun changePassword() {
        // Validaciones
        if (currentPassword.isBlank()) {
            errorMessage = "La contraseña actual es requerida"
            return
        }

        if (newPassword.isBlank()) {
            errorMessage = "La nueva contraseña es requerida"
            return
        }

        if (newPassword.length < 6) {
            errorMessage = "La nueva contraseña debe tener al menos 6 caracteres"
            return
        }

        if (newPassword != confirmPassword) {
            errorMessage = "Las contraseñas no coinciden"
            return
        }

        isChangingPassword = true
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                if (token != null) {
                    val passwordRequest = ChangePasswordRequest(
                        current_password = currentPassword,
                        new_password = newPassword
                    )

                    val response = ApiClient.apiService.changePassword("Bearer $token", passwordRequest)

                    if (response.isSuccessful) {
                        successMessage = "Contraseña cambiada exitosamente"
                        // Limpiar campos
                        currentPassword = ""
                        newPassword = ""
                        confirmPassword = ""
                        // [ERROR P8] ANTES: esta línea no existía.
                        // CAMBIO: se avisa a la pantalla que el cambio terminó bien.
                        // POR QUÉ: la pantalla cierra el diálogo al ver passwordChanged = true.
                        passwordChanged = true
                    } else {
                        // [ERROR P8] ANTES:
                        //     401 -> "Contraseña actual incorrecta"
                        //     else -> "Error al cambiar la contraseña"
                        // CAMBIO: 400 = contraseña actual incorrecta; 401 = sesión vencida.
                        // POR QUÉ: el backend (auth_routes.py, change_password) responde 400
                        // "Password actual incorrecto" y reserva 401 para token inválido o expirado.
                        // Antes una contraseña incorrecta mostraba "Error al cambiar la contraseña".
                        errorMessage = when (response.code()) {
                            400 -> "La contraseña actual es incorrecta"
                            401 -> "Tu sesión expiró o no es válida. Cierra sesión e inicia de nuevo"
                            else -> "Error al cambiar la contraseña (HTTP ${response.code()})"
                        }
                    }
                } else {
                    // [ERROR P7] ANTES: este else no existía.
                    errorMessage = "No hay sesión activa. Inicia sesión de nuevo"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("PROFILE_DEBUG", "Error changing password", e)
            } finally {
                isChangingPassword = false
            }
        }
    }

    // [ERROR P4] ANTES: existía "private fun loadCities()" que pedía TODAS las ciudades y cuyo catch
    // solo escribía en Logcat (el desplegable quedaba vacío sin avisar).
    // CAMBIO: se reemplaza por tres funciones (países, estados de un país, ciudades de un estado) que
    // devuelven la lista y, si fallan, muestran el error en pantalla.
    // POR QUÉ: la cascada necesita pedir cada nivel filtrado por el anterior, y los errores deben verse.
    private suspend fun fetchCountries(): List<Country> {
        isLoadingCountries = true
        return try {
            val response = ApiClient.apiService.getCountries()
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.countries
            } else {
                errorMessage = "Error al cargar países (HTTP ${response.code()})"
                emptyList()
            }
        } catch (e: Exception) {
            errorMessage = "No se pudieron cargar los países: ${e.message}"
            Log.e("PROFILE_DEBUG", "Error loading countries", e)
            emptyList()
        } finally {
            isLoadingCountries = false
        }
    }

    private suspend fun fetchStates(countryId: Int): List<State> {
        isLoadingStates = true
        return try {
            val response = ApiClient.apiService.getStates(countryId)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.states
            } else {
                errorMessage = "Error al cargar estados (HTTP ${response.code()})"
                emptyList()
            }
        } catch (e: Exception) {
            errorMessage = "No se pudieron cargar los estados: ${e.message}"
            Log.e("PROFILE_DEBUG", "Error loading states", e)
            emptyList()
        } finally {
            isLoadingStates = false
        }
    }

    private suspend fun fetchCities(stateId: Int): List<City> {
        isLoadingCities = true
        return try {
            val response = ApiClient.apiService.getCities(stateId)
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.cities
            } else {
                errorMessage = "Error al cargar ciudades (HTTP ${response.code()})"
                emptyList()
            }
        } catch (e: Exception) {
            errorMessage = "No se pudieron cargar las ciudades: ${e.message}"
            Log.e("PROFILE_DEBUG", "Error loading cities", e)
            emptyList()
        } finally {
            isLoadingCities = false
        }
    }

    // [ERROR P4] ANTES: no existía. CAMBIO: permite reintentar si la lista de países no cargó.
    fun reloadCountries() {
        viewModelScope.launch { countries = fetchCountries() }
    }

    fun updateUserName(newName: String) {
        userName = newName
        clearMessages()
    }

    // [ERROR P5] ANTES: no existía (el campo email era de solo lectura).
    // CAMBIO: actualiza el email mientras el usuario escribe.
    fun updateEmail(newEmail: String) {
        email = newEmail
        clearMessages()
    }

    // [ERROR P4] ANTES: no existía.
    // CAMBIO: al elegir un país se limpian estado y ciudad y se cargan los estados de ese país.
    // POR QUÉ: igual que en el reto 1, si no se limpia la selección quedaría un estado o una ciudad
    // de otro país, y se enviaría al backend una ubicación inconsistente.
    fun updateSelectedCountry(country: Country) {
        selectedCountry = country
        selectedState = null
        selectedCityId = null
        states = emptyList()
        cities = emptyList()
        clearMessages()
        viewModelScope.launch { states = fetchStates(country.iD_Country) }
    }

    // [ERROR P4] ANTES: no existía.
    // CAMBIO: al elegir un estado se limpia la ciudad y se cargan las ciudades de ese estado.
    // POR QUÉ: la ciudad elegida antes podía pertenecer a otro estado.
    fun updateSelectedState(state: State) {
        selectedState = state
        selectedCityId = null
        cities = emptyList()
        clearMessages()
        viewModelScope.launch { cities = fetchCities(state.iD_States) }
    }

    fun updateSelectedCity(cityId: Int) {
        selectedCityId = cityId
        clearMessages()
    }

    fun updateCurrentPassword(password: String) {
        currentPassword = password
        clearMessages()
    }

    fun updateNewPassword(password: String) {
        newPassword = password
        clearMessages()
    }

    fun updateConfirmPassword(password: String) {
        confirmPassword = password
        clearMessages()
    }

    // [ERROR P7] ANTES: esta función no existía.
    // CAMBIO: traduce el código HTTP a un mensaje entendible.
    // POR QUÉ: el backend responde 401 cuando el token falta, es inválido o venció (dura 24 horas).
    private fun mensajeDeError(code: Int, accion: String): String = when (code) {
        401 -> "Tu sesión expiró o no es válida. Cierra sesión e inicia de nuevo"
        403 -> "No tienes permisos para $accion"
        else -> "Error al $accion (HTTP $code)"
    }

    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }
}
