package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class PocketWatchViewModel(
    application: Application,
    private val repository: TransactionRepository
) : AndroidViewModel(application) {

    // Bottom Navigation Tab index (0 = Home, 1 = Grid, 2 = Clock, 3 = Stats, 4 = Chat)
    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    // Toggle Tab: true = "Spent Today" (active by default), false = "Balance Remaining"
    private val _isSpentTodaySelected = MutableStateFlow(true)
    val isSpentTodaySelected: StateFlow<Boolean> = _isSpentTodaySelected.asStateFlow()

    // Spending Chart Time range filter: "1m", "3m", "6m" (active), "1y"
    private val _chartTimeRange = MutableStateFlow("6m")
    val chartTimeRange: StateFlow<String> = _chartTimeRange.asStateFlow()

    // Theme toggling: false = Misty Blue (specification light version), true = Slate Dark (darker variant)
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Dialog presence
    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    // Budget Limit Setting (default = ₹20,000)
    private val _budgetLimit = MutableStateFlow(20000.0)
    val budgetLimit: StateFlow<Double> = _budgetLimit.asStateFlow()

    // List of active transactions ordered by date desc
    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Chat History
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Hello! I am your visual PocketWatch assistant. How can I help optimize your budget or analyze your commutes and food orders today?",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Live dashboard advice snippet in the main home card (Reactive based on database)
    val dashboardAdvice: StateFlow<String> = transactions.map { transList ->
        val foodCount = transList.count { it.category.lowercase().contains("food") || it.merchant.lowercase().contains("swiggy") || it.merchant.lowercase().contains("zomato") }
        if (foodCount > 0) {
            "You're spending **23% less** on food vs. last month — great progress. You could save an extra **₹8,200** by end of May."
        } else {
            "No recent dining out detected. Your spending is looking **perfectly optimized**! You are ahead of your budget savings targets by **₹12,400**."
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Crunching transaction patterns...")

    init {
        // Cold start database checker: Seeding items if DB is empty
        viewModelScope.launch(Dispatchers.IO) {
            repository.allTransactions.first().let { currentList ->
                if (currentList.isEmpty()) {
                    val seedData = listOf(
                        TransactionEntity(
                            merchant = "wdad",
                            amount = 10008.0,
                            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 3, // 3 hours ago
                            category = "Credits",
                            initials = "WD",
                            avatarBgColorHex = "#6496C8" // Blue-grey
                        ),
                        TransactionEntity(
                            merchant = "Swiggy",
                            amount = -448.0,
                            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 2, // 2 hours ago
                            category = "Food",
                            initials = "SW",
                            avatarBgColorHex = "#C9A84C" // Accent Gold
                        ),
                        TransactionEntity(
                            merchant = "Uber Auto",
                            amount = -89.0,
                            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 4, // 4 hours ago
                            category = "Transit",
                            initials = "UA",
                            avatarBgColorHex = "#1E2D46" // Slate
                        ),
                        TransactionEntity(
                            merchant = "Zomato",
                            amount = -312.0,
                            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 15, // Yesterday 8ish PM
                            category = "Food",
                            initials = "ZO",
                            avatarBgColorHex = "#B45050" // Soft Red
                        )
                    )
                    seedData.forEach { repository.insert(it) }
                }
            }
        }
    }

    fun selectTab(index: Int) {
        _activeTab.value = index
    }

    fun setSpentTodaySelected(selected: Boolean) {
        _isSpentTodaySelected.value = selected
    }

    fun setChartTimeRange(range: String) {
        _chartTimeRange.value = range
    }

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setShowAddDialog(show: Boolean) {
        _showAddDialog.value = show
    }

    fun setBudgetLimit(limit: Double) {
        _budgetLimit.value = limit
    }

    fun addNewTransaction(merchant: String, amount: Double, category: String, customTime: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val autoInitials = when {
                merchant.length >= 2 -> merchant.substring(0, 2).uppercase()
                merchant.length == 1 -> merchant.uppercase()
                else -> "TX"
            }

            // Assign attractive colors based on category
            val colorHex = when (category.lowercase()) {
                "food", "dining" -> "#C9A84C" // Gold
                "transit", "transport" -> "#1E2D46" // Slate
                "credits", "salary", "income" -> "#6496C8" // Blue-grey
                "shopping" -> "#8C5CB4" // Violet
                else -> "#B45050" // Custom Red
            }

            val timestamp = if (customTime != null) {
                try {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val parsedDate = sdf.parse(customTime)
                    val calendar = Calendar.getInstance()
                    if (parsedDate != null) {
                        val parsedCal = Calendar.getInstance().apply { time = parsedDate }
                        calendar.set(Calendar.HOUR_OF_DAY, parsedCal.get(Calendar.HOUR_OF_DAY))
                        calendar.set(Calendar.MINUTE, parsedCal.get(Calendar.MINUTE))
                    }
                    calendar.timeInMillis
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            } else {
                System.currentTimeMillis()
            }

            val entity = TransactionEntity(
                merchant = merchant,
                amount = amount,
                category = category,
                initials = autoInitials,
                avatarBgColorHex = colorHex,
                timestamp = timestamp
            )
            repository.insert(entity)
        }
    }

    fun deleteTransaction(entity: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(entity)
        }
    }

    fun sendChatMessage(prompt: String) {
        if (prompt.isBlank()) return

        // Post user bubble
        val userMsg = ChatMessage(text = prompt, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg

        _isAiLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val transList = transactions.value
            val response = GeminiClient.getFinancialAdvice(prompt, transList)
            _chatMessages.value = _chatMessages.value + ChatMessage(text = response, isUser = false)
            _isAiLoading.value = false
        }
    }

    fun triggerBudgetOptimization() {
        sendChatMessage("Optimise my budget based on Swiggy and Zomato spending.")
    }

    fun triggerSmartSaving() {
        sendChatMessage("Give me a smart savings target analysis and projected savings.")
    }
}

class PocketWatchViewModelFactory(
    private val application: Application,
    private val repository: TransactionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PocketWatchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PocketWatchViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
