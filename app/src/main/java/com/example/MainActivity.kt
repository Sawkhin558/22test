package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import com.example.ui.AppViewModel
import com.example.ui.theme.*
import kotlin.math.absoluteValue

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    MainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: AppViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Observe Flows
    val vouchers by viewModel.vouchers.collectAsState()
    val masterVouchers by viewModel.masterVouchers.collectAsState()
    val settings by viewModel.settingsFlow.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val pat19 by viewModel.pat19.collectAsState()
    val rawInput by viewModel.rawInput.collectAsState()
    val errorText by viewModel.errorText.collectAsState()
    val winNum by viewModel.winNum.collectAsState()
    val modalText by viewModel.modalVoucherText.collectAsState()

    var activeTab by remember { mutableStateOf("entry") }

    // Aggregate Current Session Live Balances for top card
    val activeVouchers = vouchers.filter { it.session == currentSession }
    val totalsMap = remember(activeVouchers) {
        val map = mutableMapOf<String, Int>()
        activeVouchers.forEach { v ->
            CustomJsonHelper.fromJson(v.itemsJson).forEach { item ->
                map[item.num] = (map[item.num] ?: 0) + item.amt
            }
        }
        map
    }

    val limit = settings.limitPrice
    var selfTotal = 0
    var masterTotal = 0
    totalsMap.forEach { (_, amt) ->
        if (amt > limit) {
            selfTotal += limit
            masterTotal += (amt - limit)
        } else {
            selfTotal += amt
        }
    }

    val netIncome = selfTotal - (selfTotal * settings.commissionPercentage / 100.0)
    val risk = netIncome - (limit * settings.payoutMultiplier)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Title Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Slate800, Slate900)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "2D Smart Pro Star Logo",
                tint = Emerald500,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "2D Smart Pro (v7.5)",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate100,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Emerald500.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Zero-Overlimit",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Emerald500
                )
            }
        }

        // Notice Board Summary Board Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate800),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = BorderStroke(1.dp, Slate700)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Session Info Icon",
                            tint = Amber500,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "🕒 Active Session: ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate300
                        )
                        Text(
                            text = currentSession,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Amber500
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = Slate700, thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "မိမိကိုင်ထားငွေ (Self)",
                            fontSize = 12.sp,
                            color = Slate300
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${String.format("%,d", selfTotal)} Ks",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald500
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Master ပို့ရန် (Over)",
                            fontSize = 12.sp,
                            color = Slate300
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${String.format("%,d", masterTotal)} Ks",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (masterTotal > 0) Crimson500 else Slate300
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate900.copy(alpha = 0.3f))
                        .padding(10.dp)
                ) {
                    if (selfTotal > 0) {
                        Column {
                            Row {
                                Text(
                                    text = "အသားတင်: ",
                                    fontSize = 13.sp,
                                    color = Slate300
                                )
                                Text(
                                    text = "${String.format("%,.0f", netIncome)} Ks",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate100
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ပေါက်ခဲ့လျှင်: ",
                                    fontSize = 13.sp,
                                    color = Slate300
                                )
                                val color = if (risk >= 0) Emerald500 else Crimson500
                                val outcomeText = if (risk >= 0) "မြတ်" else "ရှုံး"
                                Text(
                                    text = "${String.format("%,.0f", risk.absoluteValue)} $outcomeText",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = color
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "📢 စောင့်ဆိုင်းနေသည် (စာရင်းသွင်းပါ)...",
                            fontSize = 13.sp,
                            color = Slate300,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Custom Categories Tabs Navigation
        ScrollableTabRow(
            selectedTabIndex = when (activeTab) {
                "entry" -> 0
                "vouchers" -> 1
                "master" -> 2
                "report" -> 3
                "profit" -> 4
                "settings" -> 5
                else -> 0
            },
            containerColor = Slate900,
            contentColor = Emerald500,
            edgePadding = 16.dp,
            divider = {}
        ) {
            val tabsList = listOf(
                "entry" to "စာရင်းသွင်း",
                "vouchers" to "မှတ်တမ်း",
                "master" to "Master",
                "report" to "စာရင်းချုပ်",
                "profit" to "အမြတ်အရှုံး",
                "settings" to "Settings"
            )

            tabsList.forEach { (id, label) ->
                Tab(
                    selected = activeTab == id,
                    onClick = { activeTab = id },
                    text = {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (activeTab == id) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.testTag("tab_$id")
                        )
                    },
                    selectedContentColor = Emerald500,
                    unselectedContentColor = Slate300
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Tab Content Panel
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            when (activeTab) {
                "entry" -> EntryTab(viewModel = viewModel, currentSession = currentSession, pat19 = pat19, rawInput = rawInput, errorText = errorText, settings = settings)
                "vouchers" -> VouchersTab(viewModel = viewModel, currentSession = currentSession, vouchers = vouchers)
                "master" -> MasterTab(viewModel = viewModel, currentSession = currentSession, masterVouchers = masterVouchers)
                "report" -> ReportTab(viewModel = viewModel, currentSession = currentSession, limit = limit, totalsMap = totalsMap)
                "profit" -> ProfitTab(viewModel = viewModel, currentSession = currentSession, winNum = winNum, totalsMap = totalsMap, masterVouchers = masterVouchers, limit = limit, settings = settings)
                "settings" -> SettingsTab(viewModel = viewModel, settings = settings)
            }
        }
    }

    // Modal Sheet representation
    if (modalText != null) {
        Dialog(onDismissRequest = { viewModel.dismissModal() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                border = BorderStroke(1.dp, Slate700)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "🔎 အသေးစိတ်ကြည့်ရှုရန်",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Slate700)
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .background(Slate900)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = modalText ?: "",
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Slate100
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(modalText ?: ""))
                                Toast.makeText(context, "Copy ကူးယူပြီးပါပြီ!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Copy")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy")
                        }

                        Button(
                            onClick = { viewModel.dismissModal() },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("ပိတ်မည်")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EntryTab(
    viewModel: AppViewModel,
    currentSession: String,
    pat19: Boolean,
    rawInput: String,
    errorText: String?,
    settings: SettingsEntity
) {
    val sessionList = remember(settings.sessionsCsv) {
        settings.sessionsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    var expandedSessionMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Session Selector
            Box {
                OutlinedButton(
                    onClick = { expandedSessionMenu = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate100),
                    border = BorderStroke(1.dp, Slate700)
                ) {
                    Text(text = "Session: $currentSession")
                    val arrowIcon = if (expandedSessionMenu) Icons.Default.ArrowBack else Icons.Default.Add
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = arrowIcon, contentDescription = "Dropdown status", modifier = Modifier.size(16.dp))
                }
                DropdownMenu(
                    expanded = expandedSessionMenu,
                    onDismissRequest = { expandedSessionMenu = false },
                    modifier = Modifier.background(Slate800)
                ) {
                    sessionList.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(text = s, color = Slate100) },
                            onClick = {
                                viewModel.setSession(s)
                                expandedSessionMenu = false
                            }
                        )
                    }
                }
            }

            // 19 pat check
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { viewModel.setPat19(!pat19) }
            ) {
                Checkbox(
                    checked = pat19,
                    onCheckedChange = { viewModel.setPat19(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Emerald500,
                        uncheckedColor = Slate700
                    )
                )
                Text(
                    text = "၁၉ ကွက် (ပတ်)",
                    fontSize = 13.sp,
                    color = Slate100
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large Formula Area
        OutlinedTextField(
            value = rawInput,
            onValueChange = { viewModel.setRawInput(it) },
            placeholder = { Text(text = "ဥပမာ-\n12.34=r100\nအပူး=500\n1.2ထိပ်=200", color = Slate300.copy(alpha = 0.6f)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .testTag("raw_formula_input"),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Slate800,
                unfocusedContainerColor = Slate800,
                focusedBorderColor = Emerald500,
                unfocusedBorderColor = Slate700,
                focusedTextColor = Slate100,
                unfocusedTextColor = Slate100
            )
        )

        // Show parsing error if exists
        if (errorText != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Crimson500.copy(alpha = 0.15f))
                    .border(1.dp, Crimson500, RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Danger Icon",
                        tint = Crimson500,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorText ?: "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Crimson500
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Input keyboard grid helper
        Text(
            text = "⚡ အမြန်စာရိုက်ရန် ကီးဘုတ်",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Slate300
        )
        Spacer(modifier = Modifier.height(6.dp))

        val quickKeys = listOf(
            "အပူး=" to "အပူး",
            "ညီကို=" to "ညီကို",
            "ထိပ်=" to "ထိပ်",
            "နောက်=" to "နောက်",
            "ခွေ=" to "ခွေ",
            "ပတ်=" to "ပတ်",
            "ပါဝါ=" to "ပါဝါ",
            "နက္ခတ်=" to "နက္ခတ်"
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(quickKeys) { (code, label) ->
                Button(
                    onClick = {
                        viewModel.setRawInput(rawInput + code)
                    },
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Indigo900.copy(alpha = 0.5f),
                        contentColor = Slate100
                    ),
                    modifier = Modifier.height(44.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main entry processing button
        Button(
            onClick = { viewModel.processEntry() },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("submit_button")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Icon")
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "စာရင်းသွင်းမည်",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(100.dp)) // ensure safe scrolling
    }
}

@Composable
fun VouchersTab(
    viewModel: AppViewModel,
    currentSession: String,
    vouchers: List<VoucherEntity>
) {
    val sessionVouchers = remember(vouchers, currentSession) {
        vouchers.filter { it.session == currentSession }
    }

    if (sessionVouchers.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Empty list",
                    tint = Slate700,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "မှတ်တမ်းစာရင်းမရှိသေးပါ။",
                    fontSize = 14.sp,
                    color = Slate300
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(sessionVouchers, key = { it.id }) { v ->
                val items = CustomJsonHelper.fromJson(v.itemsJson)
                val sum = items.sumOf { it.amt }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    border = BorderStroke(1.dp, Slate700)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🕒 ${v.timeStr}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Slate300
                                )
                            }
                            Text(
                                text = "${String.format("%,d", sum)} Ks",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald500
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Slate700, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.showModal(v.id, false) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("ကြည့်မည်", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.editVoucher(v.id) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Amber500, contentColor = Slate900),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("ပြင်မည်", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { viewModel.deleteVoucher(v.id) },
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Crimson500),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("ဖျက်မည်", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MasterTab(
    viewModel: AppViewModel,
    currentSession: String,
    masterVouchers: List<MasterVoucherEntity>
) {
    val sessionMasters = remember(masterVouchers, currentSession) {
        masterVouchers.filter { it.session == currentSession }
    }
    val grandTotal = remember(sessionMasters) {
        sessionMasters.sumOf { mv ->
            CustomJsonHelper.fromJson(mv.itemsJson).sumOf { it.amt }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Crimson700.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, Crimson500)
        ) {
            Text(
                text = "Master စုစုပေါင်း: ${String.format("%,d", grandTotal)} Ks",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Crimson500,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }

        if (sessionMasters.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "မာစတာစာရင်းမရှိသေးပါ။", color = Slate300, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sessionMasters, key = { it.id }) { mv ->
                    val totalAmt = CustomJsonHelper.fromJson(mv.itemsJson).sumOf { it.amt }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate800),
                        border = BorderStroke(1.dp, Slate700)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📤 Master: ${mv.timeStr}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate100
                                )
                                Text(
                                    text = "${String.format("%,d", totalAmt)} Ks",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Crimson500
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.showModal(mv.id, true) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                ) {
                                    Text("ကြည့်မည်", fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { viewModel.deleteMasterVoucher(mv.id) },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Crimson500),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                ) {
                                    Text("Restore", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportTab(
    viewModel: AppViewModel,
    currentSession: String,
    limit: Int,
    totalsMap: Map<String, Int>
) {
    val context = LocalContext.current
    val sortedNumbers = remember(totalsMap) {
        totalsMap.keys.sorted()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = {
                viewModel.sendToMaster(
                    onSuccess = {
                        Toast.makeText(context, "Limit ကျော်များကို Master ပို့ပြီးပါပြီ!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = Crimson500),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .height(46.dp)
        ) {
            Icon(imageVector = Icons.Default.Share, contentDescription = "Send Master")
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Limit ကျော်များကို Master ပို့မည်",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // Table Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Slate700)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "ဂဏန်း", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = Slate100, textAlign = TextAlign.Center)
                Text(text = "ပမာဏ", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Slate100, textAlign = TextAlign.End)
                Text(text = "Limit ကျော်", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, color = Slate100, textAlign = TextAlign.End)
            }
        }

        if (sortedNumbers.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Slate800),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "စာရင်းမရှိပါ။", color = Slate300, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, Slate700, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(Slate800)
            ) {
                items(sortedNumbers) { num ->
                    val amt = totalsMap[num] ?: 0
                    val over = amt - limit

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = num,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            color = Emerald500,
                            textAlign = TextAlign.Center,
                            fontSize = 15.sp
                        )
                        Text(
                            text = String.format("%,d", amt),
                            modifier = Modifier.weight(1.5f),
                            color = Slate100,
                            textAlign = TextAlign.End,
                            fontSize = 14.sp
                        )
                        val overText = if (over > 0) String.format("%,d", over) else "-"
                        val overColor = if (over > 0) Crimson500 else Emerald600
                        Text(
                            text = overText,
                            modifier = Modifier.weight(1.5f),
                            fontWeight = FontWeight.Bold,
                            color = overColor,
                            textAlign = TextAlign.End,
                            fontSize = 14.sp
                        )
                    }
                    Divider(color = Slate700)
                }
            }
        }
    }
}

@Composable
fun ProfitTab(
    viewModel: AppViewModel,
    currentSession: String,
    winNum: String,
    totalsMap: Map<String, Int>,
    masterVouchers: List<MasterVoucherEntity>,
    limit: Int,
    settings: SettingsEntity
) {
    var computed by remember { mutableStateOf(false) }

    // Settlement States
    var sSales by remember { mutableStateOf(0) }
    var sWin by remember { mutableStateOf(0) }
    var sComm by remember { mutableStateOf(0.0) }
    var sPayout by remember { mutableStateOf(0) }
    var sNet by remember { mutableStateOf(0.0) }

    var mTotal by remember { mutableStateOf(0) }
    var mWinAmt by remember { mutableStateOf(0) }
    var mComm by remember { mutableStateOf(0.0) }
    var mPayout by remember { mutableStateOf(0) }
    var mNet by remember { mutableStateOf(0.0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = winNum,
            onValueChange = { viewModel.setWinNum(it) },
            label = { Text("ပေါက်ဂဏန်းရိုက်ပါ (ဥပမာ- 95)") },
            placeholder = { Text("ရိုက်ပါ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("winning_number_input"),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Slate800,
                unfocusedContainerColor = Slate800,
                focusedBorderColor = Emerald500,
                unfocusedBorderColor = Slate700,
                focusedTextColor = Slate100,
                unfocusedTextColor = Slate100,
                focusedLabelColor = Emerald500,
                unfocusedLabelColor = Slate300
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (winNum.length != 2) return@Button
                computed = true

                // Self Settlement
                var salesSum = 0
                var winSum = 0
                totalsMap.forEach { (num, amt) ->
                    val effective = minOf(amt, limit)
                    salesSum += effective
                    if (num == winNum) {
                        winSum = effective
                    }
                }
                sSales = salesSum
                sWin = winSum
                sComm = sSales * settings.commissionPercentage / 100.0
                sPayout = sWin * settings.payoutMultiplier
                sNet = sSales - sComm - sPayout

                // Master Settlement
                val sessionMasters = masterVouchers.filter { it.session == currentSession }
                var masterSalesSum = 0
                var masterWinSum = 0
                sessionMasters.forEach { mv ->
                    val items = CustomJsonHelper.fromJson(mv.itemsJson)
                    masterSalesSum += items.sumOf { it.amt }
                    masterWinSum += items.filter { it.num == winNum }.sumOf { it.amt }
                }

                mTotal = masterSalesSum
                mWinAmt = masterWinSum
                mComm = mTotal * settings.commissionPercentage / 100.0
                mPayout = mWinAmt * settings.payoutMultiplier
                mNet = mTotal - mComm - mPayout
            },
            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("calculate_pnl_button")
        ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = "Calculate")
            Spacer(modifier = Modifier.width(6.dp))
            Text("ရှင်းတမ်းထုတ်မည်", fontWeight = FontWeight.Bold)
        }

        if (computed) {
            Spacer(modifier = Modifier.height(16.dp))

            // Self Account Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                border = BorderStroke(1.dp, Slate700)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🙋‍♂️ မိမိစာရင်း (Self Balance)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Slate700)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ရောင်းရငွေ", color = Slate300, fontSize = 13.sp)
                        Text("${String.format("%,d", sSales)} Ks", color = Slate100, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ကော်မရှင် (${settings.commissionPercentage}%)", color = Slate300, fontSize = 13.sp)
                        Text("-${String.format("%,.0f", sComm)} Ks", color = Crimson500, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ပေါက်သီး (${winNum})", color = Slate300, fontSize = 13.sp)
                        Text("-${String.format("%,d", sPayout)} Ks", color = Crimson500, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Slate700)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("အသားတင် မြတ်/ရှုံး", fontWeight = FontWeight.Bold, color = Slate100, fontSize = 14.sp)
                        val color = if (sNet >= 0) Emerald500 else Crimson500
                        val text = if (sNet >= 0) "မြတ်" else "ရှုံး"
                        Text(
                            text = "${String.format("%,.0f", sNet.absoluteValue)} $text",
                            fontWeight = FontWeight.ExtraBold,
                            color = color,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Master Account Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                border = BorderStroke(1.dp, Slate700)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🏢 မာစတာစာရင်း (Master Balance)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Slate700)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("လွှဲငွေစုစုပေါင်း", color = Slate300, fontSize = 13.sp)
                        Text("${String.format("%,d", mTotal)} Ks", color = Slate100, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ကော်မရှင် (${settings.commissionPercentage}%)", color = Slate300, fontSize = 13.sp)
                        Text("-${String.format("%,.0f", mComm)} Ks", color = Crimson500, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ပေါက်သီး (${winNum})", color = Slate300, fontSize = 13.sp)
                        Text("-${String.format("%,d", mPayloadWin(mPayout))} Ks", color = Crimson500, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = Slate700)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("အသားတင် မြတ်/ရှုံး", fontWeight = FontWeight.Bold, color = Slate100, fontSize = 14.sp)
                        val color = if (mNet >= 0) Emerald500 else Crimson500
                        val text = if (mNet >= 0) "မြတ်" else "ရှုံး"
                        Text(
                            text = "${String.format("%,.0f", mNet.absoluteValue)} $text",
                            fontWeight = FontWeight.ExtraBold,
                            color = color,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

private fun mPayloadWin(v: Int): Int {
    return v
}

@Composable
fun SettingsTab(
    viewModel: AppViewModel,
    settings: SettingsEntity
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var limitInput by remember(settings.limitPrice) { mutableStateOf(settings.limitPrice.toString()) }
    var payoutInput by remember(settings.payoutMultiplier) { mutableStateOf(settings.payoutMultiplier.toString()) }
    var commInput by remember(settings.commissionPercentage) { mutableStateOf(settings.commissionPercentage.toString()) }
    var sessionsInput by remember(settings.sessionsCsv) { mutableStateOf(settings.sessionsCsv) }

    var showClearConfirm by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var pasteBackupInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate800),
            border = BorderStroke(1.dp, Slate700)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "⚙️ စနစ်သတ်မှတ်ချက်များ (Configuration)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text("နံပါတ်တစ်ခုစီ၏ Limit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100,
                        focusedBorderColor = Emerald500,
                        unfocusedBorderColor = Slate700
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = payoutInput,
                    onValueChange = { payoutInput = it },
                    label = { Text("ပေါက်ကြေး (အဆ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100,
                        focusedBorderColor = Emerald500,
                        unfocusedBorderColor = Slate700
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = commInput,
                    onValueChange = { commInput = it },
                    label = { Text("ကော်မရှင် (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100,
                        focusedBorderColor = Emerald500,
                        unfocusedBorderColor = Slate700
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = sessionsInput,
                    onValueChange = { sessionsInput = it },
                    label = { Text("သတ်မှတ်ထားသော ပွဲချိန်များ (Sessions)") },
                    placeholder = { Text("ဥပမာ- Morning, Evening") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Slate100,
                        unfocusedTextColor = Slate100,
                        focusedBorderColor = Emerald500,
                        unfocusedBorderColor = Slate700
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val limitInt = limitInput.toIntOrNull() ?: 200
                        val payoutInt = payoutInput.toIntOrNull() ?: 80
                        val commInt = commInput.toIntOrNull() ?: 10
                        if (sessionsInput.trim().isEmpty()) {
                            Toast.makeText(context, "Sessions ထည့်သွင်းပေးပါ!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.updateSettings(limitInt, payoutInt, commInt, sessionsInput)
                        Toast.makeText(context, "သတ်မှတ်ချက်များ သိမ်းဆည်းပြီးပါပြီ!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("save_settings_button")
                ) {
                    Text("Settings သိမ်းဆည်းမည်", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Backup and restore
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Slate800),
            border = BorderStroke(1.dp, Slate700)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "💾 အရန်သိမ်းဆည်းခြင်းနှင့် ပြန်လည်ရယူခြင်း",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val backupText = viewModel.generateBackupString()
                            clipboardManager.setText(AnnotatedString(backupText))
                            
                            // Also invoke system Share dialog
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, backupText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Save 2D Backup"))
                            Toast.makeText(context, "Backup Text copied to Clipboard!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo700),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export Backup")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Backup ထုတ်မည်", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { showRestoreDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Import Backup")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore သွင်းမည်", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Dangerous Zone
        Button(
            onClick = { showClearConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = Crimson500),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("clear_data_button")
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Reset database")
            Spacer(modifier = Modifier.width(6.dp))
            Text("စာရင်းအားလုံးဖျက်ပစ်မည် (Reset DB)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(60.dp))
    }

    // Reset Confirm Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("စာရင်းအားလုံး ဖျက်မည်လား?", color = Slate100) },
            text = { Text("ဤလုပ်ဆောင်ချက်သည် သိမ်းဆည်းထားသော မှတ်တမ်းများ၊ မာစတာမှတ်တမ်းများနှင့် သတ်မှတ်ချက်များကို အမိုးအပြတ် ဖျက်ဆီးပစ်ပါမည်။ ပြန်လည်ရရှိနိုင်မည် မဟုတ်ပါ။", color = Slate300) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirm = false
                        Toast.makeText(context, "ဒေတာအားလုံး တိုက်ဖျက်ပြီးပါပြီ!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Crimson500)
                ) {
                    Text("ဖျက်မည်")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showClearConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Slate700)
                ) {
                    Text("မလုပ်တော့ပါ")
                }
            },
            containerColor = Slate800
        )
    }

    // Restore Backup text pasting dialog
    if (showRestoreDialog) {
        Dialog(onDismissRequest = { showRestoreDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate800),
                border = BorderStroke(1.dp, Slate700)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "📥 Backup text ပြန်လည်ရယူရန်",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pasteBackupInput,
                        onValueChange = { pasteBackupInput = it },
                        placeholder = { Text("ဤနေရာတွင် Backup payload ကုဒ်များ Paste ချပါ...", color = Slate300.copy(alpha = 0.5f)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100,
                            focusedBorderColor = Emerald500,
                            unfocusedBorderColor = Slate700
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (pasteBackupInput.trim().isEmpty()) return@Button
                                viewModel.restoreBackupString(
                                    backupJson = pasteBackupInput,
                                    onSuccess = {
                                        showRestoreDialog = false
                                        pasteBackupInput = ""
                                        Toast.makeText(context, "Restore ဒေတာများ အောင်မြင်စွာ ပြန်သွင်းပြီးပါပြီ!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("တစ်ပြိုင်နက်သွင်းမည်")
                        }
                        Button(
                            onClick = { showRestoreDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ပယ်ဖျက်မည်")
                        }
                    }
                }
            }
        }
    }
}
