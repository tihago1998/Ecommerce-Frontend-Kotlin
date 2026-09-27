package com.ecommerce.ecommerceapp.viewmodels

import android.util.Log
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

class UserManagementViewModel(private val sessionManager: SessionManager) : ViewModel() {

    var users by mutableStateOf<List<UserProfile>>(emptyList())
    var currentPage by mutableStateOf(1)
    var totalPages by mutableStateOf(1)
    var searchQuery by mutableStateOf("")
    var selectedUser by mutableStateOf<UserProfile?>(null)
    var availableRoles by mutableStateOf<List<Role>>(emptyList())

    // [ERROR U6] ANTES: esta variable no existía.
    // CAMBIO: se guarda el email del usuario que tiene la sesión abierta.
    // POR QUÉ: la pantalla lo compara con el email de cada tarjeta para impedir que el
    // administrador se cambie su propio rol (y se quede sin acceso de administrador).
    var currentUserEmail by mutableStateOf<String?>(null)

    // Estados de carga
    var isLoadingUsers by mutableStateOf(false)
    var isLoadingRoles by mutableStateOf(false)
    var isUpdatingRole by mutableStateOf(false)
    var isDeletingUser by mutableStateOf(false)

    // Estados de error y éxito
    var errorMessage by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)

    init {
        // [ERROR U6] ANTES: esta línea no existía.
        // CAMBIO: al crear el ViewModel se lee el email guardado en la sesión (DataStore).
        // POR QUÉ: se necesita para identificar la tarjeta del propio administrador.
        viewModelScope.launch { currentUserEmail = sessionManager.userEmail.first() }
        loadUsers()
        loadRoles()
    }

    fun loadUsers(page: Int = 1, search: String = "") {
        isLoadingUsers = true
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                if (token != null) {
                    val response = ApiClient.apiService.getUsers(
                        token = "Bearer $token",
                        page = page,
                        perPage = 10,
                        search = search.ifBlank { null }
                    )

                    if (response.isSuccessful && response.body() != null) {
                        val usersResponse = response.body()!!
                        users = usersResponse.users
                        currentPage = page
                        totalPages = usersResponse.pagination?.total_pages ?: 1
                    } else {
                        // [ERROR U4] ANTES: errorMessage = "Error al cargar usuarios"
                        // CAMBIO: el mensaje depende del código HTTP (401 sesión, 403 permisos, etc.).
                        // POR QUÉ: con un mensaje genérico el usuario no sabía si debía volver a iniciar
                        // sesión (token vencido) o si no tenía permisos.
                        errorMessage = mensajeDeError(response.code(), "cargar usuarios")
                    }
                } else {
                    // [ERROR U4] ANTES: este else no existía; sin token no pasaba nada y la lista quedaba vacía.
                    // CAMBIO: se informa que no hay sesión activa.
                    // POR QUÉ: la guía pide verificar qué ocurre cuando falta o vence el token.
                    errorMessage = "No hay sesión activa. Inicia sesión de nuevo"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("USER_MGMT_DEBUG", "Error loading users", e)
            } finally {
                isLoadingUsers = false
            }
        }
    }

    private fun loadRoles() {
        isLoadingRoles = true
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getRoles()
                if (response.isSuccessful && response.body() != null) {
                    // [ERROR U1] ANTES: availableRoles = response.body()!!
                    // CAMBIO: se toma la lista que viene dentro del objeto: response.body()!!.roles
                    // POR QUÉ: getRoles() ahora devuelve RolesResponse ({"roles": [...]}), no la lista directa.
                    availableRoles = response.body()!!.roles
                    Log.d("USER_MGMT_DEBUG", "Roles cargados: ${availableRoles.size}")
                } else {
                    // [ERROR U2] ANTES: este else no existía; si el servidor respondía con error no se avisaba nada.
                    // CAMBIO: se muestra un mensaje con el código HTTP.
                    // POR QUÉ: sin él, el desplegable quedaba vacío sin ninguna explicación.
                    errorMessage = "Error al cargar roles (HTTP ${response.code()})"
                }
            } catch (e: Exception) {
                // [ERROR U2] ANTES: solo existía la línea Log.e(...); el error no llegaba a la pantalla.
                // CAMBIO: además del log, se asigna errorMessage para que se vea en el snackbar.
                // POR QUÉ: es el "catch vacío que esconde la causa real" que menciona la guía: la excepción
                // de Gson (BEGIN_ARRAY vs BEGIN_OBJECT) solo se veía en Logcat y el usuario veía un desplegable vacío.
                errorMessage = "No se pudieron cargar los roles: ${e.message}"
                Log.e("USER_MGMT_DEBUG", "Error loading roles", e)
            } finally {
                isLoadingRoles = false
            }
        }
    }

    fun searchUsers(query: String) {
        searchQuery = query
        loadUsers(page = 1, search = query)
    }

    fun loadPage(page: Int) {
        loadUsers(page = page, search = searchQuery)
    }

    fun selectUser(user: UserProfile) {
        selectedUser = user
    }

    fun updateUserRole(userId: Int, roleId: Int) {
        isUpdatingRole = true
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()

                // [ERROR U5] ANTES: este bloque no existía; la app enviaba la petición sin revisar el rol.
                // CAMBIO: antes de llamar al backend se verifica que el usuario de la sesión sea Administrador.
                // POR QUÉ: la guía pide comprobar que solo un Administrador pueda cambiar roles. Así se avisa
                // al instante sin enviar la petición. La seguridad REAL sigue estando en el backend, que
                // responde 403 a quien no es administrador (la app se puede modificar, el servidor no).
                if (!sessionManager.isAdmin.first()) {
                    errorMessage = "Solo un Administrador puede cambiar roles"
                    return@launch
                }

                if (token != null) {
                    val roleRequest = UpdateUserRoleRequest(role_ids = listOf(roleId))
                    val response = ApiClient.apiService.updateUserRoles(
                        token = "Bearer $token",
                        id = userId,
                        roleRequest = roleRequest
                    )

                    if (response.isSuccessful) {
                        successMessage = "Rol de usuario actualizado exitosamente"
                        loadUsers(currentPage, searchQuery) // Recargar lista
                    } else {
                        // [ERROR U4] ANTES: errorMessage = "Error al actualizar el rol del usuario"
                        // CAMBIO: mensaje según el código HTTP.
                        // POR QUÉ: si el backend responde 403 ("No tienes permisos de administrador") o
                        // 401 (token vencido), el usuario ahora entiende qué pasó.
                        errorMessage = mensajeDeError(response.code(), "actualizar el rol")
                    }
                } else {
                    // [ERROR U4] ANTES: este else no existía.
                    // CAMBIO / POR QUÉ: igual que en loadUsers(), se avisa que no hay sesión.
                    errorMessage = "No hay sesión activa. Inicia sesión de nuevo"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("USER_MGMT_DEBUG", "Error updating user role", e)
            } finally {
                isUpdatingRole = false
            }
        }
    }

    fun deleteUser(userId: Int) {
        isDeletingUser = true
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()

                // [ERROR U5] ANTES: este bloque no existía.
                // CAMBIO: se verifica el rol de Administrador antes de eliminar.
                // POR QUÉ: misma razón que en updateUserRole(): eliminar usuarios es una acción de administrador.
                if (!sessionManager.isAdmin.first()) {
                    errorMessage = "Solo un Administrador puede eliminar usuarios"
                    return@launch
                }

                if (token != null) {
                    val response = ApiClient.apiService.deleteUser("Bearer $token", userId)

                    if (response.isSuccessful) {
                        successMessage = "Usuario eliminado exitosamente"
                        loadUsers(currentPage, searchQuery) // Recargar lista
                    } else {
                        // [ERROR U4] ANTES: errorMessage = "Error al eliminar el usuario"
                        // CAMBIO / POR QUÉ: mensaje según el código HTTP, igual que arriba.
                        errorMessage = mensajeDeError(response.code(), "eliminar el usuario")
                    }
                } else {
                    // [ERROR U4] ANTES: este else no existía.
                    errorMessage = "No hay sesión activa. Inicia sesión de nuevo"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("USER_MGMT_DEBUG", "Error deleting user", e)
            } finally {
                isDeletingUser = false
            }
        }
    }

    // [ERROR U4] ANTES: esta función no existía.
    // CAMBIO: traduce el código HTTP de la respuesta a un mensaje entendible.
    // POR QUÉ: el backend responde 401 cuando el token falta, es inválido o venció (dura 24 horas),
    // y 403 cuando el usuario no es administrador. Antes todos esos casos mostraban el mismo texto genérico.
    private fun mensajeDeError(code: Int, accion: String): String = when (code) {
        401 -> "Tu sesión expiró o no es válida. Cierra sesión e inicia de nuevo"
        403 -> "No tienes permisos de administrador para $accion"
        404 -> "El usuario ya no existe"
        else -> "Error al $accion (HTTP $code)"
    }

    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }
}
