package com.hyundaiapp.chachelin.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * 결과 알림(PRD 5.3, 11-02). 앱이 화면에 없을 때 S4/S5에서 처리할 세션이 생기면 알린다. 담당: 이상준.
 * Android는 뒤에 있는 앱이 화면을 띄우지 못하게 막으므로, 알림을 누르면 앱이 열리고 Nav가 S4/S5를 보여 준다.
 *
 * - 세션마다 한 번만 알린다. 앱을 열어 결과 화면을 봤으면 다시 뒤로 가도 또 알리지 않는다.
 * - 결과를 처리했거나 앱이 화면에 나오면 알림을 지운다.
 */
class ChargeResultNotices(
    private val pending: StateFlow<ChargeSessionRecord?>,
    private val appVisible: StateFlow<Boolean>,
    private val show: suspend (ChargeSessionRecord) -> Unit,
    private val hide: () -> Unit,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            var handledId: String? = null
            combine(pending, appVisible, ::Pair).collect { (record, visible) ->
                when {
                    record == null -> hide()
                    visible -> {
                        handledId = record.id
                        hide()
                    }
                    record.id != handledId -> {
                        handledId = record.id
                        show(record)
                    }
                }
            }
        }
    }
}
