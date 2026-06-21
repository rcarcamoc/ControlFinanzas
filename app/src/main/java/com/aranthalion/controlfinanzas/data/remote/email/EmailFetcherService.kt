package com.aranthalion.controlfinanzas.data.remote.email

import android.util.Log
import com.aranthalion.controlfinanzas.data.local.ConfiguracionPreferences
import com.aranthalion.controlfinanzas.data.local.entity.MovimientoEntity
import java.util.*
import java.util.regex.Pattern
import javax.mail.Flags
import javax.inject.Inject
import javax.inject.Singleton
import javax.mail.*
import javax.mail.internet.MimeMultipart
import javax.mail.search.ComparisonTerm
import javax.mail.search.FlagTerm
import javax.mail.search.SentDateTerm
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
            if (config.protocol.startsWith("pop3")) {
                properties["mail.pop3.ssl.enable"] = "true"
                properties["mail.pop3.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
                properties["mail.pop3.socketFactory.fallback"] = "false"
                properties["mail.pop3.socketFactory.port"] = config.port.toString()
                
                properties["mail.pop3s.ssl.enable"] = "true"
                properties["mail.pop3s.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
                properties["mail.pop3s.socketFactory.fallback"] = "false"
                properties["mail.pop3s.socketFactory.port"] = config.port.toString()
            } else {
                properties["mail.imap.ssl.enable"] = "true"
                properties["mail.imap.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
                properties["mail.imap.socketFactory.fallback"] = "false"
                properties["mail.imap.socketFactory.port"] = config.port.toString()
                
                properties["mail.imaps.ssl.enable"] = "true"
                properties["mail.imaps.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
                properties["mail.imaps.socketFactory.fallback"] = "false"
                properties["mail.imaps.socketFactory.port"] = config.port.toString()
            }
        }

        var store: Store? = null
        try {
            val usernameToUse = if (config.protocol.startsWith("pop3") && 
                                   config.host.contains("gmail.com", ignoreCase = true) && 
                                   !config.username.startsWith("recent:", ignoreCase = true)) {
                "recent:${config.username}"
            } else {
                config.username
            }
            val session = Session.getInstance(properties, null)
            store = session.getStore(config.protocol)
            store.connect(config.host, config.port, usernameToUse, config.password)
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
        val configs = configuracionPreferences.obtenerEmailConfigs().filter { it.enabled }
        val movements = mutableListOf<MovimientoEntity>()
        
        for (config in configs) {
            val properties = Properties()
            properties["mail.store.protocol"] = config.protocol
            
            if (config.useSSL) {
                if (config.protocol.startsWith("pop3")) {
                    properties["mail.pop3.ssl.enable"] = "true"
                    properties["mail.pop3.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
                    properties["mail.pop3.socketFactory.fallback"] = "false"
                    properties["mail.pop3.socketFactory.port"] = config.port.toString()
                    
                    properties["mail.pop3s.ssl.enable"] = "true"
                    properties["mail.pop3s.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
                    properties["mail.pop3s.socketFactory.fallback"] = "false"
                    properties["mail.pop3s.socketFactory.port"] = config.port.toString()
                } else {
                    properties["mail.imap.ssl.enable"] = "true"
                    properties["mail.imap.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
                    properties["mail.imap.socketFactory.fallback"] = "false"
                    properties["mail.imap.socketFactory.port"] = config.port.toString()
                    
                    properties["mail.imaps.ssl.enable"] = "true"
                    properties["mail.imaps.socketFactory.class"] = "javax.net.ssl.SSLSocketFactory"
                    properties["mail.imaps.socketFactory.fallback"] = "false"
                    properties["mail.imaps.socketFactory.port"] = config.port.toString()
                }
            }

            var store: Store? = null
            var inbox: Folder? = null
            try {
                val usernameToUse = if (config.protocol.startsWith("pop3") && 
                                       config.host.contains("gmail.com", ignoreCase = true) && 
                                       !config.username.startsWith("recent:", ignoreCase = true)) {
                    "recent:${config.username}"
                } else {
                    config.username
                }
                val session = Session.getInstance(properties, null)
                store = session.getStore(config.protocol)
                store.connect(config.host, config.port, usernameToUse, config.password)
                
                inbox = store.getFolder("INBOX")
                inbox.open(Folder.READ_WRITE)
                
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val thresholdDate = calendar.time

                val messages = if (config.protocol.startsWith("imap")) {
                    Log.d("EmailFetcherService", "Buscando correos no leídos por IMAP desde: $thresholdDate")
                    val dateTerm = SentDateTerm(ComparisonTerm.GE, thresholdDate)
                    val unseenTerm = FlagTerm(Flags(Flags.Flag.SEEN), false)
                    val combinedTerm = javax.mail.search.AndTerm(unseenTerm, dateTerm)
                    val result = inbox.search(combinedTerm)
                    Log.d("EmailFetcherService", "IMAP search (no leídos + fecha) devolvió ${result.size} correos")
                    if (result.isEmpty()) {
                        // Fallback: solo buscar por no leídos sin filtro de fecha
                        val unseenOnly = inbox.search(FlagTerm(Flags(Flags.Flag.SEEN), false))
                        Log.d("EmailFetcherService", "IMAP search (solo no leídos) devolvió ${unseenOnly.size} correos")
                        unseenOnly
                    } else {
                        result
                    }
                } else {
                    // POP3: iterate backwards from the newest messages
                    val messageCount = inbox.messageCount
                    Log.d("EmailFetcherService", "Bandeja POP3 tiene un total de $messageCount correos")
                    
                    // Log the last 10 messages for debugging
                    val startDebug = messageCount
                    val endDebug = if (messageCount > 10) messageCount - 9 else 1
                    for (debugIdx in startDebug downTo endDebug) {
                        try {
                            val msg = inbox.getMessage(debugIdx)
                            Log.d("EmailFetcherService", "Debug POP3 - Índice: $debugIdx, Asunto: ${msg.subject}, Fecha: ${msg.sentDate}")
                        } catch (e: Exception) {
                            Log.e("EmailFetcherService", "Error leyendo debug msg $debugIdx", e)
                        }
                    }

                    val messagesList = mutableListOf<Message>()
                    var olderCount = 0
                    for (i in messageCount downTo 1) {
                        try {
                            val msg = inbox.getMessage(i)
                            val sentDate = msg.sentDate
                            if (sentDate != null) {
                                if (sentDate.before(thresholdDate)) {
                                    olderCount++
                                    // Stop scanning once we see a few messages older than 30 days
                                    if (olderCount >= 3) {
                                        Log.d("EmailFetcherService", "POP3: Deteniendo escaneo en índice $i porque detectamos $olderCount mensajes antiguos")
                                        break
                                    }
                                } else {
                                    messagesList.add(msg)
                                }
                            } else {
                                messagesList.add(msg)
                            }
                        } catch (e: Exception) {
                            Log.e("EmailFetcherService", "Error reading headers for message index $i", e)
                        }
                        if (messagesList.size >= 100) {
                            break
                        }
                    }
                    messagesList.toTypedArray()
                }

                Log.d("EmailFetcherService", "Leídos ${messages.size} correos de la bandeja de entrada para ${config.username}")
                
                for (message in messages) {
                    try {
                        val sentDate = message.sentDate ?: Date()
                        if (sentDate.before(thresholdDate)) {
                            continue
                        }
                        val subject = message.subject ?: ""
                        val body = getTextFromMessage(message)
                        
                        if (esCorreoBancario(subject, body)) {
                            val movimiento = parsearCorreoAMovimiento(subject, body, sentDate)
                            if (movimiento != null) {
                                if (movements.none { it.idUnico == movimiento.idUnico }) {
                                    movements.add(movimiento)
                                }
                            }
                        }

                        // Marcar correo como leído después de procesarlo
                        try {
                            message.setFlag(Flags.Flag.SEEN, true)
                            Log.d("EmailFetcherService", "Correo marcado como leído: $subject")
                        } catch (flagEx: Exception) {
                            Log.w("EmailFetcherService", "No se pudo marcar como leído: ${flagEx.message}")
                        }
                    } catch (e: Exception) {
                        Log.e("EmailFetcherService", "Error al procesar mensaje individual", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("EmailFetcherService", "Error general al descargar correos para ${config.username}", e)
            } finally {
                try {
                    inbox?.close(true) // true = expunge + persiste cambios de flags (leído/no leído)
                } catch (ignored: Exception) {}
                try {
                    store?.close()
                } catch (ignored: Exception) {}
            }
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

        // Limpiar de la palabra "Cuotas" si aparece
        val idxCuotas = comercio.indexOf("cuotas", ignoreCase = true)
        if (idxCuotas != -1) {
            comercio = comercio.substring(0, idxCuotas).trim()
        }

        // Buscar número de tarjeta de crédito
        var tarjeta: String? = null
        val tarjetaPattern = Pattern.compile("(?i)(?:Número tarjeta crédito|Tarjeta)\\s*\\r?\\n*\\s*([\\*0-9]{4,10})")
        val matcherTarjeta = tarjetaPattern.matcher(plainBody)
        if (matcherTarjeta.find()) {
            tarjeta = matcherTarjeta.group(1)?.trim()
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

        val customPeriodConfigs = configuracionPreferences.obtenerPeriodoDatesMap()
        val periodo = com.aranthalion.controlfinanzas.data.util.BillingPeriodHelper.obtenerPeriodoParaFecha(fechaTransaccion, customPeriodConfigs)

        return MovimientoEntity(
            tipo = tipo,
            monto = monto,
            descripcion = comercio,
            fecha = fechaTransaccion,
            periodoFacturacion = periodo,
            tipoTarjeta = tarjeta,
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
