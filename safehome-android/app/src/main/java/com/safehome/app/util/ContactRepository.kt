package com.safehome.app.util

import android.util.Log
import com.safehome.app.api.ContactApi
import com.safehome.app.api.RetrofitClient
import com.safehome.app.model.ContactCreateRequest
import com.safehome.app.model.ContactResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactRepository(private val tokenManager: TokenManager) {

    private val api by lazy { RetrofitClient.create(ContactApi::class.java) }

    /** 캐시 (오프라인에서도 사용) */
    fun cached(): List<ContactResponse> = tokenManager.getContactCache()

    /** 서버에서 받아 캐시 갱신. 실패하면 기존 캐시 유지 */
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
    private suspend fun migrateLegacyIfNeeded() {
        val legacy = tokenManager.getLegacyContacts()
        if (legacy.isEmpty()) return

        var allOk = true
        legacy.forEach { (name, phone) ->
            try {
                val res = api.addContact(ContactCreateRequest(name, phone))
                // 이미 등록된 번호(중복)는 성공으로 간주
                if (!res.isSuccessful && res.code() != 400 && res.code() != 409) allOk = false
            } catch (e: Exception) {
                allOk = false
            }
        }
        // 네트워크 실패 시 다음에 다시 시도하도록 남겨둠
        if (allOk) tokenManager.clearLegacyContacts()
    }

    private fun errorMessage(body: String?): String =
        try {
            org.json.JSONObject(body ?: "").optString("message").ifBlank { "요청에 실패했어요." }
        } catch (e: Exception) { "요청에 실패했어요." }

    companion object { private const val TAG = "ContactRepository" }
}