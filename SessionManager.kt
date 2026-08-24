package com.afklauncher.protocol

import android.content.Context
import android.content.Intent
import android.util.Log
import com.afklauncher.auth.MicrosoftAuthService
import com.afklauncher.data.repository.AccountRepository
import com.afklauncher.data.repository.InstanceRepository
import com.afklauncher.service.SessionForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionManager(
    private val context: Context,
    private val accountRepository: AccountRepository,
    private val instanceRepository: InstanceRepository
) {
    companion object {
        private const val TAG = "SessionManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val authService = MicrosoftAuthService(context)
    private val activeSessions = mutableMapOf<Long, MinecraftSession>()

    private val _sessions = MutableStateFlow<List<SessionState>>(emptyList())
    val sessions: StateFlow<List<SessionState>> = _sessions.asStateFlow()

    val activeSessionCount: Int get() = activeSessions.size

    fun startSession(instanceId: Long) {
        if (activeSessions.containsKey(instanceId)) {
            Log.d(TAG, "startSession: session already active for instance $instanceId")
            return
        }

        scope.launch {
            val instance = instanceRepository.getInstanceById(instanceId) ?: run {
                Log.e(TAG, "startSession: instance not found")
                return@launch
            }
            val accountId = instance.accountId ?: run {
                Log.e(TAG, "startSession: instance has no account")
                return@launch
            }
            val account = accountRepository.getAccountById(accountId) ?: run {
                Log.e(TAG, "startSession: account not found")
                return@launch
            }

            Log.d(TAG, "startSession: creating session for instance ${instance.id}")

            val session = MinecraftSession(
                scope = scope,
                instance = instance,
                account = account,
                authService = authService,
                accountRepository = accountRepository,
                onSessionEnded = {
                    Log.d(TAG, "Session ended for instance ${instance.id}")
                    activeSessions.remove(instanceId)
                    refreshSessionList()
                    updateForegroundService()
                }
            )

            activeSessions[instanceId] = session

            scope.launch {
                session.state.collect { state ->
                    _sessions.update { current ->
                        val updated = current.toMutableList()
                        val index = updated.indexOfFirst { it.instanceId == instanceId }
                        if (index >= 0) {
                            updated[index] = state
                        } else {
                            updated.add(state)
                        }
                        updated
                    }
                }
            }

            session.connect()
            refreshSessionList()
            updateForegroundService()
            Log.d(TAG, "startSession: session started, active count = $activeSessionCount")
        }
    }

    fun stopSession(instanceId: Long) {
        Log.d(TAG, "stopSession: stopping instance $instanceId")
        activeSessions[instanceId]?.disconnect()
        activeSessions.remove(instanceId)
        _sessions.update { list -> list.filter { it.instanceId != instanceId } }
        updateForegroundService()
        Log.d(TAG, "stopSession: active count = $activeSessionCount")
    }

    fun getSession(instanceId: Long): MinecraftSession? = activeSessions[instanceId]

    fun sendChat(instanceId: Long, message: String) {
        activeSessions[instanceId]?.sendChat(message)
    }

    // ---------- These two methods are called by the ViewModel ----------
    fun updateMovement(instanceId: Long, input: MovementInput) {
        activeSessions[instanceId]?.updateMovementInput(input)
    }

    fun jump(instanceId: Long) {
        activeSessions[instanceId]?.jump()
    }

    private fun refreshSessionList() {
        _sessions.update { current ->
            current.map { state ->
                activeSessions[state.instanceId]?.state?.value ?: state
            }
        }
    }

    private fun updateForegroundService() {
        if (activeSessions.isNotEmpty()) {
            Log.d(TAG, "updateForegroundService: starting service (${activeSessions.size} active sessions)")
            try {
                context.startForegroundService(
                    Intent(context, SessionForegroundService::class.java)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service", e)
            }
        } else {
            Log.d(TAG, "updateForegroundService: stopping service (no active sessions)")
            try {
                context.stopService(Intent(context, SessionForegroundService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop foreground service", e)
            }
        }
    }
}
