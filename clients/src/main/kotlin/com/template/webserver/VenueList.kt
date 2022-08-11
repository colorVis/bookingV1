package com.template.webserver

import net.corda.core.contracts.Amount
import java.util.*

object VenueList{

    fun getVenueA(): List<SampleVenue>{
        return listOf(
            SampleVenue(
                1,
                "Jay Chou's world tour",
                "img/jay.jpg",
                "2022-08-12 19:00",
                "2022-08-12 22:00",
                Amount.parseCurrency("50 GBP"),
                "Buyer",
                30
            ),
            SampleVenue(
                2,
                "Ketty Perry's world tour",
                "img/ketty.jpg",
                "2022-08-13 15:00",
                "2022-08-13 20:00",
                Amount.parseCurrency("40 GBP"),
                "Buyer",
                40
        ),
            SampleVenue(
                3,
                "Coldplay's world tour",
                "img/coldplay.jpg",
                "2022-08-14 15:00",
                "2022-08-14 20:00",
                Amount.parseCurrency("30 GBP"),
                "Buyer",
                50
            )
        )
    }

}

data class SampleVenue(val venueId:Int, val description: String, val imgUrl:String, val startString : String, val endString: String, val price : Amount<Currency>,val buyer:String, val maxSeat: Int) {
}