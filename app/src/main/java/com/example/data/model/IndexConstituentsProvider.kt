package com.example.data.model

object IndexConstituentsProvider {

    fun getConstituents(indexSymbol: String, liveQuotes: List<StockQuote> = emptyList()): List<IndexConstituent> {
        val quotesMap = liveQuotes.associateBy { it.symbol }

        val rawList = when (indexSymbol.uppercase().replace(" ", "").replace("_", "").replace("-", "")) {
            "NIFTY50", "^NSEI" -> getNifty50Constituents()
            "SENSEX", "BSESENSEX", "^BSESN" -> getSensexConstituents()
            "BANKNIFTY", "NIFTYBANK", "^NSEBANK" -> getBankNiftyConstituents()
            "NIFTYIT", "^CNXIT" -> getNiftyItConstituents()
            "NIFTYAUTO", "^CNXAUTO" -> getNiftyAutoConstituents()
            "NIFTYMETAL", "^CNXMETAL" -> getNiftyMetalConstituents()
            "INDIAVIX", "^INDIAVIX" -> getIndiaVixDrivers()
            else -> getNifty50Constituents()
        }

        // Overlay with live prices if available in current market state
        return rawList.map { item ->
            val live = quotesMap[item.symbol]
            if (live != null) {
                item.copy(
                    lastPrice = live.lastPrice,
                    change = live.change,
                    percentChange = live.percentChange,
                    dayHigh = live.dayHigh,
                    dayLow = live.dayLow,
                    isPositive = live.isPositive
                )
            } else {
                item
            }
        }
    }

    private fun getNifty50Constituents(): List<IndexConstituent> = listOf(
        IndexConstituent("HDFCBANK", "HDFC Bank Ltd", "Banking", 11.45, 1664.20, -6.80, -0.41),
        IndexConstituent("RELIANCE", "Reliance Industries Ltd", "Energy", 9.82, 1388.50, 18.20, 1.33),
        IndexConstituent("ICICIBANK", "ICICI Bank Ltd", "Banking", 7.92, 1235.40, 4.10, 0.33),
        IndexConstituent("INFY", "Infosys Ltd", "IT", 5.84, 1920.60, 24.80, 1.31),
        IndexConstituent("TCS", "Tata Consultancy Services", "IT", 4.12, 4120.00, 54.00, 1.33),
        IndexConstituent("BHARTIARTL", "Bharti Airtel Ltd", "Telecom", 3.95, 1640.80, 22.40, 1.38),
        IndexConstituent("ITC", "ITC Ltd", "FMCG", 3.78, 486.10, -2.40, -0.49),
        IndexConstituent("LT", "Larsen & Toubro Ltd", "Infrastructure", 3.65, 3620.00, 32.00, 0.89),
        IndexConstituent("SBIN", "State Bank of India", "Banking", 3.14, 812.30, -3.20, -0.39),
        IndexConstituent("AXISBANK", "Axis Bank Ltd", "Banking", 2.98, 1182.40, -4.60, -0.39),
        IndexConstituent("KOTAKBANK", "Kotak Mahindra Bank", "Banking", 2.62, 1748.50, -5.30, -0.30),
        IndexConstituent("HINDUNILVR", "Hindustan Unilever Ltd", "FMCG", 2.45, 2712.00, -8.50, -0.31),
        IndexConstituent("TATAMOTORS", "Tata Motors Ltd", "Automobile", 2.15, 848.20, 14.50, 1.74),
        IndexConstituent("M&M", "Mahindra & Mahindra Ltd", "Automobile", 2.05, 2890.00, 42.00, 1.47),
        IndexConstituent("BAJFINANCE", "Bajaj Finance Ltd", "Financial Services", 1.94, 7120.00, 68.00, 0.96),
        IndexConstituent("MARUTI", "Maruti Suzuki India", "Automobile", 1.82, 11840.00, 110.00, 0.94),
        IndexConstituent("SUNPHARMA", "Sun Pharmaceutical Ind.", "Pharma", 1.64, 1845.00, 18.00, 0.99),
        IndexConstituent("TITAN", "Titan Company Ltd", "Consumer Durables", 1.52, 3420.00, 22.00, 0.65),
        IndexConstituent("NTPC", "NTPC Ltd", "Energy", 1.41, 398.50, 4.20, 1.07),
        IndexConstituent("POWERGRID", "Power Grid Corp of India", "Energy", 1.34, 324.00, 3.10, 0.97),
        IndexConstituent("ADANIENT", "Adani Enterprises Ltd", "Metals & Mining", 1.28, 2860.00, 38.00, 1.35),
        IndexConstituent("ULTRACEMCO", "UltraTech Cement Ltd", "Materials", 1.24, 11250.00, 85.00, 0.76),
        IndexConstituent("TRENT", "Trent Ltd", "Retail", 1.18, 6940.00, 112.00, 1.64),
        IndexConstituent("TATASTEEL", "Tata Steel Ltd", "Metals", 1.12, 154.60, 2.10, 1.38),
        IndexConstituent("ONGC", "Oil & Natural Gas Corp", "Energy", 1.05, 294.50, 3.80, 1.31),
        IndexConstituent("BAJAJFINSV", "Bajaj Finserv Ltd", "Financial Services", 0.98, 1780.00, 14.00, 0.79),
        IndexConstituent("WIPRO", "Wipro Ltd", "IT", 0.94, 542.80, 8.40, 1.57),
        IndexConstituent("COALINDIA", "Coal India Ltd", "Energy", 0.91, 488.20, 5.10, 1.06),
        IndexConstituent("JSWSTEEL", "JSW Steel Ltd", "Metals", 0.88, 978.40, 11.20, 1.16),
        IndexConstituent("HCLTECH", "HCL Technologies Ltd", "IT", 0.86, 1765.00, 22.00, 1.26),
        IndexConstituent("TECHM", "Tech Mahindra Ltd", "IT", 0.82, 1584.00, 16.50, 1.05),
        IndexConstituent("BEL", "Bharat Electronics Ltd", "Capital Goods", 0.81, 298.50, 4.20, 1.43),
        IndexConstituent("INDUSINDBK", "IndusInd Bank Ltd", "Banking", 0.80, 1420.00, -8.00, -0.56),
        IndexConstituent("NESTLEIND", "Nestle India Ltd", "FMCG", 0.79, 2460.00, -12.00, -0.49),
        IndexConstituent("HINDALCO", "Hindalco Industries Ltd", "Metals", 0.78, 684.00, 9.40, 1.39),
        IndexConstituent("SHRIRAMFIN", "Shriram Finance Ltd", "Financial Services", 0.75, 3180.00, 35.00, 1.11),
        IndexConstituent("GRASIM", "Grasim Industries Ltd", "Materials", 0.74, 2580.00, 24.00, 0.94),
        IndexConstituent("HDFCLIFE", "HDFC Life Insurance Co", "Financial Services", 0.72, 712.00, 5.40, 0.76),
        IndexConstituent("APOLLOHOSP", "Apollo Hospitals Enterprise", "Healthcare", 0.71, 6840.00, 72.00, 1.06),
        IndexConstituent("BAJAJ-AUTO", "Bajaj Auto Ltd", "Automobile", 0.69, 10840.00, 130.00, 1.21),
        IndexConstituent("EICHERMOT", "Eicher Motors Ltd", "Automobile", 0.67, 4790.00, 52.00, 1.10),
        IndexConstituent("DRREDDY", "Dr. Reddy's Laboratories", "Pharma", 0.65, 6520.00, 48.00, 0.74),
        IndexConstituent("TATACONSUM", "Tata Consumer Products", "FMCG", 0.63, 1140.00, -4.00, -0.35),
        IndexConstituent("CIPLA", "Cipla Ltd", "Pharma", 0.62, 1540.00, 12.00, 0.79),
        IndexConstituent("SBILIFE", "SBI Life Insurance Co", "Financial Services", 0.61, 1780.00, 14.00, 0.79),
        IndexConstituent("BRITANNIA", "Britannia Industries Ltd", "FMCG", 0.60, 5840.00, -18.00, -0.31),
        IndexConstituent("DIVISLAB", "Divi's Laboratories Ltd", "Pharma", 0.58, 5180.00, 42.00, 0.82),
        IndexConstituent("HEROMOTOCO", "Hero MotoCorp Ltd", "Automobile", 0.56, 5420.00, 55.00, 1.03),
        IndexConstituent("BPCL", "Bharat Petroleum Corp", "Energy", 0.54, 348.00, 3.20, 0.93),
        IndexConstituent("ASIANPAINT", "Asian Paints Ltd", "Consumer Durables", 0.52, 3140.00, -14.00, -0.44)
    )

    private fun getSensexConstituents(): List<IndexConstituent> = listOf(
        IndexConstituent("HDFCBANK", "HDFC Bank Ltd", "Banking", 13.80, 1664.20, -6.80, -0.41),
        IndexConstituent("RELIANCE", "Reliance Industries Ltd", "Energy", 11.20, 1388.50, 18.20, 1.33),
        IndexConstituent("ICICIBANK", "ICICI Bank Ltd", "Banking", 9.10, 1235.40, 4.10, 0.33),
        IndexConstituent("INFY", "Infosys Ltd", "IT", 6.70, 1920.60, 24.80, 1.31),
        IndexConstituent("TCS", "Tata Consultancy Services", "IT", 4.90, 4120.00, 54.00, 1.33),
        IndexConstituent("BHARTIARTL", "Bharti Airtel Ltd", "Telecom", 4.60, 1640.80, 22.40, 1.38),
        IndexConstituent("ITC", "ITC Ltd", "FMCG", 4.30, 486.10, -2.40, -0.49),
        IndexConstituent("LT", "Larsen & Toubro Ltd", "Infrastructure", 4.10, 3620.00, 32.00, 0.89),
        IndexConstituent("SBIN", "State Bank of India", "Banking", 3.60, 812.30, -3.20, -0.39),
        IndexConstituent("AXISBANK", "Axis Bank Ltd", "Banking", 3.40, 1182.40, -4.60, -0.39),
        IndexConstituent("KOTAKBANK", "Kotak Mahindra Bank", "Banking", 3.00, 1748.50, -5.30, -0.30),
        IndexConstituent("HINDUNILVR", "Hindustan Unilever Ltd", "FMCG", 2.80, 2712.00, -8.50, -0.31),
        IndexConstituent("M&M", "Mahindra & Mahindra Ltd", "Automobile", 2.40, 2890.00, 42.00, 1.47),
        IndexConstituent("TATAMOTORS", "Tata Motors Ltd", "Automobile", 2.30, 848.20, 14.50, 1.74),
        IndexConstituent("BAJFINANCE", "Bajaj Finance Ltd", "Financial Services", 2.20, 7120.00, 68.00, 0.96),
        IndexConstituent("MARUTI", "Maruti Suzuki India", "Automobile", 2.10, 11840.00, 110.00, 0.94),
        IndexConstituent("SUNPHARMA", "Sun Pharmaceutical Ind.", "Pharma", 1.90, 1845.00, 18.00, 0.99),
        IndexConstituent("TITAN", "Titan Company Ltd", "Consumer Durables", 1.70, 3420.00, 22.00, 0.65),
        IndexConstituent("NTPC", "NTPC Ltd", "Energy", 1.60, 398.50, 4.20, 1.07),
        IndexConstituent("POWERGRID", "Power Grid Corp of India", "Energy", 1.50, 324.00, 3.10, 0.97),
        IndexConstituent("TATASTEEL", "Tata Steel Ltd", "Metals", 1.30, 154.60, 2.10, 1.38),
        IndexConstituent("ULTRACEMCO", "UltraTech Cement Ltd", "Materials", 1.40, 11250.00, 85.00, 0.76),
        IndexConstituent("BAJAJFINSV", "Bajaj Finserv Ltd", "Financial Services", 1.10, 1780.00, 14.00, 0.79),
        IndexConstituent("TECHM", "Tech Mahindra Ltd", "IT", 0.95, 1584.00, 16.50, 1.05),
        IndexConstituent("HCLTECH", "HCL Technologies Ltd", "IT", 1.00, 1765.00, 22.00, 1.26),
        IndexConstituent("INDUSINDBK", "IndusInd Bank Ltd", "Banking", 0.90, 1420.00, -8.00, -0.56),
        IndexConstituent("NESTLEIND", "Nestle India Ltd", "FMCG", 0.90, 2460.00, -12.00, -0.49),
        IndexConstituent("JSWSTEEL", "JSW Steel Ltd", "Metals", 1.00, 978.40, 11.20, 1.16),
        IndexConstituent("ASIANPAINT", "Asian Paints Ltd", "Consumer Durables", 0.60, 3140.00, -14.00, -0.44),
        IndexConstituent("ADANIPORTS", "Adani Ports & SEZ", "Infrastructure", 1.20, 1460.00, 21.00, 1.46)
    )

    private fun getBankNiftyConstituents(): List<IndexConstituent> = listOf(
        IndexConstituent("HDFCBANK", "HDFC Bank Ltd", "Private Bank", 28.50, 1664.20, -6.80, -0.41),
        IndexConstituent("ICICIBANK", "ICICI Bank Ltd", "Private Bank", 23.40, 1235.40, 4.10, 0.33),
        IndexConstituent("SBIN", "State Bank of India", "PSU Bank", 11.20, 812.30, -3.20, -0.39),
        IndexConstituent("AXISBANK", "Axis Bank Ltd", "Private Bank", 10.60, 1182.40, -4.60, -0.39),
        IndexConstituent("KOTAKBANK", "Kotak Mahindra Bank", "Private Bank", 9.40, 1748.50, -5.30, -0.30),
        IndexConstituent("INDUSINDBK", "IndusInd Bank Ltd", "Private Bank", 5.20, 1420.00, -8.00, -0.56),
        IndexConstituent("BANKBARODA", "Bank of Baroda", "PSU Bank", 2.80, 248.50, -1.20, -0.48),
        IndexConstituent("PNB", "Punjab National Bank", "PSU Bank", 2.30, 108.40, -0.60, -0.55),
        IndexConstituent("FEDERALBNK", "Federal Bank Ltd", "Private Bank", 2.10, 192.30, 0.80, 0.42),
        IndexConstituent("IDFCFIRSTB", "IDFC First Bank Ltd", "Private Bank", 1.80, 72.80, -0.30, -0.41),
        IndexConstituent("AUBANK", "AU Small Finance Bank", "Small Finance", 1.50, 645.00, 2.50, 0.39),
        IndexConstituent("BANDHANBNK", "Bandhan Bank Ltd", "Private Bank", 1.20, 188.00, -1.50, -0.79)
    )

    private fun getNiftyItConstituents(): List<IndexConstituent> = listOf(
        IndexConstituent("TCS", "Tata Consultancy Services", "IT Services", 26.80, 4120.00, 54.00, 1.33),
        IndexConstituent("INFY", "Infosys Ltd", "IT Services", 25.40, 1920.60, 24.80, 1.31),
        IndexConstituent("HCLTECH", "HCL Technologies Ltd", "IT Services", 10.50, 1765.00, 22.00, 1.26),
        IndexConstituent("WIPRO", "Wipro Ltd", "IT Services", 8.90, 542.80, 8.40, 1.57),
        IndexConstituent("TECHM", "Tech Mahindra Ltd", "IT Services", 8.40, 1584.00, 16.50, 1.05),
        IndexConstituent("LTIM", "LTIMindtree Ltd", "IT Services", 7.10, 5820.00, 65.00, 1.13),
        IndexConstituent("PERSISTENT", "Persistent Systems Ltd", "Software", 5.20, 5140.00, 72.00, 1.42),
        IndexConstituent("COFORGE", "Coforge Ltd", "IT Services", 3.80, 6890.00, 85.00, 1.25),
        IndexConstituent("MPHASIS", "Mphasis Ltd", "IT Services", 2.30, 2980.00, 31.00, 1.05),
        IndexConstituent("LTTS", "L&T Technology Services", "Engineering R&D", 1.60, 5460.00, 48.00, 0.89)
    )

    private fun getNiftyAutoConstituents(): List<IndexConstituent> = listOf(
        IndexConstituent("M&M", "Mahindra & Mahindra Ltd", "Automobile", 20.40, 2890.00, 42.00, 1.47),
        IndexConstituent("TATAMOTORS", "Tata Motors Ltd", "Automobile", 18.20, 848.20, 14.50, 1.74),
        IndexConstituent("MARUTI", "Maruti Suzuki India", "Automobile", 17.50, 11840.00, 110.00, 0.94),
        IndexConstituent("BAJAJ-AUTO", "Bajaj Auto Ltd", "2-Wheeler", 11.20, 10840.00, 130.00, 1.21),
        IndexConstituent("EICHERMOT", "Eicher Motors Ltd", "2-Wheeler", 8.60, 4790.00, 52.00, 1.10),
        IndexConstituent("HEROMOTOCO", "Hero MotoCorp Ltd", "2-Wheeler", 6.80, 5420.00, 55.00, 1.03),
        IndexConstituent("TVSMOTOR", "TVS Motor Company", "2-Wheeler", 5.40, 2480.00, 31.00, 1.27),
        IndexConstituent("BHARATFORG", "Bharat Forge Ltd", "Auto Ancillary", 3.80, 1480.00, 16.00, 1.09),
        IndexConstituent("BOSCHLTD", "Bosch Ltd", "Auto Ancillary", 2.60, 34200.00, 280.00, 0.83),
        IndexConstituent("ASHOKLEY", "Ashok Leyland Ltd", "Commercial Vehicles", 2.10, 228.50, 2.40, 1.06),
        IndexConstituent("MOTHERSON", "Samvardhana Motherson", "Auto Ancillary", 1.80, 196.40, 2.10, 1.08),
        IndexConstituent("BALKRISIND", "Balkrishna Industries", "Tyres", 1.60, 2980.00, 22.00, 0.74)
    )

    private fun getNiftyMetalConstituents(): List<IndexConstituent> = listOf(
        IndexConstituent("TATASTEEL", "Tata Steel Ltd", "Steel", 23.80, 154.60, 2.10, 1.38),
        IndexConstituent("JSWSTEEL", "JSW Steel Ltd", "Steel", 20.40, 978.40, 11.20, 1.16),
        IndexConstituent("HINDALCO", "Hindalco Industries Ltd", "Aluminium", 18.20, 684.00, 9.40, 1.39),
        IndexConstituent("VEDL", "Vedanta Ltd", "Diversified Metals", 10.50, 468.00, 6.20, 1.34),
        IndexConstituent("JINDALSTEL", "Jindal Steel & Power", "Steel", 8.60, 982.00, 12.00, 1.24),
        IndexConstituent("COALINDIA", "Coal India Ltd", "Mining", 6.40, 488.20, 5.10, 1.06),
        IndexConstituent("NMDC", "NMDC Ltd", "Iron Ore", 4.20, 224.50, 2.80, 1.26),
        IndexConstituent("SAIL", "Steel Authority of India", "Steel", 3.10, 134.80, 1.40, 1.05),
        IndexConstituent("NATIONALUM", "National Aluminium Co", "Aluminium", 2.60, 188.40, 2.60, 1.40),
        IndexConstituent("APLAPOLLO", "APL Apollo Tubes Ltd", "Steel Pipes", 2.20, 1460.00, 14.00, 0.97)
    )

    private fun getIndiaVixDrivers(): List<IndexConstituent> = listOf(
        IndexConstituent("NIFTY_FUT", "NIFTY 50 Futures", "Benchmark Index", 35.00, 24985.00, 148.00, 0.60),
        IndexConstituent("BANK_FUT", "BANK NIFTY Futures", "Banking Index", 25.00, 51380.00, -90.00, -0.17),
        IndexConstituent("RELIANCE", "Reliance Industries (High Beta)", "Energy", 12.00, 1388.50, 18.20, 1.33),
        IndexConstituent("HDFCBANK", "HDFC Bank (Index Heavyweight)", "Banking", 10.00, 1664.20, -6.80, -0.41),
        IndexConstituent("ICICIBANK", "ICICI Bank (Volatility Contributor)", "Banking", 8.00, 1235.40, 4.10, 0.33),
        IndexConstituent("TATAMOTORS", "Tata Motors (High Volatility)", "Automobile", 5.00, 848.20, 14.50, 1.74),
        IndexConstituent("BAJFINANCE", "Bajaj Finance (High Beta)", "Financial Services", 5.00, 7120.00, 68.00, 0.96)
    )
}
