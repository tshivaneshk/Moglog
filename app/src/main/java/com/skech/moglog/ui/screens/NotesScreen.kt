package com.skech.moglog.ui.screens

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.toArgb

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    initialNote: String,
    onSaveNote: (String) -> Unit,
    onBack: () -> Unit
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryColorHex = String.format("#%06X", (0xFFFFFF and primaryColor.toArgb()))

    val safeInitial = if (initialNote.isBlank()) "" else initialNote.replace("'", "\\'").replace("\n", "\\n").replace("\r", "")

    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <style>
                body {
                    background-color: transparent;
                    color: white;
                    font-family: sans-serif;
                    font-size: 18px;
                    padding: 16px;
                    margin: 0;
                    line-height: 1.5;
                }
                #editor {
                    width: 100%;
                    min-height: 90vh;
                    outline: none;
                }
                input[type="checkbox"] {
                    transform: scale(1.5);
                    margin-right: 8px;
                    accent-color: $primaryColorHex;
                }
                [contenteditable="true"]:empty:before {
                    content: attr(placeholder);
                    color: gray;
                    pointer-events: none;
                    display: block;
                }
            </style>
            <script>
                function formatDoc(cmd, value = null) {
                    document.execCommand(cmd, false, value);
                    document.getElementById('editor').focus();
                }
                function insertCheckbox() {
                    var html = '<div><input type="checkbox">&nbsp;</div>';
                    document.execCommand('insertHTML', false, html);
                    document.getElementById('editor').focus();
                }
                function getHtml() {
                    Android.saveNoteContent(document.getElementById('editor').innerHTML);
                }
                document.addEventListener('change', function(e) {
                    if (e.target && e.target.type === 'checkbox') {
                        if (e.target.checked) {
                            e.target.setAttribute('checked', 'checked');
                            e.target.parentNode.style.textDecoration = 'line-through';
                            e.target.parentNode.style.opacity = '0.5';
                        } else {
                            e.target.removeAttribute('checked');
                            e.target.parentNode.style.textDecoration = 'none';
                            e.target.parentNode.style.opacity = '1';
                        }
                    }
                });
            </script>
        </head>
        <body>
            <div id="editor" contenteditable="true" placeholder="Start typing your notes here...">$safeInitial</div>
        </body>
        </html>
    """.trimIndent()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Notes", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { 
                        webViewRef?.evaluateJavascript("getHtml()", null)
                        onBack() 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        webViewRef?.evaluateJavascript("getHtml()", null)
                        onBack() 
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save Note", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(32.dp))
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 12.dp, horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                // Undo / Redo
                FormatIconButton(text = "↶", onClick = { webViewRef?.evaluateJavascript("formatDoc('undo')", null) })
                FormatIconButton(text = "↷", onClick = { webViewRef?.evaluateJavascript("formatDoc('redo')", null) })
                
                Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))
                
                // Formatting Controls
                FormatIconButton(text = "B", fontWeight = FontWeight.Black, onClick = { webViewRef?.evaluateJavascript("formatDoc('bold')", null) })
                FormatIconButton(text = "I", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, onClick = { webViewRef?.evaluateJavascript("formatDoc('italic')", null) })
                FormatIconButton(text = "U", textDecoration = TextDecoration.Underline, onClick = { webViewRef?.evaluateJavascript("formatDoc('underline')", null) })

                Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))

                // Lists and Checklists
                FormatIconButton(text = "•", fontWeight = FontWeight.Black, onClick = { webViewRef?.evaluateJavascript("formatDoc('insertUnorderedList')", null) })
                FormatIconButton(text = "1.", fontWeight = FontWeight.Bold, onClick = { webViewRef?.evaluateJavascript("formatDoc('insertOrderedList')", null) })
                FormatIconButton(text = "✓", fontWeight = FontWeight.Black, onClick = { webViewRef?.evaluateJavascript("insertCheckbox()", null) })
                
                Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))

                // Color Controls
                ColorButton(Color(0xFFFF5252)) { webViewRef?.evaluateJavascript("formatDoc('foreColor', '#FF5252')", null) }
                ColorButton(Color(0xFF448AFF)) { webViewRef?.evaluateJavascript("formatDoc('foreColor', '#448AFF')", null) }
                ColorButton(Color(0xFF4CAF50)) { webViewRef?.evaluateJavascript("formatDoc('foreColor', '#4CAF50')", null) }
                ColorButton(Color(0xFFFFEB3B)) { webViewRef?.evaluateJavascript("formatDoc('foreColor', '#FFEB3B')", null) }
                ColorButton(Color.White) { webViewRef?.evaluateJavascript("formatDoc('foreColor', '#FFFFFF')", null) }
                
                Box(modifier = Modifier.height(24.dp).width(1.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)))

                // Size Controls
                FormatIconButton(text = "A+", fontWeight = FontWeight.Bold, onClick = { webViewRef?.evaluateJavascript("formatDoc('fontSize', '7')", null) })
                FormatIconButton(text = "a-", fontWeight = FontWeight.Bold, onClick = { webViewRef?.evaluateJavascript("formatDoc('fontSize', '3')", null) })
            }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().clip(RoundedCornerShape(16.dp))) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        webChromeClient = WebChromeClient()
                        webViewClient = WebViewClient()
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun saveNoteContent(html: String) {
                                onSaveNote(html)
                            }
                        }, "Android")
                        loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
                    }.also { webViewRef = it }
                },
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun FormatIconButton(
    text: String,
    onClick: () -> Unit,
    fontWeight: FontWeight? = null,
    fontStyle: androidx.compose.ui.text.font.FontStyle? = null,
    textDecoration: TextDecoration? = null
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = fontWeight ?: FontWeight.Medium,
            fontStyle = fontStyle,
            textDecoration = textDecoration,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ColorButton(color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .clickable(onClick = onClick)
    )
}
