package com.template.contracts

import com.template.states.VenueState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.requireThat

// ************
// * Contract *
// ************
class VenueContract : Contract {

    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.VenueContract"
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.

        val commandData = tx.commands[0].value
        when(commandData){
            is Commands.Create -> requireThat {
                val output = tx.outputsOfType(VenueState::class.java)[0]
                "The ticket must have one output".using(tx.outputs.size ==2)

            }
            is Commands.Update -> requireThat{
                val output = tx.outputsOfType(VenueState::class.java)[0]
//                "The ticket must have one output".using(tx.outputs.size ==1)
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create:Commands
        class Update:Commands
    }
}