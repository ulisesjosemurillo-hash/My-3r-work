import sys

with open("app/src/main/java/com/example/ui/JurisTechLibraryScreen.kt", "r") as f:
    text = f.read()

# Fix EmptyLibraryView
text = text.replace(
    "fun EmptyLibraryView(onAddDocument: () -> Unit) {",
    "fun EmptyLibraryView(modifier: Modifier = Modifier, onAddDocument: () -> Unit) {"
)
text = text.replace(
    "modifier = Modifier.fillMaxWidth().weight(1f),",
    "modifier = modifier.fillMaxWidth(),"
)

# Fix LibraryCarouselView
text = text.replace(
    "fun LibraryCarouselView(\n    documents: List<RecentDocumentEntity>,",
    "fun LibraryCarouselView(\n    modifier: Modifier = Modifier,\n    documents: List<RecentDocumentEntity>,"
)

# Update call sites
text = text.replace(
    "EmptyLibraryView(onAddDocument)",
    "EmptyLibraryView(Modifier.weight(1f), onAddDocument)"
)
text = text.replace(
    "LibraryCarouselView(\n                    documents = recentDocuments,",
    "LibraryCarouselView(\n                    modifier = Modifier.weight(1f),\n                    documents = recentDocuments,"
)

with open("app/src/main/java/com/example/ui/JurisTechLibraryScreen.kt", "w") as f:
    f.write(text)
