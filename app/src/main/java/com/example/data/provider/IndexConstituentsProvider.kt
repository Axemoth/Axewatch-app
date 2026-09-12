package com.example.data.provider

import com.example.data.model.IndexConstituent

object IndexConstituentsProvider {

    fun getConstituentsForIndex(indexSymbol: String): List<IndexConstituent> {
        val sym = indexSymbol.uppercase().trim()
        return when {
            sym.contains("BANK") -> bankNiftyConstituents
            sym.contains("IT") -> niftyItConstituents
            sym.contains("FIN") -> finNiftyConstituents
            sym.contains("SENSEX") || sym.contains("BSE") -> sensexConstituents
            sym.contains("MIDCAP") || sym.contains("NEXT") -> midcapConstituents
            else -> nifty50Constituents
        }
    }

    val nifty50Constituents = listOf(
        IndexConstituent("HDFCBANK", "HDFC Bank Ltd", "Financial Services", 13.5, 1642.50, 14.80, 0.91),
        IndexConstituent("RELIANCE", "Reliance Industries Ltd", "Oil Gas & Fuels", 10.2, 1365.20, -8.40, -0.61),
        IndexConstituent("ICICIBANK", "ICICI Bank Ltd", "Financial Services", 9.1, 1265.40, 18.20, 1.46),
        IndexConstituent("INFY", "Infosys Ltd", "Information Technology", 5.8, 1875.00, -12.30, -0.65),
        IndexConstituent("TCS", "Tata Consultancy Services", "Information Technology", 4.3, 4048.80, 22.50, 0.56),
        IndexConstituent("ITC", "ITC Ltd", "Fast Moving Consumer Goods", 4.1, 508.40, 3.10, 0.61),
        IndexConstituent("LT", "Larsen & Toubro Ltd", "Construction", 3.9, 3620.00, 45.00, 1.26),
        IndexConstituent("BHARTIARTL", "Bharti Airtel Ltd", "Telecommunication", 3.8, 1628.70, 24.30, 1.51),
        IndexConstituent("AXISBANK", "Axis Bank Ltd", "Financial Services", 3.2, 1210.60, -4.50, -0.37),
        IndexConstituent("SBIN", "State Bank of India", "Financial Services", 3.0, 815.30, 7.80, 0.97),
        IndexConstituent("KOTAKBANK", "Kotak Mahindra Bank Ltd", "Financial Services", 2.8, 1780.00, -6.20, -0.35),
        IndexConstituent("M&M", "Mahindra & Mahindra Ltd", "Automobile", 2.6, 2940.50, 52.10, 1.80),
        IndexConstituent("HINDUNILVR", "Hindustan Unilever Ltd", "Fast Moving Consumer Goods", 2.4, 2780.00, -15.40, -0.55),
        IndexConstituent("BAJFINANCE", "Bajaj Finance Ltd", "Financial Services", 2.3, 7250.00, 88.00, 1.23),
        IndexConstituent("TATAMOTORS", "Tata Motors Ltd", "Automobile", 2.2, 824.50, -9.20, -1.10),
        IndexConstituent("SUNPHARMA", "Sun Pharmaceutical Ind", "Healthcare", 2.1, 1890.00, 21.40, 1.15),
        IndexConstituent("MARUTI", "Maruti Suzuki India Ltd", "Automobile", 1.9, 12480.00, 140.00, 1.13),
        IndexConstituent("NTPC", "NTPC Ltd", "Power", 1.8, 412.30, 6.20, 1.53),
        IndexConstituent("POWERGRID", "Power Grid Corp of India", "Power", 1.7, 338.40, 2.80, 0.83),
        IndexConstituent("TITAN", "Titan Company Ltd", "Consumer Durables", 1.6, 3510.00, -18.00, -0.51),
        IndexConstituent("TATASTEEL", "Tata Steel Ltd", "Metals & Mining", 1.5, 154.20, 1.90, 1.25),
        IndexConstituent("ULTRACEMCO", "UltraTech Cement Ltd", "Construction Materials", 1.4, 11240.00, 95.00, 0.85),
        IndexConstituent("ADANIENT", "Adani Enterprises Ltd", "Metals & Mining", 1.3, 2980.00, -32.00, -1.06),
        IndexConstituent("COALINDIA", "Coal India Ltd", "Oil Gas & Fuels", 1.3, 498.50, 4.10, 0.83),
        IndexConstituent("ONGC", "Oil & Natural Gas Corp", "Oil Gas & Fuels", 1.2, 294.60, -1.80, -0.61),
        IndexConstituent("BAJAJFINSV", "Bajaj Finserv Ltd", "Financial Services", 1.2, 1845.00, 16.50, 0.90),
        IndexConstituent("HINDALCO", "Hindalco Industries Ltd", "Metals & Mining", 1.1, 688.00, 9.40, 1.39),
        IndexConstituent("ASIANPAINT", "Asian Paints Ltd", "Consumer Durables", 1.1, 3120.00, -22.00, -0.70),
        IndexConstituent("TECHM", "Tech Mahindra Ltd", "Information Technology", 1.1, 1610.00, 14.00, 0.88),
        IndexConstituent("INDUSINDBK", "IndusInd Bank Ltd", "Financial Services", 1.0, 1420.00, -12.00, -0.84),
        IndexConstituent("JSWSTEEL", "JSW Steel Ltd", "Metals & Mining", 1.0, 992.00, 8.20, 0.83),
        IndexConstituent("GRASIM", "Grasim Industries Ltd", "Construction Materials", 0.9, 2680.00, 24.00, 0.90),
        IndexConstituent("CIPLA", "Cipla Ltd", "Healthcare", 0.9, 1580.00, 11.20, 0.71),
        IndexConstituent("WIPRO", "Wipro Ltd", "Information Technology", 0.9, 545.00, -3.20, -0.58),
        IndexConstituent("NESTLEIND", "Nestle India Ltd", "Fast Moving Consumer Goods", 0.9, 2490.00, -11.00, -0.44),
        IndexConstituent("HCLTECH", "HCL Technologies Ltd", "Information Technology", 0.8, 1785.00, 12.50, 0.70),
        IndexConstituent("ADANIPORTS", "Adani Ports & SEZ Ltd", "Services", 0.8, 1420.00, 8.00, 0.57),
        IndexConstituent("BPCL", "Bharat Petroleum Corp", "Oil Gas & Fuels", 0.8, 348.00, 3.20, 0.93),
        IndexConstituent("DRREDDY", "Dr Reddy's Laboratories", "Healthcare", 0.8, 6640.00, -45.00, -0.67),
        IndexConstituent("SBILIFE", "SBI Life Insurance Co", "Financial Services", 0.7, 1720.00, 15.00, 0.88),
        IndexConstituent("BRITANNIA", "Britannia Industries Ltd", "Fast Moving Consumer Goods", 0.7, 5890.00, 38.00, 0.65),
        IndexConstituent("APOLLOHOSP", "Apollo Hospitals Enterprise", "Healthcare", 0.7, 7120.00, 92.00, 1.31),
        IndexConstituent("EICHERMOT", "Eicher Motors Ltd", "Automobile", 0.7, 4820.00, 65.00, 1.37),
        IndexConstituent("DIVISLAB", "Divi's Laboratories Ltd", "Healthcare", 0.6, 5420.00, -28.00, -0.51),
        IndexConstituent("SHRIRAMFIN", "Shriram Finance Ltd", "Financial Services", 0.6, 3280.00, 42.00, 1.30),
        IndexConstituent("BEL", "Bharat Electronics Ltd", "Capital Goods", 0.6, 298.00, 5.40, 1.85),
        IndexConstituent("BAJAJ-AUTO", "Bajaj Auto Ltd", "Automobile", 0.6, 11620.00, 180.00, 1.57),
        IndexConstituent("TATACONSUM", "Tata Consumer Products", "Fast Moving Consumer Goods", 0.6, 1180.00, -4.50, -0.38),
        IndexConstituent("HEROMOTOCO", "Hero MotoCorp Ltd", "Automobile", 0.5, 5460.00, 58.00, 1.07),
        IndexConstituent("TRENT", "Trent Ltd", "Consumer Services", 0.5, 7420.00, 160.00, 2.20)
    )

    val bankNiftyConstituents = listOf(
        IndexConstituent("HDFCBANK", "HDFC Bank Ltd", "Banking", 28.4, 1642.50, 14.80, 0.91),
        IndexConstituent("ICICIBANK", "ICICI Bank Ltd", "Banking", 23.8, 1265.40, 18.20, 1.46),
        IndexConstituent("AXISBANK", "Axis Bank Ltd", "Banking", 9.6, 1210.60, -4.50, -0.37),
        IndexConstituent("SBIN", "State Bank of India", "Banking", 9.2, 815.30, 7.80, 0.97),
        IndexConstituent("KOTAKBANK", "Kotak Mahindra Bank Ltd", "Banking", 8.9, 1780.00, -6.20, -0.35),
        IndexConstituent("INDUSINDBK", "IndusInd Bank Ltd", "Banking", 5.1, 1420.00, -12.00, -0.84),
        IndexConstituent("BANKBARODA", "Bank of Baroda", "Banking", 2.8, 252.40, 3.60, 1.45),
        IndexConstituent("PNB", "Punjab National Bank", "Banking", 2.4, 108.50, 1.20, 1.12),
        IndexConstituent("FEDERALBNK", "Federal Bank Ltd", "Banking", 2.2, 198.60, 2.40, 1.22),
        IndexConstituent("IDFCFIRSTB", "IDFC First Bank Ltd", "Banking", 1.8, 73.40, -0.40, -0.54),
        IndexConstituent("AUBANK", "AU Small Finance Bank", "Banking", 1.5, 645.00, 7.50, 1.18),
        IndexConstituent("BANDHANBNK", "Bandhan Bank Ltd", "Banking", 1.1, 188.20, -1.10, -0.58)
    )

    val niftyItConstituents = listOf(
        IndexConstituent("TCS", "Tata Consultancy Services", "IT Software", 27.5, 4048.80, 22.50, 0.56),
        IndexConstituent("INFY", "Infosys Ltd", "IT Software", 26.8, 1875.00, -12.30, -0.65),
        IndexConstituent("HCLTECH", "HCL Technologies Ltd", "IT Software", 11.4, 1785.00, 12.50, 0.70),
        IndexConstituent("TECHM", "Tech Mahindra Ltd", "IT Software", 9.8, 1610.00, 14.00, 0.88),
        IndexConstituent("WIPRO", "Wipro Ltd", "IT Software", 8.2, 545.00, -3.20, -0.58),
        IndexConstituent("LTIM", "LTIMindtree Ltd", "IT Software", 6.5, 5980.00, 48.00, 0.81),
        IndexConstituent("PERSISTENT", "Persistent Systems Ltd", "IT Software", 3.8, 5240.00, 68.00, 1.31),
        IndexConstituent("COFORGE", "Coforge Ltd", "IT Software", 2.9, 7420.00, 110.00, 1.50),
        IndexConstituent("MPHASIS", "Mphasis Ltd", "IT Software", 1.8, 2980.00, -15.00, -0.50),
        IndexConstituent("KPITTECH", "KPIT Technologies Ltd", "IT Software", 1.3, 1680.00, 26.00, 1.57)
    )

    val sensexConstituents = listOf(
        IndexConstituent("HDFCBANK", "HDFC Bank Ltd", "Banking", 14.8, 1642.50, 14.80, 0.91),
        IndexConstituent("RELIANCE", "Reliance Industries Ltd", "Energy", 11.2, 1365.20, -8.40, -0.61),
        IndexConstituent("ICICIBANK", "ICICI Bank Ltd", "Banking", 9.9, 1265.40, 18.20, 1.46),
        IndexConstituent("INFY", "Infosys Ltd", "IT Services", 6.4, 1875.00, -12.30, -0.65),
        IndexConstituent("TCS", "Tata Consultancy Services", "IT Services", 4.8, 4048.80, 22.50, 0.56),
        IndexConstituent("ITC", "ITC Ltd", "FMCG", 4.5, 508.40, 3.10, 0.61),
        IndexConstituent("LT", "Larsen & Toubro Ltd", "Engineering", 4.2, 3620.00, 45.00, 1.26),
        IndexConstituent("BHARTIARTL", "Bharti Airtel Ltd", "Telecom", 4.1, 1628.70, 24.30, 1.51),
        IndexConstituent("AXISBANK", "Axis Bank Ltd", "Banking", 3.5, 1210.60, -4.50, -0.37),
        IndexConstituent("SBIN", "State Bank of India", "Banking", 3.3, 815.30, 7.80, 0.97),
        IndexConstituent("KOTAKBANK", "Kotak Mahindra Bank", "Banking", 3.1, 1780.00, -6.20, -0.35),
        IndexConstituent("M&M", "Mahindra & Mahindra", "Automobile", 2.9, 2940.50, 52.10, 1.80),
        IndexConstituent("HINDUNILVR", "Hindustan Unilever", "FMCG", 2.6, 2780.00, -15.40, -0.55),
        IndexConstituent("BAJFINANCE", "Bajaj Finance Ltd", "Finance", 2.5, 7250.00, 88.00, 1.23),
        IndexConstituent("TATAMOTORS", "Tata Motors Ltd", "Automobile", 2.4, 824.50, -9.20, -1.10),
        IndexConstituent("SUNPHARMA", "Sun Pharma Ltd", "Pharma", 2.3, 1890.00, 21.40, 1.15),
        IndexConstituent("MARUTI", "Maruti Suzuki Ltd", "Automobile", 2.1, 12480.00, 140.00, 1.13),
        IndexConstituent("NTPC", "NTPC Ltd", "Power", 2.0, 412.30, 6.20, 1.53),
        IndexConstituent("POWERGRID", "Power Grid Corp", "Power", 1.9, 338.40, 2.80, 0.83),
        IndexConstituent("TITAN", "Titan Company Ltd", "Consumer Goods", 1.8, 3510.00, -18.00, -0.51),
        IndexConstituent("TATASTEEL", "Tata Steel Ltd", "Metals", 1.7, 154.20, 1.90, 1.25),
        IndexConstituent("ULTRACEMCO", "UltraTech Cement", "Cement", 1.5, 11240.00, 95.00, 0.85),
        IndexConstituent("ASIANPAINT", "Asian Paints Ltd", "Paints", 1.3, 3120.00, -22.00, -0.70),
        IndexConstituent("TECHM", "Tech Mahindra Ltd", "IT Services", 1.2, 1610.00, 14.00, 0.88),
        IndexConstituent("INDUSINDBK", "IndusInd Bank Ltd", "Banking", 1.1, 1420.00, -12.00, -0.84),
        IndexConstituent("JSWSTEEL", "JSW Steel Ltd", "Metals", 1.1, 992.00, 8.20, 0.83),
        IndexConstituent("NESTLEIND", "Nestle India Ltd", "FMCG", 1.0, 2490.00, -11.00, -0.44),
        IndexConstituent("HCLTECH", "HCL Technologies", "IT Services", 0.9, 1785.00, 12.50, 0.70),
        IndexConstituent("BAJAJFINSV", "Bajaj Finserv Ltd", "Finance", 0.9, 1845.00, 16.50, 0.90),
        IndexConstituent("WIPRO", "Wipro Ltd", "IT Services", 0.8, 545.00, -3.20, -0.58)
    )

    val finNiftyConstituents = listOf(
        IndexConstituent("HDFCBANK", "HDFC Bank Ltd", "Banking", 26.2, 1642.50, 14.80, 0.91),
        IndexConstituent("ICICIBANK", "ICICI Bank Ltd", "Banking", 21.4, 1265.40, 18.20, 1.46),
        IndexConstituent("BAJFINANCE", "Bajaj Finance Ltd", "NBFC", 10.5, 7250.00, 88.00, 1.23),
        IndexConstituent("AXISBANK", "Axis Bank Ltd", "Banking", 9.1, 1210.60, -4.50, -0.37),
        IndexConstituent("SBIN", "State Bank of India", "Banking", 8.8, 815.30, 7.80, 0.97),
        IndexConstituent("KOTAKBANK", "Kotak Mahindra Bank", "Banking", 8.4, 1780.00, -6.20, -0.35),
        IndexConstituent("BAJAJFINSV", "Bajaj Finserv Ltd", "HoldCo", 4.8, 1845.00, 16.50, 0.90),
        IndexConstituent("SBILIFE", "SBI Life Insurance", "Insurance", 3.2, 1720.00, 15.00, 0.88),
        IndexConstituent("HDFCLIFE", "HDFC Life Insurance", "Insurance", 2.9, 712.00, -4.20, -0.59),
        IndexConstituent("SHRIRAMFIN", "Shriram Finance Ltd", "NBFC", 2.5, 3280.00, 42.00, 1.30),
        IndexConstituent("CHOLAFIN", "Cholamandalam Investment", "NBFC", 1.8, 1485.00, 24.00, 1.64),
        IndexConstituent("MUTHOOTFIN", "Muthoot Finance Ltd", "NBFC", 0.8, 1920.00, 18.00, 0.95)
    )

    val midcapConstituents = listOf(
        IndexConstituent("BEL", "Bharat Electronics Ltd", "Capital Goods", 4.8, 298.00, 5.40, 1.85),
        IndexConstituent("TRENT", "Trent Ltd", "Retail", 4.5, 7420.00, 160.00, 2.20),
        IndexConstituent("HAL", "Hindustan Aeronautics", "Aerospace", 4.2, 4650.00, 82.00, 1.80),
        IndexConstituent("DLF", "DLF Ltd", "Real Estate", 3.8, 885.00, -6.40, -0.72),
        IndexConstituent("SIEMENS", "Siemens Ltd", "Capital Goods", 3.6, 7350.00, 95.00, 1.31),
        IndexConstituent("CUMMINSIND", "Cummins India Ltd", "Capital Goods", 3.2, 3840.00, 44.00, 1.16),
        IndexConstituent("PERSISTENT", "Persistent Systems Ltd", "IT Services", 2.9, 5240.00, 68.00, 1.31),
        IndexConstituent("POLYCAB", "Polycab India Ltd", "Cables", 2.8, 6720.00, 78.00, 1.17),
        IndexConstituent("PFC", "Power Finance Corp", "Financial Services", 2.6, 528.00, 7.50, 1.44),
        IndexConstituent("RECLTD", "REC Ltd", "Financial Services", 2.4, 582.00, 8.20, 1.43),
        IndexConstituent("INDHOTEL", "Indian Hotels Co Ltd", "Hospitality", 2.3, 715.00, 9.40, 1.33),
        IndexConstituent("ASHOKLEY", "Ashok Leyland Ltd", "Automobile", 2.1, 238.50, 3.20, 1.36)
    )
}
