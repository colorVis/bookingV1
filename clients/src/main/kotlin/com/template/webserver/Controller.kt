package com.template.webserver

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.template.flows.*
import com.template.states.RequestState
import com.template.states.TicketState
import com.template.states.VenueState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.messaging.CordaRPCOps
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.*
import java.util.*

/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/api/booking") // The paths for HTTP requests are relative to this base path.
class Controller() {
    @Autowired lateinit var BankProxy: CordaRPCOps

    @Autowired lateinit var VenueProxy: CordaRPCOps

    @Autowired lateinit var BuyerProxy: CordaRPCOps

    @Autowired lateinit var AgencyProxy: CordaRPCOps


    @Autowired
    @Qualifier("BankProxy")  lateinit var proxy: CordaRPCOps
    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }


    @GetMapping(value = "/templateendpoint", produces = arrayOf("text/plain"))
    private fun templateendpoint(): String {
        return "Define an endpoint here."
    }

    @PostMapping(value = ["issueMoney"])
    fun issueMoney(@RequestBody issueMoneyForm: Forms.IssueMoneyForm): APIResponse<String> {
        return try {
            proxy.startFlowDynamic(
                IssueCurrency::class.java,
                issueMoneyForm.currencyType,
                issueMoneyForm.amount,
                proxy.partiesFromName(issueMoneyForm.party!!, false).iterator().next()
            ).returnValue.get()
            APIResponse.success("Money issued to ${issueMoneyForm.party}. Amount: ${issueMoneyForm.amount}")
        } catch (e: Exception) {
            handleError(e)
        }
    }
    @PostMapping(value = "issueTicket")
    fun issueTicket(@RequestBody issueForm:Forms.SearchRequest): APIResponse<String>{
        return try{
            proxy.startFlowDynamic(
                IssueTicket::class.java,
                issueForm.requestId
            ).returnValue.get()
            return APIResponse.success("Ticket issued")
        }catch (e: Exception){
          handleError(e)
        }
    }
    @PostMapping(value ="createVenue")
    fun createVenue(@RequestBody venueInfo:Forms.VenueInfo) : APIResponse<String>{
        return try{
            proxy.startFlowDynamic(
                VenueSale::class.java,
                venueInfo.venueId,
                venueInfo.description,
                venueInfo.imgUrl,
                venueInfo.startTime,
                venueInfo.endTime,
                Amount.parseCurrency("${venueInfo.price} GBP"),
                proxy.partiesFromName(venueInfo.buyer!!, false).iterator().next(),
                venueInfo.maxSeat
            ).returnValue.get()
            APIResponse.success("${venueInfo.venueId} is issued")
        }catch (e: Exception){
            handleError(e)
        }
    }




    private fun createSample(proxy:CordaRPCOps, dataSet: List<SampleVenue>){
        dataSet.forEach {
            proxy.startFlowDynamic(VenueSale::class.java, it.venueId, it.description, it.imgUrl,it.startString,it.endString,
                Amount.parseCurrency("${it.price} GBP"), proxy.partiesFromName(it.buyer!!, false).iterator().next(),
                it.maxSeat)
        }
    }

    @PostMapping(value ="getBalance", produces = [ APPLICATION_JSON_VALUE ])
    fun getBalance(@RequestBody party: String): APIResponse<List<StateAndRef<FungibleToken>>>{
        when (party) {
            "Bank"-> proxy = BankProxy
            "Venue"-> proxy = VenueProxy
            "Buyer"-> proxy = BuyerProxy
            "Agency"-> proxy = AgencyProxy
            else -> return APIResponse.error("Unrecognised Party")
        }
        return APIResponse.success(proxy.vaultQuery(FungibleToken::class.java).states)
    }



    @GetMapping(value=["getVenueState"], produces = [APPLICATION_JSON_VALUE])
    fun getVenueState() : APIResponse<List<StateAndRef<VenueState>>>{
        return APIResponse.success(proxy.vaultQuery(VenueState::class.java).states)
    }
    @GetMapping(value=["getTicketState"], produces = [APPLICATION_JSON_VALUE])
    fun getTicketState() : APIResponse<List<StateAndRef<TicketState>>>{
        return APIResponse.success(proxy.vaultQuery(TicketState::class.java).states)
    }
    @GetMapping(value=["getRequestState"], produces = [APPLICATION_JSON_VALUE])
    fun getRequestState() : APIResponse<List<StateAndRef<RequestState>>>{
        return APIResponse.success(proxy.vaultQuery(RequestState::class.java).states)
    }

    @GetMapping(value = ["setup"])
    fun setupDemoData(): APIResponse<String> {

        val dataSetA = VenueList.getVenueA()
        createSample(VenueProxy,dataSetA)
        return APIResponse.success("Setup success")
    }

    @PostMapping(value = "requestTicket")
    fun requestTicket(@RequestBody requestForm:Forms.RequestForm) :APIResponse<String>{
        return try{
            proxy.startFlowDynamic(
                RequestTicket::class.java,
                requestForm.venueId,
                proxy.partiesFromName(requestForm.agency!!, false).iterator().next(),
                Amount.parseCurrency("${requestForm.price} GBP")
            ).returnValue.get()
            APIResponse.success("${requestForm.agency} wants to buy the ${requestForm.venueId}")
        }catch (e: Exception){
            handleError(e)
        }
    }

    private fun handleError(e: Exception): APIResponse<String> {
        logger.error("RequestError", e)
        return when (e) {
            is TransactionVerificationException.ContractRejection ->
                APIResponse.error(e.cause?.message ?: e.message!!)
            else ->
                APIResponse.error(e.message!!)
        }
    }



}