import re

with open('app/src/main/java/com/example/ui/screens/InvoiceEditorScreen.kt', 'r') as f:
    content = f.read()

correct = """                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            }

            // --- Line Items Title ---
            item {"""

content = content.replace('                           // --- Line Items Title ---\n            item {', correct)

# Remove the duplicated:
#                   Text(text = if (isRtl) "افزودن ردیف" else "Add Row", fontSize = 11.sp)
#                        }
#                    }
#                }
#            }
#            // --- Line Item Rows ---
dup = """                   Text(text = if (isRtl) "افزودن ردیف" else "Add Row", fontSize = 11.sp)
                        }
                    }
                }
            }
            // --- Line Item Rows ---"""
content = content.replace(dup, "")

with open('app/src/main/java/com/example/ui/screens/InvoiceEditorScreen.kt', 'w') as f:
    f.write(content)
