package org.comon.streamlauncher.network.converter

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import nl.adaptivity.xmlutil.serialization.XML
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

@OptIn(ExperimentalSerializationApi::class)
class XmlConverterFactory private constructor(
    private val xml: XML
) : Converter.Factory() {

    companion object {
        fun create(): XmlConverterFactory = XmlConverterFactory(XML {})
        fun create(xml: XML): XmlConverterFactory = XmlConverterFactory(xml)
    }

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        val serializer = try {
            @Suppress("UNCHECKED_CAST")
            serializer(type) as KSerializer<Any>
        } catch (e: SerializationException) {
            return null
        }
        return XmlResponseBodyConverter(xml, serializer)
    }
}

private class XmlResponseBodyConverter<T : Any>(
    private val xml: XML,
    private val serializer: KSerializer<T>
) : Converter<ResponseBody, T> {
    override fun convert(value: ResponseBody): T {
        return xml.decodeFromString(serializer, value.string())
    }
}
