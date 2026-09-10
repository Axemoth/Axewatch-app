package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AllotmentRecordEntity
import com.example.data.local.entity.HoldingEntity
import com.example.data.local.entity.PanVaultEntity
import com.example.data.local.entity.PaperAccountEntity
import com.example.data.local.entity.PaperOrderEntity
import com.example.data.local.entity.PaperPositionEntity
import com.example.data.local.entity.WatchlistEntity
import com.example.data.model.CandleBar
import com.example.data.model.FiiDiiFlow
import com.example.data.model.GmpItem
import com.example.data.model.IpoIssue
import com.example.data.model.MarketIndex
import com.example.data.model.MutualFundScheme
import com.example.data.model.PastIpoItem
import com.example.data.model.PortfolioConcentration
import com.example.data.model.PortfolioSummary
import com.example.data.model.RegistrarLink
import com.example.data.model.RegistrarSourceHealth
import com.example.data.model.SectorHeatmapItem
import com.example.data.model.StockQuote
import com.example.data.model.TradeIdea
import com.example.data.model.TradeOutlook
import com.example.data.repository.AxewatchRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AxewatchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AxewatchRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AxewatchRepository(db)
        viewModelScope.launch {
            repository.ensurePaperAccountCreated()
        }
    }

    // Navigation Tab (0: Market, 1: IPO & GMP, 2: Paper Trading, 3: Portfolio, 4: Allotment)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    fun selectTab(tab: Int) {
        _selectedTab.value = tab
    }

    // Market Data Flows
    val indices: StateFlow<List<MarketIndex>> = repository.indices.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val stocks: StateFlow<List<StockQuote>> = repository.stocks.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val ipos: StateFlow<List<IpoIssue>> = repository.ipos.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val gmpItems: StateFlow<List<GmpItem>> = repository.gmpItems.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val pastIpos: StateFlow<List<PastIpoItem>> = repository.pastIpos.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val sectorHeatmap: StateFlow<List<SectorHeatmapItem>> = repository.sectorHeatmap.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val fiiDiiData: StateFlow<List<FiiDiiFlow>> = repository.fiiDiiData.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val tradeIdeas: StateFlow<List<TradeIdea>> = repository.tradeIdeas.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val lastRefreshTime: StateFlow<Long> = repository.lastRefreshTime.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis()
    )

    // Local DB Flows
    val holdings: StateFlow<List<HoldingEntity>> = repository.getHoldings().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val paperAccount: StateFlow<PaperAccountEntity?> = repository.getPaperAccount().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val paperPositions: StateFlow<List<PaperPositionEntity>> = repository.getPaperPositions().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val paperOrders: StateFlow<List<PaperOrderEntity>> = repository.getPaperOrders().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val savedPans: StateFlow<List<PanVaultEntity>> = repository.getAllPans().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val allotmentRecords: StateFlow<List<AllotmentRecordEntity>> = repository.getAllotmentRecords().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val watchlist: StateFlow<List<WatchlistEntity>> = repository.getWatchlist().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val watchlistedSymbols: StateFlow<Set<String>> = repository.getWatchlist().combine(repository.stocks) { wList, _ ->
        wList.map { it.symbol }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Calculated Portfolio Summary
    val portfolioSummary: StateFlow<PortfolioSummary> = combine(holdings, stocks) { hList, sList ->
        val priceMap = sList.associateBy({ it.symbol }, { it.lastPrice })
        var totalInvested = 0.0
        var currentValue = 0.0
        hList.forEach { h ->
            val curPrice = priceMap[h.symbol] ?: h.buyPrice
            val inv = h.quantity * h.buyPrice
            val cur = h.quantity * curPrice
            totalInvested += inv
            currentValue += cur
        }
        val pnl = currentValue - totalInvested
        val pnlPct = if (totalInvested > 0) (pnl / totalInvested) * 100 else 0.0
        PortfolioSummary(
            totalInvested = totalInvested,
            currentValue = currentValue,
            totalProfitLoss = pnl,
            profitLossPercent = pnlPct,
            dayReturn = pnl * 0.08, // estimated session gain
            dayReturnPercent = 0.65,
            holdingsCount = hList.size
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PortfolioSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 14.8, 0)
    )

    // Portfolio Concentration Insights
    val portfolioConcentration: StateFlow<PortfolioConcentration> = combine(holdings, stocks) { hList, sList ->
        val priceMap = sList.associateBy({ it.symbol }, { it.lastPrice })
        repository.getPortfolioConcentration(hList, priceMap)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        PortfolioConcentration("None", 0.0, false, 0.0, 0.0, 0.0, "None", 0.0, "None", 0.0)
    )

    // Mutual Funds & Registrar Health
    val mutualFunds: StateFlow<List<MutualFundScheme>> = repository.mutualFunds.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val registrarHealth: StateFlow<List<RegistrarSourceHealth>> = repository.registrarHealth.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val registrarLinks: List<RegistrarLink> = repository.getRegistrarLinks()

    fun seedSampleHoldings() {
        viewModelScope.launch {
            repository.seedSampleHoldings()
            _toastMessage.emit("Sample Indian Blue-Chips added to Portfolio")
        }
    }

    // UI State: Stock Details Modal / BottomSheet
    private val _selectedStock = MutableStateFlow<StockQuote?>(null)
    val selectedStock: StateFlow<StockQuote?> = _selectedStock.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow("1M")
    val selectedTimeframe: StateFlow<String> = _selectedTimeframe.asStateFlow()

    private val _selectedStockCandles = MutableStateFlow<List<CandleBar>>(emptyList())
    val selectedStockCandles: StateFlow<List<CandleBar>> = _selectedStockCandles.asStateFlow()

    private val _selectedStockOutlook = MutableStateFlow<TradeOutlook?>(null)
    val selectedStockOutlook: StateFlow<TradeOutlook?> = _selectedStockOutlook.asStateFlow()

    fun selectStock(stock: StockQuote) {
        _selectedStock.value = stock
        _selectedStockCandles.value = repository.getCandlesForStock(stock.symbol, _selectedTimeframe.value)
        _selectedStockOutlook.value = repository.getStockOutlook(stock.symbol)
    }

    fun setTimeframe(tf: String) {
        _selectedTimeframe.value = tf
        _selectedStock.value?.let { stock ->
            _selectedStockCandles.value = repository.getCandlesForStock(stock.symbol, tf)
        }
    }

    fun dismissStockModal() {
        _selectedStock.value = null
        _selectedStockCandles.value = emptyList()
        _selectedStockOutlook.value = null
    }

    // Refresh State
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repository.refreshMarket()
            // If a stock is currently opened, update its outlook as well
            _selectedStock.value?.let { st ->
                val updated = repository.stocks.let { stList ->
                    stocks.value.find { it.symbol == st.symbol } ?: st
                }
                _selectedStock.value = updated
                _selectedStockCandles.value = repository.getCandlesForStock(updated.symbol)
                _selectedStockOutlook.value = repository.getStockOutlook(updated.symbol)
            }
            _isRefreshing.value = false
        }
    }

    // Toast / Snack message channel
    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    fun emitMessage(msg: String) {
        viewModelScope.launch {
            _toastMessage.emit(msg)
        }
    }

    // Paper Trading Actions
    fun placePaperOrder(
        symbol: String,
        name: String,
        side: String,
        quantity: Int,
        price: Double,
        stopLoss: Double? = null,
        targetPrice: Double? = null
    ) {
        viewModelScope.launch {
            val result = repository.executePaperOrder(symbol, name, side, quantity, price, stopLoss, targetPrice)
            _toastMessage.emit(result)
        }
    }

    fun resetPaperAccount() {
        viewModelScope.launch {
            repository.resetPaperAccount()
            _toastMessage.emit("Paper account reset to ₹10,00,000 virtual cash.")
        }
    }

    // Holdings Actions
    fun addHolding(symbol: String, name: String, assetType: String, quantity: Double, buyPrice: Double, sector: String) {
        viewModelScope.launch {
            repository.addHolding(
                HoldingEntity(
                    symbol = symbol.trim().uppercase(),
                    name = name.trim(),
                    assetType = assetType,
                    quantity = quantity,
                    buyPrice = buyPrice,
                    sector = sector
                )
            )
            _toastMessage.emit("Added $quantity shares of $symbol to portfolio")
        }
    }

    fun deleteHolding(id: Long) {
        viewModelScope.launch {
            repository.deleteHolding(id)
            _toastMessage.emit("Holding deleted")
        }
    }

    fun updateHolding(id: Long, quantity: Double, buyPrice: Double) {
        viewModelScope.launch {
            repository.updateHolding(id, quantity, buyPrice)
            _toastMessage.emit("Holding updated successfully")
        }
    }

    fun importHoldingsFromCsv(csvText: String) {
        viewModelScope.launch {
            val count = repository.importHoldingsFromCsv(csvText)
            if (count > 0) {
                _toastMessage.emit("Successfully imported $count holdings from CSV")
            } else {
                _toastMessage.emit("No valid holdings recognized. Check CSV format.")
            }
        }
    }

    // PAN Vault & Allotment
    fun savePan(pan: String, holderName: String, relation: String) {
        viewModelScope.launch {
            repository.savePan(pan, holderName, relation)
            _toastMessage.emit("PAN saved securely to Vault")
        }
    }

    fun deletePan(pan: PanVaultEntity) {
        viewModelScope.launch {
            repository.deletePan(pan)
            _toastMessage.emit("PAN removed from Vault")
        }
    }

    fun checkAllotment(pan: String, ipoSymbol: String, holderName: String = "Self") {
        viewModelScope.launch {
            val record = repository.checkIpoAllotment(pan, ipoSymbol, holderName)
            val outcome = if (record.status == "ALLOTTED") {
                "Congratulations! ${record.sharesAllotted} shares ALLOTTED for ${record.ipoSymbol}"
            } else {
                "Status for ${record.ipoSymbol}: Not Allotted"
            }
            _toastMessage.emit(outcome)
        }
    }

    fun checkBulkAllotment(ipoSymbol: String) {
        viewModelScope.launch {
            val records = repository.checkBulkAllotment(ipoSymbol)
            val allottedCount = records.count { it.status == "ALLOTTED" }
            val msg = if (records.isEmpty()) {
                "No saved PANs in Vault to check"
            } else {
                "Checked ${records.size} PANs for $ipoSymbol: $allottedCount Allotted, ${records.size - allottedCount} Not Allotted"
            }
            _toastMessage.emit(msg)
        }
    }

    fun recordManualAllotment(
        maskedPan: String,
        ipoSymbol: String,
        status: String,
        sharesAllotted: Int,
        applicationNo: String
    ) {
        viewModelScope.launch {
            repository.recordManualAllotment(maskedPan, ipoSymbol, status, sharesAllotted, applicationNo)
            _toastMessage.emit("Allotment status manually recorded")
        }
    }

    fun deleteRecord(record: AllotmentRecordEntity) {
        viewModelScope.launch {
            repository.deleteRecord(record)
            _toastMessage.emit("Record removed")
        }
    }

    fun clearAllotmentHistory() {
        viewModelScope.launch {
            repository.clearAllotmentHistory()
            _toastMessage.emit("Allotment history cleared")
        }
    }

    // Watchlist
    fun toggleWatchlist(symbol: String, name: String, currentPrice: Double, isCurrentlyWatchlisted: Boolean) {
        viewModelScope.launch {
            if (isCurrentlyWatchlisted) {
                repository.removeFromWatchlist(symbol)
                _toastMessage.emit("Removed $symbol from Watchlist")
            } else {
                repository.toggleWatchlist(symbol, name, currentPrice)
                _toastMessage.emit("Added $symbol to Watchlist")
            }
        }
    }
}
