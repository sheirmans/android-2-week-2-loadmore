package com.ucsdextandroid2.petfinder

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

/**
 * Created by rjaylward on 2019-07-12
 */

object DataSource {

    private var baseOkHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
        .build()

    private var baseRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.petfinder.com/v2/")
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()))
        .client(baseOkHttpClient)
        .build()

    private var api: PetFinderApi? = null

    private fun createApi(): PetFinderApi {
        if(api == null) {
            api = baseRetrofit
                .newBuilder()
                .client(baseOkHttpClient
                    .newBuilder()
                    .addInterceptor { chain ->
                        val request: Request = chain.request().newBuilder()
                            .header("Authorization", getAccessTokenOrThrow(false)).build()

                        return@addInterceptor chain.proceed(request)
                    }
                    .authenticator(object : Authenticator {
                        override fun authenticate(route: Route?, response: okhttp3.Response): Request? {
                            //if the call was unauthenticated we need to clear the cached token
                            if(response.request.header("dont-retry") != null) {
                                return response.request.newBuilder()
                                    .addHeader("dont-retry", "true")
                                    .header("Authorization", getAccessTokenOrThrow(true)).build()
                            }

                            return null
                        }
                    })
                    .build()
                )
                .build()
                .create(PetFinderApi::class.java)
        }

        return api!!
    }

    @Synchronized
    private fun getAccessTokenOrThrow(clearCache: Boolean): String {
        if(clearCache)
            AccessTokenCache.instance.updateToken(null, 0)

        val cachedToken: String? = AccessTokenCache.instance.getCachedToken()
        if(cachedToken != null)
            return cachedToken

        val accessTokenCall: Call<AccessToken> = baseRetrofit.create(PetFinderAuthApi::class.java)
            .grantApiToken(BuildConfig.pet_finder_client_id, BuildConfig.pet_finder_client_secret)

        val response = accessTokenCall.execute()
        if(!response.isSuccessful)
            throw HttpException(response)

//        val responseBody = AccessToken(response.body()!!)
        val responseBody = response.body()!!

        val formattedToken = "${responseBody.tokenType} ${responseBody.accessToken}"
        AccessTokenCache.instance.updateToken(formattedToken, responseBody.expiresAtMillis)

        return formattedToken
    }


    fun findAnimals(lat: Double?, lng: Double?, page: Int = 1, limit: Int = 20, resultListener: (ApiResult<AnimalsResponse>) -> Unit) {
        createApi().getAnimals(
            location = if(lat != null && lng != null) "$lat,$lng" else null,
            page = page,
            limit = limit
        ).enqueue(RetrofitResultListener(resultListener))
    }

    interface PetFinderApi {

        @GET("animals")
        fun getAnimals(
            @Query("location") location: String?,
            @Query("type") type: String = "dog",
            @Query("page") page: Int = 1,
            @Query("limit") limit: Int = 20
        ): Call<AnimalsResponse>

    }
    
    interface PetFinderAuthApi {

        @FormUrlEncoded
        @POST("oauth2/token")
        fun grantApiToken(
            @Field("client_id") clientId: String,
            @Field("client_secret") clientSecret: String,
            @Field("grant_type") grantType: String = "client_credentials"
        ): Call<AccessToken>
        
    }

    private class RetrofitResultListener<T>(val resultListener: (ApiResult<T>) -> Unit) : Callback<T> {

        override fun onFailure(call: Call<T>, t: Throwable) {
            resultListener(ApiFail(t))
        }

        override fun onResponse(call: Call<T>, response: Response<T>) {
            if(response.isSuccessful && response.body() != null)
                resultListener(ApiSuccess(response.body()!!))
            else
                resultListener(ApiFail(HttpException(response)))
        }
    }

}
