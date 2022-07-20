package com.template

import com.template.flows.IssueCurrency
import com.template.states.TicketState
import groovy.util.GroovyTestCase.assertEquals
import net.corda.core.contracts.Amount
import net.corda.core.contracts.FungibleState
import net.corda.core.identity.CordaX500Name
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import sun.security.krb5.internal.Ticket
import java.util.*
import java.util.concurrent.Future

class FlowTests {
    private lateinit var network: MockNetwork
    private lateinit var bank: StartedMockNode
    private lateinit var agency: StartedMockNode
    private lateinit var venue: StartedMockNode


    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
            TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows")
        ), networkParameters = testNetworkParameters(minimumPlatformVersion = 4),
            notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
        ))
        bank = network.createPartyNode(CordaX500Name("Bank", "London", "GB"))
        agency = network.createNode(CordaX500Name("Agency", "London", "GB"))
        venue = network.createNode(CordaX500Name("Venue", "London", "GB"))
        network.runNetwork()
    }

    @After
    fun tearDown() = network!!.stopNodes()

    @Test
    fun testTransactionFlow() {
        val issueCurrency = IssueCurrency( "GBP", 100, agency.info.legalIdentities[0])
        bank!!.startFlow(issueCurrency)
        network!!.runNetwork()

        val agencyAccount = agency!!.services.vaultService.queryBy(FungibleState::class.java).states

        println(agencyAccount.toString())
        val vaultMoney= agencyAccount.get(0).state.data.amount
        assertEquals(vaultMoney.quantity,Amount.parseCurrency("1 GBP").quantity)

    }
}
