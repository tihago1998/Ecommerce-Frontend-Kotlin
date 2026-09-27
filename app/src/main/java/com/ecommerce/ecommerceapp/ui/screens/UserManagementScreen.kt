package com.ecommerce.ecommerceapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecommerce.ecommerceapp.models.UserProfile
import com.ecommerce.ecommerceapp.utils.SessionManager
import com.ecommerce.ecommerceapp.viewmodels.UserManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    sessionManager: SessionManager,
    onNavigateBack: () -> Unit,
    viewModel: UserManagementViewModel = viewModel { UserManagementViewModel(sessionManager) }
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var showUserDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Usuarios") },
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
        ) {
            // Barra de búsqueda
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { query ->
                    viewModel.searchUsers(query)
                },
                label = { Text("Buscar usuarios") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de usuarios
            if (viewModel.isLoadingUsers) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.users) { user ->
                        UserCard(
                            user = user,
                            // [ERROR U6] ANTES: este parámetro no existía.
                            // CAMBIO: se indica si la tarjeta corresponde al usuario con la sesión abierta.
                            // POR QUÉ: permite bloquear el cambio de rol sobre uno mismo (ver UserCard).
                            esUsuarioActual = user.Email == viewModel.currentUserEmail,
                            availableRoles = viewModel.availableRoles,
                            isUpdatingRole = viewModel.isUpdatingRole,
                            onUpdateRole = { roleId ->
                                viewModel.updateUserRole(user.iD_User, roleId)
                            },
                            onDeleteUser = {
                                viewModel.deleteUser(user.iD_User)
                            }
                        )
                    }

                    // Paginación
                    if (viewModel.totalPages > 1) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { viewModel.loadPage(viewModel.currentPage - 1) },
                                    enabled = viewModel.currentPage > 1
                                ) {
                                    Text("Anterior")
                                }

                                Text(
                                    text = "Página ${viewModel.currentPage} de ${viewModel.totalPages}",
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )

                                Button(
                                    onClick = { viewModel.loadPage(viewModel.currentPage + 1) },
                                    enabled = viewModel.currentPage < viewModel.totalPages
                                ) {
                                    Text("Siguiente")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserCard(
    user: UserProfile,
    // [ERROR U6] ANTES: este parámetro no existía.
    // CAMBIO: nuevo parámetro esUsuarioActual.
    // POR QUÉ: PUT /api/users/{id}/roles REEMPLAZA todos los roles del usuario. Si el administrador
    // se asignaba otro rol a sí mismo perdía el de Administrador y nadie quedaba para devolvérselo.
    esUsuarioActual: Boolean,
    availableRoles: List<com.ecommerce.ecommerceapp.models.Role>,
    isUpdatingRole: Boolean,
    onUpdateRole: (Int) -> Unit,
    onDeleteUser: () -> Unit
) {
    var expandedRoles by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = user.UserName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user.Email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showDeleteDialog = true }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar usuario",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Información de ubicación
            user.city?.let { city ->
                Text(
                    text = "📍 ${city.CityName}, ${city.state}, ${city.country}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Roles actuales
            user.roles?.let { roles ->
                Text(
                    text = "Roles: ${roles.joinToString(", ") { it.TypeRole }}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            // Selector de nuevo rol
            ExposedDropdownMenuBox(
                expanded = expandedRoles,
                // [ERROR U6] ANTES: onExpandedChange = { expandedRoles = !expandedRoles }
                // CAMBIO: el menú no se abre si la tarjeta es la del propio usuario.
                // POR QUÉ: evita que el administrador se quite su propio rol por accidente.
                onExpandedChange = { if (!esUsuarioActual) expandedRoles = !expandedRoles }
            ) {
                OutlinedTextField(
                    // [ERROR U6] ANTES: value = "Cambiar rol",
                    // CAMBIO: en la tarjeta propia se muestra "No puedes cambiar tu propio rol".
                    // POR QUÉ: explica al usuario por qué el campo está bloqueado.
                    value = if (esUsuarioActual) "No puedes cambiar tu propio rol" else "Cambiar rol",
                    onValueChange = { },
                    readOnly = true,
                    // [ERROR U6] ANTES: esta línea no existía (el campo siempre estaba habilitado).
                    // CAMBIO: el campo se ve deshabilitado (gris) en la tarjeta propia.
                    // POR QUÉ: refuerza visualmente que esa acción no está permitida.
                    enabled = !esUsuarioActual,
                    label = { Text("Asignar rol") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRoles) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expandedRoles,
                    onDismissRequest = { expandedRoles = false }
                ) {
                    availableRoles.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.TypeRole) },
                            onClick = {
                                onUpdateRole(role.iDRole)
                                expandedRoles = false
                            },
                            enabled = !isUpdatingRole
                        )
                    }
                }
            }
        }
    }

    // Dialog de confirmación para eliminar
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Usuario") },
            text = { Text("¿Estás seguro de que deseas eliminar a ${user.UserName}? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteUser()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}