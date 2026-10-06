# Qoody

**A free expense tracker for Android that fills itself in.**

When you pay with UPI, a card or your bank account, your phone gets a notification or an SMS saying how much
was spent and where. Qoody reads those messages, writes the payment into your ledger and puts it in a category
like *Food & Drink* or *Transport*. You don't have to type in every chai and auto ride yourself.

Everything happens on your phone. There is no account to create, nothing is uploaded to a server, and there is
no tracking or advertising.

> Qoody is made for India: amounts are in rupees (₹) and it understands messages from Indian banks and payment
> apps. It runs on Android 8.0 or newer. An iPhone version may come later.

## What it does

- **Logs payments for you.** It works with 64 Indian payment, bank, wallet and card apps (Google Pay, PhonePe,
  Paytm, bank apps and more) and with bank SMS in your messaging app.
- **Sorts them into categories.** Qoody guesses the category on your phone. If it gets one wrong, change it once
  and it will remember that shop next time. You can also make your own categories, with a name and an emoji.
- **Shows where the money went.** See this month's total, search and filter your payments, and check the
  Insights screen for spending by week, month and category.
- **Helps you stick to a budget.** Set a monthly limit for any category and see how close you are.
- **Lets you add things by hand.** Cash payments and anything Qoody missed can be added in a few taps.
- **Keeps your data safe.** Make a password-protected backup file, move it to a new phone and restore it.
  You can also export everything to a spreadsheet (CSV).

## Install

1. On your Android phone, open the [latest release](https://github.com/theprinceraj/qoody/releases/latest).
2. Under **Assets**, tap the file ending in `.apk` (for example `qoody-v0.6.0.apk`) to download it.
3. Open the downloaded file. If Android asks, allow your browser or file manager to install apps.
4. Open Qoody and follow the short setup.

Qoody is not on the Google Play Store yet.

### If your phone won't install it

Some phones in India refuse to install apps from outside the Play Store if the app asks to read notifications.
This is a Google Play Protect safety feature against scam apps, and it can block Qoody too. Right now the only
reliable way around it is to install from a computer using Android's developer tools (`adb install`). If you
are not comfortable doing that, ask a tech-savvy friend, or wait for the Play Store release.

### Turn on notification access

Qoody can only log payments automatically once you let it read notifications. The setup screen takes you to the
right page in your phone's settings; find **Qoody** in the list and switch it on. You can turn it off again
any time from the same place, or from Qoody's Settings.

Some phone brands close apps in the background to save battery. If payments stop showing up, look in your
phone's battery settings and allow Qoody to run without restrictions.

## Questions

**Does Qoody read my personal messages?**
It only looks at notifications from the payment, bank and messaging apps it knows about. In SMS apps it ignores
messages from personal phone numbers and only keeps ones that look like bank payment alerts.

**Can Qoody move money or see my bank password?**
No. It can only read notifications that already appear on your screen. It cannot pay, log in to your bank or
open your payment apps.

**A payment didn't show up. What now?**
If Qoody saw a payment message but couldn't read the amount, you'll find it under
**Settings → Failed to parse**, where you can add it with one tap. Otherwise, add it by hand with the **Add Expense** button on the ledger.

**What happens if I uninstall the app?**
Your data is deleted with it, because it only ever lived on your phone. Make a backup first from Settings if you
want to keep it.

**Does it cost anything?**
No. Qoody is free and its source code is open for anyone to read.

## Found a problem or have an idea?

[Open an issue](https://github.com/theprinceraj/qoody/issues) and describe what happened. Please **don't paste
real bank messages** with your account numbers or balances; replace the numbers with made-up ones first.

## For developers

Qoody is a Kotlin Multiplatform Android app with a TanStack Start website. Setup, project layout and how to send
changes are in [CONTRIBUTING.md](CONTRIBUTING.md).
