package com.lingdict.app.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lingdict.app.presentation.theme.LingDictTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "输入单词，如 dictionary...",
    onSearch: (String) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    // Update expanded state based on query and suggestions
    LaunchedEffect(query, suggestions) {
        expanded = query.isNotEmpty() && suggestions.isNotEmpty()
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it && query.isNotEmpty() && suggestions.isNotEmpty() },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
                expanded = it.isNotEmpty() && suggestions.isNotEmpty()
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        onQueryChange("")
                        expanded = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearch(query)
                    expanded = false
                }
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        onSuggestionClick(suggestion)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchBarPreview() {
    LingDictTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                SearchBar(
                    query = "dict",
                    onQueryChange = {},
                    suggestions = listOf("dictionary", "dictate", "dictation"),
                    onSuggestionClick = {}
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SearchBarEmptyPreview() {
    LingDictTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                SearchBar(
                    query = "",
                    onQueryChange = {},
                    suggestions = emptyList(),
                    onSuggestionClick = {}
                )
            }
        }
    }
}
