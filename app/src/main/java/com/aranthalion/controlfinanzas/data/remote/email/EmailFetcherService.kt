package com.aranthalion.controlfinanzas.data.remote.email

import android.util.Log
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import java.util.*
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton
import javax.mail.*
import javax.mail.internet.MimeMultipart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class EmailFetcherService @Inject constructor(
    private val configuracionPreferences: ConfiguracionPreferences
) {
    suspend fun testConnection(config: EmailConfig): Boolean = withContext(Dispatchers.IO) {
        val properties = Properties()
        properties["mail.store.protocol"] = config.protocol
        
        if (config.useSSL) {
            properties["mail.imap.ssl.enable"] = "true"
            properties["mail.imap.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
            properties["mail.imap.socketFactory.fallback"] = "false"
            properties["mail.imap.socketFactory.port"] = config.port.toString()
        }

        var store: Store? = null
        try {
            val session = Session.getDefaultInstance(properties, null)
            store = session.getStore(config.protocol)
            store.connect(config.host, config.port, config.username, config.password)
            return@withContext true
        } catch (e: Exception) {
            Log.e("EmailFetcherService", "Connection test failed", e)
            return@withContext false
        } finally {
            try {
                store?.close()
            } catch (ignored: Exception) {}
        }
    }

    suspend fun fetchTransactionsFromEmail(): List<MovimientoEntity> = withContext(Dispatchers.IO) {
        val config = configuracionPreferences.obtenerEmailConfig()
        val movements = mutableListOf<MovimientoEntity>()
        
        val properties = Properties()
        properties["mail.store.protocol"] = config.protocol
        
        if (config.useSSL) {
            properties["mail.imap.ssl.enable"] = "true"
            properties["mail.imap.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
            properties["mail.imap.socketFactory.fallback"] = "false"
            properties["mail.imap.socketFactory.port"] = config.port.toString()
        }

        var store: Store? = null
        var inbox: Folder? = null
        try {
            val session = Session.getDefaultInstance(properties, null)
            store = session.getStore(config.protocol)
            store.connect(config.host, config.port, config.username, config.password)
            
            inbox = store.getFolder("INBOX")
            inbox.open(Folder.READ_ONLY)
            
            val messageCount = inbox.messageCount
            val limit = if (messageCount > 30) messageCount - 30 else 1
            
            val messages = inbox.getMessages(limit, messageCount)
            Log.d("EmailFetcherService", "Leídos ${messages.size} correos de la bandeja de entrada")
            
            for (message in messages) {
                try {
                    val subject = message.subject ?: ""
                    val body = getTextFromMessage(message)
                    
                    // Solo parsear si parece ser una notificación bancaria o financiera
                    if (esCorreoBancario(subject, body)) {
                        val movimiento = parsearCorreoAMovimiento(subject, body, message.sentDate ?: Date())
                        if (movimiento != null) {
                            movements.add(movimiento)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("EmailFetcherService", "Error al procesar mensaje individual", e)
                }
            }
        } catch (e: Exception) {
            Log.e("EmailFetcherService", "Error general al descargar correos", e)
        } finally {
            try {
                inbox?.close(false)
            } catch (ignored: Exception) {}
            try {
                store?.close()
            } catch (ignored: Exception) {}
        }
        
        return@withContext movements
    }

    private fun esCorreoBancario(subject: String, body: String): Boolean {
        val palabrasClave = listOf(
            "transferencia", "compra", "cargo", "abono", "pago", "notificacion",
            "tarjeta", "banco", "santander", "bci", "estado", "chile", "debito", "credito"
        )
        val textToSearch = (subject + " " + body).lowercase()
        return palabrasClave.any { textToSearch.contains(it) }
    }

    private fun stripHtml(html: String): String {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                android.text.Html.fromHtml(html, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
            } else {
                @Suppress("DEPRECATION")
                android.text.Html.fromHtml(html).toString()
            }
        } catch (e: Exception) {
            // Fallback for unit tests or when android.text.Html is unavailable
            html.replace(Regex("<[^>]*>"), " ")
                .replace("&nbsp;", " ")
                .replace("&aacute;", "á")
                .replace("&eacute;", "é")
                .replace("&iacute;", "í")
                .replace("&oacute;", "ó")
                .replace("&uacute;", "ú")
                .replace("&ntilde;", "ñ")
                .replace("&Aacute;", "Á")
                .replace("&Eacute;", "É")
                .replace("&Iacute;", "Í")
                .replace("&Oacute;", "Ó")
                .replace("&Uacute;", "Ú")
                .replace("&Ntilde;", "Ñ")
                .replace("&Iquest;", "¿")
                .replace("&iquest;", "¿")
                .replace("&iexcl;", "¡")
        }
    }

    private fun parsearCorreoAMovimiento(subject: String, body: String, sentDate: Date): MovimientoEntity? {
        val plainBody = stripHtml(body)

        // Expresión regular genérica para extraer montos en pesos chilenos o dólares (ej: $15.000, $ 10.000, 5000 CLP)
        val montoPattern = Pattern.compile("(?i)(?:\\$|CLP|USD)\\s*([0-9]{1,3}(?:\\.[0-9]{3})+|[0-9]+)")
        val matcherMonto = montoPattern.matcher(plainBody)
        
        var monto = 0.0
        if (matcherMonto.find()) {
            val montoStr = matcherMonto.group(1)?.replace(".", "") ?: "0"
            monto = montoStr.toDoubleOrNull() ?: 0.0
        }
        
        if (monto <= 0) return null

        // Determinar tipo (GASTO o INGRESO)
        var tipo = "GASTO"
        val textToSearch = (subject + " " + plainBody).lowercase()
        if (textToSearch.contains("recibiste") || textToSearch.contains("abono") || textToSearch.contains("transferencia recibida")) {
            tipo = "INGRESO"
        }

        // Buscar comercio/compañía en el cuerpo
        var comercio = "Transacción Correo"
        
        // Patrón explícito para campo "Comercio" en emails con tablas (como BCI)
        val comercioExplicitPattern = Pattern.compile("(?i)Comercio\\s*\\r?\\n*\\s*([\\p{L}0-9 .,'\\-/]{3,50})")
        val matcherExplicit = comercioExplicitPattern.matcher(plainBody)
        if (matcherExplicit.find()) {
            comercio = matcherExplicit.group(1)?.replace(Regex(" +"), " ")?.trim() ?: comercio
        } else {
            // Patrón genérico de respaldo para otros correos
            val comercioPattern = Pattern.compile("(?i)\\b(?:en|destinatario|establecimiento|a|para)\\s+([\\p{L}0-9 .,'\\-/]{3,50})")
            val matcherComercio = comercioPattern.matcher(plainBody)
            if (matcherComercio.find()) {
                comercio = matcherComercio.group(1)?.replace(Regex(" +"), " ")?.trim() ?: comercio
            }
        }

        // Obtener la fecha y hora de la transacción
        var fechaTransaccion = sentDate
        try {
            val fechaPattern = Pattern.compile("(?i)Fecha\\s*\\r?\\n*\\s*([0-9]{2}/[0-9]{2}/[0-9]{4}|[0-9]{2}-[0-9]{2}-[0-9]{4})")
            val horaPattern = Pattern.compile("(?i)Hora\\s*\\r?\\n*\\s*([0-9]{2}:[0-9]{2})")
            
            val matcherFecha = fechaPattern.matcher(plainBody)
            val matcherHora = horaPattern.matcher(plainBody)
            
            if (matcherFecha.find()) {
                val fechaStr = matcherFecha.group(1)
                var horaStr = "00:00"
                if (matcherHora.find()) {
                    horaStr = matcherHora.group(1) ?: "00:00"
                }
                
                val formatStr = if (fechaStr != null && fechaStr.contains("/")) "dd/MM/yyyy HH:mm" else "dd-MM-yyyy HH:mm"
                val sdf = java.text.SimpleDateFormat(formatStr, Locale.getDefault())
                val parsedDate = if (fechaStr != null) sdf.parse("$fechaStr $horaStr") else null
                if (parsedDate != null) {
                    fechaTransaccion = parsedDate
                }
            }
        } catch (e: Exception) {
            Log.e("EmailFetcherService", "Error parsing transaction date/time from body, using sentDate", e)
        }

        val calendar = Calendar.getInstance()
        calendar.time = fechaTransaccion
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val periodo = String.format("%04d-%02d", year, month)

        return MovimientoEntity(
            tipo = tipo,
            monto = monto,
            descripcion = comercio,
            fecha = fechaTransaccion,
            periodoFacturacion = periodo,
            idUnico = "EMAIL_${fechaTransaccion.time}_${monto.toInt()}"
        )
    }

    @Throws(Exception::class)
    private fun getTextFromMessage(message: Message): String {
        if (message.isMimeType("text/plain")) {
            return message.content.toString()
        } else if (message.isMimeType("text/html")) {
            return message.content.toString()
        } else if (message.isMimeType("multipart/*")) {
            val mimeMultipart = message.content as MimeMultipart
            return getTextFromMimeMultipart(mimeMultipart)
        }
        return ""
    }

    @Throws(Exception::class)
    private fun getTextFromMimeMultipart(mimeMultipart: MimeMultipart): String {
        var result = ""
        val count = mimeMultipart.count
        for (i in 0 until count) {
            val bodyPart = mimeMultipart.getBodyPart(i)
            if (bodyPart.isMimeType("text/plain")) {
                result += bodyPart.content
            } else if (bodyPart.isMimeType("text/html")) {
                val html = bodyPart.content as String
                result += html
            } else if (bodyPart.content is MimeMultipart) {
                result += getTextFromMimeMultipart(bodyPart.content as MimeMultipart)
            }
        }
        return result
    }
}
