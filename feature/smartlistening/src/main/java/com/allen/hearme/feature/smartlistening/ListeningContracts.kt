package com.allen.hearme.feature.smartlistening

import com.allen.hearme.feature.smartlistening.model.DecisionContext
import com.allen.hearme.feature.smartlistening.model.ListeningState
import com.allen.hearme.feature.smartlistening.model.ListeningTrigger
import com.allen.hearme.feature.smartlistening.model.SmartAction
import kotlinx.coroutines.flow.StateFlow

interface ListeningDecisionEngine {
    fun decide(context: DecisionContext): SmartAction
}

interface ListeningOrchestrator {
    val currentState: StateFlow<ListeningState>
    val isListening: Boolean
    suspend fun startListening(trigger: ListeningTrigger = ListeningTrigger.USER_MANUAL)
    suspend fun stopListening()
}

class DefaultListeningDecisionEngine : ListeningDecisionEngine {
    override fun decide(context: DecisionContext): SmartAction {
        if (!context.isEarphoneConnected) {
            return SmartAction.NO_ACTION
        }

        if (context.hasSpeech && context.isDirected && context.confidence >= 0.65f) {
            return if (context.preferPause) {
                SmartAction.PAUSE_MUSIC
            } else {
                SmartAction.LOWER_VOLUME
            }
        }

        return SmartAction.NO_ACTION
    }
}
