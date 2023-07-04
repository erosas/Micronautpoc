package com.bytestream.controller

import com.bytestream.db.Account
import com.bytestream.db.AccountRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.kotest.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*


@MicronautTest(rollback = false, transactional = false)
class AccountControllerTest(
    @Client("/") private val client: HttpClient,
    @Inject private val accountRepository: AccountRepository
) : StringSpec() {

    private val july4Instant: Instant = Instant.parse("2021-07-04T00:00:00.00Z")
    private val july4 = OffsetDateTime.ofInstant(july4Instant, ZoneOffset.UTC)
    private val july5 = OffsetDateTime.ofInstant(july4Instant.plus(1, ChronoUnit.DAYS),ZoneOffset.UTC)
    //this is an override so that we have a consistent time for testing
    @Singleton
    @MockBean(Clock::class)
    fun fixedClock(): Clock {
        return Clock.fixed(july4Instant, ZoneOffset.UTC)
    }

    init {

        "test account creation no collected date provided" {
            val request = HttpRequest.POST(
                "/account", AccountCreateRequest(
                    consumerId = 1L,
                    productId = 1L,
                    name = "Test Account",
                    depositAcct = "12345678",
                    collectedDate = null,
                    denied = null,
                )
            )
            val response: HttpResponse<Account> =
                client.toBlocking().exchange(request, Argument.of(Account::class.java), Argument.of(String::class.java))
            response.status shouldBe HttpStatus.CREATED
            response.body() shouldNotBe null
            response.body()!!.also {
                println(it)
                it.id shouldNotBe null
                it.consumerId shouldBe 1L
                it.productId shouldBe 1L
                it.name shouldBe "Test Account"
                it.depositAcct shouldBe "12345678"
                it.collectedDate shouldBe july4
                it.denied shouldBe null
            }

        }

        "test account creation with collected date provided" {
            val request = HttpRequest.POST(
                "/account", AccountCreateRequest(
                    consumerId = 1L,
                    productId = 1L,
                    name = "Test Account",
                    depositAcct = "12345678",
                    collectedDate = july5,
                    denied = null,
                )
            )
            val response: HttpResponse<Account> =
                client.toBlocking().exchange(request, Argument.of(Account::class.java), Argument.of(String::class.java))
            response.status shouldBe HttpStatus.CREATED
            response.body() shouldNotBe null
            response.body()!!.also {
                println(it)
                it.id shouldNotBe null
                it.consumerId shouldBe 1L
                it.productId shouldBe 1L
                it.name shouldBe "Test Account"
                it.depositAcct shouldBe "12345678"
                it.collectedDate shouldBe july5
                it.denied shouldBe null
            }

        }



        "test account retrieval" {
            val savedAccount = accountRepository.save(
                Account(
                    id = null,
                    consumerId = 1L,
                    productId = 1L,
                    name = "Test Account Retrieval",
                    depositAcct = "54321",
                    collectedDate = null,
                    denied = july4
                )
            )

            println(savedAccount)
            val accountIdToSearch = savedAccount.id!!
            val request = HttpRequest.GET<Account>("/account/$accountIdToSearch")
            val response = client.toBlocking()
                .exchange(request, Argument.of(Account::class.java), Argument.of(String::class.java))
            response.status shouldBe HttpStatus.OK
            response.body() shouldNotBe null
            response.body()!!.also {
                println(it)
                it.id shouldBe accountIdToSearch
                it.consumerId shouldBe 1L
                it.productId shouldBe 1L
                it.name shouldBe "Test Account Retrieval"
                it.depositAcct shouldBe "54321"
                it.collectedDate shouldBe null //this is null since the default clock is only used when saved through the controller
                it.denied shouldBe july4
            }

        }

        "test account retrieval with no account found English" {
            val request = HttpRequest.GET<Account>("/account/999999999")
            shouldThrow<HttpClientResponseException> {
                client.toBlocking()
                    .exchange(request, Argument.of(Account::class.java), Argument.of(String::class.java))
            }.also { e ->
                val response = e.response
                e.message shouldBe """{"error":"Account not found"}"""
                response.status shouldBe HttpStatus.NOT_FOUND
            }
        }

        "test account retrieval with no account found Spanish" {
            val request = HttpRequest.GET<Account>("/account/999999999")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "es") //set the language to spanish
            shouldThrow<HttpClientResponseException> {
                client.toBlocking()
                    .exchange(request, Argument.of(Account::class.java), Argument.of(String::class.java))
            }.also { e ->
                val response = e.response
                e.message shouldBe """{"error":"La cuenta no fue encontrada"}"""
                response.status shouldBe HttpStatus.NOT_FOUND
            }
        }

        "test account update" {
            val initialAccount = accountRepository.save(
                Account(
                    id = null,
                    consumerId = 2L,
                    productId = 3L,
                    name = "Updated Account",
                    depositAcct = "87654321",
                    collectedDate = null,
                    denied = null,
                )
            )
            val accountUpdateRequest = AccountUpdateRequest(
                id = initialAccount.id!!,
                consumerId = 2L,
                productId = 3L,
                name = "Updated Account 22",
                depositAcct = "102938947",
                collectedDate = july4,
                denied = null,
            )

            val request = HttpRequest.PUT("/account", accountUpdateRequest)
            val response =
                client.toBlocking().exchange(request, Argument.of(Account::class.java), Argument.of(String::class.java))
            response.status shouldBe HttpStatus.OK
            response.body()!!.also {
                it.id shouldBe initialAccount.id
                it.consumerId shouldBe 2L
                it.productId shouldBe 3L
                it.name shouldBe "Updated Account 22"
                it.depositAcct shouldBe "102938947"
                it.collectedDate shouldBe july4
                it.denied shouldBe null
            }
        }

        "test account update of non-existent account" {
            val updateRequest = AccountUpdateRequest(
                    id = 333,
                    consumerId = 2L,
                    productId = 3L,
                    name = "Updated Account",
                    depositAcct = "87654321",
                    collectedDate = null,
                    denied = null,
                )


            val request = HttpRequest.PUT("/account", updateRequest)
            shouldThrow<HttpClientResponseException> {
                client.toBlocking()
                    .exchange(request, Argument.of(Account::class.java), Argument.of(String::class.java))
            }.also { e ->
                val response = e.response
                e.message shouldBe """{"error":"Account not found"}"""
                response.status shouldBe HttpStatus.NOT_FOUND
            }
        }

        "test account update of account with no id provided" {
            val updateRequest = AccountUpdateRequest(
                id = null,
                consumerId = 2L,
                productId = 3L,
                name = "Updated Account",
                depositAcct = "87654321",
                collectedDate = null,
                denied = null,
            )

            val request = HttpRequest.PUT("/account", updateRequest)
            shouldThrow<HttpClientResponseException> {
                client.toBlocking()
                    .exchange(request, Argument.of(Account::class.java), Argument.of(String::class.java))
            }.also { e ->
                val response = e.response
                e.message shouldBe """{"error":"accountUpdateRequest.id: must not be null"}"""
                response.status shouldBe HttpStatus.BAD_REQUEST
            }
        }

        "test account update of account with no id provided Spanish" {
            val updateRequest = AccountUpdateRequest(
                id = null,
                consumerId = 2L,
                productId = 3L,
                name = "Updated Account",
                depositAcct = "87654321",
                collectedDate = null,
                denied = null,
            )

            val request = HttpRequest.PUT("/account", updateRequest)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "es") //set the language to spanish

            shouldThrow<HttpClientResponseException> {
                client.toBlocking()
                    .exchange(request, Argument.of(Account::class.java), Argument.of(String::class.java))
            }.also { e ->
                val response = e.response
                e.message shouldBe """{"error":"accountUpdateRequest.id: no debe ser nulo"}"""
                response.status shouldBe HttpStatus.BAD_REQUEST
            }
        }


        "test invalid account creation" {
            val invalidRequest = HttpRequest.POST(
                "/account", AccountCreateRequest(
                    consumerId = 1L,
                    productId = 1L,
                    name = "Test Account",
                    depositAcct = "", // Invalid deposit account
                    collectedDate = null,
                    denied = null,
                )
            )

            shouldThrow<HttpClientResponseException> {
                client.toBlocking()
                    .exchange(invalidRequest, Argument.of(Account::class.java), Argument.of(String::class.java))
            }.also { e ->
                val response = e.response
                e.message shouldBe """{"error":"createRequest.depositAcct: must not be blank"}"""
                response.status shouldBe HttpStatus.BAD_REQUEST
            }
        }

    }//end init, all test cases need to be in init block
}