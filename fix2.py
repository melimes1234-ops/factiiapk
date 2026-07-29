with open('app/src/main/java/com/example/ui/screens/InvoiceEditorScreen.kt', 'r') as f:
    content = f.read()

target = "            // --- Line Item Rows ---"
replacement = """                        }
                    }
                }
            }
            // --- Line Item Rows ---"""
content = content.replace(target, replacement, 1)

with open('app/src/main/java/com/example/ui/screens/InvoiceEditorScreen.kt', 'w') as f:
    f.write(content)
