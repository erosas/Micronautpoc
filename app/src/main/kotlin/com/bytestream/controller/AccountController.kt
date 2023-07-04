package com.bytestream.controller

import com.bytestream.db.Account
import com.bytestream.db.AccountRepository
import io.micronaut.context.LocalizedMessageSource
import io.micronaut.core.annotation.Introspected
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.inject.Inject
import java.time.Clock
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import javax.transaction.Transactional
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

/**
 * This is the request body for creating or updating an account
 * It is important to keep all fields nullable and let the validator do its job otherwise
 * parsing the body from the HTTP request will fail and the validation will never happen.
 * This will mask the real error from the user's perspective if we do not allow nulls.
 */
@Introspected
data class AccountCreateRequest(
    @field:NotNull val consumerId: Long?,
    @field:NotNull val productId: Long?,
    val name: String?,
    @field:NotBlank val depositAcct: String?,
    val collectedDate: OffsetDateTime ?,
    val denied: OffsetDateTime?,
)

@Introspected
data class AccountUpdateRequest(
    //can specify a different message for each validation annotation if we want, uses the default javax messages if not specified.
    @field:NotNull val id: Long?,
    @field:NotNull val consumerId: Long?,
    @field:NotNull val productId: Long?,
    val name: String?,
    @field:NotBlank val depositAcct: String?,
    val collectedDate: OffsetDateTime ?,
    val denied: OffsetDateTime?
)


@ExecuteOn(TaskExecutors.IO)
@Controller("/account")
open class AccountController(
    @Inject private val clock: Clock,
    @Inject private val accountRepository: AccountRepository,
    @Inject private val messageSource: LocalizedMessageSource
) {

    @ApiResponses(
        ApiResponse(
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = Account::class)
            )]
        ),
        ApiResponse(responseCode = "404", description = "Account not found")
    )
    @Tag(name = "Account")
    @Get("/{accountId}", produces = ["application/json"])
    fun getAccount(accountId: Long): Account {
        val account = accountRepository.findById(accountId)
        //Can force a very subtle bug by not calling get on the optional to show the value of test cases
        return account.orElseThrow { HttpStatusException(HttpStatus.NOT_FOUND, messageSource.getMessage("account.notfound").get()) }
    }


    @ApiResponses(
        ApiResponse(
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = Account::class)
            )]
        ),
        ApiResponse(responseCode = "400", description = "Invalid request")
    )
    @Tag(name = "Account")
    @Post("/", consumes = ["application/json"], produces = ["application/json"])
    open fun createAccount(@Body @Valid createRequest: AccountCreateRequest): HttpResponse<Account> {
        val account = Account(
            id = null,
            consumerId = createRequest.consumerId,
            productId = createRequest.productId,
            depositAcct = createRequest.depositAcct,
            //TODO: What should the timezone be? I hope UTC
            collectedDate = createRequest.collectedDate ?: OffsetDateTime.ofInstant(clock.instant(),ZoneOffset.UTC),
            denied = createRequest.denied,
            name = createRequest.name
        )
        //TODO: the old code checks for the existence of the consumer but it is skipped on the update
        return HttpResponse.created(accountRepository.save(account))
    }


    @ApiResponses(
        ApiResponse(
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = Account::class)
            )]
        ),
        ApiResponse(responseCode = "400", description = "Invalid request")
    )
    @Tag(name = "Account")
    @Put("/", consumes = ["application/json"], produces = ["application/json"])
    @Transactional
    open fun updateAccount(@Body @Valid accountUpdateRequest: AccountUpdateRequest): HttpResponse<Account> {
        val account = this.getAccount(accountUpdateRequest.id!!)
        account.also {
            it.collectedDate = accountUpdateRequest.collectedDate
            it.denied = accountUpdateRequest.denied
            it.depositAcct = accountUpdateRequest.depositAcct
            it.name = accountUpdateRequest.name
            it.productId = accountUpdateRequest.productId
            it.consumerId = accountUpdateRequest.consumerId
        }
        return HttpResponse.ok(accountRepository.update(account))
    }


}