package com.example.audio

data class VoiceProfile(
    val id: String,
    val name: String,
    val description: String,
    val geminiVoiceName: String,
    val pitch: Float = 1.0f,
    val speechRate: Float = 1.0f,
    val isMale: Boolean = true
) {
    companion object {
        val JARVIS = VoiceProfile(
            id = "jarvis",
            name = "J.A.R.V.I.S. (Masculina)",
            description = "Tom grave, firme, culto e elegante (Estilo Homem de Ferro)",
            geminiVoiceName = "Fenrir",
            pitch = 0.80f,
            speechRate = 0.93f,
            isMale = true
        )

        val CHARON = VoiceProfile(
            id = "charon",
            name = "Charon (Profundo)",
            description = "Voz muito grave, imponente e robótica",
            geminiVoiceName = "Charon",
            pitch = 0.72f,
            speechRate = 0.90f,
            isMale = true
        )

        val FENRIR = VoiceProfile(
            id = "fenrir",
            name = "Fenrir (Tático)",
            description = "Voz masculina dinâmica e direta",
            geminiVoiceName = "Fenrir",
            pitch = 0.88f,
            speechRate = 1.0f,
            isMale = true
        )

        val PUCK = VoiceProfile(
            id = "puck",
            name = "Puck (Enérgico)",
            description = "Voz masculina jovem e dinâmica",
            geminiVoiceName = "Puck",
            pitch = 1.02f,
            speechRate = 1.05f,
            isMale = true
        )

        val FRIDAY = VoiceProfile(
            id = "friday",
            name = "F.R.I.D.A.Y. (Feminina Tática)",
            description = "Voz feminina refinada, precisa e tecnológica",
            geminiVoiceName = "Kore",
            pitch = 1.0f,
            speechRate = 1.0f,
            isMale = false
        )

        val ARUSHI = VoiceProfile(
            id = "arushi",
            name = "Arushi (Feminina Expressiva)",
            description = "Voz feminina calorosa e amigável",
            geminiVoiceName = "Aoede",
            pitch = 1.10f,
            speechRate = 1.0f,
            isMale = false
        )

        val ALL_VOICES = listOf(JARVIS, CHARON, FENRIR, PUCK, FRIDAY, ARUSHI)
    }
}
