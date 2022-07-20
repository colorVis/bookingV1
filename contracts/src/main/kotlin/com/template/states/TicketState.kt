package com.template.states


import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.template.contracts.TicketContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.ConstructorForDeserialization
import net.corda.core.serialization.CordaSerializable
import java.time.LocalDateTime
import java.util.*
@CordaSerializable
@BelongsToContract(TicketContract::class)
class TicketState(
    val issuer: Party,
    val agency: Party,
    var buyer: Party,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val venueId: String,
    override val linearId: UniqueIdentifier = UniqueIdentifier(),
    override val fractionDigits: Int = 0,
    override val maintainers: List<Party> = listOf(issuer,agency, buyer)
) : LinearState, EvolvableTokenType() {
    override val participants: List<AbstractParty> get() = listOf(buyer,issuer)

}