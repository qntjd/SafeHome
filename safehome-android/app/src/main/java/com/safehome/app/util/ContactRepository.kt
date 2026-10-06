package com.safehome.app.util

import android.util.Log
import com.safehome.app.api.ContactApi
import com.safehome.app.api.RetrofitClient
import com.safehome.app.model.ContactCreateRequest
import com.safehome.app.model.ContactResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ContactRepository(private val tokenManager: TokenManager) {

    private val api by lazy { RetrofitClient.create(ContactApi::class.java) }
    private val migrationMutex = Mutex()


    fun cached(): List<ContactResponse> = tokenManager.getContactCache()

    suspend fun refresh(): Result<List<ContactResponse>> = withContext(Dispatchers.IO) {
        try {
            migrateLegacyIfNeeded()
            val res = api.getContacts()
            val list = res.body()?.data
            if (res.isSuccessful && list != null) {
                tokenManager.saveContactCache(list)
                Result.success(list)
            } else {
                Result.failure(Exception(errorMessage(res.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Log.e(TAG, "연락처 동기화 실패: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun add(name: String, phone: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.addContact(ContactCreateRequest(name, phone))
            if (res.isSuccessful) {
                refresh()
                Result.success(Unit)
            } else {
                Result.failure(Exception(errorMessage(res.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(Exception("인터넷 연결을 확인해주세요."))
        }
    }

    suspend fun delete(contactId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val res = api.deleteContact(contactId)
            if (res.isSuccessful) {
                refresh()
                Result.success(Unit)
            } else {
                Result.failure(Exception(errorMessage(res.errorBody()?.string())))
            }
        } catch (e: Exception) {
            Result.failure(Exception("인터넷 연결을 확인해주세요."))
        }
    }

    /** 기존 휴대폰 저장 연락처를 서버로 한 번 옮김 */
    private suspend fun migrateLegacyIfNeeded() = migrationMutex.withLock {
        val legacy = tokenManager.getLegacyContacts()
        if (legacy.isEmpty()) return@withLock

        val serverPhones = api.getContacts().body()?.data
            ?.map { it.phone }?.toMutableSet()
            ?: return@withLock

        legacy.distinctBy { it.second }.forEach { (name, phone) ->
            if (phone !in serverPhones) {
                val res = api.addContact(ContactCreateRequest(name, phone))
                if (res.isSuccessful) serverPhones += phone
            }
        }
        
        tokenManager.clearLegacyContacts()
    }

    private fun errorMessage(body: String?): String =
        try {
            org.json.JSONObject(body ?: "").optString("message").ifBlank { "요청에 실패했어요." }
        } catch (e: Exception) { "요청에 실패했어요." }

    companion object { private const val TAG = "ContactRepository" }
}