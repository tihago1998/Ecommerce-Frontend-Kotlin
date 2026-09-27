package com.ecommerce.ecommerceapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
// [ERROR 7] ANTES: esta línea no existía.
// CAMBIO: se importa rememberSaveable.
// POR QUÉ: se necesita para que la pestaña activa sobreviva a la rotación (ver selectedTab más abajo).
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
// [PASO 6c] ANTES: esta línea no existía.
// CAMBIO: se importa el modelo Country.
// POR QUÉ: CountriesTab ahora recibe una función onVerEstados: (Country) -> Unit.
import com.ecommerce.ecommerceapp.models.Country
import com.ecommerce.ecommerceapp.ui.components.LoadingButton
import com.ecommerce.ecommerceapp.viewmodels.LocationManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: LocationManagementViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    // [ERROR 7] ANTES: var selectedTab by remember { mutableStateOf(0) }
    // CAMBIO: remember -> rememberSaveable.
    // POR QUÉ: al rotar, Android destruye y recrea la Activity. "remember" pierde su valor y la
    // pantalla volvía siempre a la pestaña Países. "rememberSaveable" guarda el valor en el Bundle
    // del sistema y lo restaura, así el usuario sigue en la pestaña donde estaba.
    // (El país/estado seleccionado no se pierde porque vive en el ViewModel, que sí sobrevive a la rotación.)
    var selectedTab by rememberSaveable { mutableStateOf(0) }

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
                title = { Text("Gestión de Ubicaciones") },
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
        ) {
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Países") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        // [PASO 6b] ANTES: viewModel.loadStates()
                        // CAMBIO: se pasa el id del país seleccionado en la pestaña Estados.
                        // POR QUÉ: loadStates() sin parámetro trae los estados de TODOS los países.
                        // Cada vez que se tocaba la pestaña se perdía el filtro por país que el
                        // usuario había elegido. Si no hay país elegido, se envía null y trae todos.
                        viewModel.loadStates(viewModel.selectedCountryForState?.iD_Country)
                    },
                    text = { Text("Estados") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        // [PASO 6b] ANTES: viewModel.loadCities()
                        // CAMBIO: se pasa el id del estado seleccionado en la pestaña Ciudades.
                        // POR QUÉ: igual que arriba, sin parámetro traía las ciudades de todos los
                        // estados e ignoraba la selección del usuario.
                        viewModel.loadCities(viewModel.selectedStateForCity?.iD_States)
                    },
                    text = { Text("Ciudades") }
                )
            }

            // Contenido según tab seleccionado
            when (selectedTab) {
                // [PASO 6c] ANTES: 0 -> CountriesTab(viewModel)
                // CAMBIO: se le pasa una función que selecciona el país y cambia a la pestaña Estados.
                // POR QUÉ: el botón ⋮ de cada país cargaba sus estados pero la pantalla se quedaba en
                // Países, así que el usuario no veía ningún resultado. Solo esta pantalla puede cambiar
                // selectedTab, por eso la acción se define aquí y se pasa a CountriesTab.
                0 -> CountriesTab(viewModel) { country ->
                    viewModel.updateSelectedCountryForState(country)
                    selectedTab = 1
                }
                1 -> StatesTab(viewModel)
                2 -> CitiesTab(viewModel)
            }
        }
    }
}

@Composable
// [PASO 6c] ANTES: fun CountriesTab(viewModel: LocationManagementViewModel) {
// CAMBIO: se agrega el parámetro onVerEstados.
// POR QUÉ: permite que el botón ⋮ avise a la pantalla principal qué país se eligió,
// para que esta cambie a la pestaña Estados.
fun CountriesTab(viewModel: LocationManagementViewModel, onVerEstados: (Country) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Formulario para crear país
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Crear Nuevo País",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = viewModel.newCountryName,
                    onValueChange = viewModel::updateNewCountryName,
                    label = { Text("Nombre del País") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                LoadingButton(
                    text = "Crear País",
                    isLoading = viewModel.isCreating,
                    onClick = viewModel::createCountry,
                    enabled = viewModel.newCountryName.isNotBlank()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de países
        Text(
            text = "Países Existentes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (viewModel.isLoadingCountries) {
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
                items(viewModel.countries) { country ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = country.CountryName,
                                style = MaterialTheme.typography.bodyLarge
                            )

                            IconButton(
                                // [PASO 6c] ANTES: onClick = { viewModel.loadStates(country.iD_Country) }
                                // CAMBIO: llama a onVerEstados(country).
                                // POR QUÉ: antes se cargaban los estados en segundo plano sin mostrarlos.
                                // Ahora se selecciona el país y se abre la pestaña Estados ya filtrada.
                                onClick = { onVerEstados(country) }
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Ver estados")  }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatesTab(viewModel: LocationManagementViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Formulario para crear estado
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Crear Nuevo Estado",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Selector de país
                var expandedCountries by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedCountries,
                    onExpandedChange = { expandedCountries = !expandedCountries }
                ) {
                    OutlinedTextField(
                        // [ERROR 6] ANTES: value = viewModel.selectedCountryForState?.CountryName ?: "Seleccionar país",
                        // CAMBIO: se agrega un caso "Cargando países..." mientras isLoadingCountries es true.
                        // POR QUÉ: antes, mientras llegaba la respuesta del servidor, el campo decía
                        // "Seleccionar país" y el menú se abría vacío; el usuario no sabía si estaba
                        // cargando o si había fallado. La guía pide mostrar el estado "cargando".
                        value = when {
                            viewModel.isLoadingCountries -> "Cargando países..."
                            viewModel.selectedCountryForState != null -> viewModel.selectedCountryForState!!.CountryName
                            else -> "Seleccionar país"
                        },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("País") },
                        // [ERROR 6] ANTES: trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCountries) },
                        // CAMBIO: mientras carga se muestra un CircularProgressIndicator pequeño en lugar de la flecha.
                        // POR QUÉ: es el indicador visual de carga que pide la guía (estado "cargando").
                        trailingIcon = {
                            if (viewModel.isLoadingCountries) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCountries)
                            }
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCountries,
                        onDismissRequest = { expandedCountries = false }
                    ) {
                        // [ERROR 6] ANTES: este bloque no existía; si la lista estaba vacía el menú se abría sin nada.
                        // CAMBIO: si no hay países (y no está cargando) se muestra una opción
                        // "No hay países. Toca para reintentar" que vuelve a llamar a loadCountries().
                        // POR QUÉ: loadCountries() solo se ejecutaba una vez, en el init del ViewModel.
                        // Si en ese momento el backend no respondía (como se vio en el log, donde la app
                        // nunca pidió /countries), el desplegable quedaba vacío para siempre y la única
                        // salida era salir y volver a entrar a la pantalla. Ahora el usuario puede reintentar.
                        if (viewModel.countries.isEmpty() && !viewModel.isLoadingCountries) {
                            DropdownMenuItem(
                                text = { Text("No hay países. Toca para reintentar") },
                                onClick = {
                                    expandedCountries = false
                                    viewModel.loadCountries()
                                }
                            )
                        }
                        viewModel.countries.forEach { country ->
                            DropdownMenuItem(
                                text = { Text(country.CountryName) },
                                onClick = {
                                    viewModel.updateSelectedCountryForState(country)
                                    expandedCountries = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = viewModel.newStateName,
                    onValueChange = viewModel::updateNewStateName,
                    label = { Text("Nombre del Estado") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                LoadingButton(
                    text = "Crear Estado",
                    isLoading = viewModel.isCreating,
                    onClick = viewModel::createState,
                    enabled = viewModel.newStateName.isNotBlank() && viewModel.selectedCountryForState != null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de estados
        Text(
            text = "Estados Existentes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (viewModel.isLoadingStates) {
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
                items(viewModel.states) { state ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = state.StatesName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    // [ERROR 3] ANTES: text = state.country?.CountryName ?: "",
                                    // CAMBIO: se usa state.country directamente.
                                    // POR QUÉ: State.country ahora es String? (el nombre del país), ya no
                                    // un objeto Country, así que no tiene la propiedad CountryName.
                                    text = state.country ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { viewModel.loadCities(state.iD_States) }
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Ver Ciudades")   }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitiesTab(viewModel: LocationManagementViewModel) {
    // [PASO 6e] ANTES:
    //     // Cargar estados al inicializar la tab
    //     LaunchedEffect(Unit) {
    //         viewModel.loadStates()
    //     }
    // CAMBIO: solo se cargan los estados si ya hay un país elegido en esta pestaña, y filtrados por él.
    // POR QUÉ: antes, al abrir Ciudades se traían los estados de TODOS los países mezclados, así que el
    // desplegable no tenía relación con ningún país. La lista "states" la comparten las pestañas Estados
    // y Ciudades, por eso al entrar aquí se recarga con el país de ESTA pestaña.
    LaunchedEffect(Unit) {
        viewModel.selectedCountryForCity?.let { viewModel.loadStates(it.iD_Country) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Formulario para crear ciudad
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Crear Nueva Ciudad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // [PASO 6e] ANTES: este selector de país no existía; la pestaña solo tenía el de estado.
                // CAMBIO: se agrega un desplegable de país antes del de estado. Al elegir un país se llama
                // a updateSelectedCountryForCity(), que carga sus estados y limpia el estado elegido antes.
                // POR QUÉ: es la cascada país -> estado que pide la guía. Sin él, la función
                // updateSelectedCountryForCity() del ViewModel existía pero ningún botón la llamaba.
                var expandedCountriesCity by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedCountriesCity,
                    onExpandedChange = { expandedCountriesCity = !expandedCountriesCity }
                ) {
                    OutlinedTextField(
                        value = when {
                            viewModel.isLoadingCountries -> "Cargando países..."
                            viewModel.selectedCountryForCity != null -> viewModel.selectedCountryForCity!!.CountryName
                            else -> "Seleccionar país"
                        },
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("País") },
                        trailingIcon = {
                            if (viewModel.isLoadingCountries) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCountriesCity)
                            }
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedCountriesCity,
                        onDismissRequest = { expandedCountriesCity = false }
                    ) {
                        if (viewModel.countries.isEmpty() && !viewModel.isLoadingCountries) {
                            DropdownMenuItem(
                                text = { Text("No hay países. Toca para reintentar") },
                                onClick = {
                                    expandedCountriesCity = false
                                    viewModel.loadCountries()
                                }
                            )
                        }
                        viewModel.countries.forEach { country ->
                            DropdownMenuItem(
                                text = { Text(country.CountryName) },
                                onClick = {
                                    viewModel.updateSelectedCountryForCity(country)
                                    expandedCountriesCity = false
                                }
                            )
                        }
                    }
                }

                // Selector de estado
                var expandedStates by remember { mutableStateOf(false) }
                // [PASO 6e] Variable auxiliar nueva: indica si ya se eligió un país en esta pestaña.
                val hayPaisElegido = viewModel.selectedCountryForCity != null
                ExposedDropdownMenuBox(
                    expanded = expandedStates,
                    // [PASO 6e] ANTES: onExpandedChange = { expandedStates = !expandedStates }
                    // CAMBIO: el menú solo se abre si hay un país elegido.
                    // POR QUÉ: sin país, la lista de estados no corresponde a ningún país y el usuario
                    // podría crear una ciudad en un estado equivocado.
                    onExpandedChange = { if (hayPaisElegido) expandedStates = !expandedStates }
                ) {
                    OutlinedTextField(
                        // [PASO 6e] ANTES: value = viewModel.selectedStateForCity?.StatesName ?: "Seleccionar estado",
                        // CAMBIO: se indica al usuario qué hacer primero y cuándo se están cargando los estados.
                        // POR QUÉ: guía al usuario por la cascada y muestra el estado "cargando" que pide la guía.
                        value = when {
                            !hayPaisElegido -> "Primero elige un país"
                            viewModel.isLoadingStates -> "Cargando estados..."
                            else -> viewModel.selectedStateForCity?.StatesName ?: "Seleccionar estado"
                        },
                        onValueChange = { },
                        readOnly = true,
                        // [PASO 6e] ANTES: esta línea no existía (el campo siempre estaba habilitado).
                        // CAMBIO: el campo se ve deshabilitado (gris) hasta que haya un país.
                        // POR QUÉ: refuerza visualmente que el estado depende del país.
                        enabled = hayPaisElegido,
                        label = { Text("Estado") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedStates) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedStates,
                        onDismissRequest = { expandedStates = false }
                    ) {
                        // [PASO 6e] ANTES: este bloque no existía; si el país no tenía estados el menú salía vacío.
                        // CAMBIO: se muestra un mensaje cuando el país elegido no tiene estados registrados.
                        // POR QUÉ: diferencia "no hay datos" de "algo falló", como pide la guía.
                        if (viewModel.states.isEmpty() && !viewModel.isLoadingStates) {
                            DropdownMenuItem(
                                text = { Text("Este país no tiene estados registrados") },
                                onClick = { expandedStates = false },
                                enabled = false
                            )
                        }
                        viewModel.states.forEach { state ->
                            DropdownMenuItem(
                                // [ERROR 3] ANTES: text = { Text("${state.StatesName} - ${state.country?.CountryName}") },
                                // CAMBIO: se usa state.country (texto) y ?: "" para no mostrar "null".
                                // POR QUÉ: State.country ahora es String? con el nombre del país que envía el backend.
                                text = { Text("${state.StatesName} - ${state.country ?: ""}") },
                                onClick = {
                                    viewModel.updateSelectedStateForCity(state)
                                    expandedStates = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = viewModel.newCityName,
                    onValueChange = viewModel::updateNewCityName,
                    label = { Text("Nombre de la Ciudad") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                LoadingButton(
                    text = "Crear Ciudad",
                    isLoading = viewModel.isCreating,
                    onClick = viewModel::createCity,
                    enabled = viewModel.newCityName.isNotBlank() && viewModel.selectedStateForCity != null
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de ciudades
        Text(
            text = "Ciudades Existentes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (viewModel.isLoadingCities) {
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
                items(viewModel.cities) { city ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = city.CityName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${city.state}, ${city.country}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}