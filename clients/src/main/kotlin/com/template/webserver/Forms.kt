package com.template.webserver

class Forms {


    class IssueMoneyForm {
        var currencyType: String? = "GBP"
        var amount : Long? = 0
        var party: String? = null

    }

    class VenueInfo {
        var venueId: String ? = null
        var description: String ?= null
        var imgUrl: String? = null
        var startTime: String? = null
        var endTime: String? = null
        var price: String ?= null
        var buyer:String ?= null
        var maxSeat: Int ?= 0
    }

    class BuyVenue {
        var venueId: String ?= null
        var buyer: String ?= null
    }


    class SearchRequest {
        var requestId: String? = null
    }

    class RequestForm{
        var venueId: String ?= null
        var agency: String? = null
        var price: String?= null
    }

}