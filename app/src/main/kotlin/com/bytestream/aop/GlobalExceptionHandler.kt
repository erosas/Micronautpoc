package com.bytestream.aop

import com.bytestream.config.ValidationMessageSource
import io.micronaut.context.LocalizedMessageSource
import io.micronaut.context.annotation.Requires
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.exceptions.ExceptionHandler
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Error
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Inject
import java.util.*
import javax.validation.ConstraintViolation
import javax.validation.ConstraintViolationException
import javax.validation.ElementKind
import javax.validation.Path

@Introspected
data class GlobalErrorCaught(val error: String)

@Controller
@Requires(classes = [Exception::class, ExceptionHandler::class])
class GlobalExceptionHandler(
    @Inject private val validationMessageSource: ValidationMessageSource,
    @Inject private val applicationMessageSource: LocalizedMessageSource) {


    fun getUnknownErrorMessage(request: HttpRequest<*>): String {
        return applicationMessageSource.getMessage("unknown.error", request.locale.orElseGet {Locale.US}).get()
    }
    @Error(global = true, exception = Throwable::class)
    fun error(request: HttpRequest<*>, e: Throwable): HttpResponse<GlobalErrorCaught> {
        val error = GlobalErrorCaught(e.localizedMessage ?: getUnknownErrorMessage(request))
        return HttpResponse.serverError(error)
    }


    @Error(global = true, exception = ConstraintViolationException::class)
    fun error(request: HttpRequest<*>, e: ConstraintViolationException): HttpResponse<GlobalErrorCaught> {
        val errors = e.constraintViolations.map { violation ->
            buildMessage(violation, request.locale.orElseGet { Locale.US })
        }
        val error = GlobalErrorCaught(errors.joinToString(", "))
        return HttpResponse.status<GlobalErrorCaught?>(HttpStatus.BAD_REQUEST).body(error)
    }
    protected fun buildMessage(violation: ConstraintViolation<*>, locale: Locale): String? {
        val propertyPath = violation.propertyPath
        val message = StringBuilder()
        val i: Iterator<Path.Node> = propertyPath.iterator()
        while (i.hasNext()) {
            val node = i.next()
            if (node.kind == ElementKind.METHOD || node.kind == ElementKind.CONSTRUCTOR) {
                continue
            }
            message.append(node.name)
            if (node.index != null) {
                message.append(String.format("[%d]", node.index))
            }
            if (i.hasNext()) {
                message.append('.')
            }
        }
        val messageKey = violation.messageTemplate.removeSurrounding("{","}")

        message.append(": ").append(validationMessageSource.getMessage(messageKey, locale))
        return message.toString()
    }

    @Error(global = true, exception = HttpStatusException::class)
    fun error(request: HttpRequest<*>, e: HttpStatusException): HttpResponse<GlobalErrorCaught> {
        val error = GlobalErrorCaught(e.localizedMessage ?: getUnknownErrorMessage(request))
        return HttpResponse.status<GlobalErrorCaught?>(e.status).body(error)
    }

}
