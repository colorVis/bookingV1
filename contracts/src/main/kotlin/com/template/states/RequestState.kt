package com.template.states


import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.template.contracts.RequestContract
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
@BelongsToContract(RequestContract::class)
class RequestState(
    val requester: Party,
    val receiver: Party,
    val seatId: String,
    override val linearId: UniqueIdentifier = UniqueIdentifier()

) : LinearState {
    override val participants: List<AbstractParty> get() = listOf(requester,receiver)

}