package com.example.storka.api

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import retrofit2.Call
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import retrofit2.http.*
import java.lang.Exception
import java.util.concurrent.TimeUnit


interface Api {

    @GET("events")
    fun listEvents(): Call<PlannerResponse<EventFullModel>>

    @POST("events")
    fun createEvent(@Body event: EventPostModel): Call<PlannerResponse<EventFullModel>>

    @PATCH("events/{id}")
    fun updateEvent(@Path("id") id: Long, @Body updates: EventFullModel): Call<PlannerResponse<EventFullModel>>

    @DELETE("events/{id}")
    fun deleteEvent(@Path("id") id: Long): Call<Void>

    @GET("events/instances")
    fun getEventInstances(@Query("from") from: Long, @Query("to") to: Long): Call<PlannerResponse<EventInstanceModel>>

    @GET("patterns")
    fun listPatterns(): Call<PlannerResponse<PatternGetModel>>

    @POST("patterns")
    fun createPattern(@Query("event_id") event_id: Long, @Body pattern: PatternPostModel): Call<PlannerResponse<PatternGetModel>>

    @PATCH("patterns/{id}")
    fun updatePattern(@Path("id") id: Long, @Body updates: PatternPostModel): Call<PlannerResponse<PatternGetModel>>

    @DELETE("patterns/{id}")
    fun deletePattern(@Path("id") id: Long): Call<Void>

    @GET("tasks")
    fun listTasks(): Call<PlannerResponse<TaskGetModel>>

    @POST("tasks")
    fun createTask(@Query("event_id") event_id: Long, @Body pattern: TaskPostModel): Call<PlannerResponse<TaskGetModel>>

    @PATCH("tasks/{id}")
    fun updateTasks(@Path("id") id: Long, @Body updates: TaskPostModel): Call<PlannerResponse<TaskGetModel>>

    @DELETE("tasks/{id}")
    fun deleteTasks(@Path("id") id: Long): Call<Void>

    companion object {

        private const val BASE_URL = "http://10.0.2.2:8080/api/v1/"

        @Volatile
        private var INSTANCE: Api? = null
        private var token: String? = null

        fun create(): Api? {
            synchronized(this) {
                val user = FirebaseAuth.getInstance().currentUser ?: return null
                val result: GetTokenResult
                try {
                    result = Tasks.await(user.getIdToken(false), 100000, TimeUnit.MILLISECONDS)
                } catch (e: Exception) {
                    Log.v("lol", "Firebase token exception: ${e.message}")
                    return null
                }
                val tempInstance = INSTANCE
                if (tempInstance != null && token == result.token) {
                    return tempInstance
                }
                token = result.token
                Log.v("lol", "Got firebase token: $token")
                if (token === null) return null

                val httpClient = OkHttpClient.Builder()

                httpClient.addInterceptor { chain ->
                    val request = chain.request().newBuilder().addHeader("X-Firebase-Auth", token).build()
                    chain.proceed(request)
                }

                val retrofit = retrofit2.Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build()

                val instance = retrofit.create(Api::class.java)

                INSTANCE = instance
                return instance
            }
        }
    }
}

class PlannerResponse<T>(
    val count: Int,
    val data: List<T>,
    val message: String,
    val status: Int,
    val success: Boolean
)

class EventFullModel(
    var details: String?,
    var id: Long,
    var location: String?,
    var name: String?,
    var owner_id: String,
    var status: String?
)

class EventPostModel(
    var details: String?,
    var location: String?,
    var name: String?,
    var status: String?
)

class EventInstancesRequest(
    var from: Long,
    var to: Long
)

class EventInstanceModel(
    var ended_at: Long,
    var event_id: Long,
    var pattern_id: Long,
    var started_at: Long
)

class PatternGetModel(
    val id: Long,
    val event_id: Long,
    val ended_at: Long?,
    val started_at: Long?,
    val rrule: String?,
    val duration: Long?,
    val timezone: String?
)

class PatternPostModel(
    val duration: Long?,
    val ended_at: Long?,
    val started_at: Long?,
    val rrule: String?,
    val timezone: String?
)

class TaskGetModel(
    var details: String?,
    var id: Long,
    var deadline_at: Long?,
    var name: String?,
    var event_id: Long,
    var parent_id: Long,
    var status: String?
)

class TaskPostModel(
    var deadline_at: Long?,
    var details: String?,
    var name: String?,
    var parent_id: Long,
    var status: String?
)

