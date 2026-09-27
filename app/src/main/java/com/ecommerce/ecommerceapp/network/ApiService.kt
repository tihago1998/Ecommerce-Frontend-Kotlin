package com.ecommerce.ecommerceapp.network

import com.ecommerce.ecommerceapp.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ========== AUTENTICACIÓN ==========
    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): Response<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body user: User): Response<RegisterResponse>

    // ========== PERFIL DE USUARIO ==========
    // [ERROR P1] ANTES: suspend fun getProfile(...): Response<UserProfile>
    // CAMBIO: el tipo de respuesta pasa a UserResponse.
    // POR QUÉ: el backend responde {"user": {...}}; los datos del usuario están un nivel más adentro.
    @GET("api/users/profile")
    suspend fun getProfile(@Header("Authorization") token: String): Response<UserResponse>

    // [ERROR P1] ANTES: ...): Response<UserProfile>
    // CAMBIO: el tipo de respuesta pasa a UserResponse.
    // POR QUÉ: el backend responde {"message": "...", "user": {...}}.
    @PUT("api/users/profile")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body profile: UpdateProfileRequest
    ): Response<UserResponse>

    @PUT("api/auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") token: String,
        @Body passwordRequest: ChangePasswordRequest
    ): Response<AuthResponse>

    // ========== ADMINISTRACIÓN DE USUARIOS ==========
    @GET("api/users/")
    suspend fun getUsers(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10,
        @Query("search") search: String? = null
    ): Response<UsersResponse>

    @GET("api/users/{id}")
    suspend fun getUser(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<UserProfile>

    @PUT("api/users/{id}/roles")
    suspend fun updateUserRoles(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body roleRequest: UpdateUserRoleRequest
    ): Response<UserProfile>

    @DELETE("api/users/{id}")
    suspend fun deleteUser(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<AuthResponse>

    // ========== UBICACIONES ==========
    // [ERROR 1] ANTES: suspend fun getCountries(): Response<List<Country>>
    // CAMBIO: el tipo de respuesta pasa de List<Country> a CountriesResponse.
    // POR QUÉ: el backend responde {"count": 5, "countries": [...]} (un objeto), no [...] (una lista).
    @GET("api/locations/countries")
    suspend fun getCountries(): Response<CountriesResponse>

    @POST("api/locations/countries")
    suspend fun createCountry(@Body request: CreateLocationRequest): Response<Country>

    // [ERROR 1] ANTES: suspend fun getStates(...): Response<List<State>>
    // CAMBIO: el tipo de respuesta pasa de List<State> a StatesResponse.
    // POR QUÉ: el backend responde {"count": n, "states": [...]}.
    @GET("api/locations/states")
    suspend fun getStates(@Query("country_id") countryId: Int? = null): Response<StatesResponse>

    @POST("api/locations/states")
    suspend fun createState(@Body request: CreateLocationRequest): Response<State>

    // [ERROR 1] ANTES: suspend fun getCities(...): Response<List<City>>
    // CAMBIO: el tipo de respuesta pasa de List<City> a CitiesResponse.
    // POR QUÉ: el backend responde {"count": n, "cities": [...]}.
    @GET("api/locations/cities")
    suspend fun getCities(@Query("state_id") stateId: Int? = null): Response<CitiesResponse>

    @POST("api/locations/cities")
    suspend fun createCity(@Body request: CreateLocationRequest): Response<City>

    // [ERROR U1] ANTES: suspend fun getRoles(): Response<List<Role>>
    // CAMBIO: el tipo de respuesta pasa de List<Role> a RolesResponse.
    // POR QUÉ: Retrofit/Gson esperaba un arreglo "[" y el backend envía un objeto "{".
    // Gson lanzaba "Expected BEGIN_ARRAY but was BEGIN_OBJECT" y el desplegable de roles quedaba vacío.
    @GET("api/auth/roles")
    suspend fun getRoles(): Response<RolesResponse>


    // ========== GESTIÓN DE CATEGORÍAS ==========
    @GET("api/categories/")
    suspend fun getCategories(
        @Query("include_products") includeProducts: Boolean = false
    ): Response<CategoriesResponse>

    @GET("api/categories/{id}")
    suspend fun getCategory(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10
    ): Response<Category>

    @POST("api/categories/")
    suspend fun createCategory(
        @Header("Authorization") token: String,
        @Body category: CreateCategoryRequest
    ): Response<Category>

    @PUT("api/categories/{id}")
    suspend fun updateCategory(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body category: UpdateCategoryRequest
    ): Response<Category>

    @DELETE("api/categories/{id}")
    suspend fun deleteCategory(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<AuthResponse>

    @GET("api/categories/stats")
    suspend fun getCategoriesStats(): Response<Any>

    // ========== GESTIÓN DE PRODUCTOS ==========
    @GET("api/products/")
    suspend fun getProducts(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10,
        @Query("search") search: String? = null,
        @Query("category_id") categoryId: Int? = null,
        @Query("min_price") minPrice: Double? = null,
        @Query("max_price") maxPrice: Double? = null,
        @Query("in_stock") inStock: Boolean? = null
    ): Response<ProductsResponse>

    @GET("api/products/{id}")
    suspend fun getProduct(@Path("id") id: Int): Response<Product>

    @POST("api/products/")
    suspend fun createProduct(
        @Header("Authorization") token: String,
        @Body product: CreateProductRequest
    ): Response<Product>

    @PUT("api/products/{id}")
    suspend fun updateProduct(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body product: UpdateProductRequest
    ): Response<Product>

    @PUT("api/products/{id}/stock")
    suspend fun updateProductStock(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body stock: UpdateStockRequest
    ): Response<Product>

    @DELETE("api/products/{id}")
    suspend fun deleteProduct(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<AuthResponse>

    @GET("api/products/search")
    suspend fun searchProducts(@Query("q") query: String): Response<List<Product>>

    @GET("api/products/featured")
    suspend fun getFeaturedProducts(): Response<ProductsResponse>

    // ========== GESTIÓN DE IMÁGENES DE PRODUCTOS ==========
    @POST("api/products/{id}/images")
    suspend fun addProductImage(
        @Header("Authorization") token: String,
        @Path("id") productId: Int,
        @Body image: AddImageRequest
    ): Response<ProductImage>

    @DELETE("api/products/{productId}/images/{imageId}")
    suspend fun deleteProductImage(
        @Header("Authorization") token: String,
        @Path("productId") productId: Int,
        @Path("imageId") imageId: Int
    ): Response<AuthResponse>

    // ========== CARRITO DE COMPRAS ==========
    @GET("api/sales/cart")
    suspend fun getCart(@Header("Authorization") token: String): Response<Cart>

    @POST("api/sales/cart/add")
    suspend fun addToCart(
        @Header("Authorization") token: String,
        @Body item: AddToCartRequest
    ): Response<Cart>

    @PUT("api/sales/cart/update/{id}")
    suspend fun updateCartItem(
        @Header("Authorization") token: String,
        @Path("id") itemId: Int,
        @Body quantity: UpdateCartQuantityRequest
    ): Response<Cart>

    @DELETE("api/sales/cart/remove/{id}")
    suspend fun removeFromCart(
        @Header("Authorization") token: String,
        @Path("id") itemId: Int
    ): Response<AuthResponse>

    @DELETE("api/sales/cart/clear")
    suspend fun clearCart(@Header("Authorization") token: String): Response<AuthResponse>

    // ========== CHECKOUT Y VENTAS ==========
    @POST("api/sales/checkout")
    suspend fun checkout(
        @Header("Authorization") token: String,
        @Body checkoutData: CheckoutRequest
    ): Response<CheckoutResponse>

    @GET("api/sales/")
    suspend fun getUserSales(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 10
    ): Response<SalesResponse>

    @GET("api/sales/{id}")
    suspend fun getSale(
        @Header("Authorization") token: String,
        @Path("id") saleId: Int
    ): Response<Sale>

}