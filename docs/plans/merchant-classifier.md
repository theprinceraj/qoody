# Plan: on-device merchant classifier

Status: step 1 done (2026-10-06), see `tools/merchant-classifier/REPORT.md`. Decision: D22.

## Goal

Categorise captured payments whose merchant is not covered by a remembered correction or a keyword rule, entirely on the device, with no network call and no new native dependency.

## Chosen approach

A fastText-style supervised classifier: character n-grams of the normalised merchant string are hashed into buckets, looked up in an embedding table, averaged, and passed through one linear layer and a softmax. Training happens offline in Python; the exported weights ship as an asset; inference is plain Kotlin in `shared/commonMain` (no JNI, no LiteRT), so it also works on iOS later.

Rejected for now (see the 2026-10-06 conversation summary in D22): char-n-gram + plain linear model (slightly less accurate, same effort), LiteRT neural net (native Android-only dependency), pretrained text embedder (10–100 MB, slow on budget phones), cloud APIs such as Jev (breaks the on-device promise).

## Where it plugs in

`MerchantCategoriser` order becomes: remembered category → keyword rule → **model (if confidence ≥ threshold)** → Uncategorized. A model hit is stored as `Categorization.Model(modelName, confidence)`, which already exists in the domain; the receipt already offers "Correct category" for model guesses, and a correction is remembered like any other.

## Classes

Predict only the merchant-driven categories: Food & Drink, Transport, Shopping, Rent & Bills, Subscriptions. **Friends** (person-to-person payees) and **Uncategorized** are not predicted; low confidence yields Uncategorized. Revisit a "looks like a person's name" signal for Friends later.

## Steps (one PR each, verify green before the next)

1. **Evaluation harness (no app change).** `tools/merchant-classifier/` (Python): labelled merchant set, train/validate split, trains the model, reports accuracy and a confusion matrix, writes the weights file. Includes noisy UPI forms (`shop0011-1@fbl`, truncated bank names). Decide the confidence threshold from the precision/coverage curve. **Done:** classes are the five merchant categories plus `Other` (people, generic businesses → Uncategorized); threshold 0.77 (0.95 precision on validation); the shipped model reaches 0.945 precision / 0.846 coverage on the hand-written test set.
2. **Kotlin inference.** `shared/.../capture/classifier/` with `MerchantClassifier` (interface), `FastTextMerchantClassifier` (loads the weights, hashes n-grams, softmax). Parity test: Kotlin output equals the Python reference for a fixture of strings. Latency test (target well under 5 ms per entry).
3. **Wire it in.** Weights as an Android asset loaded through a platform interface; `MerchantCategoriser` takes the classifier; Koin binding; receipt copy for model guesses ("Guessed on this device"). Backup already carries `Categorization.Model`.
4. **Learn from corrections (optional).** The existing remembered-category map already wins; later, export consented corrections to retrain.

## Open problems

- **Training data.** No real notifications or SMS may be committed. Sources: a hand-built list of well-known Indian merchants per category, synthetic noisy variants, and (with the user's consent) corrected entries exported from their own device.
- Hash-bucket count and n-gram range are decided by the harness, not guessed here.
- Python is a dev-only tool; it must not become a build dependency of the app (weights are committed as a generated asset with the script that produced them).
