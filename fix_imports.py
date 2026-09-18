import sys

with open("app/src/main/java/com/example/ui/JurisTechScreen.kt", "r") as f:
    text = f.read()

text = text.replace("import com.example.model.ChatMessage", "")
text = text.replace("import com.example.model.FragmentoLectura", "")

with open("app/src/main/java/com/example/ui/JurisTechScreen.kt", "w") as f:
    f.write(text)
