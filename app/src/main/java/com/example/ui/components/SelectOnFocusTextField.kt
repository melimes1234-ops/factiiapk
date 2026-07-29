package com.example.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun SelectOnFocusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    var textFieldValueState by remember { mutableStateOf(TextFieldValue(text = value)) }
    var isFocused by remember { mutableStateOf(false) }
    var selectAllTrigger by remember { mutableIntStateOf(0) }
    var lastDigitsBeforeCursor by remember { mutableIntStateOf(-1) }
    val interactionSource = remember { MutableInteractionSource() }

    // Synchronize internal state with external value changes (e.g. 3-digit comma formatting from parent)
    LaunchedEffect(value) {
        if (textFieldValueState.text != value) {
            val selection = if (isFocused) {
                calculateNewSelection(
                    lastDigitsBefore = lastDigitsBeforeCursor,
                    newText = value
                )
            } else {
                TextRange(value.length)
            }
            textFieldValueState = TextFieldValue(text = value, selection = selection)
        }
    }

    // Automatically select all text on initial focus or when tapped
    LaunchedEffect(selectAllTrigger, isFocused) {
        if (isFocused && textFieldValueState.text.isNotEmpty()) {
            kotlinx.coroutines.delay(40)
            textFieldValueState = textFieldValueState.copy(
                selection = TextRange(0, textFieldValueState.text.length)
            )
        }
    }

    OutlinedTextField(
        value = textFieldValueState,
        onValueChange = { newValue ->
            val cursorPos = newValue.selection.end.coerceIn(0, newValue.text.length)

            // Track how many digits exist before the cursor in user's typed input
            lastDigitsBeforeCursor = if (cursorPos == newValue.text.length) {
                -1 // -1 means cursor is at the very end
            } else {
                newValue.text.take(cursorPos).count { it.isDigit() }
            }

            textFieldValueState = newValue
            if (newValue.text != value) {
                onValueChange(newValue.text)
            }
        },
        modifier = modifier
            .onFocusChanged { focusState ->
                if (focusState.isFocused && !isFocused) {
                    isFocused = true
                    selectAllTrigger++
                } else if (!focusState.isFocused) {
                    isFocused = false
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press) {
                            if (isFocused) {
                                selectAllTrigger++
                            }
                        }
                    }
                }
            },
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        supportingText = supportingText,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        enabled = enabled,
        readOnly = readOnly,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource
    )
}

/**
 * Calculates new cursor selection when parent updates value (e.g. inserting 3-digit commas).
 * Always anchors the cursor precisely relative to digit count.
 */
private fun calculateNewSelection(
    lastDigitsBefore: Int,
    newText: String
): TextRange {
    val totalDigitsInNewText = newText.count { it.isDigit() }

    // If cursor was at the end (-1) or beyond available digits, keep it at the end
    if (lastDigitsBefore < 0 || lastDigitsBefore >= totalDigitsInNewText) {
        return TextRange(newText.length)
    }

    if (lastDigitsBefore == 0) {
        return TextRange(0)
    }

    // Find position in newText after 'lastDigitsBefore' digits
    var currentDigits = 0
    var newCursorPos = newText.length
    for (i in newText.indices) {
        if (newText[i].isDigit()) {
            currentDigits++
            if (currentDigits == lastDigitsBefore) {
                newCursorPos = i + 1
                break
            }
        }
    }

    return TextRange(newCursorPos.coerceIn(0, newText.length))
}
