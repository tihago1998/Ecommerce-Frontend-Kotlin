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

class SalesHistoryViewModel(private val sessionManager: SessionManager) : ViewModel() {

    // Lista de ventas del usuario
    var sales by mutableStateOf<List<Sale>>(emptyList())
    var selectedSale by mutableStateOf<Sale?>(null)

    // Paginación
    var currentPage by mutableStateOf(1)
    var totalPages by mutableStateOf(1)
    var totalSales by mutableStateOf(0)

    // Estados de carga
    var isLoadingSales by mutableStateOf(false)
    var isLoadingSaleDetail by mutableStateOf(false)

    // Estados de error
    var errorMessage by mutableStateOf<String?>(null)

    // Estados para mostrar detalles
    var showSaleDetailDialog by mutableStateOf(false)

    init {
        loadSales()
    }

    fun loadSales(page: Int = 1) {
        isLoadingSales = true
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                if (token != null) {
                    val response = ApiClient.apiService.getUserSales(
                        token = "Bearer $token",
                        page = page,
                        perPage = 10
                    )

                    if (response.isSuccessful && response.body() != null) {
                        val salesResponse = response.body()!!
                        sales = salesResponse.sales
                        currentPage = page
                        totalPages = salesResponse.pagination?.total_pages ?: 1
                        // [ERROR C7] ANTES: totalSales = salesResponse.total
                        // CAMBIO: se lee el total desde pagination.total (o el tamaño de la lista si no viene).
                        // POR QUÉ: el backend envía {"sales": [...], "pagination": {..., "total": n}}; la clave
                        // "total" no existe en el primer nivel, así que el contador de pedidos mostraba 0.
                        totalSales = salesResponse.pagination?.total ?: sales.size
                        Log.d("SALES_DEBUG", "Ventas cargadas: ${sales.size}")
                    } else {
                        if (response.code() == 404) {
                            // No hay ventas
                            sales = emptyList()
                            totalSales = 0
                            totalPages = 1
                        } else {
                            errorMessage = "Error al cargar el historial de ventas"
                            Log.e("SALES_DEBUG", "Error response: ${response.code()}")
                        }
                    }
                } else {
                    errorMessage = "Sesión expirada"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("SALES_DEBUG", "Exception loading sales", e)
            } finally {
                isLoadingSales = false
            }
        }
    }

    fun loadSaleDetail(saleId: Int) {
        isLoadingSaleDetail = true
        viewModelScope.launch {
            try {
                val token = sessionManager.token.first()
                if (token != null) {
                    val response = ApiClient.apiService.getSale("Bearer $token", saleId)

                    // [ERROR C6] ANTES:
                    //     if (response.isSuccessful && response.body() != null) {
                    //         selectedSale = response.body()
                    // CAMBIO: se toma la venta que viene dentro de la clave "sale": response.body()?.sale
                    // POR QUÉ: GET /api/sales/{id} responde {"sale": {...}}. Antes se asignaba el envoltorio y
                    // el diálogo de detalle mostraba el pedido #0 sin productos ni total.
                    val sale = response.body()?.sale
                    if (response.isSuccessful && sale != null) {
                        selectedSale = sale
                        showSaleDetailDialog = true
                        Log.d("SALES_DEBUG", "Detalle de venta cargado: $saleId")
                    } else {
                        errorMessage = "Error al cargar los detalles de la venta"
                        Log.e("SALES_DEBUG", "Error loading sale detail: ${response.code()}")
                    }
                } else {
                    errorMessage = "Sesión expirada"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.message}"
                Log.e("SALES_DEBUG", "Exception loading sale detail", e)
            } finally {
                isLoadingSaleDetail = false
            }
        }
    }

    fun loadPage(page: Int) {
        loadSales(page)
    }

    fun dismissSaleDetail() {
        showSaleDetailDialog = false
        selectedSale = null
    }

    fun refreshSales() {
        loadSales(currentPage)
    }

    fun clearError() {
        errorMessage = null
    }

    // Funciones de utilidad
    fun formatDate(dateString: String): String {
        return try {
            // La API probablemente devuelve fechas en formato ISO
            // Aquí puedes formatear según necesites
            dateString.take(10) // Tomar solo la fecha (YYYY-MM-DD)
        } catch (e: Exception) {
            dateString
        }
    }

    fun getTotalSpent(): Double {
        return sales.sumOf { it.TotalSale }
    }

    fun getTotalOrders(): Int {
        return sales.size
    }
}