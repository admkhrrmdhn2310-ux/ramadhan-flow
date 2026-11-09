package com.ramadhanflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.NumberFormat
import java.util.Locale

private val ComponentActivity.dataStore by preferencesDataStore(name = "ramadhan_flow")
private val transactionsKey = stringPreferencesKey("transactions")
private val bankKey = stringPreferencesKey("bank_balance")
private val cashKey = stringPreferencesKey("cash_balance")

@Serializable
data class Tx(val id: Long, val title: String, val amount: Long, val income: Boolean, val account: String, val category: String)

private val navy = Color(0xFF172B4D)
private val gold = Color(0xFFE8C77A)
private val green = Color(0xFF16834A)
private val red = Color(0xFFC33D3D)
private fun rupiah(n: Long): String = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(n).replace(",00", "")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val prefs = dataStore.data.first()
            val tx = runCatching { Json.decodeFromString<List<Tx>>(prefs[transactionsKey] ?: "[]") }.getOrDefault(emptyList())
            val bank = prefs[bankKey]?.toLongOrNull() ?: 0L
            val cash = prefs[cashKey]?.toLongOrNull() ?: 0L
            setContent {
                MaterialTheme(colorScheme = lightColorScheme(primary = navy, secondary = gold, background = Color(0xFFF5F6F8))) {
                    FinanceApp(tx, bank, cash) { newTx, newBank, newCash ->
                        lifecycleScope.launch {
                            dataStore.edit {
                                it[transactionsKey] = Json.encodeToString(newTx)
                                it[bankKey] = newBank.toString()
                                it[cashKey] = newCash.toString()
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceApp(initial: List<Tx>, initialBank: Long, initialCash: Long, save: (List<Tx>, Long, Long) -> Unit) {
    var txs by remember { mutableStateOf(initial) }
    var bank by remember { mutableLongStateOf(initialBank) }
    var cash by remember { mutableLongStateOf(initialCash) }
    var showAdd by remember { mutableStateOf(false) }
    var income by remember { mutableStateOf(true) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("Cash") }
    var category by remember { mutableStateOf("Lainnya") }
    val totalIn = txs.filter { it.income }.sumOf { it.amount }
    val totalOut = txs.filter { !it.income }.sumOf { it.amount }
    val total = bank + cash

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column {
                    Text("RAMADHAN FLOW", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text("Personal Finance", fontSize = 11.sp, color = Color.Gray)
                } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = navy, contentColor = Color.White) {
                Icon(Icons.Default.Add, contentDescription = "Tambah transaksi")
            }
        },
        containerColor = Color(0xFFF5F6F8)
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = navy), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text("TOTAL SALDO", color = Color(0xFFD5E2F5), fontSize = 12.sp)
                        Text(rupiah(total), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(18.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            BalanceItem("Saldo Bank", bank, Icons.Default.AccountBalance)
                            BalanceItem("Saldo Cash", cash, Icons.Default.Wallet)
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("Uang masuk", totalIn, green, true, Modifier.weight(1f))
                    SummaryCard("Uang keluar", totalOut, red, false, Modifier.weight(1f))
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Grafik keuangan", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Ringkasan seluruh transaksi", color = Color.Gray, fontSize = 12.sp)
                        Spacer(Modifier.height(16.dp))
                        val max = maxOf(totalIn, totalOut, 1L).toFloat()
                        Row(Modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                            BarColumn("Masuk", totalIn, max, green)
                            BarColumn("Keluar", totalOut, max, red)
                        }
                    }
                }
            }
            item { Text("Riwayat transaksi", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            if (txs.isEmpty()) {
                item { Text("Belum ada transaksi. Tekan tombol + untuk mulai mencatat.", color = Color.Gray) }
            } else {
                items(txs, key = { it.id }) { t ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(12.dp), color = if (t.income) Color(0xFFE7F5EC) else Color(0xFFFBEAEA)) {
                                Icon(if (t.income) Icons.Default.TrendingUp else Icons.Default.TrendingDown, null,
                                    Modifier.padding(10.dp), tint = if (t.income) green else red)
                            }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(t.title, fontWeight = FontWeight.SemiBold)
                                Text("${t.category} • ${t.account}", color = Color.Gray, fontSize = 11.sp)
                            }
                            Text((if (t.income) "+" else "−") + rupiah(t.amount), color = if (t.income) green else red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Tambah transaksi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = income, onClick = { income = true }, label = { Text("Uang masuk") })
                        FilterChip(selected = !income, onClick = { income = false }, label = { Text("Uang keluar") })
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Keterangan") }, singleLine = true)
                    OutlinedTextField(value = amount, onValueChange = { amount = it.filter(Char::isDigit) }, label = { Text("Nominal (Rp)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    Text("Sumber / tujuan", fontSize = 12.sp, color = Color.Gray)
                    Row {
                        FilterChip(selected = account == "Cash", onClick = { account = "Cash" }, label = { Text("Cash") })
                        Spacer(Modifier.width(8.dp))
                        FilterChip(selected = account == "Bank", onClick = { account = "Bank" }, label = { Text("Bank") })
                    }
                    Text("Kategori", fontSize = 12.sp, color = Color.Gray)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(value = category, onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor().fillMaxWidth(), label = { Text("Kategori") })
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("Lainnya", "Makanan", "Transportasi", "Belanja", "Uang saku", "Penghasilan", "Tabungan", "Hiburan").forEach {
                                DropdownMenuItem(text = { Text(it) }, onClick = { category = it; expanded = false })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val value = amount.toLongOrNull() ?: 0L
                    if (value > 0 && title.isNotBlank()) {
                        val t = Tx(System.currentTimeMillis(), title.trim(), value, income, account, category)
                        txs = listOf(t) + txs
                        if (account == "Bank") bank += if (income) value else -value
                        else cash += if (income) value else -value
                        save(txs, bank, cash)
                        title = ""; amount = ""; showAdd = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Batal") } }
        )
    }
}

@Composable
fun BalanceItem(label: String, value: Long, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = gold, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Column {
            Text(label, color = Color(0xFFD5E2F5), fontSize = 11.sp)
            Text(rupiah(value), color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SummaryCard(label: String, value: Long, tint: Color, isIncome: Boolean, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Icon(if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown, null, tint = tint)
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text(rupiah(value), color = tint, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun BarColumn(label: String, value: Long, max: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.width(100.dp).fillMaxHeight()) {
        Text(rupiah(value), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.width(48.dp).weight(1f, fill = false).height((90f * value.toFloat() / max).coerceAtLeast(4f).dp).background(color, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)))
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}
