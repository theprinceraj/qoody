package com.qoody.shared.capture

/** An app whose notifications Qoody reads. [name] is its Google Play listing name at verification time. */
data class SupportedApp(
    val packageName: String,
    val name: String,
)

/**
 * The allowlist of Indian payment, bank, wallet and card apps whose notifications are read.
 * Notifications from any other package are dropped before their text is looked at.
 *
 * Every package name below was checked on 2026-10-05 by loading
 * `https://play.google.com/store/apps/details?id=<package>&gl=IN` (HTTP 200, listing title and
 * developer matching the bank or company). Never add a package from memory: verify it the same way
 * and keep the listing name. This is data, not logic; extend it freely.
 *
 * Deliberately excluded: messaging apps (WhatsApp: chat text would be parsed), telecom/shopping apps
 * without payments, investment apps (an SIP debit is not spending), and the global Amazon app
 * (`com.amazon.mShop.android.shopping`; the India app with Amazon Pay is listed instead).
 */
object CapturePolicy {
    val supportedApps: List<SupportedApp> =
        listOf(
            // UPI apps and wallets
            SupportedApp("com.google.android.apps.nbu.paisa.user", "Google Pay"),
            SupportedApp("com.phonepe.app", "PhonePe"),
            SupportedApp("net.one97.paytm", "Paytm"),
            SupportedApp("in.org.npci.upiapp", "BHIM"),
            SupportedApp("com.dreamplug.androidapp", "CRED"),
            SupportedApp("in.amazon.mShop.android.shopping", "Amazon India (Amazon Pay)"),
            SupportedApp("com.mobikwik_new", "MobiKwik"),
            SupportedApp("com.freecharge.android", "Freecharge"),
            SupportedApp("money.super.payments", "super.money"),
            SupportedApp("com.naviapp", "Navi"),
            SupportedApp("com.fampay.in", "FamApp"),
            SupportedApp("com.popclub.android", "POP"),
            SupportedApp("in.gokiwi.kiwitpap", "Kiwi"),
            SupportedApp("com.tatadigital.tcp", "Tata Neu"),
            SupportedApp("in.jfs.jiofinance", "JioFinance"),
            SupportedApp("com.myairtelapp", "Airtel Thanks (Airtel Payments Bank)"),
            SupportedApp("org.altruist.BajajExperia", "Bajaj Finance"),
            SupportedApp("money.jupiter", "Jupiter"),
            SupportedApp("indwin.c3.shareapp", "slice"),
            SupportedApp("com.hdfcbank.payzapp", "PayZapp"),
            SupportedApp("com.sbi.upi", "BHIM SBI Pay"),
            SupportedApp("com.mgs.induspsp", "BHIM IndusPay"),
            SupportedApp("com.yesbank.yespaynext", "YES PAY Next"),
            SupportedApp("com.euronet.iobupi", "BHIM IOB UPI"),
            SupportedApp("com.icicibank.pockets", "Pockets by ICICI Bank"),
            // Banks
            SupportedApp("com.hdfcbank.android.now", "HDFC Bank"),
            SupportedApp("com.csam.icici.bank.imobile", "ICICI iMobile"),
            SupportedApp("com.sbi.lotusintouch", "YONO SBI"),
            SupportedApp("com.sbi.SBIFreedomPlus", "YONO Lite SBI"),
            SupportedApp("com.axis.mobile", "Axis Mobile"),
            SupportedApp("com.kotak.bank.mobile", "Kotak Bank"),
            SupportedApp("com.msf.kbank.mobile", "Kotak Bank (Old)"),
            SupportedApp("com.kotak811mobilebankingapp.instantsavingsupiscanandpayrecharge", "Kotak 811"),
            SupportedApp("com.Version1", "PNB ONE"),
            SupportedApp("com.bankofbaroda.mconnect", "bob World"),
            SupportedApp("com.canarabank.mobility", "Canara ai1"),
            SupportedApp("com.infrasoft.uboi", "Union ease"),
            SupportedApp("com.boi.ua.android", "BOI Mobile"),
            SupportedApp("com.iexceed.ib.digitalbankingprod", "IndSMART (Indian Bank)"),
            SupportedApp("com.lcode.ucomobilebanking", "UCO mBanking Plus"),
            SupportedApp("com.centralbank.retail.omni", "Cent eeZ (Central Bank of India)"),
            SupportedApp("com.iob.iobconnect", "IOB Connect"),
            SupportedApp("com.bom.lifestylebanking", "Zen Lyfe (Bank of Maharashtra)"),
            SupportedApp("com.psb.omniretail", "PSB UnIC (Punjab & Sind Bank)"),
            SupportedApp("com.snapwork.IDBI", "IDBI Bank GO Mobile+"),
            SupportedApp("com.idfcfirstbank.optimus", "IDFC FIRST Bank"),
            SupportedApp("com.indusind.indie", "IndusInd Bank"),
            SupportedApp("in.irisbyyes.app", "IRIS by YES BANK"),
            SupportedApp("com.rblbank.mobank", "RBL MyBank"),
            SupportedApp("com.fedmobile", "FedMobile (Federal Bank)"),
            SupportedApp("com.bandhan.mBandhan", "mBandhan"),
            SupportedApp("com.ausmallfinancebank.amb", "AU 0101"),
            SupportedApp("com.equitas.elevate", "Equitas"),
            SupportedApp("com.ujjivanbank.mobilebankingApk", "Ujjivan EZY"),
            SupportedApp("com.kvb.mobilebanking", "KVB DLite"),
            SupportedApp("com.SIBMobile", "SIB Mirror+"),
            SupportedApp("com.lcode.jkbmpay", "JKB mPay Delight+"),
            SupportedApp("com.dbs.in.digitalbank", "digibank by DBS"),
            SupportedApp("in.hsbc.hsbcindia", "HSBC India"),
            SupportedApp("air.app.scb.breeze.android.main.in.prod", "SC Mobile India"),
            SupportedApp("com.citi.citimobile", "Citi Mobile"),
            // Credit cards
            SupportedApp("com.ge.capital.konysbiapp", "SBI Card"),
            SupportedApp("com.creditcard.onecard", "OneCard"),
            SupportedApp("com.rbl.rblmycard", "RBL MyCard"),
        )

    private val packages: Set<String> = supportedApps.mapTo(mutableSetOf()) { it.packageName }

    fun isSupported(packageName: String): Boolean = packageName in packages
}
