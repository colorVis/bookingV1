package com.template.states

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.template.contracts.SeatContract
import com.template.contracts.VenueContract
import net.corda.core.contracts.*
import net.corda.core.flows.FlowLogicRefFactory
import net.corda.core.identity.Party
import net.corda.core.serialization.ConstructorForDeserialization

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@BelongsToContract(VenueContract::class)
class VenueState (
    val venueId: String,
    val description: String,
    val imgUrl: String,
    val issuer: Party,
    var owner: Party,
    val startString : String,
    val endString: String,
    val price : Amount<Currency>,
    val maxSeat: Int,
    val soldOut: Int,
    override val linearId: UniqueIdentifier = UniqueIdentifier(),
    override val fractionDigits: Int = 0,
    override val participants : List<Party> = listOf(issuer,owner)
) : ContractState,EvolvableTokenType(){
    override val maintainers: List<Party> get() = listOf(issuer)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    val start  = LocalDateTime.parse(startString, formatter)
    val end = LocalDateTime.parse(endString,formatter)
    fun getStartTime(): LocalDateTime{return start}
    fun getEndTime(): LocalDateTime{return end}
    fun book(): VenueState{return VenueState(venueId, description,imgUrl,issuer,owner, startString,endString, price, maxSeat, soldOut+1,linearId) }


}