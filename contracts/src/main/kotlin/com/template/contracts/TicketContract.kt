package com.template.contracts

import com.template.states.TicketState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.requireThat

// ************
// * Contract *
// ************
class TicketContract : Contract {

    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.TicketContract"
     }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.

        val commandData = tx.commands[0].value
        when(commandData){
            is Commands.Create -> requireThat {
                val output = tx.outputsOfType(TicketState::class.java)
                "The ticket must have one output".using(output.size ==1)

            }
            is Commands.Transfer -> requireThat {
                val input = tx.inputsOfType(TicketState::class.java)[0]
                val output = tx.outputsOfType(TicketState::class.java)[0]
                "Can't give to self".using(input.issuer != output.buyer)
                "The ticket must have one output".using(tx.outputs.size ==1)

            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Create:Commands
        class Transfer: Commands

    }
}