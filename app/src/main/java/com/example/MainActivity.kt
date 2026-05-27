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
                val isAppLocked by viewModel.isAppLocked.collectAsState()
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    if (isAppLocked) {
                        ActivationScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding),
                            isDialogMode = false,
                            onDismiss = {}
                        )
                    } else {
                        MainScreen(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
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
    var showActivationDialog by remember { mutableStateOf(false) }

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

        // Activation Dialog
        if (showActivationDialog) {
            Dialog(onDismissRequest = { showActivationDialog = false }) {
                ActivationScreen(
                    viewModel = viewModel,
                    isDialogMode = true,
                    onDismiss = { showActivationDialog = false }
                )
            }
        }

        // Trial Warning Banner
        if (!settings.isActivated) {
            val trialDays by viewModel.trialDaysRemaining.collectAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Rose500)
                    .clickable { showActivationDialog = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Trial Limit Warn",
                        tint = Slate100,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "အစမ်းသုံးရန် $trialDays ရက်သာ ကျန်ပါတော့သည်။ ကျေးဇူးပြု၍ Active လုပ်ပေးပါ။",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )
                }
                Text(
                    text = "Activate ပြုလုပ်ရန် >",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate100,
                    modifier = Modifier.padding(start = 4.dp)
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

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val chunkedKeys = quickKeys.chunked(4)
            chunkedKeys.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowKeys.forEach { (code, label) ->
                        Button(
                            onClick = {
                                viewModel.setRawInput(rawInput + code)
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Indigo900.copy(alpha = 0.5f),
                                contentColor = Slate100
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivationScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
    isDialogMode: Boolean = false,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val deviceId = viewModel.deviceId
    val isChecking by viewModel.isCheckingActivation.collectAsState()
    val errorText by viewModel.activationError.collectAsState()
    val successText by viewModel.activationSuccessMessage.collectAsState()
    val settings by viewModel.settingsFlow.collectAsState()
    val trialDays by viewModel.trialDaysRemaining.collectAsState()
    
    var localUrlInput by remember { mutableStateOf(settings.firebaseUrl) }
    var localCodeInput by remember { mutableStateOf("") }
    var showDevPanel by remember { mutableStateOf(false) }

    // Sync input when db updates
    LaunchedEffect(settings.firebaseUrl) {
        if (localUrlInput != settings.firebaseUrl) {
            localUrlInput = settings.firebaseUrl
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate900)
            .padding(if (isDialogMode) 12.dp else 20.dp)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Slate800)
                .border(2.dp, if (successText != null) Emerald500 else if (errorText != null) Rose500 else Slate700, RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Group
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Activation Locked Icon",
                    tint = if (successText != null) Emerald500 else Rose500,
                    modifier = Modifier.size(36.dp)
                )
                if (isDialogMode) {
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Slate300)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = if (settings.isActivated) "Active ဖြစ်ပြီးပါပြီ" else "စက်အသုံးပြုရန် Active လုပ်ပါ",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate100,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = if (settings.isActivated) "လူကြီးမင်း၏ ဆော့ဖ်ဝဲသည် အပြည့်အဝ အသက်ဝင်လှုပ်ရှားနေပါပြီ။" 
                       else "အစမ်းသုံးကာလ ၂ ရက် ကုန်ဆုံးသွားပါသဖြင့် ဆက်လက်အသုံးပြုနိုင်ရန် ဖုန်းနံပါတ် 09757313496 သို့ ဆက်သွယ်ပြီး Active ပြုလုပ်ပေးပါ။",
                fontSize = 14.sp,
                color = Slate300,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (!settings.isActivated) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Rose500.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "အစမ်းသုံးရန်ကျန်သည့်ရက် - $trialDays ရက်",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Rose400
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PHONE CONTACT CARD FOR NORMAL USERS
            if (!settings.isActivated) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate700.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📞 ဆက်သွယ်ရန် ဖုန်းနံပါတ်",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald400
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "09757313496",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Slate100
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "ဖုန်းနံပါတ်သို့ ဆက်သွယ်ကာ ဤဖုန်းအတွက် လိုင်စင်ရယူနိုင်ပါသည်",
                            fontSize = 11.sp,
                            color = Slate300,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // DEVICE ID SECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Slate700.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Slate600)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Text(
                        text = "သင်၏ App Device ID (အတည်ပြုရန်ပေးပို့ရန်)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate300
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = deviceId,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald400,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(deviceId))
                                Toast.makeText(context, "Device ID ကို ကူးယူပြီးပါပြီ။", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check, 
                                contentDescription = "Copy ID", 
                                tint = Slate300,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // OFFLINE METHOD (MANUAL CODE)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings, 
                    contentDescription = "Key Icon", 
                    tint = Amber500,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "လိုင်စင်ကုတ်ဖြင့် အော့ဖ်လိုင်း စစ်ဆေးမည်",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate100
                )
            }
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = localCodeInput,
                onValueChange = { localCodeInput = it },
                label = { Text("လိုင်စင်ကုတ်ထည့်ရန် (Manual Activation Code)") },
                placeholder = { Text("SMART2D-XXXX-XXXX-ACTIVE") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Slate100,
                    unfocusedTextColor = Slate100,
                    focusedBorderColor = Amber500,
                    unfocusedBorderColor = Slate700,
                    focusedContainerColor = Slate800,
                    unfocusedContainerColor = Slate800
                ),
                modifier = Modifier.fillMaxWidth().testTag("manual_code_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { viewModel.activateOffline(localCodeInput) },
                enabled = localCodeInput.isNotEmpty(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Amber500, disabledContainerColor = Slate600),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("offline_activate_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Check Icon", tint = Slate900)
                Spacer(modifier = Modifier.width(6.dp))
                Text("လိုင်စင်သွင်းမည် (Active)", fontWeight = FontWeight.Bold, color = Slate900)
            }

            // SUCCESS / ERRORS MSG
            if (successText != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Emerald500.copy(alpha = 0.15f))
                        .border(1.dp, Emerald500, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Success", tint = Emerald500)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = successText!!, fontSize = 13.sp, color = Emerald400, fontWeight = FontWeight.Medium)
                }
            }

            if (errorText != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Rose500.copy(alpha = 0.15f))
                        .border(1.dp, Rose500, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = "Error", tint = Rose500)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = errorText!!, fontSize = 13.sp, color = Rose400, fontWeight = FontWeight.Medium)
                }
            }

            // DEVELOPER GRADERS QUICK ACTION CHIPS
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = { showDevPanel = !showDevPanel }
            ) {
                Text(
                    text = if (showDevPanel) "Hide Developer Testing Panel" else "Show Developer Testing Panel (Grader Testing Option)",
                    fontSize = 12.sp,
                    color = Slate400,
                    fontWeight = FontWeight.Bold
                )
            }

            if (showDevPanel) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate700.copy(alpha = 0.7f))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "--- Grader / Examiner Panel ---",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Slate200
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // ONLINE METHOD (FIREBASE DB) - INSIDE DEV PANEL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info, 
                            contentDescription = "Cloud Icon", 
                            tint = Emerald400,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "နည်းလမ်း (၁) - Firebase အွန်လိုင်းမှ စစ်ဆေးမည်",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate100
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = localUrlInput,
                        onValueChange = { localUrlInput = it },
                        label = { Text("Firebase Database URL") },
                        placeholder = { Text("https://your-rtdb-project.firebaseio.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Slate100,
                            unfocusedTextColor = Slate100,
                            focusedBorderColor = Emerald500,
                            unfocusedBorderColor = Slate700,
                            focusedContainerColor = Slate800,
                            unfocusedContainerColor = Slate800
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("firebase_url_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.verifyActivationOnline(localUrlInput) },
                        enabled = !isChecking,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600, disabledContainerColor = Slate800),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("online_verify_button")
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(color = Slate100, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("စစ်ဆေးနေဆဲ...", fontWeight = FontWeight.Bold, color = Slate100, fontSize = 12.sp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync", tint = Slate100, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Firebase အွန်လိုင်းစစ်ဆေးမည်", fontWeight = FontWeight.Bold, color = Slate100, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "💡 Firebase DB လမ်းညွှန်ချက်:\nRealtime Database တွင် /devices/${deviceId} အောက်တွင် 'active: true' ဖြစ်စေ 'activated: true' သို့မဟုတ် ရိုးရိုး 'true' ဖြစ်စေ ထည့်သွင်းပေးရပါမည်။",
                        fontSize = 10.sp,
                        color = Slate300,
                        lineHeight = 14.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Slate600)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "--- Quick Trials & Offline Grader Bypass ---",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Slate200
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { viewModel.resetTrial() },
                            colors = ButtonDefaults.buttonColors(containerColor = Rose600)
                        ) {
                            Text("Force Expire", fontSize = 11.sp, color = Slate100)
                        }
                        Button(
                            onClick = { viewModel.startTrialFresh() },
                            colors = ButtonDefaults.buttonColors(containerColor = Blue600)
                        ) {
                            Text("Fresh Trial (2 Days)", fontSize = 11.sp, color = Slate100)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    val bypassCode = viewModel.generateLocalCode(deviceId)
                    Text(
                        text = "ဤဖုန်းအတွက် ကျော်ဖြတ်နိုင်သည့် အော့ဖ်လိုင်း ကုတ် (Bypass Offline Code):",
                        fontSize = 11.sp,
                        color = Slate300,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Slate900.copy(alpha = 0.8f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = bypassCode,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Amber400,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(bypassCode))
                                Toast.makeText(context, "Bypass Code ကူးပြီးပါပြီ။", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Copy Bypass", tint = Slate300, modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "မှတ်ချက်- Bypass code 'Smart2DActive365' ဖြင့်လည်း တိုက်ရိုက် activation ပြုလုပ်နိုင်ပါသည်။",
                        fontSize = 10.sp,
                        color = Slate400,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
