package com.template.flows


import co.paralleluniverse.fibers.Suspendable
import com.google.common.collect.ImmutableSet
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.template.contracts.RequestContract
import com.template.contracts.TicketContract
import com.template.states.RequestState
import com.template.states.VenueState
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap

import java.util.*
import kotlin.collections.HashMap
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.SendStateAndRefFlow
import net.corda.core.node.StatesToRecord
import net.corda.core.utilities.UntrustworthyData


// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class RequestTicket(val seatId:String, val agency:Party, val money: Amount<Currency>): FlowLogic<Unit>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): Unit {

        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))
        val priceToken = Amount(money.quantity, FiatCurrency.getInstance(money.token.currencyCode))
        val inputsAndOutputs: Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> =
            DatabaseTokenSelection(serviceHub).generateMove(listOf(Pair(agency, priceToken)), ourIdentity)


        val moneySend = RequestState(ourIdentity, agency, seatId)
        val sendSession = initiateFlow(agency)
        subFlow(SendStateAndRefFlow(sendSession, inputsAndOutputs.first))
        sendSession.send(inputsAndOutputs.second)
        sendSession.send(moneySend)
        class SignTxFlow(otherPartyFlow: FlowSession) : SignTransactionFlow(otherPartyFlow) {

            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) {
            }
        }
        val issuer = sendSession.receive<Party>().unwrap{it ->it} as Party
        val signFlow = SignTxFlow(sendSession)

        val txId = subFlow(signFlow).id
        val getFlow = subFlow(ReceiveFinalityFlow(sendSession, txId))
        subFlow(ReportManually(getFlow, issuer))
    }


}


    @InitiatedBy(RequestTicket::class)
    class RequestTicketResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {

            val holderStockStates = subFlow(ReceiveStateAndRefFlow<FungibleToken>(counterpartySession))
            val moneyReceived: List<FungibleToken> =
                counterpartySession.receive<List<FungibleToken>>().unwrap { it -> it }
            val request = counterpartySession.receive<RequestState>().unwrap { it -> it }


            val venueStateAndRef = serviceHub.vaultService.queryBy<VenueState>().states.filter{it.state.data.venueId == request.seatId }.single()
            val venueInfo = venueStateAndRef.state.data
            if(venueInfo.soldOut < venueInfo.maxSeat ) {
                val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))

                val signers = (request.participants).map { it.owningKey }
                val txCommand = Command(TicketContract.Commands.Create(), signers)

                val txBuilder = TransactionBuilder(notary)
                    .addOutputState(request, RequestContract.ID)
                    .addCommand(txCommand)

                addMoveTokens(txBuilder, holderStockStates, moneyReceived)
                val ptx = serviceHub.signInitialTransaction(txBuilder, ourIdentity.owningKey);

                val sessions = ImmutableSet.of(counterpartySession);
                counterpartySession.send(venueInfo.issuer)
                val stx = subFlow(CollectSignaturesFlow(ptx, sessions));

                return subFlow(FinalityFlow(stx, sessions))
            }else{
                throw FlowException("All has been booked")
            }

        }
    }


@InitiatingFlow
class ReportManually(val signedTransaction: SignedTransaction, val regulator: Party) : FlowLogic<Unit>() {


    @Suspendable
    override fun call() {
        val session = initiateFlow(regulator)
        session.send(signedTransaction)
    }
}

@InitiatedBy(ReportManually::class)
class ReportManuallyResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signedTransaction = counterpartySession.receive<SignedTransaction>().unwrap { it }
        // The national regulator records all of the transaction's states using
        // `recordTransactions` with the `ALL_VISIBLE` flag.
        serviceHub.recordTransactions(StatesToRecord.ALL_VISIBLE, listOf(signedTransaction))
    }
}


