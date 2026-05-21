package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TransactionEntity
import com.example.ui.theme.*
import com.example.viewmodel.ChatMessage
import com.example.viewmodel.PocketWatchViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Dynamic liquid-glass card border renderer in Compose precisely implementing specifications:
// rgba(255,255,255,0.45) — top & bottom edge
// rgba(255,255,255,0.15) — near top & bottom
// rgba(255,255,255,0) — middle (transparent)
fun Modifier.liquidGlassBorder(
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.1.dp,
    isDarkMode: Boolean = true,
    borderColor: Color? = null
) = this.drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val borderCol = borderColor ?: Color(0xFF243142)
    // Premium glass border vertical gradient based on detailed style specification:
    val brush = if (borderColor != null) {
        SolidColor(borderCol)
    } else {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to borderCol.copy(alpha = 0.22f),  // top edge
                0.15f to borderCol.copy(alpha = 0.08f), // near top
                0.5f to Color.Transparent,              // middle (transparent)
                0.85f to borderCol.copy(alpha = 0.08f), // near bottom
                1.0f to borderCol.copy(alpha = 0.22f)   // bottom edge
            )
        )
    }
    onDrawWithContent {
        drawContent()
        drawOutline(
            outline = outline,
            brush = brush,
            style = Stroke(width = borderWidth.toPx())
        )
    }
}

@Composable
fun PocketWatchApp(
    viewModel: PocketWatchViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val isSpentTodaySelected by viewModel.isSpentTodaySelected.collectAsStateWithLifecycle()
    val chartTimeRange by viewModel.chartTimeRange.collectAsStateWithLifecycle()
    val showAddDialog by viewModel.showAddDialog.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val budgetLimit by viewModel.budgetLimit.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val dashboardAdvice by viewModel.dashboardAdvice.collectAsStateWithLifecycle()

    MyApplicationTheme(darkTheme = isDarkMode) {
        val isHomeActive = activeTab == 0
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // 1. Base BG: #ffffff, or #070511 on Homescreen
                    val baseBgColor = if (isHomeActive) Color(0xFF070511) else Color.White
                    drawRect(color = baseBgColor)
                }
        ) {
            // Adaptive Viewport (centers on desktop / wider screens)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 420.dp)
                    .align(Alignment.Center)
                    .testTag("app_viewport")
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        BottomNavigationBar(
                            activeIndex = activeTab,
                            isDarkMode = isDarkMode,
                            onTabSelected = { viewModel.selectTab(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        AnimatedContent(
                            targetState = activeTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) togetherWith
                                        fadeOut(animationSpec = tween(220))
                            },
                            label = "screen_navigation"
                        ) { targetTab ->
                            when (targetTab) {
                                0 -> DashboardScreen(
                                    viewModel = viewModel,
                                    transactions = transactions,
                                    isSpentTodaySelected = isSpentTodaySelected,
                                    chartTimeRange = chartTimeRange,
                                    budgetLimit = budgetLimit,
                                    dashboardAdvice = dashboardAdvice,
                                    isDarkMode = isDarkMode
                                )
                                1 -> WidgetsScreen(
                                    viewModel = viewModel,
                                    transactions = transactions
                                )
                                2 -> ArchivesScreen(
                                    viewModel = viewModel,
                                    transactions = transactions
                                )
                                3 -> AnalyticsScreen(
                                    viewModel = viewModel,
                                    transactions = transactions,
                                    budgetLimit = budgetLimit
                                )
                                4 -> ChatScreen(
                                    viewModel = viewModel,
                                    messages = chatMessages,
                                    isLoading = isAiLoading
                                )
                            }
                        }
                    }
                }

                // Custom Transaction dialogue popup
                if (showAddDialog) {
                    AddTransactionDialog(
                        onDismiss = { viewModel.setShowAddDialog(false) },
                        onAdd = { merchant, amount, category, customTime ->
                            viewModel.addNewTransaction(merchant, amount, category, customTime)
                            viewModel.setShowAddDialog(false)
                        },
                        isDarkMode = isDarkMode
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// TAB 0: DASHBOARD SCREEN
// -----------------------------------------------------------------
@Composable
fun DashboardScreen(
    viewModel: PocketWatchViewModel,
    transactions: List<TransactionEntity>,
    isSpentTodaySelected: Boolean,
    chartTimeRange: String,
    budgetLimit: Double,
    dashboardAdvice: String,
    isDarkMode: Boolean
) {
    val scrollState = rememberScrollState()

    // Authentic dark spaces theme palette configured strictly for our beautiful Homescreen
    val homeBgBlack = Color(0xFF070511)
    val homeCardBg = Color(0xFF0D0A1C)
    val homeCardBorder = Color(0xFF14241B)
    val homeElectricIndigo = Color(0xFF21BB5A)
    val homeNeonGreen = Color(0xFF21BB5A)
    val homeWhiteText = Color(0xFFF8FAFC)
    val homeMutedText = Color(0xFF94A3B8)
    val homeAlertGlow = Color(0xFF21BB5A)
    val homeSoftPurpleGlow = Color(0xFF070511)

    // Maintaining backward compatible variables with the exact same name for downstream elements
    val homePrimary = Color(0xFF4D62CD)
    val homeDarkText = homeWhiteText
    val homeTextSecondary = homeMutedText
    val homeTextMuted = homeMutedText.copy(alpha = 0.6f)
    val homeSurfaceColor6 = homeCardBg
    val homeHoverColor10 = homeCardBorder
    val homePositiveGreen = Color(0xFF21BB5A)
    val homeNegativeRed = Color(0xFFC7572D)

    // Dynamically compute spending variables
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    val startOfDay = calendar.timeInMillis

    val spentTodaySum = transactions
        .filter { it.amount < 0 && it.timestamp >= startOfDay }
        .sumOf { -it.amount }

    val totalSpentMonth = transactions
        .filter { it.amount < 0 }
        .sumOf { -it.amount }

    val balanceRemaining = budgetLimit - totalSpentMonth

    // Staggered Entrance Animations using state triggers
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateIn = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        // ① Status Bar Centered Pill Option
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 10.dp)
                .size(width = 80.dp, height = 5.dp)
                .background(
                    color = homeHoverColor10,
                    shape = RoundedCornerShape(999.dp)
                )
        )

        // ② Top Bar
        StaggeredAnim(delay = 0, trigger = animateIn) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile avatar liquid-glass
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = homeSurfaceColor6,
                                shape = CircleShape
                            )
                            .liquidGlassBorder(CircleShape, isDarkMode = isDarkMode, borderColor = homeCardBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "S",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = homeDarkText
                        )
                    }

                    Column {
                        Text(
                            text = "Good afternoon, Shlok",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = homeDarkText
                        )
                        Text(
                            text = "PocketWatch ≡",
                            fontSize = 12.sp,
                            color = homeTextSecondary
                        )
                    }
                }

                // Moon icon dynamic action mode
                IconButton(
                    onClick = { viewModel.toggleTheme() },
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = homeSurfaceColor6,
                            shape = CircleShape
                        )
                        .liquidGlassBorder(CircleShape, isDarkMode = isDarkMode, borderColor = homeCardBorder)
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.NightsStay,
                        contentDescription = "Toggle Theme",
                        tint = homeDarkText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // ③ Toggle Tabs Block
        StaggeredAnim(delay = 80, trigger = animateIn) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 16.dp, top = 4.dp)
                    .fillMaxWidth()
                    .background(
                        color = homeSurfaceColor6,
                        shape = RoundedCornerShape(999.dp)
                    )
                    .liquidGlassBorder(
                        shape = RoundedCornerShape(999.dp),
                        borderWidth = 1.6.dp,
                        isDarkMode = isDarkMode,
                        borderColor = homeCardBorder
                    )
                    .padding(3.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Balance Remaining
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { viewModel.setSpentTodaySelected(false) }
                            .then(
                                if (!isSpentTodaySelected) {
                                    Modifier.background(
                                        color = homePrimary
                                    )
                                } else Modifier
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Balance Remaining",
                            fontSize = 13.sp,
                            fontWeight = if (!isSpentTodaySelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (!isSpentTodaySelected) {
                                Color.White
                            } else {
                                homeTextMuted
                            }
                        )
                    }

                    // Spent Today
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { viewModel.setSpentTodaySelected(true) }
                            .then(
                                if (isSpentTodaySelected) {
                                    Modifier.background(
                                        color = homePrimary
                                    )
                                } else Modifier
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Spent Today",
                            fontSize = 13.sp,
                            fontWeight = if (isSpentTodaySelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSpentTodaySelected) {
                                Color.White
                            } else {
                                homeTextMuted
                            }
                        )
                    }
                }
            }
        }

        // ④ Spent Today active banner label
        StaggeredAnim(delay = 140, trigger = animateIn) {
            Text(
                text = if (isSpentTodaySelected) "Spent Today" else "Remaining Budget Space",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = homeTextSecondary,
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }

        // ⑤ Master Ledger Balance Stack
        StaggeredAnim(delay = 160, trigger = animateIn) {
            val balanceToShow = if (isSpentTodaySelected) spentTodaySum else balanceRemaining
            val rupeeText = "₹${String.format("%,.0f", balanceToShow).split(".")[0]}"
            val centText = ".00"

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.testTag("big_balance")
                ) {
                    Text(
                        text = rupeeText,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1.5).sp,
                        color = homeDarkText,
                    )
                    Text(
                        text = centText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        color = homeTextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // Sub-balance status pill
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                color = homeNeonGreen.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Drop Indicator",
                            tint = homePositiveGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "₹281 less than yesterday",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = homePositiveGreen
                        )
                    }

                    Text(
                        text = "Hide",
                        fontSize = 12.sp,
                        color = homeTextMuted,
                        modifier = Modifier.clickable { /* Toggle hide */ }
                    )
                }
            }
        }

        // ⑥ Monthly Spending Glass Chart Card
        StaggeredAnim(delay = 240, trigger = animateIn) {
            GlassCard(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                isDarkMode = isDarkMode,
                containerColor = homeCardBg,
                borderColor = homeCardBorder
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "Monthly Spending",
                                fontSize = 13.sp,
                                color = homeTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹${String.format("%,.0f", totalSpentMonth)}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                                color = homeDarkText
                            )
                        }

                        // range indicator
                        Box(
                            modifier = Modifier
                                .background(
                                    color = homeHoverColor10,
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = chartTimeRange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = homeDarkText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "↑ 14% vs last month",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = homeNegativeRed
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Area Chart Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                    ) {
                        SpendingWaveChart(
                            transactions = transactions,
                            isDarkMode = isDarkMode,
                            modifier = Modifier.fillMaxSize(),
                            strokeColor = homeNeonGreen,
                            accentColor = homeElectricIndigo,
                            fillStartColor = homeNeonGreen.copy(alpha = 0.08f),
                            fillEndColor = Color.Transparent
                        )

                        // Floating Custom tool tip representation on the right
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .offset(x = (-4).dp, y = (-20).dp)
                                .background(
                                    color = homeCardBg,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .liquidGlassBorder(
                                    shape = RoundedCornerShape(8.dp),
                                    borderWidth = 1.dp,
                                    isDarkMode = isDarkMode,
                                    borderColor = homeCardBorder
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Jul '26",
                                    fontSize = 10.sp,
                                    color = homeTextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "₹${String.format("%,.1fk", totalSpentMonth / 1000.0)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = homeDarkText
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val labels = listOf("Feb", "Mar", "Apr", "May", "Jun", "Jul")
                        labels.forEach { label ->
                             Text(
                                 text = label,
                                 fontSize = 11.sp,
                                 color = homeTextMuted
                             )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Timeline selection pill buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("1m", "3m", "6m", "1y").forEach { range ->
                            val active = chartTimeRange == range
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        color = if (active) homePrimary else homeSurfaceColor6,
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .clickable { viewModel.setChartTimeRange(range) }
                                    .liquidGlassBorder(
                                        shape = RoundedCornerShape(999.dp),
                                        borderWidth = 1.dp,
                                        isDarkMode = isDarkMode,
                                        borderColor = if (active) homePrimary else homeCardBorder
                                    )
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = range,
                                    fontSize = 12.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    color = if (active) Color.White else homeTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // ⑦ AI Assistant Panel (Dark Navy Core Atmosphere Card)
        StaggeredAnim(delay = 320, trigger = animateIn) {
            GlassCard(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                isDarkMode = isDarkMode,
                containerColor = homeCardBg,
                borderColor = homeCardBorder
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = homeSurfaceColor6,
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Action",
                                    tint = homeDarkText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "AI Assistant Insights",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = homeDarkText
                            )
                        }

                        // Insight Audit Button
                        IconButton(
                            onClick = { viewModel.selectTab(4) }, // goes to chat tab
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = homeSurfaceColor6,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Advice Audit",
                                tint = homeDarkText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Parse advice highlighting bold items
                    FormattedRichText(
                        rawText = dashboardAdvice,
                        color = homeTextSecondary,
                        boldColor = homeDarkText,
                        fontSize = 13.sp,
                        lineHeight = 1.6
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.selectTab(4)
                                viewModel.triggerBudgetOptimization()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = homeSurfaceColor6,
                                contentColor = homeDarkText
                            ),
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .liquidGlassBorder(RoundedCornerShape(999.dp), borderWidth = 1.dp, isDarkMode = isDarkMode, borderColor = homeCardBorder)
                        ) {
                            Text(
                                "⚡ Optimise Budget",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                color = homeDarkText
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.selectTab(4)
                                viewModel.triggerSmartSaving()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = homeSurfaceColor6,
                                contentColor = homeDarkText
                            ),
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .liquidGlassBorder(RoundedCornerShape(999.dp), borderWidth = 1.dp, isDarkMode = isDarkMode, borderColor = homeCardBorder)
                        ) {
                            Text(
                                "✦ Smart Saving",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                color = homeDarkText
                            )
                        }
                    }
                }
            }
        }

        // ⑧ Progress Budget Bar Tracker Card
        StaggeredAnim(delay = 400, trigger = animateIn) {
            GlassCard(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                isDarkMode = isDarkMode,
                containerColor = homeCardBg,
                borderColor = homeCardBorder
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monthly Budget Pace",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = homeDarkText
                        )
                        
                        val usedPercent = if (budgetLimit > 0) (totalSpentMonth / budgetLimit * 100).toInt() else 0
                        val speedColor = if (totalSpentMonth > budgetLimit) homeNegativeRed else homePositiveGreen
                        Text(
                            text = "$usedPercent% used",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = speedColor,
                            modifier = Modifier
                                .background(speedColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val progressFraction = if (budgetLimit > 0) (totalSpentMonth / budgetLimit).coerceIn(0.0, 1.0).toFloat() else 0f
                    val filledColor = if (totalSpentMonth > budgetLimit) homeNegativeRed else homePositiveGreen

                    // Custom progress indicator path
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(homeHoverColor10, RoundedCornerShape(999.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .background(filledColor, RoundedCornerShape(999.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Spent: ₹${String.format("%,.0f", totalSpentMonth)}",
                            fontSize = 12.sp,
                            color = homeTextSecondary
                        )
                        Text(
                            text = "Limit: ₹${String.format("%,.0f", budgetLimit)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = homeDarkText
                        )
                    }
                }
            }
        }

        // ⑨ Recent Ledger Entries (Historical Ledger Transactions block)
        StaggeredAnim(delay = 480, trigger = animateIn) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Ledger Entries",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = homeDarkText
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { viewModel.selectTab(2) } // Archives tab
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = "Sort direction",
                            tint = homeTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "View Archive",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = homeTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    isDarkMode = isDarkMode,
                    containerColor = homeCardBg,
                    borderColor = homeCardBorder
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        if (transactions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No transactions added.",
                                    fontSize = 13.sp,
                                    color = homeTextMuted
                                )
                            }
                        } else {
                            val itemsToShow = transactions.take(4)
                            itemsToShow.forEachIndexed { idx, item ->
                                TransactionRow(
                                    item = item,
                                    isDarkMode = isDarkMode,
                                    isLast = idx == itemsToShow.size - 1,
                                    onDelete = { viewModel.deleteTransaction(it) },
                                    textColor = homeDarkText,
                                    mutedColor = homeTextSecondary,
                                    positiveColor = homePositiveGreen,
                                    negativeColor = homeNegativeRed,
                                    dividerColor = homeCardBorder
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// HIGH FIDELITY ASSET / POSITION ITEM ROW
// -----------------------------------------------------------------
@Composable
fun PositionItemRow(
    name: String,
    desc: String,
    price: String,
    delta: String,
    isPositive: Boolean,
    avatarEmoji: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon sphere with blue verified credential badge overlay
        Box(
            modifier = Modifier.size(42.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF161326), CircleShape)
                    .border(1.dp, Color(0xFF1F2432), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(avatarEmoji, fontSize = 20.sp)
            }
            
            // Tiny blue check circle aligned bottom right of asset
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd)
                    .background(Color(0xFF1E90FF), CircleShape)
                    .border(1.dp, Color(0xFF070511), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Verified",
                    tint = Color.White,
                    modifier = Modifier.size(8.dp)
                )
            }
        }

        // Title MC labels
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }

        // Ticker Prices & change percentage indicators
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = price,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = delta,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPositive) Color(0xFF21BB5A) else Color(0xFFC7572D)
            )
        }
    }
}

// -----------------------------------------------------------------
// COPIED CLOSED POSITION TRANSACTION ROW
// -----------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionClosedPositionRow(
    item: TransactionEntity,
    isLast: Boolean,
    onDelete: (TransactionEntity) -> Unit,
    textColor: Color,
    mutedColor: Color,
    positiveColor: Color,
    negativeColor: Color,
    dividerColor: Color
) {
    var showContextMenu by remember { mutableStateOf(false) }

    val dateStr = remember(item.timestamp) {
        val sdf = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
        sdf.format(Date(item.timestamp))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* Detail Action */ },
                onLongClick = { showContextMenu = true }
            )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Character sphere or Initials Avatar with verification check overlay
                Box(
                    modifier = Modifier.size(42.dp)
                ) {
                    val avatarBg = remember(item.avatarBgColorHex) {
                        try {
                            Color(android.graphics.Color.parseColor(item.avatarBgColorHex))
                        } catch (e: Exception) {
                            Color(0xFF161B33)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(avatarBg, CircleShape)
                            .border(1.dp, dividerColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.initials.uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Sphere check badge
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .background(Color(0xFF1E90FF), CircleShape)
                            .border(1.dp, Color(0xFF070511), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Merchant",
                            tint = Color.White,
                            modifier = Modifier.size(8.dp)
                        )
                    }
                }

                // Metadata Details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.merchant,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = dateStr,
                        fontSize = 11.sp,
                        color = mutedColor
                    )
                }

                // Balance amounts and percentage offset tags matching Screen 1 closed positions
                val itemAmount = item.amount
                val formattedAmount = remember(itemAmount) {
                    val sign = if (itemAmount > 0) "+" else "−"
                    "$sign₹${String.format("%,.0f", Math.abs(itemAmount))}"
                }
                val textCol = if (itemAmount > 0) positiveColor else negativeColor
                val percentageStr = remember(itemAmount) {
                    val pseudoPercent = (Math.abs(itemAmount.hashCode()) % 40) + 5.5
                    val sign = if (itemAmount > 0) "▲" else "▼"
                    "$sign ${String.format("%.2f", pseudoPercent)}%"
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formattedAmount,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textCol
                    )
                    Text(
                        text = percentageStr,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textCol
                    )
                }
            }

            if (!isLast) {
                HorizontalDivider(
                    color = dividerColor,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            modifier = Modifier.background(Color(0xFF0D0A1C))
        ) {
            DropdownMenuItem(
                text = { Text("Delete Entry", color = Color.White) },
                onClick = {
                    onDelete(item)
                    showContextMenu = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = negativeColor
                    )
                }
            )
        }
    }
}

// Transaction Row representation block
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    item: TransactionEntity,
    isDarkMode: Boolean,
    isLast: Boolean,
    onDelete: (TransactionEntity) -> Unit,
    textColor: Color = TextPrimary,
    mutedColor: Color = TextMuted,
    positiveColor: Color = PositiveGreen,
    negativeColor: Color = NegativeRed,
    dividerColor: Color = SurfaceColor6
) {
    var showContextMenu by remember { mutableStateOf(false) }

    val dateStr = remember(item.timestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = sdf.format(Date(item.timestamp))

        val calendar = Calendar.getInstance()
        val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(calendar.time)

        val itemStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(item.timestamp))

        when (itemStr) {
            todayStr -> "Today · $formattedTime"
            yesterdayStr -> "Yesterday · $formattedTime"
            else -> {
                val fullSdf = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())
                fullSdf.format(Date(item.timestamp))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* Detail action */ },
                onLongClick = { showContextMenu = true }
            )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Initial avatar
                val bgCol = remember(item.avatarBgColorHex) {
                    try {
                        Color(android.graphics.Color.parseColor(item.avatarBgColorHex))
                    } catch (e: Exception) {
                        Color(0xFF6496C8)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(color = bgCol, shape = CircleShape)
                        .border(
                            width = 1.dp,
                            color = Color(0x33FFFFFF),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.initials,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Metadata details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.merchant,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = dateStr,
                        fontSize = 12.sp,
                        color = mutedColor
                    )
                }

                // Balance movement Amount
                val formattedAmount = remember(item.amount) {
                    val sign = if (item.amount > 0) "+" else "−"
                    "$sign₹${String.format("%,.0f", Math.abs(item.amount))}"
                }
                val textCol = if (item.amount > 0) positiveColor else negativeColor

                Text(
                    text = formattedAmount,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textCol,
                    textAlign = TextAlign.End
                )
            }

            if (!isLast) {
                HorizontalDivider(
                    color = dividerColor,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            modifier = Modifier.background(Color.White)
        ) {
            DropdownMenuItem(
                text = { Text("Delete Entry") },
                onClick = {
                    onDelete(item)
                    showContextMenu = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = DebitRed
                    )
                }
            )
        }
    }
}

// Custom Area Chart wave drawing
@Composable
fun SpendingWaveChart(
    transactions: List<TransactionEntity>,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    strokeColor: Color = AccentGreen,
    accentColor: Color = AccentGold,
    fillStartColor: Color = Color(0x3B2ECC8A),
    fillEndColor: Color = Color(0x002ECC8A)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Custom path fitting specifications: start low left -> peak mid -> dip -> rising right
        val path = Path()
        path.moveTo(0f, h * 0.70f)

        val cp1x = w * 0.25f
        val cp1y = h * 0.20f

        val cp2x = w * 0.55f
        val cp2y = h * 0.80f

        val cp3x = w * 0.80f
        val cp3y = h * 0.45f

        val endPoint = Offset(w, h * 0.35f)

        // Smooth cubic cubic beziers
        path.cubicTo(
            w * 0.12f, h * 0.65f,
            w * 0.18f, h * 0.20f,
            cp1x, cp1y
        )
        path.cubicTo(
            w * 0.32f, cp1y,
            w * 0.45f, cp2y,
            cp2x, cp2y
        )
        path.cubicTo(
            w * 0.65f, cp2y,
            w * 0.72f, cp3y,
            cp3x, cp3y
        )
        path.cubicTo(
            w * 0.88f, cp3y,
            w * 0.94f, h * 0.35f,
            endPoint.x, endPoint.y
        )

        // Close and paint path gradient
        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(w, h)
        fillPath.lineTo(0f, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    fillStartColor,
                    fillEndColor
                ),
                startY = h * 0.20f,
                endY = h
            )
        )

        // Main curve line stroke
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Cohesive focus dot rightmost climbing tip matching line color
        drawCircle(
            color = strokeColor,
            radius = 5.dp.toPx(),
            center = endPoint
        )
    }
}

// -----------------------------------------------------------------
// TAB 1: PORTABLE WIDGETS SECTION
// -----------------------------------------------------------------
@Composable
fun WidgetsScreen(
    viewModel: PocketWatchViewModel,
    transactions: List<TransactionEntity>
) {
    var compoundRate by remember { mutableStateOf("7.5") }
    var recurringMonthly by remember { mutableStateOf("5000") }
    var outputGoal by remember { mutableStateOf("0.0") }

    val focusManager = LocalFocusManager.current
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Financial Multipliers",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color.White else TextPrimary
        )

        // 1. Compound Simulator Widget
        GlassCard(modifier = Modifier.fillMaxWidth(), isDarkMode = isDarkMode) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Compound Interest Goal Engine",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Simulate index fund returns on an off-ledger account.",
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color(0x9AFFFFFF) else Color(0x801E2D46)
                )

                Spacer(modifier = Modifier.height(14.dp))

                TextField(
                    value = recurringMonthly,
                    onValueChange = { recurringMonthly = it },
                    label = { Text("Monthly Inflow (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = HoverColor10,
                        unfocusedContainerColor = SurfaceColor6,
                        focusedIndicatorColor = Primary,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = TextPrimary,
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = compoundRate,
                    onValueChange = { compoundRate = it },
                    label = { Text("Annual Yield Rate (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = HoverColor10,
                        unfocusedContainerColor = SurfaceColor6,
                        focusedIndicatorColor = Primary,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = TextPrimary,
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val r = (compoundRate.toDoubleOrNull() ?: 7.5) / 100 / 12
                        val p = recurringMonthly.toDoubleOrNull() ?: 5000.0
                        var total = 0.0
                        // calculate 5 years balance
                        for (i in 1..60) {
                            total = (total + p) * (1 + r)
                        }
                        outputGoal = String.format("%,.0f", total)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RequestedButtonBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Decline-to-Compete: Simulate 5 Years", color = Color.White, fontWeight = FontWeight.Bold)
                }

                if (outputGoal != "0.0") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HoverColor10, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Total Capital After 5 Years",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Text(
                                "₹$outputGoal",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // 2. Pocket Cash Limit Setter
        GlassCard(modifier = Modifier.fillMaxWidth(), isDarkMode = isDarkMode) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Configure Budget Bounds",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                var currentLimitValue by remember { mutableStateOf("20000") }

                TextField(
                    value = currentLimitValue,
                    onValueChange = { currentLimitValue = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Monthly Upper Threshold (₹)") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = HoverColor10,
                        unfocusedContainerColor = SurfaceColor6,
                        focusedIndicatorColor = Primary,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = TextPrimary,
                        unfocusedLabelColor = TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val num = currentLimitValue.toDoubleOrNull() ?: 20000.0
                        viewModel.setBudgetLimit(num)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RequestedButtonBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Modify Alert Limit Bounds", color = Color.White)
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// TAB 2: ARCHIVES LOG LIST
// -----------------------------------------------------------------
@Composable
fun ArchivesScreen(
    viewModel: PocketWatchViewModel,
    transactions: List<TransactionEntity>
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    var searchField by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    val filteredList = remember(transactions, searchField, selectedCategoryFilter) {
        transactions.filter {
            val matchesSearch = it.merchant.lowercase().contains(searchField.lowercase()) ||
                    it.category.lowercase().contains(searchField.lowercase())
            val matchesCat = selectedCategoryFilter == "All" || it.category == selectedCategoryFilter
            matchesSearch && matchesCat
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Pocket Ledger Audit",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color.White else TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar container
        TextField(
            value = searchField,
            onValueChange = { searchField = it },
            placeholder = { Text("Filter ledger by merchant...") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = HoverColor10,
                unfocusedContainerColor = SurfaceColor6,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLeadingIconColor = Primary,
                unfocusedLeadingIconColor = TextMuted
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter tags
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf("All", "Food", "Transit", "Credits", "Shopping", "Other")
            categories.forEach { cat ->
                val active = selectedCategoryFilter == cat
                Box(
                    modifier = Modifier
                        .background(
                            color = if (active) AccentGold else {
                                if (isDarkMode) Color(0x14FFFFFF) else Color(0x141E2D46)
                            },
                            shape = RoundedCornerShape(999.dp)
                        )
                        .clickable { selectedCategoryFilter = cat }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else {
                            if (isDarkMode) Color(0x8CFFFFFF) else Color(0xBF1E2D46)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            isDarkMode = isDarkMode
        ) {
            if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No match for the query filter.",
                        color = if (isDarkMode) Color(0x66FFFFFF) else Color(0x661E2D46)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 4.dp)
                ) {
                    items(
                        items = filteredList,
                        key = { it.id }
                    ) { item ->
                        TransactionRow(
                            item = item,
                            isDarkMode = isDarkMode,
                            isLast = item == filteredList.last(),
                            onDelete = { viewModel.deleteTransaction(item) }
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// TAB 3: ANALYTICS VISUAL BREAKDOWN SCREEN
// -----------------------------------------------------------------
@Composable
fun AnalyticsScreen(
    viewModel: PocketWatchViewModel,
    transactions: List<TransactionEntity>,
    budgetLimit: Double
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    val totalSpent = transactions.filter { it.amount < 0 }.sumOf { -it.amount }

    // Aggregate category splits
    val catSplits = remember(transactions) {
        val splits = mutableMapOf<String, Double>()
        transactions.filter { it.amount < 0 }.forEach {
            splits[it.category] = (splits[it.category] ?: 0.0) + (-it.amount)
        }
        splits.toList().sortedByDescending { it.second }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Category Distributions",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color.White else TextPrimary
        )

        // Progress Pie rings inside glass card
        GlassCard(modifier = Modifier.fillMaxWidth(), isDarkMode = isDarkMode) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Outflow Fractions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Beautiful interactive ring on canvas
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 14.dp.toPx()
                        var currentAngle = -90f

                        val colors = listOf(AccentGold, AccentGreen, Color(0xFFC88264), Color(0xFF7599D6), Color(0xFFC04B4B))

                        if (catSplits.isEmpty() || totalSpent == 0.0) {
                            drawArc(
                                color = Color(0x1F1E2D46),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeW)
                            )
                        } else {
                            catSplits.forEachIndexed { i, split ->
                                val fraction = (split.second / totalSpent).toFloat()
                                val sweep = fraction * 360f
                                drawArc(
                                    color = colors[i % colors.size],
                                    startAngle = currentAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                )
                                currentAngle += sweep
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Spent",
                            fontSize = 11.sp,
                            color = if (isDarkMode) Color(0x9AFFFFFF) else Color(0x801E2D46)
                        )
                        Text(
                            text = "₹${String.format("%,.0f", totalSpent)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Category lists indicating percentages
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val colors = listOf(AccentGold, AccentGreen, Color(0xFFC48B69), Color(0xFF678BCF), Color(0xFFBF5454))

                    catSplits.forEachIndexed { i, split ->
                        val pct = if (totalSpent > 0) (split.second / totalSpent * 100).toInt() else 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(colors[i % colors.size], CircleShape)
                                )
                                Text(
                                    text = split.first,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDarkMode) Color.White else TextPrimary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "$pct%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors[i % colors.size]
                                )
                                Text(
                                    text = "₹${String.format("%,.0f", split.second)}",
                                    fontSize = 13.sp,
                                    color = if (isDarkMode) Color(0x99FFFFFF) else Color(0x801E2D46)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------
// TAB 4: ACTIVE AI ASSISTANT CHAT SCREEN
// -----------------------------------------------------------------
@Composable
fun ChatScreen(
    viewModel: PocketWatchViewModel,
    messages: List<ChatMessage>,
    isLoading: Boolean
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    var rawTextPrompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll to bottom whenever new message is received
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "PocketWatch Wisdom Chat",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color.White else TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    color = if (isDarkMode) Color(0x13FFFFFF) else Color(0x14FFFFFF),
                    shape = RoundedCornerShape(16.dp)
                )
                .liquidGlassBorder(
                    shape = RoundedCornerShape(16.dp),
                    borderWidth = 1.dp,
                    isDarkMode = isDarkMode
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                items = messages,
                key = { it.id }
            ) { msg ->
                if (msg.isUser) {
                    UserTextBubble(msg.text, isDarkMode = isDarkMode)
                } else {
                    AiTextBubble(msg.text, isDarkMode = isDarkMode)
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.6.dp,
                            color = AccentGold
                        )
                        Text(
                            text = "Analyzing your spending trends with Gemini...",
                            fontSize = 11.sp,
                            color = AccentGold,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // TextInput segment
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = rawTextPrompt,
                onValueChange = { rawTextPrompt = it },
                placeholder = { Text("Ask about Zomato/saving goals...") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = HoverColor10,
                    unfocusedContainerColor = SurfaceColor6,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedPlaceholderColor = TextMuted,
                    unfocusedPlaceholderColor = TextMuted
                ),
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            FloatingActionButton(
                onClick = {
                    if (rawTextPrompt.isNotBlank()) {
                        viewModel.sendChatMessage(rawTextPrompt)
                        rawTextPrompt = ""
                    }
                },
                containerColor = AccentGold,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun UserTextBubble(text: String, isDarkMode: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = AccentGold,
                    shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 260.dp)
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun AiTextBubble(text: String, isDarkMode: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isDarkMode) Color(0x40070B1D) else Color(0x80FFFFFF),
                    shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                )
                .liquidGlassBorder(
                    shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                    borderWidth = 1.dp,
                    isDarkMode = isDarkMode
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .widthIn(max = 280.dp)
        ) {
            FormattedRichText(
                rawText = text,
                color = if (isDarkMode) Color(0xCCFFFFFF) else TextPrimary,
                boldColor = if (isDarkMode) Color.White else Color.Black,
                fontSize = 13.sp,
                lineHeight = 1.5
            )
        }
    }
}

// -----------------------------------------------------------------
// LOW-LEVEL SHARED COMPONENTS & UTILS
// -----------------------------------------------------------------
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    containerColor: Color? = null,
    borderColor: Color? = null,
    cornerRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    // Implementing premium custom background:
    // rgba(255, 255, 255, 0.1) — inset box shadow / subtle glaze -> Color(0x1AFFFFFF)
    // rgba(255, 255, 255, 0.01) — glass background -> Color(0x03FFFFFF)
    val defaultBg = if (isDarkMode) Color(0x1AFFFFFF) else CardBgGlass
    Box(
        modifier = modifier
            .background(
                color = containerColor ?: defaultBg,
                shape = RoundedCornerShape(cornerRadius)
            )
            .liquidGlassBorder(
                shape = RoundedCornerShape(cornerRadius),
                borderWidth = 1.1.dp,
                isDarkMode = isDarkMode,
                borderColor = borderColor
            )
    ) {
        content()
    }
}

@Composable
fun FormattedRichText(
    rawText: String,
    color: Color,
    boldColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: Double
) {
    val annotatedString = remember(rawText) {
        val spans = rawText.split("**")
        buildAnnotatedString {
            spans.forEachIndexed { index, spanText ->
                val isBold = index % 2 != 0
                if (isBold) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = boldColor)) {
                        append(spanText)
                    }
                } else {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal, color = color)) {
                        append(spanText)
                    }
                }
            }
        }
    }

    Text(
        text = annotatedString,
        fontSize = fontSize,
        lineHeight = (fontSize.value * lineHeight).sp
    )
}

@Composable
fun BottomNavigationBar(
    activeIndex: Int,
    isDarkMode: Boolean,
    onTabSelected: (Int) -> Unit
) {
    // Sheer glass backgrounds matched to premium specification:
    // rgba(255, 255, 255, 0.1) glaze backdrop
    val containerBg = if (isDarkMode) Color(0x16FFFFFF) else Color(0x1CFFFFFF)
    val containerBorder = Color(0x40FFFFFF) // top & bottom edge highlight (rgba(255,255,255,0.25))

    // Floating glass pill bottom navigation container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = containerBg,
                    shape = RoundedCornerShape(999.dp)
                )
                .liquidGlassBorder(
                    shape = RoundedCornerShape(999.dp),
                    borderWidth = 1.3.dp, // slightly more pronounced border highlight for glass effect
                    isDarkMode = isDarkMode
                )
                .padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 0: Home/Dashboard
            BottomTabItem(
                targetIndex = 0,
                currentIndex = activeIndex,
                onClick = { onTabSelected(0) },
                iconSelected = Icons.Default.Home,
                iconUnselected = Icons.Outlined.Home,
                label = "Home",
                isDarkMode = isDarkMode
            )

            // Tab 1: Grid Multipliers
            BottomTabItem(
                targetIndex = 1,
                currentIndex = activeIndex,
                onClick = { onTabSelected(1) },
                iconSelected = Icons.Default.Window,
                iconUnselected = Icons.Outlined.Window,
                label = "Pockets",
                isDarkMode = isDarkMode,
                testTag = "tab_widgets"
            )

            // Tab 2: Clock
            BottomTabItem(
                targetIndex = 2,
                currentIndex = activeIndex,
                onClick = { onTabSelected(2) },
                iconSelected = Icons.Default.AccessTimeFilled,
                iconUnselected = Icons.Outlined.AccessTime,
                label = "Time",
                isDarkMode = isDarkMode,
                testTag = "tab_archives"
            )

            // Tab 3: Stats
            BottomTabItem(
                targetIndex = 3,
                currentIndex = activeIndex,
                onClick = { onTabSelected(3) },
                iconSelected = Icons.Default.TrendingUp,
                iconUnselected = Icons.Default.TrendingUp,
                label = "Trends",
                isDarkMode = isDarkMode,
                testTag = "tab_stats"
            )

            // Tab 4: Chat assistant
            BottomTabItem(
                targetIndex = 4,
                currentIndex = activeIndex,
                onClick = { onTabSelected(4) },
                iconSelected = Icons.Default.Message,
                iconUnselected = Icons.Outlined.Message,
                label = "Chat",
                isDarkMode = isDarkMode,
                testTag = "tab_chat"
            )
        }
    }
}

@Composable
fun RowScope.BottomTabItem(
    targetIndex: Int,
    currentIndex: Int,
    onClick: () -> Unit,
    iconSelected: androidx.compose.ui.graphics.vector.ImageVector,
    iconUnselected: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDarkMode: Boolean,
    testTag: String? = null
) {
    val active = currentIndex == targetIndex
    // Slate/17% brand active background pill
    val activeBg = BottomNavActive
    val activeContentColor = Primary
    val inactiveContentColor = BottomNavText

    Box(
        modifier = Modifier
            .weight(if (active) 1.6f else 1.0f) // Soft weight distribution
            .then(
                if (testTag != null) Modifier.testTag(testTag) else Modifier
            )
            .then(
                if (active) {
                    Modifier.background(
                        color = activeBg,
                        shape = RoundedCornerShape(999.dp)
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(999.dp))
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.animateContentSize(tween(180, easing = LinearOutSlowInEasing))
        ) {
            Icon(
                imageVector = if (active) iconSelected else iconUnselected,
                contentDescription = label,
                tint = if (active) activeContentColor else inactiveContentColor,
                modifier = Modifier.size(18.dp)
            )
            if (active) {
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = activeContentColor,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun StaggeredAnim(
    delay: Int,
    trigger: Boolean,
    content: @Composable () -> Unit
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            launch {
                animatedProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 600,
                        delayMillis = delay,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .graphicsLayer(
                alpha = animatedProgress.value,
                translationY = (30 * (1 - animatedProgress.value))
            )
    ) {
        content()
    }
}

// Dialog Composable for inserting local data
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, String, String?) -> Unit,
    isDarkMode: Boolean
) {
    var rawMerchant by remember { mutableStateOf("") }
    var rawAmount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }
    var inputTypeIsIncome by remember { mutableStateOf(false) } // False = Expense, True = Income
    var customTime by remember { mutableStateOf("") } // option to insert custom hour

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .width(320.dp)
                .background(
                    color = BaseBG, // Solid brand matching crisp white matching specifications
                    shape = RoundedCornerShape(24.dp)
                )
                .liquidGlassBorder(RoundedCornerShape(24.dp), borderWidth = 1.2.dp, isDarkMode = isDarkMode)
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Add Transaction",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Income / Expense Selector pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = SurfaceColor6,
                            shape = RoundedCornerShape(999.dp)
                        )
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { inputTypeIsIncome = false }
                            .background(if (!inputTypeIsIncome) DebitRed else Color.Transparent)
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Expense",
                            color = if (!inputTypeIsIncome) Color.White else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { inputTypeIsIncome = true }
                            .background(if (inputTypeIsIncome) AccentGreen else Color.Transparent)
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Income",
                            color = if (inputTypeIsIncome) Color.White else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Merchant Input
                TextField(
                    value = rawMerchant,
                    onValueChange = { rawMerchant = it },
                    placeholder = { Text("Merchant (e.g. Swiggy)") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = HoverColor10,
                        unfocusedContainerColor = SurfaceColor6,
                        focusedIndicatorColor = Primary,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_merchant_input")
                )

                // Amount input
                TextField(
                    value = rawAmount,
                    onValueChange = { rawAmount = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = { Text("Amount (₹)") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = HoverColor10,
                        unfocusedContainerColor = SurfaceColor6,
                        focusedIndicatorColor = Primary,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_amount_input")
                )

                // Category tag selector dropdown replacements
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Category Association:",
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val categories = listOf("Food", "Transit", "Credits", "Shopping", "Other")
                        categories.forEach { cat ->
                            val active = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (active) Primary else SurfaceColor6,
                                        shape = RoundedCornerShape(999.dp)
                                    )
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }

                // Custom Time Segment
                TextField(
                    value = customTime,
                    onValueChange = { customTime = it },
                    placeholder = { Text("Hour (Optional HH:mm, e.g. 13:30)") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = HoverColor10,
                        unfocusedContainerColor = SurfaceColor6,
                        focusedIndicatorColor = Primary,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Call buttons
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val amtNum = rawAmount.toDoubleOrNull() ?: 0.0
                            val parsedAmount = if (inputTypeIsIncome) amtNum else -amtNum
                            if (rawMerchant.isNotBlank() && amtNum != 0.0) {
                                onAdd(rawMerchant, parsedAmount, selectedCategory, customTime.ifBlank { null })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RequestedButtonBg),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("add_submit_button")
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
fun PocketWatchAppPreviewSnapshot() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PageAmbientBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PocketWatch Preview",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(CardBgGlass, CircleShape)
                        .liquidGlassBorder(CircleShape, isDarkMode = false),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), isDarkMode = false) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Spent Today", fontSize = 12.sp, color = TextSecondary)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("₹537", fontSize = 38.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(".00", fontSize = 20.sp, fontWeight = FontWeight.Light, color = TextMuted)
                    }
                }
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), isDarkMode = false) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Spending Trend", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                        SpendingWaveChart(
                            transactions = emptyList(),
                            isDarkMode = false,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
