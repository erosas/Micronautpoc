package com.bytestream.config

import io.micronaut.context.MessageSource
import io.micronaut.context.annotation.Factory
import io.micronaut.context.i18n.ResourceBundleMessageSource
import jakarta.inject.Singleton
import java.util.*

/**
 * Created a new wrapper for a bundled resource so that we can let micronaut handle
 * the application message source. It will wrap it into a LocalizedMessageSource
 * per request which automatically handles the locale. This is nice because one does not need to
 * write controllers that have Request as the parameter in order to extract the locale or having to parse
 * the Accept-Language header in every method. By creating an unrelated class it simplifies development
 * because you do not need to add qualifiers to disambiguate between the two message sources.
 */
class ValidationMessageSource(private val bundle:ResourceBundleMessageSource) {
    fun getMessage(messageTemplate: String, locale: Locale): String {
      return bundle.getMessage(messageTemplate, locale).get()
    }

}
@Factory
class MessageSourceFactory {

    @Singleton
    fun providesAppMessageSource(): MessageSource {
        return ResourceBundleMessageSource("i18n.messages" )
    }

    @Singleton
    fun providesValidationMessageSource(): ValidationMessageSource {
        return ValidationMessageSource(ResourceBundleMessageSource("validation.ValidationMessages"))
    }
}