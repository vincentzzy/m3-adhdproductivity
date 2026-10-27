package com.nowapp.platform

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.nowapp.ServiceLocator
import com.nowapp.ui.theme.NowTheme
import kotlinx.coroutines.launch

/**
 * The capture surface behind the Quick Settings tile and the widget's "+".
 * One field, one button: type, tap Add, gone. Nothing else is on screen.
 */
class QuickCaptureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NowTheme(calmMode = true) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    var text by remember { mutableStateOf(intent.getStringExtra(EXTRA_PREFILL).orEmpty()) }
                    var intoToday by remember { mutableStateOf(false) }
                    val focus = remember { FocusRequester() }

                    LaunchedEffect(Unit) { focus.requestFocus() }

                    fun commit() {
                        val title = text.trim()
                        if (title.isNotEmpty()) {
                            lifecycleScope.launch {
                                ServiceLocator.tasks.capture(title, intoToday = intoToday)
                                finish()
                            }
                        } else {
                            finish()
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Add a thing", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it.take(200) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focus),
                            placeholder = { Text("What just came up?") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { commit() }),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { intoToday = !intoToday }) {
                                Text(if (intoToday) "Into Today" else "Into Later")
                            }
                            Button(onClick = { commit() }) { Text("Add") }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_PREFILL = "prefill"
    }
}
