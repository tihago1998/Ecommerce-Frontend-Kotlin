package com.ecommerce.ecommerceapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.google.gson.annotations.SerializedName

@Parcelize
data class CartItem(
    @SerializedName("id_TemporalSales")
    val id_cart_item: Int = 0,  // Mapear id_TemporalSales a id_cart_item

    @SerializedName("id_Product")
    val id_Product: Int = 0,

    @SerializedName("quantity")
    val quantity: Int = 1,

    @SerializedName("product")
    val product: Product? = null,

    @SerializedName("subtotal")
    val subtotal: Double = 0.0,

    // Campos adicionales que devuelve la API
    @SerializedName("DateAdded")
    val DateAdded: String? = null,

    @SerializedName("iD_User")
    val iD_User: Int = 0,

    @SerializedName("id_Sale")
    val id_Sale: Int? = null
) : Parcelable

@Parcelize
data class Cart(
    @SerializedName("cart_items")
    val items: List<CartItem> = emptyList(),

    @SerializedName("count")
    val total_items: Int = 0,

    @SerializedName("total")
    val total_amount: Double = 0.0,

    @SerializedName("message")
    val message: String? = null
) : Parcelable

data class AddToCartRequest(
    @SerializedName("id_Product")
    val id_Product: Int,

    @SerializedName("quantity")
    val quantity: Int
)

data class UpdateCartQuantityRequest(
    @SerializedName("quantity")
    val quantity: Int
)

@Parcelize
data class Sale(
    @SerializedName("id_Sale")
    val id_Sale: Int = 0,

    @SerializedName("DescripcionSale")
    val DescripcionSale: String = "",

    // [ERROR C5] ANTES: @SerializedName("TotalSale")
    // CAMBIO: se lee la clave "total". El nombre en Kotlin (TotalSale) se mantiene para no tocar las pantallas.
    // POR QUÉ: el backend (models.py, Sales.to_dict) envía "total"; Gson no encontraba "TotalSale" y
    // dejaba 0.0, por eso el historial mostraba $0.00 en todos los pedidos.
    @SerializedName("total")
    val TotalSale: Double = 0.0,

    // [ERROR C5] ANTES: @SerializedName("DateSale")
    // CAMBIO: se lee la clave "DateCreated".
    // POR QUÉ: el backend envía la fecha como "DateCreated"; con "DateSale" la fecha llegaba vacía.
    @SerializedName("DateCreated")
    val DateSale: String = "",

    @SerializedName("iD_User")
    val iD_User: Int = 0,

    // [ERROR C1] ANTES: val user: User? = null,
    // CAMBIO: pasa de objeto User? a String? (el nombre del usuario).
    // POR QUÉ: CAUSA RAÍZ del conflicto carrito/historial. El backend envía "user": "juan_perez" (texto,
    // ver Sales.to_dict: self.user.UserName). Gson esperaba un objeto "{" y encontraba texto "\"",
    // lanzaba "Expected BEGIN_OBJECT but was STRING at $.sale.user" y:
    //   - al confirmar el pedido: el backend SÍ lo creaba (201) pero la app mostraba error;
    //   - en el historial: fallaba toda la lista en cuanto existía al menos un pedido.
    @SerializedName("user")
    val user: String? = null,

    // [ERROR C5] ANTES: @SerializedName("sale_details")
    // CAMBIO: se lee la clave "details".
    // POR QUÉ: el backend envía los productos del pedido en "details"; se mostraban "0 productos".
    @SerializedName("details")
    val sale_details: List<SaleDetail> = emptyList()
) : Parcelable

@Parcelize
data class SaleDetail(
    // [ERROR C5] ANTES: @SerializedName("id_SaleDetail")
    // CAMBIO: se lee "id_SalesDetails" (con "s", como en SalesDetail.to_dict del backend).
    @SerializedName("id_SalesDetails")
    val id_SaleDetail: Int = 0,

    // [ERROR C5] ANTES: @SerializedName("Quantity")
    // CAMBIO: se lee "amount".
    // POR QUÉ: el backend guarda la cantidad comprada en "amount"; se mostraba (0).
    @SerializedName("amount")
    val Quantity: Int = 0,

    // [ERROR C5] ANTES: @SerializedName("Subtotal")
    // CAMBIO: se lee "ValueSale".
    // POR QUÉ: el backend envía el valor de la línea (precio x cantidad) como "ValueSale".
    @SerializedName("ValueSale")
    val Subtotal: Double = 0.0,

    @SerializedName("id_Product")
    val id_Product: Int = 0,

    @SerializedName("product")
    val product: Product? = null
) : Parcelable {
    // [ERROR C5] ANTES:
    //     @SerializedName("UnitPrice")
    //     val UnitPrice: Double = 0.0,
    // CAMBIO: deja de leerse del JSON y se calcula como Subtotal / Quantity.
    // POR QUÉ: el backend NO envía precio unitario; el detalle mostraba "$0.0 x 0". El precio
    // unitario se obtiene dividiendo el valor de la línea entre la cantidad comprada.
    val UnitPrice: Double
        get() = if (Quantity > 0) Subtotal / Quantity else 0.0
}

// [ERROR C6] ANTES: esta clase no existía.
// CAMBIO: envoltorio para la respuesta de GET /api/sales/{id}.
// POR QUÉ: el backend responde {"sale": {...}}, no la venta directa; el diálogo de detalle salía vacío.
data class SaleResponse(
    @SerializedName("sale")
    val sale: Sale? = null
)

data class CheckoutRequest(
    @SerializedName("DescripcionSale")
    val DescripcionSale: String
)

data class CheckoutResponse(
    @SerializedName("message")
    val message: String? = null,

    @SerializedName("sale")
    val sale: Sale? = null,

    @SerializedName("sale_id")
    val sale_id: Int? = null
) {
    fun isSuccess(): Boolean {
        return sale != null || sale_id != null
    }
}

data class SalesResponse(
    @SerializedName("sales")
    val sales: List<Sale> = emptyList(),

    @SerializedName("pagination")
    val pagination: Pagination? = null,

    // [ERROR C7] Nota: esta clave "total" NO existe en el primer nivel de la respuesta de GET /api/sales/
    // (el backend la envía dentro de "pagination"). Se deja por compatibilidad, pero ya no se usa:
    // SalesHistoryViewModel ahora lee pagination.total.
    @SerializedName("total")
    val total: Int = 0
)