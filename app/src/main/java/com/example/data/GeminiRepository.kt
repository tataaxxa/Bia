package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.device.ActionResult
import com.example.device.DeviceActionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val audioBase64: String? = null,
    val audioMimeType: String? = null,
    val actionName: String? = null,
    val actionDetails: String? = null,
    val actionSuccess: Boolean? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    ARUSHI, // Used for J.A.R.V.I.S. assistant responses
    SYSTEM_ACTION
}

sealed class AssistantResponse {
    data class VoiceText(
        val text: String,
        val audioBase64: String?,
        val audioMimeType: String?,
        val detectedLanguage: String = "pt-BR"
    ) : AssistantResponse()

    data class ActionExecuted(
        val actionName: String,
        val details: String,
        val success: Boolean,
        val followUpResponse: VoiceText? = null
    ) : AssistantResponse()

    data class MultipleContactsPrompt(
        val nameQuery: String,
        val contacts: List<String>,
        val questionText: String
    ) : AssistantResponse()

    data class Error(val message: String) : AssistantResponse()
}

class GeminiRepository(
    private val context: Context,
    private val actionManager: DeviceActionManager
) {
    private val tag = "GeminiRepository"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Multi-turn conversation contents array
    private val conversationHistory = JSONArray()

    var activeVoiceName: String = "Fenrir" // JARVIS style deep voice

    val hasApiKey: Boolean
        get() = try {
            BuildConfig.GEMINI_API_KEY.isNotBlank() &&
                    BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"
        } catch (e: Throwable) {
            false
        }

    fun clearHistory() {
        while (conversationHistory.length() > 0) {
            conversationHistory.remove(0)
        }
    }

    /**
     * Sends user prompt to Gemini Live and returns response, executing function calls if requested.
     */
    suspend fun processUserTurn(userPrompt: String): AssistantResponse = withContext(Dispatchers.IO) {
        val trimmedPrompt = userPrompt.trim()
        if (trimmedPrompt.isBlank()) {
            return@withContext AssistantResponse.Error("Comando de voz vazio.")
        }

        // Add user turn to conversation history
        val userContent = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", trimmedPrompt) })
            })
        }
        conversationHistory.put(userContent)

        if (!hasApiKey) {
            return@withContext processLocalSimulation(trimmedPrompt)
        }

        // Call Gemini Live API
        try {
            callGeminiWithTools(trimmedPrompt)
        } catch (e: Exception) {
            Log.e(tag, "Gemini API error, falling back to local simulation", e)
            processLocalSimulation(trimmedPrompt)
        }
    }

    private suspend fun callGeminiWithTools(originalPrompt: String): AssistantResponse {
        val apiKey = BuildConfig.GEMINI_API_KEY

        val modelsToTry = listOf(
            "gemini-2.5-flash-native-audio-preview-12-2025",
            "gemini-2.5-flash-preview-tts",
            "gemini-3.5-flash"
        )

        for (model in modelsToTry) {
            try {
                val requestPayload = buildRequestPayload(model)
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

                val request = Request.Builder()
                    .url(url)
                    .post(requestPayload.toString().toRequestBody(jsonMediaType))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Log.w(tag, "Model $model returned error ${response.code}: $responseBody")
                    continue
                }

                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates") ?: continue
                if (candidates.length() == 0) continue

                val candidate = candidates.getJSONObject(0)
                val content = candidate.optJSONObject("content") ?: continue
                val parts = content.optJSONArray("parts") ?: continue

                var functionCallObj: JSONObject? = null
                var textPart: String? = null
                var audioBase64: String? = null
                var audioMimeType: String? = null

                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    if (part.has("functionCall")) {
                        functionCallObj = part.getJSONObject("functionCall")
                    }
                    if (part.has("text")) {
                        textPart = part.getString("text")
                    }
                    if (part.has("inlineData")) {
                        val inline = part.getJSONObject("inlineData")
                        audioBase64 = inline.optString("data")
                        audioMimeType = inline.optString("mimeType", "audio/pcm;rate=24000")
                    }
                }

                if (functionCallObj != null) {
                    conversationHistory.put(content)

                    val functionName = functionCallObj.getString("name")
                    val functionArgs = functionCallObj.optJSONObject("args") ?: JSONObject()
                    val toolResult = executeTool(functionName, functionArgs)

                    val functionResponsePart = JSONObject().apply {
                        put("functionResponse", JSONObject().apply {
                            put("name", functionName)
                            put("response", toolResult.toJson())
                        })
                    }

                    val functionContent = JSONObject().apply {
                        put("role", "function")
                        put("parts", JSONArray().apply { put(functionResponsePart) })
                    }
                    conversationHistory.put(functionContent)

                    val followUpPayload = buildRequestPayload(model)
                    val followUpRequest = Request.Builder()
                        .url(url)
                        .post(followUpPayload.toString().toRequestBody(jsonMediaType))
                        .build()

                    val followUpResponse = okHttpClient.newCall(followUpRequest).execute()
                    val followUpBody = followUpResponse.body?.string() ?: ""

                    var followUpText = toolResult.message
                    var followUpAudio: String? = null
                    var followUpMime: String? = null

                    if (followUpResponse.isSuccessful) {
                        val fuJson = JSONObject(followUpBody)
                        val fuParts = fuJson.optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")

                        if (fuParts != null) {
                            for (j in 0 until fuParts.length()) {
                                val p = fuParts.getJSONObject(j)
                                if (p.has("text")) followUpText = p.getString("text")
                                if (p.has("inlineData")) {
                                    val inl = p.getJSONObject("inlineData")
                                    followUpAudio = inl.optString("data")
                                    followUpMime = inl.optString("mimeType")
                                }
                            }
                        }
                    }

                    if (toolResult is ToolExecutionResult.MultipleContactsFound) {
                        return AssistantResponse.MultipleContactsPrompt(
                            nameQuery = toolResult.nameQuery,
                            contacts = toolResult.contacts,
                            questionText = followUpText
                        )
                    }

                    return AssistantResponse.ActionExecuted(
                        actionName = functionName,
                        details = toolResult.message,
                        success = toolResult.success,
                        followUpResponse = AssistantResponse.VoiceText(
                            text = followUpText,
                            audioBase64 = followUpAudio,
                            audioMimeType = followUpMime
                        )
                    )
                }

                if (textPart != null || audioBase64 != null) {
                    conversationHistory.put(content)
                    return AssistantResponse.VoiceText(
                        text = textPart ?: "",
                        audioBase64 = audioBase64,
                        audioMimeType = audioMimeType ?: "audio/pcm;rate=24000"
                    )
                }
            } catch (e: Exception) {
                Log.w(tag, "Failed calling $model", e)
            }
        }

        return processLocalSimulation(originalPrompt)
    }

    private fun buildRequestPayload(modelName: String): JSONObject {
        return JSONObject().apply {
            put("contents", conversationHistory)

            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", SYSTEM_PROMPT)
                    })
                })
            })

            put("tools", JSONArray().apply {
                put(buildToolsObject())
            })

            val generationConfig = JSONObject().apply {
                put("temperature", 0.6)
                if (modelName.contains("native-audio") || modelName.contains("tts")) {
                    put("responseModalities", JSONArray().apply {
                        put("AUDIO")
                        put("TEXT")
                    })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", activeVoiceName)
                            })
                        })
                    })
                }
            }
            put("generationConfig", generationConfig)
        }
    }

    private fun buildToolsObject(): JSONObject {
        val functionDeclarations = JSONArray().apply {
            // 1. openWhatsApp
            put(JSONObject().apply {
                put("name", "openWhatsApp")
                put("description", "Abre o aplicativo WhatsApp no dispositivo Android.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject())
                })
            })

            // 2. openApp
            put(JSONObject().apply {
                put("name", "openApp")
                put("description", "Abre aplicativos instalados no Android (YouTube, Prisma 3D, Godot, Instagram, Chrome, Configurações, Câmera, etc).")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("appName", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Nome do aplicativo a ser aberto (ex: 'YouTube', 'Prisma 3D', 'Godot', 'Chrome', 'Settings')")
                        })
                    })
                    put("required", JSONArray().apply { put("appName") })
                })
            })

            // 3. openUrl
            put(JSONObject().apply {
                put("name", "openUrl")
                put("description", "Abre um link ou endereço web no navegador padrão do Android.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("url", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "URL do site")
                        })
                    })
                    put("required", JSONArray().apply { put("url") })
                })
            })

            // 4. makeCall
            put(JSONObject().apply {
                put("name", "makeCall")
                put("description", "Efetua uma ligação para um número de telefone informado.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("phoneNumber", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Número telefônico para discagem")
                        })
                    })
                    put("required", JSONArray().apply { put("phoneNumber") })
                })
            })

            // 5. callContact
            put(JSONObject().apply {
                put("name", "callContact")
                put("description", "Procura um contato pelo nome na agenda e inicia a ligação telefônica.")
                put("parameters", JSONObject().apply {
                    put("type", "OBJECT")
                    put("properties", JSONObject().apply {
                        put("contactName", JSONObject().apply {
                            put("type", "STRING")
                            put("description", "Nome do contato para ligar (ex: 'Mãe', 'Rahul', 'Pai')")
                        })
                    })
                    put("required", JSONArray().apply { put("contactName") })
                })
            })
        }

        return JSONObject().apply {
            put("functionDeclarations", functionDeclarations)
        }
    }

    private fun executeTool(name: String, args: JSONObject): ToolExecutionResult {
        return when (name) {
            "openWhatsApp" -> {
                val res = actionManager.openWhatsApp()
                ToolExecutionResult.Generic(res.success, res.message)
            }
            "openApp" -> {
                val appName = args.optString("appName", "")
                val res = actionManager.openApp(appName)
                ToolExecutionResult.Generic(res.success, res.message)
            }
            "openUrl" -> {
                val url = args.optString("url", "")
                val res = actionManager.openUrl(url)
                ToolExecutionResult.Generic(res.success, res.message)
            }
            "makeCall" -> {
                val number = args.optString("phoneNumber", "")
                val res = actionManager.makeCall(number)
                ToolExecutionResult.Generic(res.success, res.message)
            }
            "callContact" -> {
                val contactName = args.optString("contactName", "")
                val res = actionManager.callContact(contactName)
                when (res) {
                    is ActionResult.Success -> ToolExecutionResult.Generic(true, res.message)
                    is ActionResult.MultipleContacts -> {
                        ToolExecutionResult.MultipleContactsFound(
                            nameQuery = contactName,
                            contacts = res.contacts.map { "${it.name} (${it.phoneNumber})" },
                            message = "Encontrei ${res.contacts.size} contatos para '$contactName': ${res.contacts.joinToString { it.name }}."
                        )
                    }
                    is ActionResult.ContactNotFound -> {
                        ToolExecutionResult.Generic(false, "Nenhum contato encontrado com o nome '$contactName'.")
                    }
                    is ActionResult.Failure -> {
                        ToolExecutionResult.Generic(false, res.error)
                    }
                }
            }
            else -> ToolExecutionResult.Generic(false, "Protocolo desconhecido: $name")
        }
    }

    /**
     * Local simulation in 100% Brazilian Portuguese with the J.A.R.V.I.S. persona.
     */
    private fun processLocalSimulation(prompt: String): AssistantResponse {
        val lower = prompt.lowercase().trim()

        // WhatsApp Command
        if (lower.contains("whatsapp")) {
            val res = actionManager.openWhatsApp()
            val spokenMessage = "Às suas ordens, senhor. Inicializando o WhatsApp para você imediatamente."
            return AssistantResponse.ActionExecuted(
                actionName = "openWhatsApp",
                details = res.message,
                success = res.success,
                followUpResponse = AssistantResponse.VoiceText(spokenMessage, null, null)
            )
        }

        // Phone Calls & Contacts
        if (lower.contains("call") || lower.contains("liga") || lower.contains("ligar") || lower.contains("chamar") || lower.contains("discar")) {
            val digits = prompt.filter { it.isDigit() }
            if (digits.length >= 7) {
                val res = actionManager.makeCall(digits)
                val voice = "Iniciando chamada para o número $digits, senhor."
                return AssistantResponse.ActionExecuted(
                    actionName = "makeCall",
                    details = res.message,
                    success = res.success,
                    followUpResponse = AssistantResponse.VoiceText(voice, null, null)
                )
            }

            val contactName = extractContactName(prompt)
            if (contactName.isNotBlank()) {
                val res = actionManager.callContact(contactName)
                when (res) {
                    is ActionResult.Success -> {
                        val voice = "Estabelecendo comunicação com $contactName, senhor."
                        return AssistantResponse.ActionExecuted(
                            actionName = "callContact",
                            details = res.message,
                            success = true,
                            followUpResponse = AssistantResponse.VoiceText(voice, null, null)
                        )
                    }
                    is ActionResult.MultipleContacts -> {
                        val names = res.contacts.joinToString(separator = " e ") { it.name }
                        val question = "Senhor, localizei ${res.contacts.size} registros para '$contactName': $names. Para qual deles devo ligar?"
                        return AssistantResponse.MultipleContactsPrompt(
                            nameQuery = contactName,
                            contacts = res.contacts.map { "${it.name} (${it.phoneNumber} - ${it.type})" },
                            questionText = question
                        )
                    }
                    is ActionResult.ContactNotFound -> {
                        val msg = "Senhor, não localizei nenhum contato com a denominação '$contactName' em sua agenda."
                        return AssistantResponse.VoiceText(msg, null, null)
                    }
                    is ActionResult.Failure -> {
                        return AssistantResponse.VoiceText(res.error, null, null)
                    }
                }
            }
        }

        // Open App Commands
        if (lower.startsWith("open ") || lower.startsWith("abrir ") || lower.startsWith("abre ") || lower.startsWith("iniciar ") || lower.contains("inicia ")) {
            val app = when {
                lower.contains("prisma") -> "Prisma 3D"
                lower.contains("godot") -> "Godot Engine"
                lower.contains("youtube") -> "YouTube"
                lower.contains("instagram") -> "Instagram"
                lower.contains("chrome") || lower.contains("navegador") -> "Chrome"
                lower.contains("setting") || lower.contains("configura") || lower.contains("ajustes") -> "Settings"
                lower.contains("camera") || lower.contains("câmera") -> "Camera"
                lower.contains("calculator") || lower.contains("calculadora") -> "Calculator"
                lower.contains("clock") || lower.contains("alarme") || lower.contains("relogio") || lower.contains("relógio") -> "Clock"
                lower.contains("sketchfab") || lower.contains("3d model") || lower.contains("modelagem 3d") -> "Sketchfab"
                lower.contains("tripo") -> "Tripo3D"
                else -> prompt.replace("open", "", ignoreCase = true)
                    .replace("abrir", "", ignoreCase = true)
                    .replace("abre", "", ignoreCase = true)
                    .replace("iniciar", "", ignoreCase = true)
                    .replace("inicia", "", ignoreCase = true)
                    .trim()
            }
            val res = actionManager.openApp(app)
            val voice = "Protocolo executado. Inicializando o aplicativo $app para o senhor."
            return AssistantResponse.ActionExecuted(
                actionName = "openApp",
                details = res.message,
                success = res.success,
                followUpResponse = AssistantResponse.VoiceText(voice, null, null)
            )
        }

        // 3D Game History Question ("em que ano deu os 3D para jogos / quando surgiu 3D")
        if ((lower.contains("ano") || lower.contains("quando") || lower.contains("historia") || lower.contains("história")) && (lower.contains("3d") || lower.contains("tres d")) && (lower.contains("jogo") || lower.contains("game"))) {
            val historyResponse = "Senhor, o 3D nos jogos começou a se desenvolver no início dos anos 1990! Os primeiros grandes marcos em polígonos foram Virtua Racing (1992) e Virtua Fighter (1993) da Sega nos arcades, além de Star Fox (1993) no Super Nintendo com o chip Super FX. A verdadeira era de ouro do 3D consolidou-se entre 1994 e 1996 com o Sony PlayStation (1994), Sega Saturn (1994), Quake (1996) e Super Mario 64 (1996) no Nintendo 64!"
            return AssistantResponse.VoiceText(
                text = historyResponse,
                audioBase64 = null,
                audioMimeType = null
            )
        }

        // Game Development / 3D Modeling autonomous creation inquiry (Godot, Prisma 3D, autonomous AI)
        if (lower.contains("godot") || lower.contains("motor") || lower.contains("criar jogo") || lower.contains("modelar") || lower.contains("prisma")) {
            val devResponse = "Com certeza, senhor! Você pode orquestrar todo o pipeline autônomo de jogos: utilize IAs como Tripo3D ou Meshy para gerar modelos 3D automaticamente via prompts, refine a malha e animações no Prisma 3D ou Blender, e importe os ativos em formato .GLTF diretamente para a Godot Engine. Na Godot, posso auxiliá-lo com a lógica em GDScript, configuração de colisores e compilação do seu APK!"
            return AssistantResponse.VoiceText(
                text = devResponse,
                audioBase64 = null,
                audioMimeType = null
            )
        }

        // System Diagnostic / Status inquiry
        if (lower.contains("status") || lower.contains("sistema") || lower.contains("diagnostico") || lower.contains("diagnóstico") || lower.contains("energia")) {
            val statusResponse = "Diagnóstico completo, senhor: Reator Arc operando a 100% de capacidade. Sensores e módulos de telemetria ativos. Memória alocada perfeitamente e canais de comunicação prontos para novos comandos."
            return AssistantResponse.VoiceText(
                text = statusResponse,
                audioBase64 = null,
                audioMimeType = null
            )
        }

        // Greetings
        if (lower.contains("olá") || lower.contains("ola") || lower.contains("bom dia") || lower.contains("boa tarde") || lower.contains("boa noite") || lower.contains("jarvis") || lower.contains("thais")) {
            val greeting = "Às suas ordens, senhor. Todos os subsistemas do J.A.R.V.I.S. estão operando a 100% de capacidade. Como posso auxiliá-lo neste momento?"
            return AssistantResponse.VoiceText(greeting, null, null)
        }

        // General smart Jarvis fallback
        val defaultResp = "Compreendido, senhor. Meus protocolos de telemetria, abertura de aplicativos como WhatsApp, YouTube, Prisma 3D e discagem telefônica estão a postos. Basta dar a ordem."
        return AssistantResponse.VoiceText(defaultResp, null, null)
    }

    private fun extractContactName(text: String): String {
        var clean = text
            .replace("por favor", "", ignoreCase = true)
            .replace("você pode", "", ignoreCase = true)
            .replace("ligar para a", "", ignoreCase = true)
            .replace("ligar para o", "", ignoreCase = true)
            .replace("ligar para", "", ignoreCase = true)
            .replace("liga para a", "", ignoreCase = true)
            .replace("liga para o", "", ignoreCase = true)
            .replace("liga pra a", "", ignoreCase = true)
            .replace("liga pra o", "", ignoreCase = true)
            .replace("liga pra", "", ignoreCase = true)
            .replace("liga pro", "", ignoreCase = true)
            .replace("ligar", "", ignoreCase = true)
            .replace("liga", "", ignoreCase = true)
            .replace("chamar", "", ignoreCase = true)
            .replace("please", "", ignoreCase = true)
            .replace("can you", "", ignoreCase = true)
            .replace("call", "", ignoreCase = true)
            .replace("my", "", ignoreCase = true)
            .replace("to", "", ignoreCase = true)
            .trim()

        return clean
    }

    companion object {
        const val SYSTEM_PROMPT = """Você é o J.A.R.V.I.S. (Just A Rather Very Intelligent System), a avançada inteligência artificial pessoal do usuário no Android, inspirada na lendária IA de Tony Stark / Homem de Ferro.

Requisitos Fundamentais de Idioma e Tom:
- Você fala e compreende nativamente e perfeitamente em Português do Brasil (pt-BR).
- Seu tom é impecavelmente educado, sofisticado, inteligente, seguro, calmo e prestativo, como um mordomo britânico de altíssima tecnologia ("Às suas ordens, senhor", "Sistemas operacionais a 100%", "Executando protocolo imediatamente").
- Trate o usuário cordialmente como "senhor" ou pelo nome (Thais), com respeito e elegância.
- Mantenha respostas de voz concisas, inteligentes e objetivas, ideais para interação voz-a-voz rápida e fluida.
- Suporta também alternância automática caso o usuário fale em outro idioma, mas responda prioritariamente em Português do Brasil (pt-BR).

Execução Real de Ações no Dispositivo (Function Calling):
- Você DEVE compreender comandos naturais de voz e REALMENTE EXECUTAR as ações no Android via chamadas de função (tools).
- NUNCA diga apenas "Abrindo WhatsApp" sem disparar a ferramenta `openWhatsApp()`.
- Exemplos de mapeamento de comandos:
  * "Abrir WhatsApp", "Abre o WhatsApp", "Abre meu WhatsApp" -> `openWhatsApp()`
  * "Abrir YouTube", "Abre o YouTube" -> `openApp("YouTube")`
  * "Abrir Prisma 3D", "Abre o Prisma 3D" -> `openApp("Prisma 3D")`
  * "Abrir Godot", "Abre o Godot Engine" -> `openApp("Godot Engine")`
  * "Abrir Chrome", "Abre o navegador" -> `openApp("Chrome")`
  * "Abrir Configurações", "Ajustes do aparelho" -> `openApp("Settings")`
  * "Abrir Câmera" -> `openApp("Camera")`
  * "Ligar para [número]" -> `makeCall(phoneNumber)`
  * "Ligar para [contato]" -> `callContact(contactName)`
- Dúvidas sobre desenvolvimento de jogos, história do 3D e importação de modelos: responda com grande conhecimento técnico e clareza."""
    }
}

sealed class ToolExecutionResult(val success: Boolean, val message: String) {
    abstract fun toJson(): JSONObject

    class Generic(success: Boolean, message: String) : ToolExecutionResult(success, message) {
        override fun toJson(): JSONObject = JSONObject().apply {
            put("status", if (success) "success" else "failure")
            put("message", message)
        }
    }

    class MultipleContactsFound(
        val nameQuery: String,
        val contacts: List<String>,
        message: String
    ) : ToolExecutionResult(false, message) {
        override fun toJson(): JSONObject = JSONObject().apply {
            put("status", "multiple_found")
            put("nameQuery", nameQuery)
            put("contacts", JSONArray(contacts))
            put("message", message)
        }
    }
}
