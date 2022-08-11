package com.template.webserver

import com.fasterxml.jackson.module.kotlin.KotlinModule
import net.corda.client.jackson.JacksonSupport
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
open class AppConfig : WebMvcConfigurer{

    @Value("\${Bank.host}")
    lateinit var BankProxy: String

    @Value("\${Venue.host}")
    lateinit var VenueProxy: String

    @Value("\${Agency.host}")
    lateinit var AgencyProxy: String

    @Value("\${Buyer.host}")
    lateinit var BuyerProxy: String


    @Bean(destroyMethod = "")
    open fun BankProxy(): CordaRPCOps {
        val bankClient = CordaRPCClient(NetworkHostAndPort.parse(BankProxy))
        return bankClient.start("user1", "test").proxy
    }

    @Bean(destroyMethod = "")
    open fun VenueProxy(): CordaRPCOps {
        val venueClient = CordaRPCClient(NetworkHostAndPort.parse(VenueProxy))
        return venueClient.start("user1", "test").proxy
    }

    @Bean(destroyMethod = "")
    open fun AgencyProxy(): CordaRPCOps {
        val agencyClient = CordaRPCClient(NetworkHostAndPort.parse(AgencyProxy))
        return agencyClient.start("user1", "test").proxy
    }

    @Bean(destroyMethod = "")
    open fun BuyerProxy(): CordaRPCOps {
        val buyerClient = CordaRPCClient(NetworkHostAndPort.parse(BuyerProxy))
        return buyerClient.start("user1", "test").proxy
    }

    /**
     * Corda Jackson Support, to convert corda objects to json
     */
    @Bean
    open fun mappingJackson2HttpMessageConverter(): MappingJackson2HttpMessageConverter {
        val mapper = JacksonSupport.createDefaultMapper(BankProxy())
        mapper.registerModule(KotlinModule())
        val converter = MappingJackson2HttpMessageConverter()
        converter.objectMapper = mapper
        return converter
    }
}