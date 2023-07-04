package com.bytestream.db

import io.micronaut.data.annotation.GeneratedValue
import io.micronaut.data.annotation.Id
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.naming.NamingStrategies.LowerCase
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import java.time.OffsetDateTime
import javax.validation.constraints.NotNull


@MappedEntity(alias = "account", namingStrategy = LowerCase::class)
data class Account(
    //@field:NotNull //validation annotation
    @field:Id
    @field:GeneratedValue
    var id:Long?,
    var consumerId:Long?,
    var productId:Long?,
    var name:String?,
    var depositAcct:String?,
    var collectedDate:OffsetDateTime?,
    var denied:OffsetDateTime?,
)
@JdbcRepository(dialect = Dialect.POSTGRES)
interface AccountRepository:CrudRepository<Account,Long> {
}