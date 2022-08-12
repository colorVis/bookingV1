package com.template.flows

import com.template.states.RequestState
import net.corda.core.contracts.StateAndRef
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.AppServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.node.services.Vault
import net.corda.core.node.services.trackBy
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.loggerFor
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@CordaService
class AutoTicketRequest(private val serviceHub: AppServiceHub) : SingletonSerializeAsToken() {

    private companion object {
        val log = loggerFor<AutoTicketRequest>()
        val load = System.getenv("enable-my-service")
        val executor: Executor = Executors.newFixedThreadPool(8)!!
    }

    init {
        directPayment()
        log.info("Tracking new Payment Request")
    }

    private fun directPayment() {
        val ourIdentity = ourIdentity()

        if (ourIdentity == serviceHub.networkMapCache.getPeerByLegalName(CordaX500Name("Venue", "London", "GB"))!!) {
            serviceHub.vaultService.trackBy<RequestState>().updates.subscribe {
                    update: Vault.Update<RequestState> -> update.produced.forEach{
                    message: StateAndRef<RequestState> ->
                val state = message.state as RequestState
                executor.execute {
                    log.info("Directing to message $state")
                    serviceHub.startFlow(IssueTicket(state.linearId.toString()))
                }
            }
            }
        }
    }
    private fun ourIdentity(): Party = serviceHub.myInfo.legalIdentities.first()
}