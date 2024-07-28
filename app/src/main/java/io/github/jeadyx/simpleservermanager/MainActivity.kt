package io.github.jeadyx.simpleservermanager

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import io.github.jeadyx.simplenetmanager.SimpleNetManager
import io.github.jeadyx.simpleservermanager.ui.theme.SimpleServerManagerTheme
import kotlin.concurrent.thread

private const val TAG = "[MainActivity]"
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleServerManagerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Sample(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Sample(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var result by rememberSaveable {
        mutableStateOf<ServerRet?>(null)
    }
    var errMessage by remember { mutableStateOf("") }
    Column(
        modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var queryServer by remember { mutableStateOf("https://hanyu.baidu.com") }
        var queryPath by remember { mutableStateOf("hanyu/api/sentencelistv2") }
        var queryParam by remember { mutableStateOf("query=冷笑话&type=sentence") }
        FlowRow {
            TextField(value = queryServer, onValueChange = { queryServer = it }, placeholder = {
                Text("服务器地址")
            })
            TextField(value = queryPath, onValueChange = { queryPath = it }, placeholder = {
                Text("查询路径")
            })
            TextField(value = queryParam, onValueChange = { queryParam = it }, placeholder = {
                Text("查询参数")
            })
            Button(onClick = {
                result = null
                context.getSharedPreferences("input", MODE_PRIVATE).edit()
                    .putString("server", queryServer)
                    .putString("path", queryPath)
                    .putString("param", queryParam)
                    .apply()
                thread {
                    SimpleNetManager.getInstance(queryServer)
                        .get(queryPath, queryParam, ServerRet::class.java) { data, errMsg ->
                            Log.i(TAG, "TestServerManager: get data res: $data ; \nerr:$errMsg")
                            errMessage = errMsg ?: ""
                            result = data
                        }
                }
            }) {
                Text(text = "Get")
            }
        }
        Text(text = "查询结果 $errMessage", fontWeight = FontWeight.SemiBold)
        result?.data?.let {
            if (it.ret_array.isNotEmpty()) {
                LazyColumn {
                    itemsIndexed(it.ret_array[0].list) { idx, it ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(if (idx % 2 == 0) Color.White else Color.LightGray),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("${it.body}")
                        }
                    }
                }
            } else {
                Text("查询结果为空")
            }
        }
        LaunchedEffect(true) {
            val sharedPreference = context.getSharedPreferences("input", MODE_PRIVATE)
            sharedPreference.getString("server", "")?.takeIf { it.isNotEmpty() }
                ?.let { queryServer = it }
            sharedPreference.getString("path", "")?.takeIf { it.isNotEmpty() }
                ?.let { queryPath = it }
            sharedPreference.getString("param", "")?.takeIf { it.isNotEmpty() }
                ?.let { queryParam = it }
        }
    }
}
data class ServerData(
    val ret_array: List<ServerList>
)
data class ServerList(
    val list: List<ServerDetail>,
    val count: Int,
    val type: String,
    val name: String,
)
data class ServerDetail(
    val name: String,
    val literature_author: String,
    val sid: String,
    val tag: List<String>,
    val type: String,
    val body: List<String>,
    val isLike: Int,
    val is_vocab: Int,
    val like_count: Int,
    val vocab_count: Int
)
data class ServerRet(
    val errno: Int,
    val errmsg: String,
    val data: ServerData
)