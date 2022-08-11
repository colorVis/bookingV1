package com.template.flows


import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.template.contracts.RequestContract
import com.template.contracts.TicketContract
import com.template.contracts.VenueContract
import com.template.states.RequestState
import com.template.states.TicketState
import com.template.states.VenueState
import net.corda.core.contracts.*
import net.corda.core.contracts.UniqueIdentifier.Companion.fromString
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.Builder
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.getField
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

import java.util.*

// *********
// * Flows *
// *********
//@StartableByService
@StartableByRPC
@InitiatingFlow
class IssueTicket(val requestId: String) : FlowLogic<SignedTransaction>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        // get updated request
        val inputRequest = QueryCriteria.LinearStateQueryCriteria()
            .withRelevancyStatus(Vault.RelevancyStatus.NOT_RELEVANT)
            .withUuid(listOf(UUID.fromString(requestId)))
            .withStatus(Vault.StateStatus.UNCONSUMED)

        val requestStateAndRef = serviceHub.vaultService.queryBy<RequestState>(inputRequest).states.single()
        val requestInfo =  requestStateAndRef.state.data


        // get the buyer's info from request
        val buyer= requestInfo.requester

        // get the venue Information by searching the booked venueID
        val venueStateAndRef = serviceHub.vaultService.queryBy<VenueState>().states.filter{it.state.data.venueId == requestInfo.seatId }.single()

        val venueInfo = venueStateAndRef.state.data
        val venue = venueInfo.issuer

        // add the venue information and buyer to the ticket state
        val seatState = TicketState(venue,requestInfo.receiver,buyer,venueInfo.imgUrl,venueInfo.getStartTime(),venueInfo.getEndTime(),venueInfo.venueId)

        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

        /* Get a reference of own identity */
        val issuer = ourIdentity

        /* Construct the output state */

        /* Create an instance of TransactionState using the ticketState token and the notary */
        val transactionState = seatState withNotary notary!!

        /* Create the ticket token. TokenSDK provides the CreateEvolvableTokens flow which could be called to create an evolvable token in the ledger.*/
        subFlow(CreateEvolvableTokens(transactionState))

        /*
        * Create an instance of IssuedTokenType, it is used by our Non-Fungible token which would be issued to the owner. Note that the IssuedTokenType takes
        * a TokenPointer as an input, since EvolvableTokenType is not TokenType, but is a LinearState. This is done to separate the state info from the token
        * so that the state can evolve independently.
        * IssuedTokenType is a wrapper around the TokenType and the issuer.
        * */

        val issuedTicketToken = seatState.toPointer(seatState.javaClass) issuedBy issuer

        /* Create an instance of the non-fungible ticket token with the owner as the token holder. The last paramter is a hash of the jar containing the TokenType, use the helper function to fetch it. */
        val ticketToken = NonFungibleToken(issuedTicketToken, buyer, UniqueIdentifier())



        /* Issue the ticket token by calling the IssueTokens flow provided with the TokenSDK */
         subFlow(IssueTokens(listOf(ticketToken)))
        //updateVenueState
        return subFlow(UpdateVenueState(venueStateAndRef))

    }
}

//soldOutSeat+1
@InitiatingFlow
class UpdateVenueState(val venueState: StateAndRef<VenueState>) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val oldVenueState = venueState.state.data
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))
        val newVenue = oldVenueState.book()


        val txBuilder = TransactionBuilder(notary)
            .addInputState(venueState)
            .addOutputState(newVenue, VenueContract.ID )
            .addCommand(Command(VenueContract.Commands.Update(),newVenue.participants.map { it.owningKey}))

        txBuilder.verify(serviceHub)
        val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

        // Send the state to the counterparty, and receive it back with their signature.
        val session = initiateFlow(newVenue.owner)
        val fullySignedTx = subFlow(
            CollectSignaturesFlow(partSignedTx, listOf(session)))

        // Notarise and record the transaction in both parties' vaults.
        return subFlow(FinalityFlow(fullySignedTx, listOf(session)))
    }
}

@InitiatedBy(UpdateVenueState::class)
class UpdateVenueStateResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                //Addition checks
            }
        }
        val txId = subFlow(signTransactionFlow).id
        return subFlow(ReceiveFinalityFlow(counterpartySession, expectedTxId = txId))
    }
}

