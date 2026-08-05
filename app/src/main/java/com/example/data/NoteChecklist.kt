package com.example.data

data class NoteChecklistItem(
    val text: String,
    val isChecked: Boolean = false
)

object NoteChecklistSerializer {
    fun serialize(items: List<NoteChecklistItem>): String {
        if (items.isEmpty()) return ""
        return items.joinToString(";;;") { item ->
            val cleanText = item.text.replace(";", " ").replace("|", " ")
            "${item.isChecked}|||$cleanText"
        }
    }

    fun deserialize(raw: String): List<NoteChecklistItem> {
        if (raw.isBlank()) return emptyList()
        return raw.split(";;;").mapNotNull { part ->
            val pieces = part.split("|||")
            if (pieces.size == 2) {
                NoteChecklistItem(
                    text = pieces[1],
                    isChecked = pieces[0].toBooleanStrictOrNull() ?: false
                )
            } else if (part.isNotBlank()) {
                NoteChecklistItem(text = part, isChecked = false)
            } else null
        }
    }
}
