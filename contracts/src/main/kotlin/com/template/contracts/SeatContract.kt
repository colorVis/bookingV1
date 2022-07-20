package com.template.contracts

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import com.template.states.VenueState
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import java.time.LocalDateTime

/**
 * This doesn't do anything over and above the [EvolvableTokenContract].
 */
class SeatContract : EvolvableTokenContract(), Contract {
    override fun additionalCreateChecks(tx: LedgerTransaction) {
        val outputState = tx.getOutput(0) as VenueState
        val currentTime = LocalDateTime.now()
        outputState.apply {
            require(outputState.price.quantity >= 0) {"Valuation cannot be less than zero"}
            require(outputState.getStartTime().isAfter(currentTime)){"StartTime should be later than now"}
            require(outputState.getEndTime().isAfter(outputState.getStartTime())){"EndTime should be later than StartTime"}

        }
    }
    override fun additionalUpdateChecks(tx: LedgerTransaction) {
        val outputState = tx.getOutput(0) as VenueState
        require(outputState.price.quantity >= 0) {"Valuation cannot be less than zero"}
    }
}