import sys
with open(sys.argv[1], "r") as f:
    text = f.read()

text = text.replace("    fun limpiarError() {", """    fun cerrarDocumento() {
        ttsManager?.stop()
        _uiState.value = _uiState.value.copy(
            currentDocumentId = "",
            documentTitle = "",
            paginas = emptyList(),
            fragmentosLectura = emptyList(),
            indiceActual = 0,
            lecturaActiva = false,
            lecturaPausadaPorPregunta = false,
            voiceStatus = "Esperando documento...",
            showContinueButton = false,
            isContinueEnabled = false,
            chatMessages = emptyList()
        )
    }

    fun limpiarError() {""")

with open(sys.argv[1], "w") as f:
    f.write(text)
