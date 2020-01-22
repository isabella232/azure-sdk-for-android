package com.azure.android.core.implementation.util.serializer.threeten;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.util.ClassUtil;

import org.threeten.bp.DateTimeUtils;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeFormatterBuilder;

import java.io.IOException;
import java.util.Locale;

@SuppressWarnings("serial")
abstract class ThreeTenDateTimeDeserializerBase<T>
    extends ThreeTenDeserializerBase<T>
    implements ContextualDeserializer {
    protected final DateTimeFormatter _formatter;

    /**
     * Flag that indicates what leniency setting is enabled for this deserializer (either
     * due {@link JsonFormat} annotation on property or class, or due to per-type
     * "config override", or from global settings): leniency/strictness has effect
     * on accepting some non-default input value representations (such as integer values
     * for dates).
     *<p>
     * Note that global default setting is for leniency to be enabled, for Jackson 2.x,
     * and has to be explicitly change to force strict handling: this is to keep backwards
     * compatibility with earlier versions.
     *
     * @since 2.10
     */
    protected final boolean _isLenient;

    protected ThreeTenDateTimeDeserializerBase(Class<T> supportedType, DateTimeFormatter f) {
        super(supportedType);
        _formatter = f;
        _isLenient = true;
    }

    /**
     * @since 2.10
     */
    protected ThreeTenDateTimeDeserializerBase(ThreeTenDateTimeDeserializerBase<T> base,
                                               DateTimeFormatter f) {
        super(base);
        _formatter = f;
        _isLenient = base._isLenient;
    }

    /**
     * @since 2.10
     */
    protected ThreeTenDateTimeDeserializerBase(ThreeTenDateTimeDeserializerBase<T> base,
                                               Boolean leniency) {
        super(base);
        _formatter = base._formatter;
        _isLenient = !Boolean.FALSE.equals(leniency);
    }

    protected abstract ThreeTenDateTimeDeserializerBase<T> withDateFormat(DateTimeFormatter dtf);

    /**
     * @since 2.10
     */
    protected abstract ThreeTenDateTimeDeserializerBase<T> withLeniency(Boolean leniency);

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt,
                                                BeanProperty property) throws JsonMappingException {
        JsonFormat.Value format = findFormatOverrides(ctxt, property, handledType());
        ThreeTenDateTimeDeserializerBase<?> deser = this;
        if (format != null) {
            if (format.hasPattern()) {
                final String pattern = format.getPattern();
                final Locale locale = format.hasLocale() ? format.getLocale() : ctxt.getLocale();
                DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
//                if (acceptCaseInsensitiveValues(ctxt, format)) {
                    builder.parseCaseInsensitive();
//                }
                builder.appendPattern(pattern);
                DateTimeFormatter df;
                if (locale == null) {
                    df = builder.toFormatter();
                } else {
                    df = builder.toFormatter(locale);
                }
                //Issue #69: For instant serializers/deserializers we need to configure the formatter with
                //a time zone picked up from JsonFormat annotation, otherwise serialization might not work
                if (format.hasTimeZone()) {
                    df = df.withZone(DateTimeUtils.toZoneId(format.getTimeZone()));
                }
                deser = deser.withDateFormat(df);
            }
            // 17-Aug-2019, tatu: For 2.10 let's start considering leniency/strictness too
            if (format.hasLenient()) {
                Boolean leniency = format.getLenient();
                if (leniency != null) {
                    deser = deser.withLeniency(leniency);
                }
            }
            // any use for TimeZone?
        }
        return deser;
    }

    /**
     * @return {@code true} if lenient handling is enabled; {code false} if not (strict mode)
     *
     * @since 2.10
     */
    protected boolean isLenient() {
        return _isLenient;
    }

//    private boolean acceptCaseInsensitiveValues(DeserializationContext ctxt, JsonFormat.Value format)
//    {
//        Boolean enabled = format.getFeature( Feature.ACCEPT_CASE_INSENSITIVE_VALUES);
//        if( enabled == null) {
//            enabled = ctxt.isEnabled(MapperFeature.ACCEPT_CASE_INSENSITIVE_VALUES);
//        }
//        return enabled;
//    }

    protected void _throwNoNumericTimestampNeedTimeZone(JsonParser p, DeserializationContext ctxt)
        throws IOException {
        ctxt.reportInputMismatch(handledType(),
            "raw timestamp (%d) not allowed for `%s`: need additional information such as an offset or time-zone (see class Javadocs)",
            p.getNumberValue(), handledType().getName());
    }

    @SuppressWarnings("unchecked")
    protected T _failForNotLenient(JsonParser p, DeserializationContext ctxt,
                                   JsonToken expToken) throws IOException {
        return (T) ctxt.handleUnexpectedToken(handledType(), expToken, p,
            "Cannot deserialize instance of %s out of %s token: not allowed because 'strict' mode set for property or type (enable 'lenient' handling to allow)",
            ClassUtil.nameOf(handledType()), p.currentToken());
    }
}
