package com.aistudio.axewatch.trader.data.provider

import com.aistudio.axewatch.trader.data.model.IndexConstituent

object IndexConstituentsProvider {

    fun getConstituentsForIndex(indexSymbol: String): List<IndexConstituent> {
        val sym = indexSymbol.uppercase().trim()
        return when {
            sym.contains("BANK") && !sym.contains("NIFTY 50") -> bankNiftyConstituents
            sym.contains("FIN") -> finNiftyConstituents
            sym.contains("MIDCAP") || sym.contains("NEXT") -> midcapConstituents
            sym.contains("IT") -> niftyItConstituents
            sym.contains("SENSEX") || sym.contains("BSE") -> sensexConstituents
            // The static nifty50 list is NIFTY-50 membership only — claim it for
            // the exact NIFTY-50 aliases. Anything else unmatched (NIFTY AUTO,
            // INDIA VIX, …) gets NOTHING instead of a mislabeled NIFTY-50 list.
            sym == "NIFTY 50" || sym == "NIFTY50" || sym == "NIFTY" || sym == "^NSEI" -> nifty50Constituents
// Unknown index: return nothing rather than pretending the NIFTY-50
            // list belongs to it. The UI renders an honest empty state.
            else -> emptyList()
        }
    }

    val nifty50Constituents = listOf(
        IndexConstituent("HDFCBANK", "HDFC Bank Ltd", "Financial Services", 13.5, 0.0, 0.0, 0.0),
        IndexConstituent("RELIANCE", "Reliance Industries Ltd", "Oil Gas & Fuels", 10.2, 0.0, 0.0, 0.0),
        IndexConstituent("ICICIBANK", "ICICI Bank Ltd", "Financial Services", 9.1, 0.0, 0.0, 0.0),
        IndexConstituent("INFY", "Infosys Ltd", "Information Technology", 5.8, 0.0, 0.0, 0.0),
        IndexConstituent("TCS", "Tata Consultancy Services", "Information Technology", 4.3, 0.0, 0.0, 0.0),
        IndexConstituent("ITC", "ITC Ltd", "Fast Moving Consumer Goods", 4.1, 0.0, 0.0, 0.0),
        IndexConstituent("LT", "Larsen & Toubro Ltd", "Construction", 3.9, 0.0, 0.0, 0.0),
        IndexConstituent("BHARTIARTL", "Bharti Airtel Ltd", "Telecommunication", 3.8, 0.0, 0.0, 0.0),
        IndexConstituent("AXISBANK", "Axis Bank Ltd", "Financial Services", 3.2, 0.0, 0.0, 0.0),
        IndexConstituent("SBIN", "State Bank of India", "Financial Services", 3.0, 0.0, 0.0, 0.0),
        IndexConstituent("KOTAKBANK", "Kotak Mahindra Bank Ltd", "Financial Services", 2.8, 0.0, 0.0, 0.0),
        IndexConstituent("M&M", "Mahindra & Mahindra Ltd", "Automobile", 2.6, 0.0, 0.0, 0.0),
        IndexConstituent("HINDUNILVR", "Hindustan Unilever Ltd", "Fast Moving Consumer Goods", 2.4, 0.0, 0.0, 0.0),
        IndexConstituent("BAJFINANCE", "Bajaj Finance Ltd", "Financial Services", 2.3, 0.0, 0.0, 0.0),
        IndexConstituent("TATAMOTORS", "Tata Motors Ltd", "Automobile", 2.2, 0.0, 0.0, 0.0),
        IndexConstituent("SUNPHARMA", "Sun Pharmaceutical Ind", "Healthcare", 2.1, 0.0, 0.0, 0.0),
        IndexConstituent("MARUTI", "Maruti Suzuki India Ltd", "Automobile", 1.9, 0.0, 0.0, 0.0),
        IndexConstituent("NTPC", "NTPC Ltd", "Power", 1.8, 0.0, 0.0, 0.0),
        IndexConstituent("POWERGRID", "Power Grid Corp of India", "Power", 1.7, 0.0, 0.0, 0.0),
        IndexConstituent("TITAN", "Titan Company Ltd", "Consumer Durables", 1.6, 0.0, 0.0, 0.0),
        IndexConstituent("TATASTEEL", "Tata Steel Ltd", "Metals & Mining", 1.5, 0.0, 0.0, 0.0),
        IndexConstituent("ULTRACEMCO", "UltraTech Cement Ltd", "Construction Materials", 1.4, 0.0, 0.0, 0.0),
        IndexConstituent("ADANIENT", "Adani Enterprises Ltd", "Metals & Mining", 1.3, 0.0, 0.0, 0.0),
        IndexConstituent("COALINDIA", "Coal India Ltd", "Oil Gas & Fuels", 1.3, 0.0, 0.0, 0.0),
        IndexConstituent("ONGC", "Oil & Natural Gas Corp", "Oil Gas & Fuels", 1.2, 0.0, 0.0, 0.0),
        IndexConstituent("BAJAJFINSV", "Bajaj Finserv Ltd", "Financial Services", 1.2, 0.0, 0.0, 0.0),
        IndexConstituent("HINDALCO", "Hindalco Industries Ltd", "Metals & Mining", 1.1, 0.0, 0.0, 0.0),
        IndexConstituent("ASIANPAINT", "Asian Paints Ltd", "Consumer Durables", 1.1, 0.0, 0.0, 0.0),
        IndexConstituent("TECHM", "Tech Mahindra Ltd", "Information Technology", 1.1, 0.0, 0.0, 0.0),
        IndexConstituent("INDUSINDBK", "IndusInd Bank Ltd", "Financial Services", 1.0, 0.0, 0.0, 0.0),
        IndexConstituent("JSWSTEEL", "JSW Steel Ltd", "Metals & Mining", 1.0, 0.0, 0.0, 0.0),
        IndexConstituent("GRASIM", "Grasim Industries Ltd", "Construction Materials", 0.9, 0.0, 0.0, 0.0),
        IndexConstituent("CIPLA", "Cipla Ltd", "Healthcare", 0.9, 0.0, 0.0, 0.0),
        IndexConstituent("WIPRO", "Wipro Ltd", "Information Technology", 0.9, 0.0, 0.0, 0.0),
        IndexConstituent("NESTLEIND", "Nestle India Ltd", "Fast Moving Consumer Goods", 0.9, 0.0, 0.0, 0.0),
        IndexConstituent("HCLTECH", "HCL Technologies Ltd", "Information Technology", 0.8, 0.0, 0.0, 0.0),
        IndexConstituent("ADANIPORTS", "Adani Ports & SEZ Ltd", "Services", 0.8, 0.0, 0.0, 0.0),
        IndexConstituent("BPCL", "Bharat Petroleum Corp", "Oil Gas & Fuels", 0.8, 0.0, 0.0, 0.0),
        IndexConstituent("DRREDDY", "Dr Reddy's Laboratories", "Healthcare", 0.8, 0.0, 0.0, 0.0),
        IndexConstituent("SBILIFE", "SBI Life Insurance Co", "Financial Services", 0.7, 0.0, 0.0, 0.0),
        IndexConstituent("BRITANNIA", "Britannia Industries Ltd", "Fast Moving Consumer Goods", 0.7, 0.0, 0.0, 0.0),
        IndexConstituent("APOLLOHOSP", "Apollo Hospitals Enterprise", "Healthcare", 0.7, 0.0, 0.0, 0.0),
        IndexConstituent("EICHERMOT", "Eicher Motors Ltd", "Automobile", 0.7, 0.0, 0.0, 0.0),
        IndexConstituent("DIVISLAB", "Divi's Laboratories Ltd", "Healthcare", 0.6, 0.0, 0.0, 0.0),
        IndexConstituent("SHRIRAMFIN", "Shriram Finance Ltd", "Financial Services", 0.6, 0.0, 0.0, 0.0),
        IndexConstituent("BEL", "Bharat Electronics Ltd", "Capital Goods", 0.6, 0.0, 0.0, 0.0),
        IndexConstituent("BAJAJ-AUTO", "Bajaj Auto Ltd", "Automobile", 0.6, 0.0, 0.0, 0.0),
        IndexConstituent("TATACONSUM", "Tata Consumer Products", "Fast Moving Consumer Goods", 0.6, 0.0, 0.0, 0.0),
        IndexConstituent("HEROMOTOCO", "Hero MotoCorp Ltd", "Automobile", 0.5, 0.0, 0.0, 0.0),
        IndexConstituent("TRENT", "Trent Ltd", "Consumer Services", 0.5, 0.0, 0.0, 0.0)
    )

    val bankNiftyConstituents = listOf(
        IndexConstituent("HDFCBANK", "HDFC Bank Ltd", "Banking", 28.4, 0.0, 0.0, 0.0),
        IndexConstituent("ICICIBANK", "ICICI Bank Ltd", "Banking", 23.8, 0.0, 0.0, 0.0),
        IndexConstituent("AXISBANK", "Axis Bank Ltd", "Banking", 9.6, 0.0, 0.0, 0.0),
        IndexConstituent("SBIN", "State Bank of India", "Banking", 9.2, 0.0, 0.0, 0.0),
        IndexConstituent("KOTAKBANK", "Kotak Mahindra Bank Ltd", "Banking", 8.9, 0.0, 0.0, 0.0),
        IndexConstituent("INDUSINDBK", "IndusInd Bank Ltd", "Banking", 5.1, 0.0, 0.0, 0.0),
        IndexConstituent("BANKBARODA", "Bank of Baroda", "Banking", 2.8, 0.0, 0.0, 0.0),
        IndexConstituent("PNB", "Punjab National Bank", "Banking", 2.4, 0.0, 0.0, 0.0),
        IndexConstituent("FEDERALBNK", "Federal Bank Ltd", "Banking", 2.2, 0.0, 0.0, 0.0),
        IndexConstituent("IDFCFIRSTB", "IDFC First Bank Ltd", "Banking", 1.8, 0.0, 0.0, 0.0),
        IndexConstituent("AUBANK", "AU Small Finance Bank", "Banking", 1.5, 0.0, 0.0, 0.0),
        IndexConstituent("BANDHANBNK", "Bandhan Bank Ltd", "Banking", 1.1, 0.0, 0.0, 0.0)
    )

    val niftyItConstituents = listOf(
        IndexConstituent("TCS", "Tata Consultancy Services", "IT Software", 27.5, 0.0, 0.0, 0.0),
        IndexConstituent("INFY", "Infosys Ltd", "IT Software", 26.8, 0.0, 0.0, 0.0),
        IndexConstituent("HCLTECH", "HCL Technologies Ltd", "IT Software", 11.4, 0.0, 0.0, 0.0),
        IndexConstituent("TECHM", "Tech Mahindra Ltd", "IT Software", 9.8, 0.0, 0.0, 0.0),
        IndexConstituent("WIPRO", "Wipro Ltd", "IT Software", 8.2, 0.0, 0.0, 0.0),
        IndexConstituent("LTIM", "LTIMindtree Ltd", "IT Software", 6.5, 0.0, 0.0, 0.0),
        IndexConstituent("PERSISTENT", "Persistent Systems Ltd", "IT Software", 3.8, 0.0, 0.0, 0.0),
        IndexConstituent("COFORGE", "Coforge Ltd", "IT Software", 2.9, 0.0, 0.0, 0.0),
        IndexConstituent("MPHASIS", "Mphasis Ltd", "IT Software", 1.8, 0.0, 0.0, 0.0),
        IndexConstituent("KPITTECH", "KPIT Technologies Ltd", "IT Software", 1.3, 0.0, 0.0, 0.0)
    )

    val sensexConstituents = listOf(
        IndexConstituent("HDFCBANK", "HDFC Bank Ltd", "Banking", 14.8, 0.0, 0.0, 0.0),
        IndexConstituent("RELIANCE", "Reliance Industries Ltd", "Energy", 11.2, 0.0, 0.0, 0.0),
        IndexConstituent("ICICIBANK", "ICICI Bank Ltd", "Banking", 9.9, 0.0, 0.0, 0.0),
        IndexConstituent("INFY", "Infosys Ltd", "IT Services", 6.4, 0.0, 0.0, 0.0),
        IndexConstituent("TCS", "Tata Consultancy Services", "IT Services", 4.8, 0.0, 0.0, 0.0),
        IndexConstituent("ITC", "ITC Ltd", "FMCG", 4.5, 0.0, 0.0, 0.0),
        IndexConstituent("LT", "Larsen & Toubro Ltd", "Engineering", 4.2, 0.0, 0.0, 0.0),
        IndexConstituent("BHARTIARTL", "Bharti Airtel Ltd", "Telecom", 4.1, 0.0, 0.0, 0.0),
        IndexConstituent("AXISBANK", "Axis Bank Ltd", "Banking", 3.5, 0.0, 0.0, 0.0),
        IndexConstituent("SBIN", "State Bank of India", "Banking", 3.3, 0.0, 0.0, 0.0),
        IndexConstituent("KOTAKBANK", "Kotak Mahindra Bank", "Banking", 3.1, 0.0, 0.0, 0.0),
        IndexConstituent("M&M", "Mahindra & Mahindra", "Automobile", 2.9, 0.0, 0.0, 0.0),
        IndexConstituent("HINDUNILVR", "Hindustan Unilever", "FMCG", 2.6, 0.0, 0.0, 0.0),
        IndexConstituent("BAJFINANCE", "Bajaj Finance Ltd", "Finance", 2.5, 0.0, 0.0, 0.0),
        IndexConstituent("TATAMOTORS", "Tata Motors Ltd", "Automobile", 2.4, 0.0, 0.0, 0.0),
        IndexConstituent("SUNPHARMA", "Sun Pharma Ltd", "Pharma", 2.3, 0.0, 0.0, 0.0),
        IndexConstituent("MARUTI", "Maruti Suzuki Ltd", "Automobile", 2.1, 0.0, 0.0, 0.0),
        IndexConstituent("NTPC", "NTPC Ltd", "Power", 2.0, 0.0, 0.0, 0.0),
        IndexConstituent("POWERGRID", "Power Grid Corp", "Power", 1.9, 0.0, 0.0, 0.0),
        IndexConstituent("TITAN", "Titan Company Ltd", "Consumer Goods", 1.8, 0.0, 0.0, 0.0),
        IndexConstituent("TATASTEEL", "Tata Steel Ltd", "Metals", 1.7, 0.0, 0.0, 0.0),
        IndexConstituent("ULTRACEMCO", "UltraTech Cement", "Cement", 1.5, 0.0, 0.0, 0.0),
        IndexConstituent("ASIANPAINT", "Asian Paints Ltd", "Paints", 1.3, 0.0, 0.0, 0.0),
        IndexConstituent("TECHM", "Tech Mahindra Ltd", "IT Services", 1.2, 0.0, 0.0, 0.0),
        IndexConstituent("INDUSINDBK", "IndusInd Bank Ltd", "Banking", 1.1, 0.0, 0.0, 0.0),
        IndexConstituent("JSWSTEEL", "JSW Steel Ltd", "Metals", 1.1, 0.0, 0.0, 0.0),
        IndexConstituent("NESTLEIND", "Nestle India Ltd", "FMCG", 1.0, 0.0, 0.0, 0.0),
        IndexConstituent("HCLTECH", "HCL Technologies", "IT Services", 0.9, 0.0, 0.0, 0.0),
        IndexConstituent("BAJAJFINSV", "Bajaj Finserv Ltd", "Finance", 0.9, 0.0, 0.0, 0.0),
        IndexConstituent("WIPRO", "Wipro Ltd", "IT Services", 0.8, 0.0, 0.0, 0.0)
    )

    val finNiftyConstituents = listOf(
        IndexConstituent("HDFCBANK", "HDFC Bank Ltd", "Banking", 26.2, 0.0, 0.0, 0.0),
        IndexConstituent("ICICIBANK", "ICICI Bank Ltd", "Banking", 21.4, 0.0, 0.0, 0.0),
        IndexConstituent("BAJFINANCE", "Bajaj Finance Ltd", "NBFC", 10.5, 0.0, 0.0, 0.0),
        IndexConstituent("AXISBANK", "Axis Bank Ltd", "Banking", 9.1, 0.0, 0.0, 0.0),
        IndexConstituent("SBIN", "State Bank of India", "Banking", 8.8, 0.0, 0.0, 0.0),
        IndexConstituent("KOTAKBANK", "Kotak Mahindra Bank", "Banking", 8.4, 0.0, 0.0, 0.0),
        IndexConstituent("BAJAJFINSV", "Bajaj Finserv Ltd", "HoldCo", 4.8, 0.0, 0.0, 0.0),
        IndexConstituent("SBILIFE", "SBI Life Insurance", "Insurance", 3.2, 0.0, 0.0, 0.0),
        IndexConstituent("HDFCLIFE", "HDFC Life Insurance", "Insurance", 2.9, 0.0, 0.0, 0.0),
        IndexConstituent("SHRIRAMFIN", "Shriram Finance Ltd", "NBFC", 2.5, 0.0, 0.0, 0.0),
        IndexConstituent("CHOLAFIN", "Cholamandalam Investment", "NBFC", 1.8, 0.0, 0.0, 0.0),
        IndexConstituent("MUTHOOTFIN", "Muthoot Finance Ltd", "NBFC", 0.8, 0.0, 0.0, 0.0)
    )

    val midcapConstituents = listOf(
        IndexConstituent("BEL", "Bharat Electronics Ltd", "Capital Goods", 4.8, 0.0, 0.0, 0.0),
        IndexConstituent("TRENT", "Trent Ltd", "Retail", 4.5, 0.0, 0.0, 0.0),
        IndexConstituent("HAL", "Hindustan Aeronautics", "Aerospace", 4.2, 0.0, 0.0, 0.0),
        IndexConstituent("DLF", "DLF Ltd", "Real Estate", 3.8, 0.0, 0.0, 0.0),
        IndexConstituent("SIEMENS", "Siemens Ltd", "Capital Goods", 3.6, 0.0, 0.0, 0.0),
        IndexConstituent("CUMMINSIND", "Cummins India Ltd", "Capital Goods", 3.2, 0.0, 0.0, 0.0),
        IndexConstituent("PERSISTENT", "Persistent Systems Ltd", "IT Services", 2.9, 0.0, 0.0, 0.0),
        IndexConstituent("POLYCAB", "Polycab India Ltd", "Cables", 2.8, 0.0, 0.0, 0.0),
        IndexConstituent("PFC", "Power Finance Corp", "Financial Services", 2.6, 0.0, 0.0, 0.0),
        IndexConstituent("RECLTD", "REC Ltd", "Financial Services", 2.4, 0.0, 0.0, 0.0),
        IndexConstituent("INDHOTEL", "Indian Hotels Co Ltd", "Hospitality", 2.3, 0.0, 0.0, 0.0),
        IndexConstituent("ASHOKLEY", "Ashok Leyland Ltd", "Automobile", 2.1, 0.0, 0.0, 0.0)
    )
}
