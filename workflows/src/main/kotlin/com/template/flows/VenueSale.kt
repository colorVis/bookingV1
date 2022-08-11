package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.money.FiatCurrency.Companion.getInstance
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.template.states.VenueState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import org.checkerframework.common.aliasing.qual.Unique
import java.text.NumberFormat
import java.util.*

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class VenueSale(val venueId: String,val description: String, val url: String, val startString: String, val endString:String, val price: Amount<Currency>,val buyer: Party, val maxSeat : Int) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): String {

        val issuer = ourIdentity
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB"))
        /* Construct the output state */
        val uuid = UniqueIdentifier.fromString(UUID.randomUUID().toString())
        val venueState = VenueState(venueId,description,url, issuer, buyer, startString, endString, price,maxSeat,0)

        /* Create an instance of TransactionState using the venueState token and the notary */
        val transactionState = venueState withNotary notary!!

        /* Create the house token. TokenSDK provides the CreateEvolvableTokens flow which could be called to create an evolvable token in the ledger.*/
        subFlow(CreateEvolvableTokens(transactionState))

        /*
        * Create an instance of IssuedTokenType, it is used by our Non-Fungible token which would be issued to the owner. Note that the IssuedTokenType takes
        * a TokenPointer as an input, since EvolvableTokenType is not TokenType, but is a LinearState. This is done to separate the state info from the token
        * so that the state can evolve independently.
        * IssuedTokenType is a wrapper around the TokenType and the issuer.
        * */

        val issuedHouseToken = venueState.toPointer(venueState.javaClass) issuedBy issuer

        /* Create an instance of the non-fungible house token with the owner as the token holder. The last paramter is a hash of the jar containing the TokenType, use the helper function to fetch it. */
        val venueToken = NonFungibleToken(issuedHouseToken, issuer, UniqueIdentifier())

        /* Issue the house token by calling the IssueTokens flow provided with the TokenSDK */
        val stx1 = subFlow(IssueTokens(listOf(venueToken)))
        // Obtain a reference from a notary we wish to use.


        /* Build the transaction builder */
        val txBuilder = TransactionBuilder(notary)

        /* Create a move token proposal for the venue token using the helper function provided by Token SDK. This would create the movement proposal and would
         * be committed in the ledgers of parties once the transaction in finalized.
        **/
        addMoveNonFungibleTokens(txBuilder, serviceHub,venueState.toPointer(venueState.javaClass), buyer)

        /* Initiate a flow session with the buyer to send the venue valuation and transfer of the fiat currency */
        val buyerSession = initiateFlow(buyer)

        buyerSession.send(price*maxSeat)

        // Recieve inputStatesAndRef for the fiat currency exchange from the buyer, these would be inputs to the fiat currency exchange transaction.
        val inputs = subFlow(ReceiveStateAndRefFlow<FungibleToken>(buyerSession))

        // Recieve output for the fiat currency from the buyer, this would contain the transfered amount from buyer to yourself
        val moneyReceived: List<FungibleToken> = buyerSession.receive<List<FungibleToken>>().unwrap { it -> it}

        /* Create a fiat currency proposal for the venue token using the helper function provided by Token SDK. */
        addMoveTokens(txBuilder, inputs, moneyReceived)

        /* Sign the transaction with your private */
        val initialSignedTrnx = serviceHub.signInitialTransaction(txBuilder)

        /* Call the CollectSignaturesFlow to recieve signature of the buyer */
        val ftx= subFlow(CollectSignaturesFlow(initialSignedTrnx, listOf(buyerSession)))

        /* Call finality flow to notarise the transaction */
        val stx = subFlow(FinalityFlow(ftx, listOf(buyerSession)))

        /* Distribution list is a list of identities that should receive updates. For this mechanism to behave correctly we call the UpdateDistributionListFlow flow */
        subFlow(UpdateDistributionListFlow(stx))

        return ("\nThe venue is sold to " + buyer.name.organisation + "\nTransaction ID: "
                + stx.id)
    }
}

@InitiatedBy(VenueSale::class)
class VenueSaleResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call():SignedTransaction {
        /* Recieve the valuation of the venue */
        val price = counterpartySession.receive<Amount<Currency>>().unwrap { it }

        /* Create instance of the fiat currecy token amount */
        val priceToken = Amount(price.quantity/2, getInstance(price.token.currencyCode))

        /*
        *  Generate the move proposal, it returns the input-output pair for the fiat currency transfer, which we need to send to the Initiator.
        * */

        val inputsAndOutputs : Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> =
            DatabaseTokenSelection(serviceHub).generateMove(listOf(Pair(counterpartySession.counterparty,priceToken)),ourIdentity)


        /* Call SendStateAndRefFlow to send the inputs to the Initiator*/
        subFlow(SendStateAndRefFlow(counterpartySession, inputsAndOutputs.first))
        /* Send the output generated from the fiat currency move proposal to the initiator */
        counterpartySession.send(inputsAndOutputs.second)

        //signing
        subFlow(object : SignTransactionFlow(counterpartySession) {
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) { // Custom Logic to validate transaction.
            }
        })
        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}