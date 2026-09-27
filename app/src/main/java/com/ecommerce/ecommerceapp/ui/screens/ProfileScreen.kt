package com.ecommerce.ecommerceapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
// [ERROR P5] ANTES: esta línea no existía. CAMBIO: import de KeyboardOptions.
// POR QUÉ: el campo email ahora es editable y muestra el teclado de correo (con @).
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
// [ERROR P5] ANTES: esta línea no existía. CAMBIO: import de KeyboardType (tipo de teclado Email).
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecommerce.ecommerceapp.ui.components.LoadingButton
import com.ecommerce.ecommerceapp.utils.SessionManager
import com.ecommerce.ecommerceapp.viewmodels.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    sessionManager: SessionManager,
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel(sessionManager) }
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    // Manejar mensajes
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let { success ->
            snackbarHostState.showSnackbar(success)
            viewModel.clearMessages()
        }
    }

    // [ERROR P8] ANTES: este bloque no existía.
    // CAMBIO: cuando el ViewModel confirma el cambio de contraseña (passwordChanged = true), se cierra
    // el diálogo y se reinicia la bandera.
    // POR QUÉ: el cierre se revisaba justo después de llamar a changePassword(), pero la petición
    // corre en una corrutina y todavía no había respondido; el diálogo nunca se cerraba solo.
    LaunchedEffect(viewModel.passwordChanged) {
        if (viewModel.passwordChanged) {
            showChangePasswordDialog = false
            viewModel.passwordChanged = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Información del perfil
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Información Personal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    // [ERROR P7] ANTES: no se mostraba nada mientras cargaba el perfil (los campos se veían vacíos).
                    // CAMBIO: barra de progreso mientras isLoadingProfile es true.
                    // POR QUÉ: la guía pide contemplar el estado "cargando"; así el usuario no confunde
                    // "todavía está cargando" con "el perfil está vacío".
                    if (viewModel.isLoadingProfile) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    // Campo Nombre
                    OutlinedTextField(
                        value = viewModel.userName,
                        onValueChange = viewModel::updateUserName,
                        label = { Text("Nombre de Usuario") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isUpdatingProfile
                    )

                    // [ERROR P4] ANTES: había un único desplegable "Ciudad" con TODAS las ciudades mezcladas
                    // (que además salía vacío porque getCities() fallaba en silencio).
                    // CAMBIO: tres desplegables en cascada: País -> Estado -> Ciudad. Cada nivel se habilita
                    // cuando el anterior tiene valor, y al cambiar un nivel se limpian los de abajo.
                    // POR QUÉ: la guía pide que el país y el estado seleccionados (reto 1) se usen dentro de la
                    // actualización del perfil. El backend guarda iD_City; la cascada garantiza que esa ciudad
                    // pertenece al estado y al país elegidos.
                    PerfilDropdown(
                        label = "País",
                        textoSeleccionado = viewModel.selectedCountry?.CountryName,
                        textoVacio = "Seleccionar país",
                        opciones = viewModel.countries,
                        textoOpcion = { it.CountryName },
                        onSeleccionar = viewModel::updateSelectedCountry,
                        cargando = viewModel.isLoadingCountries,
                        textoSinOpciones = "No hay países. Toca para reintentar",
                        onReintentar = viewModel::reloadCountries,
                        habilitado = !viewModel.isUpdatingProfile
                    )

                    PerfilDropdown(
                        label = "Estado / Departamento",
                        textoSeleccionado = viewModel.selectedState?.StatesName,
                        textoVacio = if (viewModel.selectedCountry == null) "Primero elige un país" else "Seleccionar estado",
                        opciones = viewModel.states,
                        textoOpcion = { it.StatesName },
                        onSeleccionar = viewModel::updateSelectedState,
                        cargando = viewModel.isLoadingStates,
                        textoSinOpciones = "Este país no tiene estados registrados",
                        habilitado = viewModel.selectedCountry != null && !viewModel.isUpdatingProfile
                    )

                    PerfilDropdown(
                        label = "Ciudad",
                        textoSeleccionado = viewModel.cities.find { it.iD_City == viewModel.selectedCityId }?.CityName,
                        textoVacio = if (viewModel.selectedState == null) "Primero elige un estado" else "Seleccionar ciudad",
                        opciones = viewModel.cities,
                        textoOpcion = { it.CityName },
                        onSeleccionar = { city -> viewModel.updateSelectedCity(city.iD_City) },
                        cargando = viewModel.isLoadingCities,
                        textoSinOpciones = "Este estado no tiene ciudades registradas",
                        habilitado = viewModel.selectedState != null && !viewModel.isUpdatingProfile
                    )

                    // [ERROR P5] ANTES:
                    //     // Email (solo lectura)
                    //     value = viewModel.userProfile?.Email ?: "",
                    //     onValueChange = { },
                    //     enabled = false
                    // CAMBIO: el email es editable, usa el teclado de correo y marca error si el formato es inválido.
                    // POR QUÉ: la guía pide editar los datos del perfil y validar el formato del correo. El backend
                    // acepta el cambio de Email. Antes el campo estaba bloqueado y nunca se enviaba.
                    val emailInvalido = viewModel.email.isNotBlank() &&
                        !android.util.Patterns.EMAIL_ADDRESS.matcher(viewModel.email.trim()).matches()
                    OutlinedTextField(
                        value = viewModel.email,
                        onValueChange = viewModel::updateEmail,
                        label = { Text("Email") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = emailInvalido,
                        supportingText = {
                            if (emailInvalido) Text("Formato de correo no válido")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.isUpdatingProfile
                    )

                    // Botón actualizar perfil
                    LoadingButton(
                        text = "Actualizar Perfil",
                        isLoading = viewModel.isUpdatingProfile,
                        onClick = viewModel::updateProfile
                    )
                }
            }

            // Sección de contraseña
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Seguridad",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { showChangePasswordDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cambiar Contraseña")
                    }
                }
            }

            // Información de roles
            viewModel.userProfile?.roles?.let { roles ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Roles",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        roles.forEach { role ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(role.TypeRole)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog para cambiar contraseña
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Cambiar Contraseña") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.currentPassword,
                        onValueChange = viewModel::updateCurrentPassword,
                        label = { Text("Contraseña Actual") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = viewModel.newPassword,
                        onValueChange = viewModel::updateNewPassword,
                        label = { Text("Nueva Contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = viewModel.confirmPassword,
                        onValueChange = viewModel::updateConfirmPassword,
                        label = { Text("Confirmar Nueva Contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                LoadingButton(
                    text = "Cambiar",
                    isLoading = viewModel.isChangingPassword,
                    // [ERROR P8] ANTES:
                    //     onClick = {
                    //         viewModel.changePassword()
                    //         if (viewModel.successMessage != null) {
                    //             showChangePasswordDialog = false
                    //         }
                    //     },
                    // CAMBIO: solo se llama a changePassword(); el cierre lo hace el LaunchedEffect de arriba.
                    // POR QUÉ: successMessage siempre era null en ese instante (la petición aún no respondía).
                    onClick = { viewModel.changePassword() },
                    modifier = Modifier.width(120.dp)
                )
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

// [ERROR P4] ANTES: este componente no existía.
// CAMBIO: desplegable reutilizable para País, Estado y Ciudad. Muestra "Cargando..." con un spinner
// mientras llegan los datos, un mensaje si la lista está vacía (con reintento opcional) y se puede
// deshabilitar cuando el nivel anterior de la cascada no tiene valor.
// POR QUÉ: los tres desplegables funcionan igual; con un solo componente se evita repetir el mismo
// bloque tres veces y todos contemplan los estados cargando / éxito / vacío que pide la guía.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> PerfilDropdown(
    label: String,
    textoSeleccionado: String?,
    textoVacio: String,
    opciones: List<T>,
    textoOpcion: (T) -> String,
    onSeleccionar: (T) -> Unit,
    cargando: Boolean,
    textoSinOpciones: String,
    habilitado: Boolean,
    onReintentar: (() -> Unit)? = null
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido && habilitado,
        onExpandedChange = { if (habilitado) expandido = !expandido }
    ) {
        OutlinedTextField(
            value = when {
                cargando -> "Cargando..."
                textoSeleccionado != null -> textoSeleccionado
                else -> textoVacio
            },
            onValueChange = { },
            readOnly = true,
            enabled = habilitado,
            label = { Text(label) },
            leadingIcon = {
                Icon(Icons.Default.LocationOn, contentDescription = null)
            },
            trailingIcon = {
                if (cargando) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido)
                }
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expandido && habilitado,
            onDismissRequest = { expandido = false }
        ) {
            if (opciones.isEmpty() && !cargando) {
                DropdownMenuItem(
                    text = { Text(textoSinOpciones) },
                    onClick = {
                        expandido = false
                        onReintentar?.invoke()
                    },
                    enabled = onReintentar != null
                )
            }
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(textoOpcion(opcion)) },
                    onClick = {
                        onSeleccionar(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}